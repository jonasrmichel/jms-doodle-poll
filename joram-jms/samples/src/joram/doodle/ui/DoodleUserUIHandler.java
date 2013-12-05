package doodle.ui;

import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import doodle.Poll;
import doodle.PollStatusPayload;
import doodle.Response;
import doodle.TimeSlot;

/**
 * Defines the CLI exposed to human user of the a doodle poll system.
 * 
 * @author Jonas Michel
 * 
 */
public class DoodleUserUIHandler extends UIHandler {

	/** User commands. */
	public static final String SHOW_AVAILABLE_USERS = "u";
	public static final String SHOW_POLL_RESPONSES = "s";
	public static final String SHOW_OPEN_INITIATED_POLLS = "o";
	public static final String SHOW_CLOSED_INITIATED_POLLS = "c";
	public static final String SHOW_OPEN_INVITED_POLLS = "i";
	public static final String SHOW_RESPONDED_INVITED_POLLS = "a";
	public static final String SHOW_CLOSED_INVITED_POLLS = "x";
	public static final String CREATE_POLL = "n";
	public static final String CLOSE_POLL = "e";
	public static final String RESPOND_TO_POLL = "r";

	public DoodleUserUIHandler(UIHandlerDelegate delegate) {
		super(delegate);
	}

	@Override
	protected void initCommands() {
		commands.put(SHOW_AVAILABLE_USERS, "Show currently available users");
		commands.put(SHOW_POLL_RESPONSES, "Show the responses for a poll");
		commands.put(SHOW_OPEN_INITIATED_POLLS,
				"Show open polls initiated by you");
		commands.put(SHOW_CLOSED_INITIATED_POLLS,
				"Show closed polls initiated by you");
		commands.put(SHOW_OPEN_INVITED_POLLS,
				"Show open polls to which you have been invited, "
						+ "but have not yet responded to");
		commands.put(SHOW_RESPONDED_INVITED_POLLS,
				"Show open polls you have responded to");
		commands.put(SHOW_CLOSED_INVITED_POLLS,
				"Show closed polls to which you were inivited");
		commands.put(CREATE_POLL, "Create a new poll");
		commands.put(CLOSE_POLL, "Close one of your open polls");
		commands.put(RESPOND_TO_POLL, "Respond to poll");
	}

	@Override
	public void process(String input) {
		if (input.equals(SHOW_AVAILABLE_USERS)) {
			doShowAvailableUsers();

		} else if (input.equals(SHOW_POLL_RESPONSES)) {
			doShowPollResponses();

		} else if (input.equals(SHOW_OPEN_INITIATED_POLLS)) {
			doShowOpenInitiatedPolls(0);

		} else if (input.equals(SHOW_CLOSED_INITIATED_POLLS)) {
			doShowClosedInitiatedPolls(0);

		} else if (input.equals(SHOW_OPEN_INVITED_POLLS)) {
			doShowOpenInvitedPolls(0);

		} else if (input.equals(SHOW_RESPONDED_INVITED_POLLS)) {
			doShowRespondedInvitedPolls(0);

		} else if (input.equals(SHOW_CLOSED_INVITED_POLLS)) {
			doShowClosedInvitedPolls(0);

		} else if (input.equals(CREATE_POLL)) {
			doCreatePoll();

		} else if (input.equals(CLOSE_POLL)) {
			doClosePoll();

		} else if (input.equals(RESPOND_TO_POLL)) {
			doRespondToPoll();

		} else {
			showHelp();

		}
	}

	@Override
	public void alert(String alert) {
		println("\t ** " + alert);
	}

