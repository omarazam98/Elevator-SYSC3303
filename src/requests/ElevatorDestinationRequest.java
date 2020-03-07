package requests;

import enums.SystemEnumTypes.Fault;

public class ElevatorDestinationRequest extends Request {

	private String DestinationFloor;
	private String ElevatorName;
	private String PickupFloor;
	private Fault fault;
	private static byte[] RequestType = new byte[] { 1, 8 };

	/**
	 * This method is responsible for setting up the setting up the destination
	 * floor for the elevator
	 * 
	 * @param pickupFloor  Floor from which a request was made
	 * @param destName     Destination floor
	 * @param elevatorName Name of elevator
	 */
	public ElevatorDestinationRequest(String pickupFloor, String destName, String elevatorName) {
		this.setRequestType(RequestType);
		this.setPickupFloor(pickupFloor);
		this.setDestinationFloor(destName);
		this.setElevatorName(elevatorName);
	}
	public ElevatorDestinationRequest(String pickupFloor, String destName, String elevatorName, Fault fault){
		this(pickupFloor, destName, elevatorName);
		this.setFault(fault);
	}

	public static byte[] getRequestType() {
		return RequestType;
	}

	/**
	 * @return the destination floor for current elevator
	 */
	public String getDestinationFloor() {
		return DestinationFloor;
	}

	/**
	 * @param floorName name of the floor to be visited
	 */
	public void setDestinationFloor(String floorName) {
		DestinationFloor = floorName;
	}

	/**
	 * @return the elevatorName
	 */
	public String getElevatorName() {
		return ElevatorName;
	}

	/**
	 * @param elevatorName the elevatorName
	 */
	public void setElevatorName(String elevatorName) {
		ElevatorName = elevatorName;
	}

	/**
	 * @return the floor from which a request was made
	 */
	public String getPickupFloor() {
		return PickupFloor;
	}

	public Fault getFault() {
		return fault;
	}

	/**
	 * @param fault the fault to set
	 */
	public void setFault(Fault fault) {
		this.fault = fault;
	}
	/**
	 * @param pickupFloor the from which a request is made
	 */
	public void setPickupFloor(String pickupFloor) {
		PickupFloor = pickupFloor;
	}

}
