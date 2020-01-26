package elevator;

import info.Request;

public interface ElevatorEvents {
	public void receiveEvent(Request event);

	public Request getNextEvent();

	public String getName();
}
