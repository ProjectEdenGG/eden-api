package gg.projecteden.models.scheduledjobs;

import gg.projecteden.EdenAPI;
import gg.projecteden.models.scheduledjobs.common.AbstractJob;
import gg.projecteden.models.scheduledjobs.common.AbstractJob.JobStatus;
import gg.projecteden.utils.Log;
import gg.projecteden.utils.Tasks;
import gg.projecteden.utils.TimeUtils.MillisTime;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static gg.projecteden.models.scheduledjobs.common.AbstractJob.getNextExecutionTime;

public class ScheduledJobsRunner {
	private static final ScheduledJobsService service = new ScheduledJobsService();
	private static final ScheduledJobs jobs = service.get(EdenAPI.get().getAppUuid());

	public static void start() {
		// static init
	}

	static {
		checkInterrupted();
		processor();
		rescheduler();
	}

	private static void checkInterrupted() {
		try {
			final List<AbstractJob> interrupted = new ArrayList<>(jobs.get(JobStatus.RUNNING));
			if (!interrupted.isEmpty()) {
				for (AbstractJob job : interrupted) {
					if (job.canRetry()) {
						job.setStatus(JobStatus.PENDING);
						Log.warn("[Jobs] Found interrupted job, retrying: " + job);
					} else {
						job.setStatus(JobStatus.INTERRUPTED);
						Log.severe("[Jobs] Found interrupted job: " + job);
					}
				}

				service.save(jobs);
			}
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}

	private static void processor() {
		Tasks.repeat(0, MillisTime.SECOND, () -> {
			final Set<AbstractJob> ready = jobs.getReady();
			if (ready.isEmpty())
				return;

			for (AbstractJob job : ready)
				job.process();

			service.save(jobs);
		});
	}

	private static void rescheduler() {
		final Set<Class<? extends AbstractJob>> subclasses = AbstractJob.getSubclasses();
		Tasks.repeat(0, MillisTime.MINUTE, () -> subclasses.forEach(clazz -> {
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
