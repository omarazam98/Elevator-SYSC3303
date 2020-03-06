package scheduler;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;

import elevator.ElevatorState;
import enums.SystemEnumTypes;

/**
 * This class is responsible to maintain the current state of the elevator and a
 * queue containing information regarding the trips. Every time the elevator
 * changes its state, the class shall be updated. The class also determines if
 * for a single Elevator.
 *
 */
public class Monitor {
	private String currentElevatorName;
	private LinkedHashSet<MakeTrip> requestInQueue;
	private HashSet<Integer> destinationFloors;
	private HashSet<Integer> pickupFloors;
	private SystemEnumTypes.Direction queueDirection;
	private ArrayList<MakeTrip> tripRequestSuccess;
	private ElevatorState elevatorCurrentState;

	public Monitor(String elevatorName, Integer elevatorStartFloorLocation, Integer currentElevatorFloorLocation,
			SystemEnumTypes.Direction currentElevatorDirection,
			SystemEnumTypes.ElevatorCurrentStatus currentElevatorStatus,
			SystemEnumTypes.ElevatorCurrentDoorStatus currentElevatorDoorStatus, Integer totalNumberOfFloors, Integer timeBetweenFloors, Integer passengerWaitTime, Integer doorOperationTime) {
		this.currentElevatorName = elevatorName;
		this.requestInQueue = new LinkedHashSet<MakeTrip>();
		this.destinationFloors = new HashSet<Integer>();
		this.pickupFloors = new HashSet<Integer>();
		this.tripRequestSuccess = new ArrayList<MakeTrip>();
		this.queueDirection = SystemEnumTypes.Direction.STAY;
		this.elevatorCurrentState = new ElevatorState(elevatorStartFloorLocation, currentElevatorFloorLocation,
				currentElevatorDirection, currentElevatorStatus, currentElevatorDoorStatus, totalNumberOfFloors, timeBetweenFloors, 
				passengerWaitTime, 
				doorOperationTime);
	}

	/**
	 * This method is responsible for updating the elevator's direction.
	 * 
	 * @param direction the direction of the elevator motion.
	 */
	public void updateElevatorDirection(SystemEnumTypes.Direction direction) {
		this.elevatorCurrentState.setDirection(direction);
	}

	/**
	 * This method is responsible for updating the elevator's door status.
	 * 
	 * @param doorStatus the elevator current door status
	 */
	public void updateElevatorDoorStatus(SystemEnumTypes.ElevatorCurrentDoorStatus doorStatus) {
		this.elevatorCurrentState.setDoorStatus(doorStatus);
	}

	/**
	 * This method is responsible for updating the elevator's floor location
	 * 
	 * @param floor the current floor
	 */
	public void updateElevatorFloorLocation(Integer floor) {
		this.elevatorCurrentState.setCurrentFloor(floor);
	}

	/**
	 * This method is responsible for updating the elevator's status.
	 * 
	 * @param status the elevator current status
	 */
	public void updateElevatorStatus(SystemEnumTypes.ElevatorCurrentStatus status) {
		this.elevatorCurrentState.setStatus(status);
	}

	/**
	 * This method is responsible for getting the elevator's status.
	 * 
	 * @return the elevator current status
	 */
	public SystemEnumTypes.ElevatorCurrentStatus getElevatorStatus() {
		return this.elevatorCurrentState.getCurrentStatus();
	}

	/**
	 * This method is responsible for getting the elevator's direction.
	 * 
	 * @return the direction in which the elevator is moving
	 */
	public SystemEnumTypes.Direction getElevatorDirection() {
		return this.elevatorCurrentState.getDirection();
	}

	/**
	 * This method is responsible for getting the elevator's name.
	 * 
	 * @return the current elevator name
	 */
	public String getElevatorName() {
		return this.currentElevatorName;
	}

	/**
	 * This method is responsible for getting the elevator's current floor location.
	 * 
	 * @return the current floor
	 */
	public Integer getElevatorFloorLocation() {
		return this.elevatorCurrentState.getCurrentFloor();
	}

