package gg.projecteden.api.mongodb.models.scheduledjobs;

import gg.projecteden.api.common.utils.Log;
import gg.projecteden.api.common.utils.Tasks;
import gg.projecteden.api.common.utils.TimeUtils.MillisTime;
import gg.projecteden.api.mongodb.models.scheduledjobs.common.AbstractJob;
import gg.projecteden.api.mongodb.models.scheduledjobs.common.AbstractJob.JobStatus;

import java.time.LocalDateTime;
import java.util.Set;
import java.util.stream.Collectors;

import static gg.projecteden.api.common.utils.StringUtils.camelCase;
import static gg.projecteden.api.mongodb.models.scheduledjobs.common.AbstractJob.getNextExecutionTime;

public class ScheduledJobsRunner {
	private static ScheduledJobsService service;
	private static ScheduledJobs jobs;
	private static int retryErroredTaskId, processorTaskId, reschedulerTaskId;

	public static void start() {
		service = new ScheduledJobsService();
		jobs = service.getApp();

		retry(JobStatus.RUNNING, JobStatus.INTERRUPTED);
		retryErrored();
		processor();
		rescheduler();
	}

	public static void stop() {
		Tasks.cancel(retryErroredTaskId);
		Tasks.cancel(processorTaskId);
		Tasks.cancel(reschedulerTaskId);
	}

	private static void retryErrored() {
		retryErroredTaskId = Tasks.repeat(0, MillisTime.MINUTE, () -> retry(JobStatus.ERRORED, JobStatus.ERRORED));
	}

	private static void retry(JobStatus current, JobStatus newStatus) {
		try {
			final Set<AbstractJob> retryable = jobs.get(current).stream()
				.filter(job -> job.getCompleted() == null)
				.collect(Collectors.toSet());

			if (retryable.isEmpty())
				return;

			for (AbstractJob job : retryable) {
				if (job.canRetry()) {
					job.attempt();
					Log.warn("[Jobs] Found " + camelCase(newStatus) + " job, retrying: " + job);
				} else {
					job.setStatus(newStatus);
					job.setCompleted(LocalDateTime.now());
					Log.severe("[Jobs] Found " + camelCase(newStatus) + " job: " + job);
				}
			}

			service.save(jobs);
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}

	private static void processor() {
		processorTaskId = Tasks.repeat(0, MillisTime.SECOND, () -> {
			try {
				final Set<AbstractJob> ready = jobs.getReady();
				if (ready.isEmpty())
					return;

				for (AbstractJob job : ready)
					job.process();

				service.save(jobs);
			} catch (Exception ex) {
				ex.printStackTrace();
			}
		});
	}

	private static void rescheduler() {
		final Set<Class<? extends AbstractJob>> subclasses = AbstractJob.getSubclasses();
		reschedulerTaskId = Tasks.repeat(0, MillisTime.MINUTE, () -> subclasses.forEach(clazz -> {
			final LocalDateTime timestamp = getNextExecutionTime(clazz);
			if (timestamp == null)
				return;

			for (AbstractJob job : jobs.get(JobStatus.PENDING, clazz))
				if (job.getTimestamp().equals(timestamp))
					return;

			try {
				clazz.getConstructor().newInstance().schedule(timestamp);
			} catch (Exception ex) {
				Log.severe("Error rescheduling " + clazz.getSimpleName());
				ex.printStackTrace();
			}
		}));
	}

}
