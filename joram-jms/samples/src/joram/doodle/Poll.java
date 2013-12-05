package doodle;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.ObjectMessage;

import org.objectweb.joram.client.jms.TextMessage;

import doodle.TimeSlot.TimeSlotComparator;

/**
 * A poll is an independent peer in the doodle poll system with its own
 * listening queue and ability to send/receive messages. Once a poll has been
 * created and opened, invitees are able to respond by casting their votes for
 * the poll's time slots. An invitee may respond once or not at all to a poll. A
 * poll may be closed at any time by its initiator. Polls are responsible for
 * keeping their invitees up-to-date on their status (open/closed), current
 * responses, and final initiator-chosen time slot. A user cannot create polls
 * with identical names -- i.e., a poll's initiator-title combination is a
 * unique identifier in the doodle poll system.
 * 
 * @author Jonas Michel
 * 
 */
public class Poll extends Peer {
	/** The poll's title. */
	private String title;

	/** The name of the user that initiated the poll. */
	private String initiator;

	/** Holds the user names of the poll invitees. */
	private Set<String> invitees;

	/**
	 * Holds the user names of poll invitees with pending (undelivered) poll
	 * messages.
	 */
	private Set<String> pending;

	/** Holds the proposed time slots and invitees' responses. */
	private Map<TimeSlot, List<Response>> responses;

	/** The poll's final time slot, null until chosen by the initiator. */
	private TimeSlot timeSlot = null;

	/** We make calls on the callback. */
	private PollDelegate delegate;

	public Poll(String title, String initiator, Set<String> invitees,
			List<TimeSlot> timeSlots, PollDelegate delegate) {
		super(new PollKey(title, initiator).toString(), Role.POLL);

		this.delegate = delegate;

		this.title = title;
		this.initiator = initiator;
		this.invitees = invitees;

		pending = new HashSet<String>();
		responses = new ConcurrentHashMap<TimeSlot, List<Response>>();
		for (TimeSlot timeSlot : timeSlots)
			responses.put(timeSlot, new ArrayList<Response>());

		update();
	}

	public String getTitle() {
		return title;
	}

	public Map<TimeSlot, List<Response>> getResponses() {
		return responses;
	}

	public synchronized TimeSlot getTimeSlot() {
		return timeSlot;
	}

	public synchronized void setTimeSlot(TimeSlot timeSlot) {
		this.timeSlot = timeSlot;
	}

	public boolean isClosed() {
		return getTimeSlot() != null;
	}

	/**
	 * Returns a sorted list of the provided time slots.
	 * 
	 * @param responses
	 * @return
	 */
	public static List<TimeSlot> getSortedTimeSlots(
			Map<TimeSlot, List<Response>> responses) {
		List<TimeSlot> timeSlots = new ArrayList<TimeSlot>(responses.keySet());

		// sort the time slots
		Collections.sort(timeSlots, new TimeSlotComparator());

		return timeSlots;
	}

	/**
	 * Generates a pretty printed string of the provided poll responses.
	 * 
	 * @param title
	 *            the poll's title.
	 * @param initiator
	 *            the poll's initiator.
	 * @param responses
	 *            the poll's responses
	 * @return
	 */
	public static String displayResponses(String title, String initiator,
			Map<TimeSlot, List<Response>> responses) {
		return displayResponses(title, initiator, responses,
				getSortedTimeSlots(responses));
	}

