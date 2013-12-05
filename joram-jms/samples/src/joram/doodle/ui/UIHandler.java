package doodle.ui;

import java.util.HashMap;
import java.util.Map;

import com.eleet.dragonconsole.DragonConsoleFrame;

/**
 * A UIHandler manages the command line interface (CLI) exposed to a user. The
 * UIHandler class defines default commands and functionality.
 * 
 * This class should be extended to implement the CLI functionality of a
 * particular object (e.g., a doodle user).
 * 
 * @author Jonas Michel
 * 
 */
public abstract class UIHandler extends Thread implements
		UIAlertHandlerDelegate {

	/** The terminal emulator. */
	DragonConsoleFrame console;
	SynchCommandProcessor cmdProcessor;

	/** We make callbacks on the delegate. */
	protected UIHandlerDelegate delegate;

	/** Holds a map of commands and their explanations. */
	protected Map<String, String> commands;

	/** Default commands. */
	public static final String HELP_COMMAND = "h";
	public static final String QUIT_COMMAND = "q";

	/**
	 * The thread that issues alerts to the user when the command line is not
	 * "in use."
	 */
	private UIAlertHandler uiAlertHandler;

	public UIHandler(UIHandlerDelegate delegate) {
		super();

		console = new DragonConsoleFrame();
		cmdProcessor = new SynchCommandProcessor();
		console.getConsole().setCommandProcessor(cmdProcessor);
		console.setVisible(true);

		this.delegate = delegate;

		commands = new HashMap<String, String>();
		commands.put(HELP_COMMAND, "Display this help message");
		commands.put(QUIT_COMMAND, "Quit");

		initCommands();

		uiAlertHandler = new UIAlertHandler(this);
		uiAlertHandler.start();
	}

	public void print(String s) {
		cmdProcessor.output(s);
	}

	public void println() {
		cmdProcessor.output(System.getProperty("line.separator"));
	}

	public void println(String s) {
		cmdProcessor.output(s + System.getProperty("line.separator"));
	}

	public String readLine() throws InterruptedException {
		synchronized (cmdProcessor.getInputAlert()) {
			cmdProcessor.getInputAlert().wait();
		}

		// carriage return
		println();

		return cmdProcessor.getInput();
	}

	/**
	 * Fills the commands map.
	 */
	protected abstract void initCommands();

	/**
	 * Processes validated user input.
	 * 
	 * @param input
	 *            a valid command.
	 */
	public abstract void process(String input);

	/**
	 * Queues an Event on the UIAlertHandler to be displayed to the user when
	 * the CLI is not in use.
	 * 
	 * @param event
	 *            a user alert.
	 */
	public void deliver(String alert) {
		uiAlertHandler.enqueue(alert);
	}

	/**
	 * Prints the CLI's command map.
	 */
	public void showHelp() {
		StringBuilder sb = new StringBuilder();
		sb.append(System.getProperty("line.separator"));
		sb.append("Help: you may enter one of the following commands"
				+ System.getProperty("line.separator"));

		for (Map.Entry<String, String> entry : commands.entrySet()) {
			sb.append("\t" + entry.getKey() + "\t" + entry.getValue()
					+ System.getProperty("line.separator"));
		}

		println(sb.toString());
	}

	@Override
	public void run() {
		showHelp();
		
		while (true) {
			try {
				print("Enter command: ");

				String command = readLine();

				synchronized (this) {
					if (command.equals("") || command.equals(" ")) {
						// do nothing, the user hit enter to clear alerts
					} else if (!commands.containsKey(command)) {
						println("Sorry, " + command + " is not a valid command");
						showHelp();

					} else if (command.equals(HELP_COMMAND)) {
						showHelp();

					} else if (command.equals(QUIT_COMMAND)) {
						delegate.quit();

					} else {
						process(command);

					}

					this.notify();
				}

			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}

	/* UIAlertHandlerDelegate Interface Implementation */

	@Override
	public abstract void alert(String alert);
}
