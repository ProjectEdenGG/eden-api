package gg.projecteden.api.mongodb.models.scheduledjobs.common;

import com.cronutils.model.CronType;
import com.cronutils.model.definition.CronDefinitionBuilder;
import com.cronutils.model.time.ExecutionTime;
import com.cronutils.parser.CronParser;
import dev.morphia.annotations.Converters;
import gg.projecteden.api.common.EdenAPI;
import gg.projecteden.api.common.annotations.Async;
import gg.projecteden.api.common.exceptions.EdenException;
import gg.projecteden.api.common.utils.Log;
import gg.projecteden.api.mongodb.models.scheduledjobs.ScheduledJobs;
import gg.projecteden.api.mongodb.models.scheduledjobs.ScheduledJobsService;
import gg.projecteden.api.mongodb.serializers.LocalDateTimeConverter;
import gg.projecteden.api.mongodb.serializers.UUIDConverter;
import lombok.Data;
import lombok.Getter;
import lombok.NonNull;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.time.temporal.TemporalAmount;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static gg.projecteden.api.common.utils.ReflectionUtils.subTypesOf;
import static gg.projecteden.api.common.utils.StringUtils.camelCase;

@Data
@Converters({UUIDConverter.class, LocalDateTimeConverter.class})
public abstract class AbstractJob {
	@NonNull
	protected UUID id;
	@NonNull
	protected LocalDateTime created;
	protected LocalDateTime timestamp;
	protected LocalDateTime completed;
	@NonNull
	protected JobStatus status = JobStatus.PENDING;
	protected int attempts = 0;

	public AbstractJob() {
		this.id = UUID.randomUUID();
		this.created = LocalDateTime.now();
	}

	private ScheduledJobs jobs() {
		return new ScheduledJobsService().getApp();
	}

	@Getter
	private static Set<Class<? extends AbstractJob>> subclasses = subTypesOf(AbstractJob.class, "gg.projecteden");

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

	public void scheduleSync(int seconds) {
		scheduleSync(LocalDateTime.now().plusSeconds(seconds));
	}

	public void scheduleSync(TemporalAmount duration) { // mostly for java.time.Duration
		scheduleSync(LocalDateTime.now().plus(duration));
	}

	public void scheduleSync(LocalDateTime timestamp) {
		this.timestamp = timestamp;
		var service = new ScheduledJobsService();
		var jobs = service.getApp();
		jobs.add(this);
		service.saveSync(jobs);
	}

	public void process() {
		if (status != JobStatus.PENDING)
			throw new EdenException("Tried to process job " + getClass().getSimpleName() + " # " + id + ", but it is already " + camelCase(status));

		try {
			setStatus(JobStatus.RUNNING);

			final Runnable runnable = () -> run()
				.thenAccept(status -> {
					setStatus(status);
					completed = LocalDateTime.now();
				}).exceptionally(ex -> {
					setStatus(JobStatus.ERRORED);
					completed = LocalDateTime.now();
					return null;
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
		if (status == JobStatus.RUNNING) {
			final RetryIfInterrupted annotation = getClass().getAnnotation(RetryIfInterrupted.class);
			return annotation != null && attempts < annotation.value();
		}

		if (status == JobStatus.ERRORED) {
			final RetryIfErrored annotation = getClass().getAnnotation(RetryIfErrored.class);
			return annotation != null && attempts < annotation.value();
		}

		return false;
	}

	public void attempt() {
		++attempts;
		status = JobStatus.PENDING;
	}

	public enum JobStatus {
		PENDING,
		RUNNING,
		COMPLETED,
		ERRORED,
		INTERRUPTED,
		CANCELLED,
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
		return next.map(ZonedDateTime::toLocalDateTime).orElse(null);
	}

}
