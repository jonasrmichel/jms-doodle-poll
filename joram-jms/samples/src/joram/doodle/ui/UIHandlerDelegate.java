package doodle.ui;

import java.util.List;
import java.util.Map;
import java.util.Set;

import doodle.Poll;
import doodle.PollStatusPayload;
import doodle.Response;
import doodle.TimeSlot;
import doodle.Poll.PollKey;

/**
 * Defines the callbacks used by the UIHandler.
 * 
 * @author Jonas Michel
 */
public interface UIHandlerDelegate {
	/**
	 * Called when the user wants to quit.
	 */
	public void quit();

	/**
	 * Called to retrieve the user's name.
	 * 
	 * @return the user's name.
	 */
	public String getName();

	/**
	 * Called to retrieve the currently available and online users.
	 * 
	 * @return
	 */
	public Set<String> getAvailableUsers();

	/**
	 * Called the retrieve the open polls initiated by the user.
	 * 
	 * @return
	 */
	public Map<String, Poll> getOpenInitiatedPolls();

	/**
	 * Called to retrieve the closed polls initiated by the user.
	 * 
	 * @return
	 */
	public Map<String, Poll> getClosedInitiatedPolls();

	/**
	 * Called to retrieve the open polls to which the user has been invited, but
	 * not yet responded to.
	 * 
	 * @return
	 */
	public Map<PollKey, PollStatusPayload> getOpenInvitedPolls();

	/**
	 * Called to retrieve the open polls the user has already responded to.
	 * 
	 * @return
	 */
	public Map<PollKey, PollStatusPayload> getRespondedInvitedPolls();

	/**
	 * Called to retrieve the closed polls to which the user was invited.
	 * 
	 * @return
	 */
	public Map<PollKey, PollStatusPayload> getClosedInvitedPolls();

	/**
	 * Called to create a new poll.
	 * 
	 * @param title
	 *            the poll's title.
	 * @param invitees
	 *            the names of the users invited to participate in the poll.
	 * @param timeSlots
	 *            the time slot choices poll invitees may vote on.
	 * @return true if successful, false otherwise.
	 */
	public boolean openPoll(String title, Set<String> invitees,
			List<TimeSlot> timeSlots);

	/**
	 * Called to close an open poll initiated by the user.
	 * 
	 * @param title
	 *            the poll's title.
	 * @param timeSlot
	 *            the user's final choice of time slot.
	 */
	public void closePoll(String title, TimeSlot timeSlot);

	/**
	 * Called to respond to a poll.
	 * 
	 * @param title
	 *            the poll's title.
	 * @param initiator
	 *            the name of the user who initiated the poll.
	 * @param responses
	 *            the user's time slot votes.
	 * @return true if successful, false otherwise.
	 */
	public boolean respondPoll(String title, String initiator,
			Map<TimeSlot, Response> responses);
}
