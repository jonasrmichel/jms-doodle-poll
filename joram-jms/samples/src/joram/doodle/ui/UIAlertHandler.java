package doodle.ui;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * A UIAlertHandler is responsible for queuing user alerts and prompting its
 * delegate to display them to the user when the command line interface is not
 * in use.
 * 
 */
public class UIAlertHandler extends Thread {

	/** We make callbacks on the delegate. */
	private UIAlertHandlerDelegate delegate;

	/** Holds the queue of alerts waiting to be displayed to the user. */
	private Queue<String> alerts;

	public UIAlertHandler(UIAlertHandlerDelegate delegate) {
		super();

		this.delegate = delegate;
		alerts = new ConcurrentLinkedQueue<String>();
	}

	/**
	 * Queues an alert and notifies the thread (this thread) waiting on the
	 * alert queue.
	 * 
	 * @param event
	 */
	public void enqueue(String alert) {
		synchronized (alerts) {
			alerts.add(alert);
			alerts.notify();
		}
	}

	@Override
	public void run() {
		while (true) {
			try {
				synchronized (alerts) {
					// wait until we are notified that there are Events to
					// consume
					alerts.wait();

					// wait for the command line to be free (not in use)
					synchronized (delegate) {
						delegate.println();
						delegate.println(); // make some room on the command
												// line

						// display all alerts in the queue
						delegate.println("New alerts...");
						while (!alerts.isEmpty())
							delegate.alert(alerts.poll());
					}
				}
			} catch (InterruptedException e) {
			}
		}
	}
}
