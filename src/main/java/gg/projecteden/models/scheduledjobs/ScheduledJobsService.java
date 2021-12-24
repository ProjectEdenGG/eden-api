package gg.projecteden.models.scheduledjobs;

import gg.projecteden.mongodb.MongoPlayerService;
import gg.projecteden.mongodb.annotations.ObjectClass;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@ObjectClass(ScheduledJobs.class)
public class ScheduledJobsService extends MongoPlayerService<ScheduledJobs> {
	private final static Map<UUID, ScheduledJobs> cache = new ConcurrentHashMap<>();

	public Map<UUID, ScheduledJobs> getCache() {
		return cache;
	}

	@Override
	protected void beforeSave(ScheduledJobs scheduledJobs) {
		scheduledJobs.janitor();
	}

}
