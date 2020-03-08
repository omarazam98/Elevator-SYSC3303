package scheduler;

import enums.SystemEnumTypes;

/*
 * This class deals with the trip requests that have been made and contains 
 * getters and setters for the user current and destination locations respectively.
 */
public class MakeTrip {
	private int userinitalLocation;
	private int userFinalLocation;
	private SystemEnumTypes.Direction direction;
	private boolean hasDestination;
	private long startTime, elapsedTime, endTime;

	/**
	 * Constructor
	 * 
	 * @param pickupFloor the pickup floor
	 * @param direction   the direction
	 */
	public MakeTrip(int pickupFloor, SystemEnumTypes.Direction direction) {
		this.userinitalLocation = pickupFloor;
		this.hasDestination = false;
		this.direction = direction;
		this.startTime = System.nanoTime();
	}

	/**
	 * Checks if the elevator has destination
	 */
	public boolean hasDestination() {
		return this.hasDestination;
	}

	/**
	 * @return this method returns the initial floor on which the user is located
	 */
	public int getUserinitalLocation() {
		return this.userinitalLocation;
	}

	/**
	 * Gets the destination floor.
	 * 
	 * @return
	 */
	public int getUserFinalLocation() {
		return this.userFinalLocation;
	}

	/**
	 * Sets the destination floor.
	 * 
	 * @param destinationFloor the destination floor
	 */
	public void setDestinationFloor(int destinationFloor) {
		this.userFinalLocation = destinationFloor;
		this.hasDestination = true;
	}

	/**
	 * @return this method returns the direction in which the elevator is currently
	 *         moving
	 */
	public SystemEnumTypes.Direction getElevatorDirection() {
		return this.direction;
	}

	/**
	 * An equals method that checks if the object that is being passed is an
	 * instance of TripRequest class.
	 * 
	 * @param trip refers to the Trip that user wants to make
	 * @return true if the requested trip is an instance of the Trip class
	 */
	public boolean equals(MakeTrip tripRequest) {
		if ((this.userinitalLocation == tripRequest.getUserinitalLocation())
				&& (this.userFinalLocation == tripRequest.getUserFinalLocation())
				&& (this.direction == tripRequest.getElevatorDirection())) {
			return true;
		}
		return false;
	}
	
	/**
	 * Set the end time for this TripRequest.
	 */
	public void setCompleted() {
		this.endTime = System.nanoTime();
		this.elapsedTime = System.nanoTime();
	}

	/**
	 * This method formats the output as (userinitalLocation, userFinalLocation)
	 */
	public String toString() {
		StringBuilder sb = new StringBuilder();

		sb.append("(");
		sb.append(this.userinitalLocation);
		sb.append(",");
		sb.append(this.direction);
		sb.append(",");
		if (this.hasDestination) {
			sb.append(this.userFinalLocation);
		} else {
			sb.append("?");
		}
		sb.append(")");

		return sb.toString();
	}
}
