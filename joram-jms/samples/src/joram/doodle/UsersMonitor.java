package doodle;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.HashSet;
import java.util.Set;

/**
 * Users run a user monitor thread to monitor the shared file of available users
 * for any changes.
 * 
 * @author Jonas Michel
 * 
 */
public class UsersMonitor extends Thread {

	/** We make callbacks on the delegate. */
	private UsersMonitorDelegate delegate;

	/** Holds the names of the users currently online and available. */
	private Set<String> users;

	public UsersMonitor(UsersMonitorDelegate delegate) {
		this.delegate = delegate;

		users = getAvailableUsers();
	}

	/**
	 * Returns a list of the available (online) users.
	 * 
	 * @return the screen names of the users that are currently online and
	 *         available.
	 */
	public Set<String> getAvailableUsers() {
		try {
			Set<String> users = new HashSet<String>();

			File dir = new File(Settings.AVAILABLE_USERS_PATH);
			File file = new File(dir, Settings.AVAILABLE_USERS_FILE);
			BufferedReader in = new BufferedReader(new FileReader(file));

			synchronized (file) {
				String name;
				while (in.ready()) {
					name = in.readLine().trim();
					if (name.equals(""))
						continue; // skip blank lines

					users.add(name);
				}

				in.close();
			}

			return users;

		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return null;
	}

	/**
	 * Adds a user to the list of available (online) users.
	 * 
	 * @param name
	 *            the user name to add.
	 */
	public void addUser(String name) {
		try {
			File dir = new File(Settings.AVAILABLE_USERS_PATH);
			File file = new File(dir, Settings.AVAILABLE_USERS_FILE);
			PrintWriter writer = new PrintWriter(new BufferedWriter(
					new FileWriter(file, true)));

			synchronized (file) {
				writer.println(name);
				writer.close();
			}

		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	/**
	 * Removes a user from the list of available (online) users.
	 * 
	 * @param name
	 *            the user name to remove.
	 */
	public void removeUser(String name) {
		try {
			File dir = new File(Settings.AVAILABLE_USERS_PATH);
			File file = new File(dir, Settings.AVAILABLE_USERS_FILE);
			File temp = new File(dir, Settings.AVAILABLE_USERS_FILE + "."
					+ name);

			BufferedReader reader = new BufferedReader(new FileReader(file));
			PrintWriter writer = new PrintWriter(new BufferedWriter(
					new FileWriter(temp)));

			synchronized (file) {
				String remove = name;
				String line;

				while ((line = reader.readLine()) != null) {
					// trim newline when comparing with lineToRemove
					if (line.equals(remove))
						continue;

					writer.println(line);
				}

				writer.close();

				boolean successful = temp.renameTo(file);
			}

		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	/**
	 * Clears the contents of the available users file.
	 * 
	 * @return true if successful, false otherwise.
	 */
	public static boolean reset() {
		try {
			File dir = new File(Settings.AVAILABLE_USERS_PATH);
			File file = new File(dir, Settings.AVAILABLE_USERS_FILE);
			File temp = new File(dir, Settings.AVAILABLE_USERS_FILE + ".empty");

			PrintWriter writer = new PrintWriter(new BufferedWriter(
					new FileWriter(temp)));
			writer.print("");
			writer.close();

			return temp.renameTo(file);

		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return false;
	}

	@Override
	public void run() {
		try {
			WatchService service = FileSystems.getDefault().newWatchService();
			Path path = Paths.get(Settings.AVAILABLE_USERS_PATH);
			path.register(service, StandardWatchEventKinds.ENTRY_MODIFY);

			while (true) {
				// wait until a directory change is detected
				WatchKey key = service.take();

				// remove all pending directory change events
				key.pollEvents();

				// get an up-to-date list of the currently available users
				Set<String> availableUsers = getAvailableUsers();

				// has anyone just come online?
				Set<String> logOns = new HashSet<String>(availableUsers);
				logOns.removeAll(users);

				// has anyone just gone offline?
				Set<String> logOffs = new HashSet<String>(users);
				logOffs.removeAll(availableUsers);

				// update the list of available users
				users = availableUsers;

				// notify the delegate of the changes
				delegate.change(logOns, logOffs);

				boolean valid = key.reset();
				if (!valid) {
					break; // Exit if directory is deleted
				}
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}
}
