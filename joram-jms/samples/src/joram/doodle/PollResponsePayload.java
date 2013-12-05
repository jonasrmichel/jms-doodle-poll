package doodle;

import java.util.Map;

/**
 * User peers respond to a poll with a message containing a PollResponse
 * payload.
 * 
 * @author Jonas Michel
 * 
 */
public class PollResponsePayload extends Payload {
	private static final long serialVersionUID = -2763712824350020640L;

	/** The responder's user name. */
	private String responder;

	/** Holds the user's responses for each proposed time slot. */
	private Map<TimeSlot, Response> responses;

	public PollResponsePayload(String responder,
			Map<TimeSlot, Response> responses) {
		super();

		this.responder = responder;
		this.responses = responses;
	}

	public String getResponder() {
		return responder;
	}

	public Map<TimeSlot, Response> getResponses() {
		return responses;
	}

}
