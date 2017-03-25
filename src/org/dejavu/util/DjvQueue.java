/*
 * jQueue.java
 *
 * Created on October 26, 2003, 11:51 AM
 */
package org.dejavu.util;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 * Queue implementation
 *
 * @author Hai Vu
 */
public class DjvQueue {

	private final ArrayBlockingQueue<Object> messageQueue;
	private final int queueSize;

	/**
	 * Creates a new instance of DjvQueue
	 *
	 * @param maxQueueSize Queue size.
	 */
	public DjvQueue(int maxQueueSize) {
		queueSize = maxQueueSize;
		messageQueue = new ArrayBlockingQueue<>(queueSize);
	}

	/**
	 * Sends a message to the queue, no blocking.
	 *
	 * @param msg The message to be sent to the queue.
	 * @return True if the message is sent, false otherwise.
	 */
	public boolean sendMsg(Object msg) {
		return messageQueue.offer(msg);
	}

	/**
	 * Sends a message to the queue, waiting for some timeout.
	 *
	 * @param msg The message to be sent to the queue.
	 * @param timeoutMs The timeout waiting for the message to be sent. Zero or
	 * less means no wait.
	 * @return True if the message is sent, false otherwise.
	 * @throws InterruptedException
	 */
	public boolean sendMsg(Object msg, int timeoutMs) throws InterruptedException {
		if (timeoutMs > 0) {
			return messageQueue.offer(msg, timeoutMs, TimeUnit.MILLISECONDS);
		}
		return messageQueue.offer(msg);
	}

	/**
	 * Retrieves the number of messages pending in the queue
	 *
	 * @return The number of messages pending.
	 */
	public int getNumPendingMsgs() {
		return messageQueue.size();
	}

	/**
	 * Retrieves the next message from the head of the queue.
	 *
	 * @param timeoutMs Timeout used in waiting for the next message, in
	 * milliseconds. Zero or less means no wait.
	 * @return The message received, or null if none was available.
	 * @throws InterruptedException
	 */
	public Object receiveMsg(int timeoutMs) throws InterruptedException {
		if (timeoutMs > 0) {
			// Wait for timeoutMs
			return messageQueue.poll(timeoutMs, TimeUnit.MILLISECONDS);
		}

		// No wait
		return messageQueue.poll();
	}

	/**
	 * Receives a message from the queue, waiting forever.
	 *
	 * @return The message received, not null.
	 * @throws InterruptedException
	 */
	public Object receiveMsg() throws InterruptedException {
		return messageQueue.take();
	}

	/**
	 * Flushes the queue.
	 *
	 * @return This object.
	 */
	public DjvQueue clearAllMsg() {
		messageQueue.clear();
		return this;
	}

	/**
	 * Determines whether the queue is empty.
	 *
	 * @return True = empty, false = not empty.
	 */
	public boolean isEmpty() {
		return messageQueue.isEmpty();
	}
}
