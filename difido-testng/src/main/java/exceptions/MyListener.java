package exceptions;

import java.awt.Event;
import java.util.EventListener;

public interface MyListener extends EventListener {
	// event dispatch methods
	void somethingHappened(Event e);

	void somethingElseHappened(Event e);

}
