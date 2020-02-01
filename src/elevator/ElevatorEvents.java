package elevator;

import requests.Request;

/**
 * Interface that contains methods that will be used for communication among different systems *
 */
public interface ElevatorEvents {
	public void receiveEvent(Request event);

	public Request getNextEvent();

	public String getName();
}
