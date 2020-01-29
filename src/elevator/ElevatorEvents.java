package elevator;

import requests.Request;

public interface ElevatorEvents {
	public void receiveEvent(Request event);

	public Request getNextEvent();

	public String getName();
}