	/**
	 * Generates a pretty printed string of the provided poll responses.
	 * 
	 * @param title
	 *            the poll's title.
	 * @param initiator
	 *            the poll's initiator.
	 * @param responses
	 *            the poll's responses.
	 * @param timeSlots
	 *            a ordered list of the poll's time slots.
	 * @return
	 */
	public static String displayResponses(String title, String initiator,
			Map<TimeSlot, List<Response>> responses, List<TimeSlot> timeSlots) {
		// build the pretty print responses string
		StringBuilder sb = new StringBuilder();
		sb.append("Responses for poll [" + title + "] initiated by ["
				+ initiator + "]:" + System.getProperty("line.separator"));

		int count = 0;
		sb.append("[Date: " + timeSlots.get(0).toDayString() + "]"
				+ System.getProperty("line.separator"));
		for (TimeSlot timeSlot : timeSlots) {
			float[] breakdown = calculateResponseBreakdown(responses
					.get(timeSlot));

			sb.append("\t");
			sb.append(Integer.toString(++count) + ".  ");
			sb.append(timeSlot.toTimeString());
			sb.append(" [yes=" + Integer.toString((int) breakdown[0])
					+ ", maybe=" + Integer.toString((int) breakdown[1])
					+ ", no=" + Integer.toString((int) breakdown[2]) + ", n/a="
					+ Integer.toString((int) breakdown[3]) + ", score="
					+ Float.toString(breakdown[4]) + "]: ");
			sb.append("|");
			for (Response response : responses.get(timeSlot)) {
				sb.append("  " + response.getResponder() + " ("
						+ response.getChoice() + ")  |");
			}
			sb.append(System.getProperty("line.separator"));
		}

		return sb.toString();
	}

	/**
	 * Generates a pretty printed string of time slot choices in line.
	 * 
	 * @param timeSlots
	 *            a poll's timeSlots.
	 * @return
	 */
	public static String displayTimeSlotsInLine(List<TimeSlot> timeSlots) {
		StringBuilder sb = new StringBuilder();
		sb.append(timeSlots.get(0).toDayString());
		sb.append("||");
		for (TimeSlot ts : timeSlots)
			sb.append("  " + ts.toTimeString() + " ||");

		return sb.toString();
	}

	/**
	 * Calculates the current number of poll responses.
	 * 
	 * @param responses
	 *            a poll's responses.
	 * @return the number of responses to the poll.
	 */
	public static int getNumResponses(Map<TimeSlot, List<Response>> responses) {
		Set<String> responders = new HashSet<String>();

		for (List<Response> rr : responses.values()) {
			for (Response r : rr) {
				if (!responders.contains(r.getResponder()))
					responders.add(r.getResponder());
			}
		}

		return responders.size();
	}

	/**
	 * Returns the time slot with the highest vote score.
	 * 
	 * @return the most popular time slot.
	 */
	public static Map.Entry<TimeSlot, List<Response>> getTopTimeSlot(
			Map<TimeSlot, List<Response>> responses) {
		float max = 0;
		Map.Entry<TimeSlot, List<Response>> top = null;
		for (Map.Entry<TimeSlot, List<Response>> entry : responses.entrySet()) {
			float score = calculateResponseBreakdown(entry.getValue())[4];
			if (score > max) {
				max = score;
				top = entry;
			}
		}

		return top;
	}

	/**
	 * Calculates the breakdown for a list of poll responses (e.g., for a
	 * particular time slot). The breakdown includes the number of yes, no, and
	 * maybe responses as well as a "score" calculated using the following
	 * scale, [YES: 1, MAYBE: 0.5, NO, N/A: 0].
	 * 
	 * @param responses
	 *            a list of poll responses.
	 * @return an array of response breakdown [# yes, # maybe, # no, # n/a,
	 *         score].
	 */
	public static float[] calculateResponseBreakdown(List<Response> responses) {
		float yes = 0, maybe = 0, no = 0, na = 0, score = 0;

		for (Response response : responses) {
			switch (response.getChoice()) {
			case YES:
				yes += 1;
				score += 1.0;
				break;
			case MAYBE:
				maybe += 1;
				score += 0.5;
				break;
			case NO:
				no += 1;
				score += 0.0;
				break;
			case NA:
				na += 1;
				score += 0.0;
				break;
			}
		}

		return new float[] { yes, maybe, no, na, score };
	}

	/**
	 * Updates all invitees with the poll's current status and responses.
	 */
	private void update() {
		Payload payload = null;
		synchronized (responses) {
			payload = new PollStatusPayload(title, initiator, responses,
					getTimeSlot());
		}

		for (String user : invitees) {
			boolean success = send(user, Role.USER, payload);

			// add this user to the pending set if the message was undeliverable
			synchronized (pending) {
				if (!success)
					pending.add(user);
				else if (pending.contains(user))
					pending.remove(user);
			}
		}
	}