	/**
	 * This method is responsible for getting the current direction of the queue.
	 * 
	 * @return the current direction in the queue
	 */
	public SystemEnumTypes.Direction getQueueDirection() {
		return this.queueDirection;
	}

	/**
	 * This method is responsible for getting the elevator's starting floor
	 * location.
	 * 
	 * @return the starting floor
	 */
	public Integer getElevatorStartingFloorLocation() {
		return this.elevatorCurrentState.getStartFloor();
	}
	public Integer getPassengerWaitTime() {
		return this.elevatorCurrentState.getPassengerWaitTime();
	}

	public Integer getDoorOperationTime() {
		return this.elevatorCurrentState.getDoorOperationTime();
	}

	public Integer getTimeBetweenFloors() {
		return this.elevatorCurrentState.getTimeBetweenFloors();
	}

	/**
	 * Estimates the pick up time from a floor
	 * 
	 * @param MakeTrip the MakeTrip class
	 * @return returns the estimated time required to pickup from the current floor.
	 *         In case elevator is in STAY state: (number of floors) x (average time
	 *         between floors). In case of an re-route trip: = [(number of floors) x
	 *         (average time between floors)] + [(number he return is 0 of stops) x
	 *         (average time per stop)]. If the elevator can't facilitate the
	 *         request, returns 0
	 */
	@SuppressWarnings("incomplete-switch")
	public Integer estimatePickupTime(MakeTrip MakeTrip) {
		int averageTravelTimePerFloor = 5;
		int averageTimePerStop = 9;
		if (this.isTripQueueEmpty()) {
			return (Math.abs(this.elevatorCurrentState.getCurrentFloor() - MakeTrip.getUserinitalLocation())
					* averageTravelTimePerFloor);
		} else if (this.isTripEnRoute(MakeTrip)) {
			int interimStops = 0;
			HashSet<Integer> allFloors = new HashSet<Integer>();
			allFloors.addAll(this.pickupFloors);
			allFloors.addAll(this.destinationFloors);
			// if the scheduled floor stop is in between the current floor and the MakeTrips
			// floor,
			// this will be an interim stop
			switch (this.queueDirection) {
			case UP:
				for (Integer floor : allFloors) {
					if ((this.elevatorCurrentState.getCurrentFloor() < floor)
							&& (floor < MakeTrip.getUserinitalLocation())) {
						interimStops++;
					}
				}
				break;
			case DOWN:
				for (Integer floor : allFloors) {
					if ((this.elevatorCurrentState.getCurrentFloor() > floor)
							&& (floor < MakeTrip.getUserinitalLocation())) {
						interimStops++;
					}
				}
				break;
			}
			return ((Math.abs(this.elevatorCurrentState.getCurrentFloor() - MakeTrip.getUserinitalLocation())
					* averageTravelTimePerFloor) + (interimStops * averageTimePerStop));
		}
		return null;
	}

	/**
	 * This method checks if the queues are empty or not
	 * 
	 * @return returns false if the pickup and destination queues are empty
	 */
	public boolean isTripQueueEmpty() {
		if (this.requestInQueue.isEmpty()) {
			return true;
		}
		return false;
	}
	
	/**
	 * Return the size of the queue.
	 * @return
	 */
	public Integer getQueueLength() {
		return this.requestInQueue.size();
	}

	/**
	 * Checks if stop is required at current floor. Stop is required: If the floor
	 * is a destination and elevator is in service, then stop is needed. If the
	 * queue is STAY and the elevator is at starting floor, stop is needed. If the
	 * floor is a pickup floor, stop is necessary.
	 * 
	 * @param floor the current floor
	 * @return true if the stop is required at the current floor
	 */
	public boolean isStopRequired(int floor) {
		if ((this.isDestinationFloor(floor) && (this.elevatorCurrentState.getDirection() == this.queueDirection))
				|| ((this.queueDirection == SystemEnumTypes.Direction.STAY)
						&& (this.elevatorCurrentState.getCurrentFloor() == this.elevatorCurrentState.getStartFloor()))
				|| (this.isPickupFloor(floor))) {
			return true;
		}

		return false;
	}

