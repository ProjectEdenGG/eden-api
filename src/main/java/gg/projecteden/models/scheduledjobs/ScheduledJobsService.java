package gg.projecteden.models.scheduledjobs;

import gg.projecteden.mongodb.MongoService;
import gg.projecteden.mongodb.annotations.PlayerClass;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@PlayerClass(ScheduledJobs.class)
public class ScheduledJobsService extends MongoService<ScheduledJobs> {
	private final static Map<UUID, ScheduledJobs> cache = new ConcurrentHashMap<>();

	public Map<UUID, ScheduledJobs> getCache() {
		return cache;
	}

	@Override
	protected void beforeSave(ScheduledJobs scheduledJobs) {
		scheduledJobs.janitor();
	}

}
