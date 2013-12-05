package doodle;

import java.io.Serializable;

/**
 * A response represents a user's "vote" for a particular poll time slot.
 * 
 * @author Jonas Michel
 * 
 */
public class Response implements Serializable {
	private static final long serialVersionUID = 7819504188677909455L;

	/** The choices available when responding to a poll. */
	public enum Choice {
		YES, NO, MAYBE, NA
	};

	/** The user responsible for this response. */
	private String responder;

	/** The responder's choice. */
	private Choice choice;

	public Response(String responder, String choiceStr) {
		this.responder = responder;

		if (choiceStr.toLowerCase().equals("y"))
			this.choice = Choice.YES;
		else if (choiceStr.toLowerCase().equals("n"))
			this.choice = Choice.NO;
		else if (choiceStr.toLowerCase().equals("m"))
			this.choice = Choice.MAYBE;
		else
			this.choice = Choice.NA;
	}

	public Response(String responder, Choice choice) {
		this.responder = responder;
		this.choice = choice;
	}

	public String getResponder() {
		return responder;
	}

	public Choice getChoice() {
		return choice;
	}
}