	/**
	 * Checks if the Monitor is waiting for a destination floor. This Indicates that
	 * a trip has been assigned to the queue for the elevator and the destination
	 * request is not received yet.
	 * 
	 * @return returns true if the elevator is not in STAY, and has no destination
	 *         requests.
	 */
	public boolean isWaitingForDestinationRequest() {
		if (this.queueDirection != SystemEnumTypes.Direction.STAY) {
			for (MakeTrip MakeTrip : this.requestInQueue) {
				if (MakeTrip.getUserinitalLocation() == this.elevatorCurrentState.getCurrentFloor()) {
					if (MakeTrip.hasDestination() == false) {
						return true;
					}
				}
			}
		}
		return false;
	}

	/**
	 * This method is responsible for getting the next direction for the elevator
	 * based on the current state of the elevator and the contents of the queues.
	 * 
	 * @return the elevator next direction
	 */
	@SuppressWarnings("incomplete-switch")
	public SystemEnumTypes.Direction getNextElevatorDirection() {
		SystemEnumTypes.Direction nextDirection = null;

		// If no more trips, the elevator next direction is STAY
		// and it is determined if the elevator needs to move to get back to its
		// starting floor.
		if (this.isTripQueueEmpty()) {
			if (this.elevatorCurrentState.getCurrentFloor() > this.elevatorCurrentState.getStartFloor()) {
				nextDirection = SystemEnumTypes.Direction.DOWN;
			} else if (this.elevatorCurrentState.getCurrentFloor() < this.elevatorCurrentState.getStartFloor()) {
				nextDirection = SystemEnumTypes.Direction.UP;
			} else {
				nextDirection = SystemEnumTypes.Direction.STAY;
			}
		} else {
			if (!this.pickupFloors.isEmpty() || !this.destinationFloors.isEmpty()) {
				// Will be set to true if the elevator is traveling in opposite direction
				// compared to
				// the start of the queue
				switch (this.queueDirection) {
				case UP:

					if (this.elevatorCurrentState.getCurrentFloor() > this.getLowestScheduledFloor()) {
						nextDirection = SystemEnumTypes.Direction.DOWN;
					} else {
						nextDirection = SystemEnumTypes.Direction.UP;
					}
					break;
				case DOWN:
					if (this.elevatorCurrentState.getCurrentFloor() < this.getHighestScheduledFloor()) {
						nextDirection = SystemEnumTypes.Direction.UP;
					} else {
						nextDirection = SystemEnumTypes.Direction.DOWN;
					}
					break;
				}
			} else {
				// Since the queue is not empty, and no pickup or destination floors in queue,
				// a stop is needed at a pickup floor, and the direction is set to the next
				// direction
				nextDirection = this.queueDirection;
			}
		}

		return nextDirection;
	}

	/**
	 * Get the highest floor from the scheduled stops.
	 * 
	 * @return the highest floor
	 */
	private Integer getHighestScheduledFloor() {
		HashSet<Integer> allStops = new HashSet<Integer>();
		allStops.addAll(pickupFloors);
		allStops.addAll(destinationFloors);

		if (allStops.isEmpty()) {
			return null;
		}

		Iterator<Integer> iterator = allStops.iterator();
		Integer currentHighestFloor = iterator.next();
		while (iterator.hasNext()) {
			Integer nextFloorToCompare = iterator.next();
			if (nextFloorToCompare > currentHighestFloor) {
				currentHighestFloor = nextFloorToCompare;
			}
		}

		return currentHighestFloor;
	}

	/**
	 * Gets the lowest floor from the scheduled stops.
	 * 
	 * @return the lowest floor
	 */
	private Integer getLowestScheduledFloor() {
		HashSet<Integer> allStops = new HashSet<Integer>();
		allStops.addAll(pickupFloors);
		allStops.addAll(destinationFloors);

		if (allStops.isEmpty()) {
			return null;
		}

		Iterator<Integer> iterator = allStops.iterator();
		Integer currentLowestFloor = iterator.next();
		while (iterator.hasNext()) {
			Integer nextFloorToCompare = iterator.next();
			if (nextFloorToCompare < currentLowestFloor) {
				currentLowestFloor = nextFloorToCompare;
			}
		}

		return currentLowestFloor;
	}

