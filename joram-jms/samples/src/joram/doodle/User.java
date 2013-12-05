package doodle;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.ObjectMessage;

import org.objectweb.joram.client.jms.TextMessage;

import doodle.Poll.PollKey;
import doodle.ui.DoodleUserUIHandler;
import doodle.ui.UIHandlerDelegate;

/**
 * Users are independent peers in the doodle poll system and drive system
 * activity. Each user peer exposes a command line interface (CLI) that enables
 * a human user to view other available users and to create, close, and
 * participate in polls. When a user is created it makes itself available in the
 * doodle poll system by adding its (unique) user name to a shared file
 * containing all available system users. This is equivalent to a user
 * "logging into" the system. Correspondingly, a user "logs out" by removing its
 * name from the shared file. All users continuously monitor this file for any
 * changes.
 * 
 * Users participate in polls by being explicitly invited. If a user is offline
 * and unavailable when a poll's invitations initially go out, the user will be
 * invited to the poll as soon as it comes online with a message containing the
 * poll's current status and responses.
 * 
 * @author Jonas Michel
 * 
 */
public class User extends Peer implements UsersMonitorDelegate,
		UIHandlerDelegate, PollDelegate {
	/** The user's name. */
	private String name;

	/** Holds the open polls initiated by this user. */
	private Map<String, Poll> openInitiatedPolls;

	/** Holds the closed polls initiated by this user. */
	private Map<String, Poll> closedInitiatedPolls;

	/**
	 * Holds the open polls this user has been invited to participate in, but
	 * not yet responded to.
	 */
	private Map<PollKey, PollStatusPayload> openInvitedPolls;

	/**
	 * Holds the open polls this user has been invited to participate in and has
	 * already responded to.
	 */
	private Map<PollKey, PollStatusPayload> respondedInvitedPolls;

	/** Holds the closed polls this user has been invited to participate in. */
	private Map<PollKey, PollStatusPayload> closedInvitedPolls;

	/** The command line interface exposed to the user. */
	private DoodleUserUIHandler uiHandler;

	/**
	 * Monitors the currently available (online) users and alerts us of any
	 * changes.
	 */
	private UsersMonitor usersMonitor;

	public User() {
		super(Role.USER);

		openInitiatedPolls = new HashMap<String, Poll>();
		closedInitiatedPolls = new HashMap<String, Poll>();
		openInvitedPolls = new ConcurrentHashMap<PollKey, PollStatusPayload>();
		respondedInvitedPolls = new ConcurrentHashMap<PollKey, PollStatusPayload>();
		closedInvitedPolls = new ConcurrentHashMap<PollKey, PollStatusPayload>();

		uiHandler = new DoodleUserUIHandler(this);
		usersMonitor = new UsersMonitor(this);

		// acquire a unique user name
		while (true) {
			String uName = uiHandler.doPromptForName();
			if (!usersMonitor.getAvailableUsers().contains(uName)) {
				uiHandler.println("Welcome " + uName + "!");
				this.name = uName;
				break;
			}
			uiHandler
					.println("Sorry, that user name is taken. Please try again...");
		}

		// start the user as a peer in the doodle system
		start(name);

		// log the user onto the doodle system
		// (this will alert other users that we are online and available)
		logOn();
		usersMonitor.start();
		uiHandler.start();
	}

	/**
	 * Logs a user onto the doodle system.
	 */
	private void logOn() {
		usersMonitor.addUser(name);
	}

	/**
	 * Logs a user off of the doodle system.
	 */
	private void logOff() {
		usersMonitor.removeUser(name);
	}

	@Override
	public void onMessage(Message message) {
		try {
			if (message instanceof TextMessage)
				return; // ObjectMessages only

			Payload payload = (Payload) ((ObjectMessage) message).getObject();
			if (!(payload instanceof PollStatusPayload))
				return; // user peers only receive PollStatus messages

			PollStatusPayload poll = (PollStatusPayload) payload;
			if (openInvitedPolls.containsKey(poll.getKey())
					|| respondedInvitedPolls.containsKey(poll.getKey())) {
				// this is a status update for a poll we've already been invited
				// to
				if (poll.isClosed()) {
					// the poll was closed
					openInvitedPolls.remove(poll.getKey());
					respondedInvitedPolls.remove(poll.getKey());

					closedInvitedPolls.put(poll.getKey(), poll);

					// deliver an alert to the UI
					uiHandler.deliver("The poll [" + poll.getTitle()
							+ "] initated by [" + poll.getInitiator()
							+ "] was closed with the final time slot ["
							+ poll.getTimeSlot().toString() + "]");

				} else {
					// someone responded to the poll
					if (openInvitedPolls.containsKey(poll.getKey()))
						openInvitedPolls.put(poll.getKey(), poll);
					else if (respondedInvitedPolls.containsKey(poll.getKey()))
						respondedInvitedPolls.put(poll.getKey(), poll);

					// deliver an alert to the UI
					uiHandler.deliver("The poll [" + poll.getTitle()
							+ "] initated by [" + poll.getInitiator()
							+ "] received new responses");

				}

			} else if (closedInvitedPolls.containsKey(poll.getKey())) {
				// this is a status update for a closed poll we were invited to
				closedInvitedPolls.put(poll.getKey(), poll);

			} else {
				// this is an invitation to a new poll
				openInvitedPolls.put(poll.getKey(), poll);

				// deliver an alert to the UI
				uiHandler.deliver("You have been invited to a new poll ["
						+ poll.getTitle() + "] initiated by ["
						+ poll.getInitiator() + "]");

			}

		} catch (JMSException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	/* FileMonitorDelegate Interface Implementation */

	@Override
	public void change(Set<String> logOns, Set<String> logOffs) {
		// relay logon changes to open initiated polls
		for (Poll poll : openInitiatedPolls.values()) {
			for (String user : logOns)
				poll.update(user);
		}

		// relay logon changes to closed initiated polls
		for (Poll poll : closedInitiatedPolls.values()) {
			for (String user : logOns)
				poll.update(user);
		}
	}

	/* UIHandlerDelegate Interface Implementation */

	@Override
	public void quit() {
		logOff();
		stop();

		uiHandler.println("Goodbye " + name);
		System.exit(0);
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public Set<String> getAvailableUsers() {
		return usersMonitor.getAvailableUsers();
	}

	@Override
	public Map<String, Poll> getOpenInitiatedPolls() {
		return openInitiatedPolls;
	}

	@Override
	public Map<String, Poll> getClosedInitiatedPolls() {
		return closedInitiatedPolls;
	}

	@Override
	public Map<PollKey, PollStatusPayload> getOpenInvitedPolls() {
		return openInvitedPolls;
	}

	@Override
	public Map<PollKey, PollStatusPayload> getRespondedInvitedPolls() {
		return respondedInvitedPolls;
	}

	@Override
	public Map<PollKey, PollStatusPayload> getClosedInvitedPolls() {
		return closedInvitedPolls;
	}

	@Override
	public boolean openPoll(String title, Set<String> invitees,
			List<TimeSlot> timeSlots) {
		if (openInitiatedPolls.containsKey(title))
			return false; // ensures poll name is unique

		openInitiatedPolls.put(title, new Poll(title, name, invitees,
				timeSlots, this));

		return true;
	}

	@Override
	public void closePoll(String title, TimeSlot timeSlot) {
		Poll poll = openInitiatedPolls.remove(title);
		poll.close(timeSlot);
		closedInitiatedPolls.put(title, poll);
	}

	@Override
	public boolean respondPoll(String title, String initiator,
			Map<TimeSlot, Response> responses) {
		PollKey key = new PollKey(title, initiator);

		boolean success = send(key.toString(), Role.POLL,
				new PollResponsePayload(name, responses));

		if (success) {
			// move the poll from the open-invited to open-responded map
			respondedInvitedPolls.put(key, openInvitedPolls.remove(key));

			return true;
		}

		return false;
	}

	/* PollDelegate Interface Implementation */

	@Override
	public void pollActivity(String title, String activity) {
		uiHandler.deliver("Poll [" + title + "]: " + activity);
	}

	public static void main(String[] args) {
		new User();
	}
}
