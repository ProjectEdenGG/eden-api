package gg.projecteden.models.scheduledjobs.common;

import com.cronutils.model.CronType;
import com.cronutils.model.definition.CronDefinitionBuilder;
import com.cronutils.model.time.ExecutionTime;
import com.cronutils.parser.CronParser;
import gg.projecteden.EdenAPI;
import gg.projecteden.annotations.Async;
import gg.projecteden.exceptions.EdenException;
import gg.projecteden.models.scheduledjobs.ScheduledJobs;
import gg.projecteden.models.scheduledjobs.ScheduledJobsService;
import gg.projecteden.utils.Log;
import lombok.Data;
import lombok.Getter;
import lombok.NonNull;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.reflections.Reflections;

import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.time.temporal.TemporalAmount;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static gg.projecteden.utils.StringUtils.camelCase;

@Data
public abstract class AbstractJob {
	@NonNull
	protected UUID id;
	@NonNull
	protected LocalDateTime created;
	protected LocalDateTime timestamp;
	protected LocalDateTime completed;
	@NonNull
	protected JobStatus status = JobStatus.PENDING;

	public AbstractJob() {
		this.id = UUID.randomUUID();
		this.created = LocalDateTime.now();
	}

	private ScheduledJobs jobs() {
		return new ScheduledJobsService().getApp();
	}

	@Getter
	private static Set<Class<? extends AbstractJob>> subclasses = new Reflections(EdenAPI.class.getPackage().getName())
			.getSubTypesOf(AbstractJob.class);

	public void setStatus(JobStatus status) {
		jobs().get(this.status).remove(this);
		this.status = status;
		jobs().get(this.status).add(this);
	}

	public void schedule(int seconds) {
		schedule(LocalDateTime.now().plusSeconds(seconds));
	}

	public void schedule(TemporalAmount duration) { // mostly for java.time.Duration
		schedule(LocalDateTime.now().plus(duration));
	}

	public void schedule(LocalDateTime timestamp) {
		this.timestamp = timestamp;
		new ScheduledJobsService().editApp(jobs -> jobs.add(this));
	}

	public void process() {
		if (status != JobStatus.PENDING)
			throw new EdenException(getClass().getSimpleName() + " # " + id + " is already " + camelCase(status));

		try {
			setStatus(JobStatus.RUNNING);

			final Runnable runnable = () -> run().thenAccept(status -> {
				setStatus(status);
				completed = LocalDateTime.now();
			});

			if (getClass().getAnnotation(Async.class) != null)
				runnable.run();
			else
				EdenAPI.get().sync(runnable);

		} catch (Exception ex) {
			Log.severe("Error while running " + getClass().getSimpleName() + " # " + id);
			ex.printStackTrace();
			setStatus(JobStatus.ERRORED);
			completed = LocalDateTime.now();
		}
	}

	@NotNull
	protected CompletableFuture<JobStatus> completed() {
		return CompletableFuture.completedFuture(JobStatus.COMPLETED);
	}

	@NotNull
	protected CompletableFuture<JobStatus> completable() {
		return new CompletableFuture<>();
	}

	protected abstract CompletableFuture<JobStatus> run();

	public boolean canRetry() {
		return getClass().getAnnotation(RetryIfInterrupted.class) != null;
	}

	public enum JobStatus {
		PENDING,
		RUNNING,
		COMPLETED,
		ERRORED,
		INTERRUPTED,
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		AbstractJob that = (AbstractJob) o;
		return id.equals(that.id);
	}

	@Override
	public int hashCode() {
		return Objects.hash(id);
	}

	private static final CronParser parser = new CronParser(CronDefinitionBuilder.instanceDefinitionFor(CronType.UNIX));

	@Nullable
	public static LocalDateTime getNextExecutionTime(Class<? extends AbstractJob> clazz) {
		final Schedule schedule = clazz.getAnnotation(Schedule.class);
		if (schedule == null)
			return null;

		final ExecutionTime executionTime = ExecutionTime.forCron(parser.parse(schedule.value()));
		final Optional<ZonedDateTime> next = executionTime.nextExecution(ZonedDateTime.now());
		if (next.isEmpty())
			return null;

		return next.get().toLocalDateTime();
	}

}
