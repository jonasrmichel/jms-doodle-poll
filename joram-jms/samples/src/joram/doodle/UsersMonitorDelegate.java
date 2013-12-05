package doodle;

import java.util.Set;

/**
 * Defines the callbacks used by the UserMonitor.
 * 
 * @author Jonas Michel
 */
public interface UsersMonitorDelegate {

	/**
	 * Called when a change in available users has been detected.
	 * 
	 * @param logOns
	 *            a set of the new users that have come online.
	 * @param logOffs
	 *            a set of existing users that have gone offline.
	 */
	public void change(Set<String> logOns, Set<String> logOffs);
}
