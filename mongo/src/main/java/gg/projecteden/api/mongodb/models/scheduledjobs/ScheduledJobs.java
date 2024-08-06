package gg.projecteden.api.mongodb.models.scheduledjobs;

import dev.morphia.annotations.Converters;
import dev.morphia.annotations.Entity;
import dev.morphia.annotations.Id;
import dev.morphia.annotations.PostLoad;
import gg.projecteden.api.mongodb.interfaces.PlayerOwnedObject;
import gg.projecteden.api.mongodb.models.scheduledjobs.common.AbstractJob;
import gg.projecteden.api.mongodb.models.scheduledjobs.common.AbstractJob.JobStatus;
import gg.projecteden.api.mongodb.serializers.JobConverter;
import gg.projecteden.api.mongodb.serializers.LocalDateTimeConverter;
import gg.projecteden.api.mongodb.serializers.UUIDConverter;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.ConcurrentModificationException;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Data
@Builder
@Entity(value = "scheduled_jobs", noClassnameStored = true)
@NoArgsConstructor
@AllArgsConstructor
@RequiredArgsConstructor
@Converters({UUIDConverter.class, JobConverter.class, LocalDateTimeConverter.class})
public class ScheduledJobs implements PlayerOwnedObject {
	@Id
	@NonNull
	private UUID uuid;
	private Map<JobStatus, Set<AbstractJob>> jobs = new ConcurrentHashMap<>();
	private Set<AbstractJob> scheduledJobs = ConcurrentHashMap.newKeySet();

	@PostLoad
	void fix() {
		scheduledJobs.addAll(jobs.values()
			.stream()
			.flatMap(Collection::stream)
			.collect(Collectors.toSet()));
	}

	public Set<AbstractJob> get(JobStatus status) {
		return scheduledJobs.stream()
			.filter(job -> job.getStatus() == status)
			.collect(Collectors.toSet());
	}

	public Set<AbstractJob> getReady() {
		final LocalDateTime now = LocalDateTime.now();
		return get(JobStatus.PENDING).stream()
				.filter(job -> job.getTimestamp().isBefore(now))
				.collect(Collectors.toSet());
	}

	public <T extends AbstractJob> Set<T> get(JobStatus status, Class<T> clazz) {
		return get(status).stream()
				.filter(job -> clazz.equals(job.getClass()))
				.map(job -> (T) job)
				.collect(Collectors.toSet());
	}

	public void add(AbstractJob job) {
		scheduledJobs.add(job);
	}

	public void janitor() {
		try {
			final LocalDateTime threshold = LocalDateTime.now().minusDays(5);
			scheduledJobs.removeIf(job -> job.getStatus() == JobStatus.COMPLETED || job.getTimestamp().isBefore(threshold));
		} catch (ConcurrentModificationException tryAgainLater) {}
	}

}
