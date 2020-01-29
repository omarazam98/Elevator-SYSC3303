package scheduler;

import enums.SystemEnumTypes;
/*
 * This class deals with the trip requests that have been made and contains 
 * getters and setters for the user current and destination locations respectively.
 */
public class MakeTrip {

	private int userinitalLocation;
	private int userFinalLocation;
	private SystemEnumTypes.Direction elevatorDirection;

	public MakeTrip(int initialFloor, int finalFloor) {
		this.userinitalLocation = initialFloor;
		this.userFinalLocation = finalFloor;

		if (finalFloor > initialFloor) {
			this.elevatorDirection = SystemEnumTypes.Direction.UP;
		} else {
			this.elevatorDirection = SystemEnumTypes.Direction.DOWN;
		}
	}

	/**
	 * @return this method returns the initial floor on which the user is located
	 */
	public int getUserinitalLocation() {
		return this.userinitalLocation;
	}

	/**
	 * @return this method returns the final floor on which the user is planning to go
	 */
	public int getUserFinalLocation() {
		return this.userFinalLocation;
	}

	/**
	 * @return this method returns the direction in which the elevator is currently moving
	 */
	public SystemEnumTypes.Direction getElevatorDirection() {
		return this.elevatorDirection;
	}

	/**
	 * An equals method that checks if the object that is being passed is an instance of TripRequest class.
	 * @param trip refers to the Trip that user wants to make
	 * @return true if the requested trip is an instance of the Trip class
	 */
	public boolean equals(MakeTrip trip) {
		if ((this.userinitalLocation == trip.getUserinitalLocation())
				&& (this.userFinalLocation == trip.getUserFinalLocation())
				&& (this.elevatorDirection == trip.getElevatorDirection())) {
			return true;
		}
		return false;
	}

	/**
	 * This method formats the output as (userinitalLocation, userFinalLocation)
	 */
	public String toString() {
		String s = "";
		s += "(" + this.userinitalLocation + "," + this.userFinalLocation + ")";
		return s;
	}
}