	/**
	 * Add a destination floor to the current elevators queue.
	 * 
	 * @param destinationFloor the destination floor
	 * @param pickupFloor      the pickup floor
	 * @return true if the destination is added successfully
	 */
	@SuppressWarnings("incomplete-switch")
	public boolean addDestination(Integer pickupFloor, Integer destinationFloor) {
		
		//Ensure that if the elevator is out of service, that it can not have any trips assigned to it
				if (this.elevatorCurrentState.getCurrentStatus() == SystemEnumTypes.ElevatorCurrentStatus.OUT_OF_SERVICE) {
					return false;
				}
				boolean destinationFloorValid = false;
				//Check whether the elevator can take this destinationFloor given the elevators current state (the elevator must be in service of the queue (not travelling towards the first pickup floor)
				//The destination must not require the elevator to change directions from its current location.
				switch (this.queueDirection) {
					case UP:
						if (destinationFloor > this.elevatorCurrentState.getCurrentFloor()){
							destinationFloorValid = true;
						}
						break;
					case DOWN:
						if (destinationFloor< this.elevatorCurrentState.getCurrentFloor()){
							destinationFloorValid = true;		
						}
						break;
				}

				//If the destination floor is valid, add it to the destination floors queue and add it to its corresponding tripRequest
				if (destinationFloorValid) {
					this.destinationFloors.add(destinationFloor);
					this.elevatorCurrentState.toggleLamp(destinationFloor, true);
					this.addDestinationToMakeTrip(pickupFloor, destinationFloor);
					return true;
				} else {
					return false;
				}
	}

	/**
	 * This method adds the destination floor to the MakeTrip
	 * 
	 * @param pickupFloor      the pickup floor
	 * @param destinationFloor the destination floor
	 */
	private void addDestinationToMakeTrip(Integer pickupFloor, Integer destinationFloor) {
		for (MakeTrip MakeTrip : requestInQueue) {
			if ((MakeTrip.getUserinitalLocation() == pickupFloor) && (!MakeTrip.hasDestination())) {
				MakeTrip.setDestinationFloor(destinationFloor);
				return;
			}
		}
	}

	/**
	 * Adds trip request to the queue
	 * 
	 * @param makeTrip the MakeTrip class instance
	 * @return true if the tip is added successfully
	 */
	public boolean addTripRequest(MakeTrip makeTrip) {
		if (this.isTripQueueEmpty()) {
			return this.addFirstMakeTrip(makeTrip);
		} else {
			return this.addEnRouteMakeTrip(makeTrip);
		}
	}
	
	/**
	 * Unassigns all pending trip requests from this Elevator. 
	 * By definition a pending trip request has not been started yet, so it's pickup floor 
	 * will still be in the pickupFloors collection. Also the TripRequest will not have a destination yet.
	 * This will remove all of the pending TripRequests from the queue and all the pending Trip's pickup floors
	 * from the pickupFloors collection.
	 * 
	 * @return
	 */
	public ArrayList<MakeTrip> unassignPendingTripRequests(){
		ArrayList<MakeTrip> pendingTripRequests = new ArrayList<MakeTrip>();
		
		//For each TripRequest in the queue, if the pickup has not yet been completed
		//remove this TripRequest from the queue, add to pendingTripRequests (to be returned)
		for (MakeTrip tripRequest : new ArrayList<MakeTrip>(this.requestInQueue)) {
			if (this.pickupFloors.contains(tripRequest.getUserinitalLocation())){
				pendingTripRequests.add(tripRequest);
				this.requestInQueue.remove(tripRequest);
			}
		}
		
		//Now remove all pendingTripRequests from pickupFloors collection, (pendingTripRequests will not have a destination)
		for (MakeTrip tripRequest : pendingTripRequests) {
			this.pickupFloors.remove(tripRequest.getUserinitalLocation());
		}
		
		return pendingTripRequests;
	}

