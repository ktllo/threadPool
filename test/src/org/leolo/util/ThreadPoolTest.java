package org.leolo.util;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Random;

import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class ThreadPoolTest {

	@Test
	void test() {
		Logger log = LoggerFactory.getLogger("main");
		ThreadPool tp = new ThreadPool();
		log.info("{}:{}", tp.MAX_POOL_SIZE, tp.TIME_FACTOR);
	}
	
	int doneCount = 0;
	@Test
	void queueTest() {
		ThreadPool tp = new ThreadPool();
		for(int i=0;i<100;i++) {
			tp.execute(new Runnable() {
				Random r = new Random(System.nanoTime());
				@Override
				public void run() {
					Logger log = LoggerFactory.getLogger("JobThread"+Long.toHexString(System.nanoTime()%0xffffff));
					for(int i=0;i<10;i++) {
						log.info("Loop {}",i);
						try {
							Thread.sleep(r.nextInt(48)+2);
						} catch (InterruptedException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
					}
					doneCount++;
				}
				
			});
		}
		while(true) {
			if(doneCount!=100)
				continue;
		}
	}

}
