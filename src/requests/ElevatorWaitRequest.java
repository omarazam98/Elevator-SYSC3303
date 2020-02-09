package requests;

public class ElevatorWaitRequest extends Request {

	String ElevatorName;
	private static byte[] RequestType = new byte[] { 1, 9 };

	/**
	 * Method responsible for putting an elevator in the wait state. This method is
	 * called by the scheduler.
	 * 
	 * @param elevatorName the name of the elevator to be put into the wait state
	 */
	public ElevatorWaitRequest(String elevatorName) {
		this.setRequestType(RequestType);
		this.setElevatorName(elevatorName);
	}

	/**
	 * @return the name of the current elevator
	 */
	public String getElevatorName() {
		return ElevatorName;
	}

	/**
	 * This method is responsible for setting the elevator name
	 * 
	 * @param elevatorName the name of the elevator.
	 */
	public void setElevatorName(String elevatorName) {
		ElevatorName = elevatorName;
	}

	/**
	 * @return the type of request being made
	 */
	public static byte[] getRequestType() {
		return RequestType;
	}

}
