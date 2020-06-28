package org.leolo.util;


import java.util.Collections;
import java.util.LinkedList;
import java.util.concurrent.Executor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.Marker;
import org.slf4j.MarkerFactory;

public class ThreadPool implements Executor{
	
	public final int TIME_FACTOR;
	public final int MAX_POOL_SIZE;
	public final String POOL_NAME;
	private final Object ENTRY_LOCK = new Object();
	private static Logger log = LoggerFactory.getLogger(ThreadPool.class);
	private static Marker marker = MarkerFactory.getMarker("ThreadPool");
	private static int poolID = 0;
	
	
	private LinkedList<ExectorEntry> entries = new LinkedList<>();
	private int sortID = 0;
	private int threadID = 0;
	private PooledThreadGroup threadGroup;
	
	
	public ThreadPool() {
		this(10_000, 20);
	}
	
	public ThreadPool(int timeFactor) {
		this(timeFactor, 20);
	}
	
	public ThreadPool(String poolName) {
		this(poolName, 10_000, 20);
	}
	
	public ThreadPool(String poolName, int timeFactor) {
		this(poolName, timeFactor, 20);
	}
	
	public ThreadPool(int timeFactor, int maxThread) {
		this("Threadpool"+(++poolID), timeFactor, maxThread);
	}
	
	public ThreadPool(String poolName, int timeFactor, int maxThread) {
		TIME_FACTOR = timeFactor;
		MAX_POOL_SIZE = maxThread;
		POOL_NAME = poolName;
		log.info(marker,"A Thread Pool {} with time factor {} and max thread {} is created",poolName, timeFactor, maxThread);
		threadGroup = new PooledThreadGroup();
		for(int i=0;i<MAX_POOL_SIZE;i++) {
			new PoolThread().start();
		}
	}
	
	private void sort() {
		final int START_SORT_ID = sortID;
		synchronized(ENTRY_LOCK) {
			if(START_SORT_ID != sortID) {
				//Already being sorted when queueing
				return;
			}
			Collections.sort(entries);
			sortID++;//Avoid being sorted again
		}
	}
	
	@Override
	public void execute(Runnable command) {
		execute(command, 0);
	}
	
	public void execute(Runnable command, int priority) {
		ExectorEntry entry = new StandardExectorEntry(command, priority);
		synchronized(ENTRY_LOCK) {
			entries.offer(entry);
			ENTRY_LOCK.notify();
		}
	}
	
	private ExectorEntry next() {
		//TODO: Only sort after certain time is passed
		sort();
		ExectorEntry entry = null;
		synchronized(ENTRY_LOCK) {
			if(entries.size()==0) {
				//No entry available, wait for next one
				try {
					ENTRY_LOCK.wait();
				} catch (InterruptedException e) {
					log.error(e.getMessage(), e);
					return null;
				}
			}
			entry = entries.poll();
		}
		return entry;
	}
	
	private class StandardExectorEntry extends ExectorEntry{
		final long QUEUED_TIME;
		final int INITIAL_PRIORITY;
		
		StandardExectorEntry(Runnable command, int priority){
			QUEUED_TIME = System.currentTimeMillis();
			INITIAL_PRIORITY = priority;
			this.command = command;
		}
		
		public int getPriority() {
			if(TIME_FACTOR==0) {
				return INITIAL_PRIORITY;
			}
			return (int)(INITIAL_PRIORITY+(System.currentTimeMillis()-QUEUED_TIME)/TIME_FACTOR);
		}
	}
	
	private class HighestPriorityExectorEntry extends ExectorEntry{

		@Override
		public int getPriority() {
			return Integer.MAX_VALUE;
		}
		
	}
	
	private abstract class ExectorEntry implements Comparable<ExectorEntry>{
		
		public abstract int getPriority();
		Runnable command;
		
		@Override
		public int compareTo(ExectorEntry o) {
			return Integer.compare(o.getPriority(), this.getPriority());
		}
		
	}
	
	class PoolThread extends Thread{
		
		public PoolThread() {
			super(threadGroup, null, POOL_NAME+"-Thread"+(++threadID));
			this.setUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {
				@Override
				public void uncaughtException(Thread t, Throwable e) {
					log.error(e.getMessage(), e);
				}
			});
		}
		
		public void run() {
			while(true) {
				ExectorEntry ee = next();
				if(ee==null) {
					continue;
				}
				try {
					ee.command.run();
				}catch(Exception e) {
					log.error(e.getMessage(), e);
				}
			}
		}
		
	}
	
	class PooledThreadGroup extends ThreadGroup{

		public PooledThreadGroup() {
			super(POOL_NAME);
		}
		
		
	}

}
