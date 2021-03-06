package nl.tudelft.watchdog.intellij.logic.ui;

import java.util.Date;

import nl.tudelft.watchdog.core.logic.ui.events.WatchDogEvent;
import nl.tudelft.watchdog.core.logic.ui.events.WatchDogEvent.EventType;

/**
 * A Special InactivityNotifier that makes sure a wrapping
 * {@link nl.tudelft.watchdog.core.logic.ui.events.WatchDogEvent.EventType#USER_ACTIVITY} event exists if its trigger is called.
 */
public class UserInactivityNotifier extends InactivityNotifier {

	/** Constructor. */
	public UserInactivityNotifier(EventManager eventManager,
			int activityTimeout, EventType type) {
		super(eventManager, activityTimeout, type);
	}

	/** Trigger accepting a forcedDate when the event should be fired. */
	public void trigger(Date forcedDate) {
		trigger();
		eventManager.update(new WatchDogEvent(this, EventType.USER_ACTIVITY),
				forcedDate);
	}
}