	/**
	 * if an elevaor is moving in the same direction as towards which a button is
	 * pressed; then it adds it to the queue else it continues its journey
	 */
	@SuppressWarnings("incomplete-switch")
	private boolean isTripEnRoute(MakeTrip MakeTrip) {
		// Trip is only made if it is in the direction of the elevator motion and is
		// added to the queue except if the next direction after the current one is same
		if ((this.queueDirection == MakeTrip.getElevatorDirection())
				&& ((this.elevatorCurrentState.getDirection() == this.queueDirection)
						|| (this.getNextElevatorDirection() == this.queueDirection))
				|| ((this.queueDirection == MakeTrip.getElevatorDirection())
						&& (this.pickupFloors.contains(MakeTrip.getUserinitalLocation())))) {

			// If request floor is elevator current location, and the elevator is in
			// STOP position and the doors are open, only then add it to queue
			if (this.elevatorCurrentState.getCurrentFloor() == MakeTrip.getUserinitalLocation()) {

				// if the above conditions are not satisfied, do not add request to the queue.
				if ((this.elevatorCurrentState.getCurrentStatus() != SystemEnumTypes.ElevatorCurrentStatus.STOP)
						|| (this.elevatorCurrentState
								.getDoorStatus() != SystemEnumTypes.ElevatorCurrentDoorStatus.OPEN)) {
					return false;
				}
			} else {
				// Check if the elevator has passed the request floor already or not and if so,
				// do not add it to the request queue.
				if (this.queueDirection == this.elevatorCurrentState.getDirection()
						|| (this.getNextElevatorDirection() == this.queueDirection)) {
					// Depending on the direction of the queue, determine whether the elevator has
					// already passed the pickup floor of the MakeTrip
					switch (this.queueDirection) {
					case UP:
						if (this.elevatorCurrentState.getCurrentFloor() > MakeTrip.getUserinitalLocation()) {
							return false;
						}
						break;
					case DOWN:
						if (this.elevatorCurrentState.getCurrentFloor() < MakeTrip.getUserinitalLocation()) {
							return false;
						}
						break;
					}
				}
			}
			return true;
		}
		return false;
	}

	/**
	 * Add a first trip to the queue of an idle elevator.
	 * 
	 * @param MakeTrip
	 * @return
	 */
	private boolean addFirstMakeTrip(MakeTrip MakeTrip) {
		if (this.isTripQueueEmpty()) {
			requestInQueue.add(MakeTrip);
			this.queueDirection = MakeTrip.getElevatorDirection();
			if (this.elevatorCurrentState.getCurrentFloor() != MakeTrip.getUserinitalLocation()) {
				this.pickupFloors.add(MakeTrip.getUserinitalLocation());
			}
			return true;
		}
		return false;
	}

	/**
	 * This method tries to add an re-route trip request to the trip request queue.
	 * Queue should be empty for a re-route trip to be added. Checks if the new
	 * MakeTrip is in the same direction as the MakeTripQueue. This method also uses
	 * the current elevator direction, to check if the trip can be added as re-route
	 * request. This method also checks the current elevator location to determine
	 * if or not the trip Request can be added. This method ignores duplicate
	 * requests.
	 * 
	 * @param makeTrip the instance of makeTrip
	 * @return true if successfully adds the request to the en-route trips
	 */
	private boolean addEnRouteMakeTrip(MakeTrip makeTrip) {
		if (!this.isTripQueueEmpty() && (this.isTripEnRoute(makeTrip))) {
			// The trip is accepted.
			requestInQueue.add(makeTrip);

			// If the elevator is at the pickup floor it is not added to pickupFloors queue.
			if (this.elevatorCurrentState.getCurrentFloor() != makeTrip.getUserinitalLocation()) {
				this.pickupFloors.add(makeTrip.getUserinitalLocation());
			}
			return true;
		}
		return false;
	}

