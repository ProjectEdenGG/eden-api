package gg.projecteden.api.common.utils;

import gg.projecteden.api.common.utils.TimeUtils.MillisTime;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class Tasks {
	private static final AtomicInteger nextTaskId = new AtomicInteger(1);
	private static final Map<Integer, ScheduledFuture<?>> tasks = new HashMap<>();

	public static int wait(MillisTime delay, Runnable runnable) {
		return wait(delay.get(), runnable);
	}

	public static int wait(long delay, Runnable runnable) {
		return run(scheduler().schedule(runnable, delay, TimeUnit.MILLISECONDS));
	}

	public static int repeat(MillisTime startDelay, long interval, Runnable runnable) {
		return repeat(startDelay.get(), interval, runnable);
	}

	public static int repeat(long startDelay, MillisTime interval, Runnable runnable) {
		return repeat(startDelay, interval.get(), runnable);
	}

	public static int repeat(MillisTime startDelay, MillisTime interval, Runnable runnable) {
		return repeat(startDelay.get(), interval.get(), runnable);
	}

	public static int repeat(long startDelay, long interval, Runnable runnable) {
		return run(scheduler().scheduleAtFixedRate(runnable, startDelay, interval, TimeUnit.MILLISECONDS));
	}

	@NotNull
	private static ScheduledExecutorService scheduler() {
		return Executors.newScheduledThreadPool(1);
	}

	private static <F extends ScheduledFuture<?>> int run(F future) {
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
