package gg.projecteden.utils;

import gg.projecteden.utils.TimeUtils.MillisTime;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class Tasks {
	private static final AtomicInteger nextTaskId = new AtomicInteger(1);
	private static final Map<Integer, Future<?>> tasks = new HashMap<>();

	public static int wait(MillisTime delay, Runnable runnable) {
		return wait(delay.get(), runnable);
	}

	public static int wait(long delay, Runnable runnable) {
		return run(scheduler().schedule(runnable, delay, TimeUnit.MILLISECONDS));
	}

	@NotNull
	private static ScheduledExecutorService scheduler() {
		return Executors.newScheduledThreadPool(1);
	}

	private static <F extends Future<?>> int run(F future) {
		final int taskId = nextTaskId.getAndIncrement();
		tasks.put(taskId, future);
		return taskId;
	}

	public static boolean cancel(int taskId) {
		return cancel(taskId, false);
	}

	public static boolean cancel(int taskId, boolean interrupt) {
		final Future<?> future = tasks.get(taskId);
		if (future == null)
			return false;

		future.cancel(interrupt);
		return true;
	}


}