	/**
	 * Updates a particular invitee with the poll's current status and
	 * responses.
	 * 
	 * @param user
	 *            the user to update.
	 */
	public void update(String user) {
		if (!invitees.contains(user))
			return;

		// alert the delegate
		delegate.pollActivity(user,
				"Invited user [] just came online and is being updated");

		Payload payload = null;
		synchronized (responses) {
			payload = new PollStatusPayload(title, initiator, responses,
					getTimeSlot());
		}

		boolean success = send(user, Role.USER, payload);

		// add this user to the pending set if the message was undeliverable
		synchronized (pending) {
			if (!success)
				pending.add(user);
			else if (pending.contains(user))
				pending.remove(user);
		}
	}

	/**
	 * Closes the poll notifying invitees of the finalized time slot (chosen by
	 * the poll initiator).
	 * 
	 * @param timeSlot
	 *            the final time slot chosen by the poll initiator.
	 */
	public void close(TimeSlot timeSlot) {
		setTimeSlot(timeSlot);
		update();
		delegate.pollActivity(title,
				"Closed with final time slot [" + timeSlot.toString() + "]");
	}

	@Override
	public void onMessage(Message message) {
		try {
			if (message instanceof TextMessage)
				return; // ObjectMessages only

			Payload payload = (Payload) ((ObjectMessage) message).getObject();
			if (!(payload instanceof PollResponsePayload))
				return; // poll peers only receive PollResponse messages

			if (isClosed())
				return;

			// update the poll's responses
			Map<TimeSlot, Response> userResponses = ((PollResponsePayload) payload)
					.getResponses();
			synchronized (responses) {
				for (Map.Entry<TimeSlot, Response> entry : userResponses
						.entrySet()) {
					responses.get(entry.getKey()).add(entry.getValue());
				}
			}

			// broadcast update to poll's invitees
			update();

			// alert the delegate
			delegate.pollActivity(title, "Received response from ["
					+ ((PollResponsePayload) payload).getResponder() + "]");

		} catch (JMSException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result
				+ ((initiator == null) ? 0 : initiator.hashCode());
		result = prime * result + ((title == null) ? 0 : title.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (!(obj instanceof Poll))
			return false;
		Poll other = (Poll) obj;
		if (initiator == null) {
			if (other.initiator != null)
				return false;
		} else if (!initiator.equals(other.initiator))
			return false;
		if (title == null) {
			if (other.title != null)
				return false;
		} else if (!title.equals(other.title))
			return false;
		return true;
	}

	@Override
	public String toString() {
		Map.Entry<TimeSlot, List<Response>> topTimeSlot = getTopTimeSlot(responses);
		String topSlot = topTimeSlot == null ? "none" : topTimeSlot.getKey()
				.toTimeString();
		String topScore = topTimeSlot == null ? "n/a"
				: Float.toString(calculateResponseBreakdown(topTimeSlot
						.getValue())[4]);
		String finalSlot = timeSlot == null ? "none" : timeSlot.toTimeString();

		return "[title: "
				+ title
				+ ", invitees: "
				+ invitees.toString()
				+ ", date: "
				+ responses.keySet().toArray(
						new TimeSlot[responses.keySet().size()])[0]
						.toDayString() + ", # responses: "
				+ Integer.toString(getNumResponses(responses))
				+ ", top time slot: " + topSlot + " (score=" + topScore + ")"
				+ ", final time slot: " + finalSlot + "]";
	}

	/**
	 * A PollKey uniquely identifies a poll in the system by its title and
	 * initiator.
	 * 
	 * @author Jonas Michel
	 * 
	 */
	public static class PollKey {
		private String title;
		private String initiator;

		public PollKey(String title, String initiator) {
			this.title = title;
			this.initiator = initiator;
		}

		@Override
		public String toString() {
			return title + "_" + initiator;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result
					+ ((initiator == null) ? 0 : initiator.hashCode());
			result = prime * result + ((title == null) ? 0 : title.hashCode());
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (!(obj instanceof PollKey))
				return false;
			PollKey other = (PollKey) obj;
			if (initiator == null) {
				if (other.initiator != null)
					return false;
			} else if (!initiator.equals(other.initiator))
				return false;
			if (title == null) {
				if (other.title != null)
					return false;
			} else if (!title.equals(other.title))
				return false;
			return true;
		}
	}
}
