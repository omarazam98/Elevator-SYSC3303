package requests;

public class ElevatorArrivalRequest extends Request {

	private String ElevatorName;
	private String FloorName;

	// request type for parsing purposes
	private static byte[] RequestType = new byte[] { 1, 2 };

	/**
	 * Create a request for an elevator's arrival at a floor
	 * 
	 * @param Elevator current elevator name
	 * @param Floor    current floor name
	 */
	public ElevatorArrivalRequest(String Elevator, String Floor) {
		this.setRequestType(RequestType);
		this.ElevatorName = Elevator;
		this.FloorName = Floor;
	}

	/**
	 * returns the elevator name
	 */
	public String getElevatorName() {
		return ElevatorName;
	}

	/**
	 * sets the elevator name
	 */
	public void setElevatorName(String elevatorName) {
		ElevatorName = elevatorName;
	}

	/**
	 * gets the floor name
	 */
	public String getFloorName() {
		return FloorName;
	}

	/**
	 * sets the floor name
	 */
	public void setFloorName(String floorName) {
		FloorName = floorName;
	}

	/**
	 * returns the request type
	 */
	public static byte[] getRequestType() {
		return RequestType;
	}

}
