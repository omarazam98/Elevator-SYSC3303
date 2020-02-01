package elevator;

import requests.Request;
/**
 * 
 * an interface that each subsystem have the same methods
 * */
public interface ElevatorEvents {
	public void receiveEvent(Request event);

	public Request getNextEvent();

	public String getName();
}
