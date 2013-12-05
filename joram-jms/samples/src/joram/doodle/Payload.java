package doodle;

import java.io.Serializable;

/**
 * All P2P messages in the doodle poll system contain payload objects.
 * 
 * This class may be extended to define specific types of message payloads.
 * 
 * @author Jonas Michel
 * 
 */
public abstract class Payload implements Serializable {
	private static final long serialVersionUID = 5984384131038722170L;

	public Payload() {

	}
}
