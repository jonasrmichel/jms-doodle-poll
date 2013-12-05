package doodle;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import doodle.Poll.PollKey;

/**
 * Polls communicate the following types of events to users by attaching a
 * PollStatus to a message: invitations, response updates, and status updates
 * (open, closed).
 * 
 * A PollStatusPayload also represents users' (invitees') distributed knowledge
 * of a poll.
 * 
 * @author Jonas Michel
 * 
 */
public class PollStatusPayload extends Payload {
	private static final long serialVersionUID = -3287188068015785504L;

	/** The poll's title. */
	private String title;

	/** The name of the user that initiated the poll. */
	private String initiator;

	/** Holds the proposed time slots and current poll responses. */
	private Map<TimeSlot, List<Response>> responses;

	/** The final poll time slot chosen by the poll initiator. */
	private TimeSlot timeSlot;

	public PollStatusPayload(String title, String initiator,
			Map<TimeSlot, List<Response>> responses, TimeSlot timeSlot) {
		super();

		this.title = title;
		this.initiator = initiator;
		this.responses = new HashMap<TimeSlot, List<Response>>(responses);
		this.timeSlot = timeSlot;
	}

	public PollKey getKey() {
		return new PollKey(title, initiator);
	}

	public String getTitle() {
		return title;
	}

	public String getInitiator() {
		return initiator;
	}

	public Map<TimeSlot, List<Response>> getResponses() {
		return responses;
	}

	public TimeSlot getTimeSlot() {
		return timeSlot;
	}

	public boolean isClosed() {
		return timeSlot != null;
	}

	@Override
	public String toString() {
		Map.Entry<TimeSlot, List<Response>> topTimeSlot = Poll
				.getTopTimeSlot(responses);
		String topSlot = topTimeSlot == null ? "none" : topTimeSlot.getKey()
				.toTimeString();
		String topScore = topTimeSlot == null ? "n/a"
				: Float.toString(Poll.calculateResponseBreakdown(topTimeSlot
						.getValue())[4]);
		String finalSlot = timeSlot == null ? "none" : timeSlot
				.toTimeString();

		return "[title: "
				+ title
				+ ", initiator: "
				+ initiator
				+ ", date: "
				+ responses.keySet().toArray(
						new TimeSlot[responses.keySet().size()])[0]
						.toDayString() + ", # responses: "
				+ Integer.toString(Poll.getNumResponses(responses))
				+ ", top time slot: " + topSlot
				+ " (score=" + topScore + ")"
				+ ", final time slot: " + finalSlot + "]";
	}

}
