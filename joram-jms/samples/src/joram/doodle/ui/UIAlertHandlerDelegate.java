package doodle.ui;

/**
 * Defines the callbacks used by the UIAlertHandler.
 * 
 * @author Jonas Michel
 */
public interface UIAlertHandlerDelegate {

	/**
	 * Called to display an Event to the user.
	 * 
	 * @param alert
	 */
	public void alert(String alert);

	/**
	 * Prints a carriage return.
	 */
	public void println();

	/**
	 * Prints the given string followed by a carriage return.
	 * 
	 * @param s
	 *            the string to print.
	 */
	public void println(String s);
}
