/* Generated by Together */
package org.dejavu.fsm;

import org.dejavu.util.DjvLogMsg.Category;
import org.dejavu.util.DjvSystem;
import java.util.HashMap;
import java.util.Map;

/**
 * Class utilities providing some value-added functionality for the FSM
 * framework.
 */
public class FsmUtilities {

	private final static Map<String, Integer> gStateIdMap = new HashMap<>();
	private static int gStateIdCount = 0;
	static final int INVALID_STATE_ID = -1;

	/**
	 * Given a new state name, returns a unique ID.
	 *
	 * @param stateName The name of the desired state.
	 * @return The ID of the desired state.
	 */
	public static int registerState(String stateName) {
		Integer stateId;
		synchronized (gStateIdMap) {
			stateId = gStateIdMap.get(stateName);
			if (null == stateId) {
				stateId = gStateIdCount++;
				gStateIdMap.put(stateName, stateId);
			} else {
				DjvSystem.logError(Category.DESIGN,	"State name collision of " + stateName
					+ ". Your state machine will not likely work properly");
			}
		}
		return stateId;
	}
}