	/**
	 * Prompts the user for a name.
	 * 
	 * @return the user input name.
	 */
	public String doPromptForName() {
		try {
			String name = null;
			while (name == null) {
				print("Enter user name: ");
				name = readLine().trim();

				if (name.equals("")) {
					println("You've gotta do better than that...");
					name = null;
				}
			}

			return name;

		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return null;
	}

	/**
	 * Displays the currently available users in the doodle poll system.
	 */
	private void doShowAvailableUsers() {
		StringBuilder sb = new StringBuilder();
		sb.append("Currently avaliable users:"
				+ System.getProperty("line.separator"));

		Set<String> users = delegate.getAvailableUsers();
		for (String user : users)
			sb.append("\t" + user + System.getProperty("line.separator"));

		println(sb.toString());
	}

	/**
	 * Displays the current responses for a given poll (open or closed)
	 * initiated by the user.
	 */
	private void doShowPollResponses() {
		try {
			List<Poll> myPolls = doShowOpenInitiatedPolls(0);
			myPolls.addAll(doShowClosedInitiatedPolls(myPolls.size()));

			List<PollStatusPayload> otherPolls = doShowOpenInvitedPolls(myPolls
					.size());
			otherPolls.addAll(doShowRespondedInvitedPolls(myPolls.size()
					+ otherPolls.size()));
			otherPolls.addAll(doShowClosedInvitedPolls(myPolls.size()
					+ otherPolls.size()));

			if (myPolls.size() == 0 && otherPolls.size() == 0)
				return;

			print("Enter poll # to show responses: ");
			int idx = Integer.parseInt(readLine().trim());

			if (idx <= myPolls.size()) {
				Poll poll = myPolls.get(idx - 1);
				println(Poll.displayResponses(poll.getTitle(),
						delegate.getName(), poll.getResponses()));

			} else if ((idx - myPolls.size()) <= otherPolls.size()) {
				PollStatusPayload poll = otherPolls.get(idx - myPolls.size()
						- 1);
				println(Poll.displayResponses(poll.getTitle(),
						poll.getInitiator(), poll.getResponses()));

			} else {
				println("Sorry, invalid poll #");
			}

		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (NumberFormatException e) {
			println("Sorry, invalid poll #");
		} catch (IndexOutOfBoundsException e) {
			println("Sorry, invalid poll #");
		}
	}

	/**
	 * Displays open polls initiated by the user.
	 * 
	 * @param init
	 *            an initial index to begin counting from.
	 * @return the list of corresponding polls.
	 */
	private List<Poll> doShowOpenInitiatedPolls(int init) {
		List<Poll> polls = new ArrayList<Poll>(delegate.getOpenInitiatedPolls()
				.values());

		if (polls.size() == 0) {
			println("You have no open polls");
			return polls;
		}

		println("Open polls initiated by you:");
		displayPolls(init, polls);

		return polls;
	}

	/**
	 * Displays closed polls initiated by the user.
	 * 
	 * @param init
	 *            an initial index to begin counting from.
	 * @return the list of corresponding polls.
	 */
	private List<Poll> doShowClosedInitiatedPolls(int init) {
		List<Poll> polls = new ArrayList<Poll>(delegate
				.getClosedInitiatedPolls().values());

		if (polls.size() == 0) {
			println("You have no closed polls");
			return polls;
		}

		println("Closed polls initiated by you:");
		displayPolls(init, polls);

		return polls;
	}

	/**
	 * Displays open polls to which the user has been invited, but not yet
	 * participated in.
	 * 
	 * @param init
	 *            an initial index to begin counting from.
	 * @return the list of corresponding polls.
	 */
	private List<PollStatusPayload> doShowOpenInvitedPolls(int init) {
		List<PollStatusPayload> polls = new ArrayList<PollStatusPayload>(
				delegate.getOpenInvitedPolls().values());

		if (polls.size() == 0) {
			println("There are no polls to which you been invited that are open");
			return polls;
		}

		println("Open polls you've been invited to participate in:");
		displayPollStatusPayloads(init, polls);

		return polls;
	}

	/**
	 * Displays invited polls the user has participated in.
	 * 
	 * @param init
	 *            an initial index to begin counting from.
	 * @return the list of corresponding polls.
	 */
	private List<PollStatusPayload> doShowRespondedInvitedPolls(int init) {
		List<PollStatusPayload> polls = new ArrayList<PollStatusPayload>(
				delegate.getRespondedInvitedPolls().values());

		if (polls.size() == 0) {
			println("There are no polls to which you been invited that you have responded to");
			return polls;
		}

		println("Open polls you've already participated in:");
		displayPollStatusPayloads(init, polls);

		return polls;
	}

	/**
	 * Displays closed polls the user was invited to.
	 * 
	 * @param init
	 *            an initial index to begin counting from.
	 * @return the list of corresponding polls.
	 */
	private List<PollStatusPayload> doShowClosedInvitedPolls(int init) {
		List<PollStatusPayload> polls = new ArrayList<PollStatusPayload>(
				delegate.getClosedInvitedPolls().values());

		if (polls.size() == 0) {
			println("There are no polls to which you been invited that are closed");
			return polls;
		}

		println("Closed polls to which you were invited:");
		displayPollStatusPayloads(init, polls);

		return polls;
	}

	/**
	 * Performs the steps to create and open a poll.
	 */
	private void doCreatePoll() {
		try {
			print("(1/4) Enter poll title: ");
			String title = readLine().trim();

			print("(2/4) Enter invitees (comma separated): ");
			String[] inviteesArr = readLine().trim().split(",");
			Set<String> invitees = new HashSet<String>();
			for (String invitee : inviteesArr)
				invitees.add(invitee.trim());

			// who's currently unavailable
			Set<String> available = delegate.getAvailableUsers();
			Set<String> unavailable = new HashSet<String>(invitees);
			unavailable.removeAll(available);

			if (unavailable.size() > 0) {
				println("The following invitees are currently unavailable, "
						+ "but will be notified when they come online:");
				println("\t" + unavailable.toString());
			}

			SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/yyyy");
			Date date = null;
			while (date == null) {
				try {
					print("(3/4) Enter date (mm/dd/yyyy): ");
					String dateStr = readLine().trim();
					date = sdf.parse(dateStr);

					if (date.before(new Date())) {
						println("So, you think you can time travel?"
								+ " Please enter a date in the future.");
						date = null;
					}
				} catch (ParseException e) {
					println("Incorrect date format. Try again...");
				}
			}

			println("[To specify time slots use 24-hour format e.g.,"
					+ " 9:00, 17:30, 14, 10:30-11, 11:45-13:15]");
			List<TimeSlot> timeSlots = new ArrayList<TimeSlot>();
			while (timeSlots.size() == 0) {
				try {

					print("(4/4) Enter time slots (comma separated): ");
					String[] timeSlotsArr = readLine().trim().split(",");
					if (timeSlotsArr.length < 2)
						throw new IOException();

					for (String timeStr : timeSlotsArr) {
						timeSlots.add(new TimeSlot(date, timeStr.trim()));
					}
				} catch (NumberFormatException e) {

					println("Detected invalid time slot format. Try again...");
					timeSlots.clear();

				} catch (IOException e) {

					println("Please enter at least two time slots. Try again...");
					timeSlots.clear();
				}
			}

			println("Creating your poll...");
			delegate.openPoll(title, invitees, timeSlots);

		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	/**
	 * Performs the steps to close a poll and finalize the time slot choice.
	 */
	private void doClosePoll() {
		try {
			List<Poll> polls = doShowOpenInitiatedPolls(0);

			if (polls.size() == 0)
				return;

			print("Enter poll # to close: ");
			int pidx = Integer.parseInt(readLine().trim());

			Poll poll = polls.get(pidx - 1);
			List<TimeSlot> timeSlots = Poll.getSortedTimeSlots(poll
					.getResponses());
			println(Poll.displayResponses(poll.getTitle(), delegate.getName(),
					poll.getResponses(), timeSlots));

			print("Enter final time slot #: ");
			int tidx = Integer.parseInt(readLine().trim());

			TimeSlot timeSlot = timeSlots.get(tidx - 1);

			println("Closing the poll with your final time slot choice...");
			delegate.closePoll(poll.getTitle(), timeSlot);

		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (NumberFormatException e) {
			println("Sorry, invalid poll or time slot #");
		} catch (IndexOutOfBoundsException e) {
			println("Sorry, invalid poll or time slot #");
		}
	}

	/**
	 * Performs the steps to respond to a poll by casting yes/no/maybe "votes"
	 * for each of the poll's time slots.
	 */
	private void doRespondToPoll() {
		try {
			List<PollStatusPayload> polls = doShowOpenInvitedPolls(0);

			if (polls.size() == 0)
				return;

			print("Enter poll # to respond to: ");
			int idx = Integer.parseInt(readLine().trim());

			PollStatusPayload poll = polls.get(idx - 1);

			println("Poll [" + poll.getTitle() + " by " + poll.getInitiator()
					+ "] time slot options:");
			List<TimeSlot> timeSlots = Poll.getSortedTimeSlots(poll
					.getResponses());
			println(Poll.displayTimeSlotsInLine(timeSlots));

			Map<TimeSlot, Response> responses = new HashMap<TimeSlot, Response>();

			println("Choose yes (y), no (n), maybe (m), or N/A (<return>)"
					+ " for each time slot options:");
			for (TimeSlot timeSlot : timeSlots) {
				print(timeSlot.toTimeString() + ": ");
				String choiceStr = readLine().trim();
				responses.put(timeSlot, new Response(delegate.getName(),
						choiceStr));
			}

			println("Responding to poll with your choices...");
			delegate.respondPoll(poll.getTitle(), poll.getInitiator(),
					responses);

		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (NumberFormatException e) {
			println("Sorry, invalid poll # or input");
		} catch (IndexOutOfBoundsException e) {
			println("Sorry, invalid poll # or input");
		}
	}

	/**
	 * A helper method used to pretty print a list of polls.
	 * 
	 * @param init
	 *            an index to begin counting from.
	 * @param polls
	 *            the polls to display.
	 */
	private void displayPolls(int init, List<Poll> polls) {
		StringBuilder sb = new StringBuilder();

		int count = init;
		for (Poll poll : polls) {
			sb.append("\t");
			sb.append(Integer.toString(++count) + ". ");
			sb.append(poll.toString());
			sb.append(System.getProperty("line.separator"));
		}

		println(sb.toString());
	}

	/**
	 * A helper method used to pretty print a list of poll status payloads (the
	 * user's local knowledge of a poll's status and responses).
	 * 
	 * @param init
	 *            an index to begin counting from.
	 * @param polls
	 *            the polls to display.
	 */
	private void displayPollStatusPayloads(int init,
			List<PollStatusPayload> polls) {
		StringBuilder sb = new StringBuilder();

		int count = init;
		for (PollStatusPayload poll : polls) {
			sb.append("\t");
			sb.append(Integer.toString(++count) + ". ");
			sb.append(poll.toString());
			sb.append(System.getProperty("line.separator"));
		}

		println(sb.toString());
	}
}