	/**
	 * checks if the elevator is stopped or not
	 */
	public HashSet<MakeTrip> stopOccurred() {
		HashSet<MakeTrip> completedTrips = new HashSet<MakeTrip>();

		// If current floor refers to the destination floor, the stop is removed from
		// the queue for both the destination as well as the requestInQueue

		if (this.isDestinationFloor(this.elevatorCurrentState.getCurrentFloor())) {
			if (this.removeDestinationFloor(this.elevatorCurrentState.getCurrentFloor())) {
				completedTrips = this.removeTripsWithDestinationFloor(this.elevatorCurrentState.getCurrentFloor());
			}

			// change the state to STAY if there are no more floors in th queue to visit
			if (this.isTripQueueEmpty()) {
				this.queueDirection = SystemEnumTypes.Direction.STAY;
			}
		}

		// If current floor is the requestFloor i.e., from which the request is made;
		// remove it from the queue.
		if (this.isPickupFloor(this.elevatorCurrentState.getCurrentFloor())) {
			this.removePickupFloor(this.elevatorCurrentState.getCurrentFloor());
		}

		return completedTrips;
	}

	/**
	 * removes the destination floor
	 */
	private boolean removeDestinationFloor(int floor) {
		if (this.destinationFloors.contains(floor)) {
			this.destinationFloors.remove(floor);
			return true;
		}
		return false;
	}

	/**
	 * deletes the pickup floor
	 */
	private boolean removePickupFloor(int floor) {
		if (this.pickupFloors.contains(floor)) {
			this.pickupFloors.remove(floor);
			return true;
		}
		return false;
	}

	/**
	 * checks if the current floor is the destination floor or not
	 */
	public boolean isDestinationFloor(int floor) {
		if (this.destinationFloors.contains(floor)) {
			return true;
		}
		return false;
	}

	/**
	 * checks if the current floor is the pick up floor or not
	 */
	public boolean isPickupFloor(int floor) {
		if (this.pickupFloors.contains(floor)) {
			return true;
		}
		return false;
	}

	/**
	 * Removes trip requests whom destination is reached.
	 * 
	 * @param destination the destination floor
	 */
	private HashSet<MakeTrip> removeTripsWithDestinationFloor(int destination) {
		HashSet<MakeTrip> completedTrips = new HashSet<MakeTrip>();

		// An iterator is used to removed from a hashset properly.
		Iterator<MakeTrip> iterator = requestInQueue.iterator();
		while (iterator.hasNext()) {
			MakeTrip MakeTrip = iterator.next();
			if (destination == MakeTrip.getUserFinalLocation()) {
				this.tripRequestSuccess.add(MakeTrip);
				completedTrips.add(MakeTrip);
				iterator.remove();
			}
		}
		return completedTrips;
	}
	/**
	 * @return the elevatorState
	 */
	public ElevatorState getElevatorState() {
		return elevatorCurrentState;
	}

	/**
	 * @param elevatorState the elevatorState to set
	 */
	public void setElevatorState(ElevatorState elevatorState) {
		this.elevatorCurrentState = elevatorState;
	}

	/**
	 * prints to the console in a specific format
	 */
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("[");
		sb.append("Elevator name: " + this.currentElevatorName + "\n");
		sb.append("Current floor: " + this.elevatorCurrentState.getCurrentFloor() + "\n");
		sb.append("Current direction: " + this.elevatorCurrentState.getDirection() + "\n");
		sb.append("Current elevator status: " + this.elevatorCurrentState.getCurrentStatus() + "\n");
		sb.append("Current door status: " + this.elevatorCurrentState.getDoorStatus() + "\n");
		sb.append("Trip request queue: ");

		sb.append("[");
		Iterator<MakeTrip> queueIterator = this.requestInQueue.iterator();
		while (queueIterator.hasNext()) {
			MakeTrip tripRequest = queueIterator.next();
			sb.append(tripRequest.toString());
			if (queueIterator.hasNext()) {
				sb.append(",");
			}
		}
		sb.append("]\n");

		sb.append("Floor pickups remaining: " + this.pickupFloors.toString() + "\n");
		sb.append("Floor destinations remaining: " + this.destinationFloors.toString() + "\n");

		sb.append("Completed trips: ");
		sb.append("[");
		Iterator<MakeTrip> completedTripsIterator = this.tripRequestSuccess.iterator();
		while (completedTripsIterator.hasNext()) {
			MakeTrip tripRequest = completedTripsIterator.next();
			sb.append(tripRequest.toString());
			if (completedTripsIterator.hasNext()) {
				sb.append(",");
			}
		}
		sb.append("]");
		sb.append("]\n");
		return sb.toString();
	}
}
