package doodle.ui;

import com.eleet.dragonconsole.CommandProcessor;

/**
 * A custom Dragon Console CommandProcessor to enable a blocking readLine()
 * operation from the console.
 * 
 * @author Jonas Michel
 * 
 */
public class SynchCommandProcessor extends CommandProcessor {
	private String input;
	private Object inputAlert;

	public SynchCommandProcessor() {
		super();

		inputAlert = new Object();
	}

	public Object getInputAlert() {
		return inputAlert;
	}

	public String getInput() {
		return input;
	}

	@Override
	public void processCommand(String input) {
		this.input = input;

		synchronized (inputAlert) {
			inputAlert.notifyAll();
		}
	}

	/**
	 * Overrides the default output in CommandProcessor to determine if ANSI
	 * Colors are processed or DCCC and converts accordingly.
	 * 
	 * @param s
	 *            The String to output.
	 */
	@Override
	public void output(String s) {
		if (getConsole().isUseANSIColorCodes())
			super.output(convertToANSIColors(s));
		else
			super.output(s);
	}
}