package requests;

import enums.SystemEnumTypes;

public class ElevatorArrivalRequest extends Request {

	private String ElevatorName;
	private String FloorName;
	private SystemEnumTypes.Direction direction;

	// request type for parsing purposes
	private static byte[] RequestType = new byte[] { 1, 2 };

	/**
	 * Create a request for an elevator's arrival at a floor
	 * 
	 * @param ElevatorName current elevator name
	 * @param FloorName   current floor name
	 * @param direction current elevator direction
	 */
	public ElevatorArrivalRequest(String Elevator, String Floor, SystemEnumTypes.Direction direction) {
		this.setRequestType(RequestType);
		this.setElevatorName(Elevator);
 		this.setFloorName(Floor); 
 		this.setDirection(direction);
		
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
	/**
	 * 
	 * @param direction sent elevator's direction
	 */
	public void setDirection(SystemEnumTypes.Direction direction) {
		this.direction = direction;
	}
	/**
	 * 
	 * @return the direction of elevator
	 */
	public SystemEnumTypes.Direction getDirection() {
		return this.direction;
	}

}