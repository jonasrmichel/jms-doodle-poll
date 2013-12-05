package doodle;

/**
 * Defines the callbacks used by a Poll.
 * 
 * @author Jonas Michel
 */
public interface PollDelegate {

	/**
	 * Called to communicate poll activity to the delegate.
	 * 
	 * @param title
	 *            the poll's title.
	 * @param activity
	 *            a string describing activity on the poll.
	 */
	public void pollActivity(String title, String activity);
}
