package semaphore;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

public final class SemaphoreImpl implements Semaphore {
	private int value;
	private Queue<Double> keys;

	public SemaphoreImpl(int initial) {
		if (initial < 0) throw new IllegalArgumentException();
		value = initial;
		keys = new ConcurrentLinkedQueue<Double>();
		System.out.println("NEW SEMAPHORE");
		System.out.println("=====================================");
	}

	@Override
	public synchronized int available() {
		return value;
	}

	@Override
	public void acquire() {
		Double s = Math.random();
		System.out.println("<> Thread acquire: "  + Thread.currentThread().hashCode() + ", key: " + s.hashCode());
		while(!tryTodecrement()){
			synchronized (s) {
				try {
					System.out.println("zzz unsucessful... sleeping: " + Thread.currentThread().hashCode());
						System.out.println("------------>adding to queue: " + s.hashCode());
						keys.add(s);
						s.wait();
				} catch (InterruptedException e) {}
			}
		}
		
		if(keys.remove(s))
			System.out.println("(-) removing from queue: " + s.hashCode());
	}

	private synchronized boolean tryTodecrement(){
		System.out.println("~ try to decrement from: " + value);
		if(value>0) {
			value--;
			return true;
		}
		return false;
	}
	
	@Override
	public synchronized void release() {
		value++;
		System.out.println("</> Thread " + Thread.currentThread().hashCode() +" release(). New value: " + value);
		System.out.println("(i) current deque list:");
		keys.forEach( t -> System.out.println(t.hashCode()));
		Double s = keys.poll();
		if(s == null) return; //if the last thread in queue exiting method and there is no more waiting objects
		
		System.out.println("<------------dequeing: " +s.hashCode());
		synchronized (s) {
			// wait() + notify() can only called to object that is owned by thread 
			// http://stackoverflow.com/questions/7126550/java-wait-and-notify-illegalmonitorstateexception
			// with synchronisation it becomes owner
			s.notify(); 
		}
	}
}
