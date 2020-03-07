package scheduler;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;

import elevator.ElevatorState;
import enums.SystemEnumTypes.Direction;
import enums.SystemEnumTypes.ElevatorCurrentDoorStatus;
import enums.SystemEnumTypes.ElevatorCurrentStatus;

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
	private Direction queueDirection;
	private ArrayList<MakeTrip> tripRequestSuccess;
	private ElevatorState elevatorCurrentState;
	
	public Monitor(String elevatorName, Integer elevatorStartFloorLocation, Integer currentElevatorFloorLocation, Direction currentElevatorDirection, ElevatorCurrentStatus currentElevatorStatus, 
			ElevatorCurrentDoorStatus currentElevatorDoorStatus, Integer totalNumberOfFloors, Integer timeBetweenFloors, Integer passengerWaitTime, Integer doorOperationTime) {
		this.currentElevatorName = elevatorName;
		this.requestInQueue = new LinkedHashSet<MakeTrip>();
		this.destinationFloors = new HashSet<Integer>();
		this.pickupFloors = new HashSet<Integer>();
		this.tripRequestSuccess = new ArrayList<MakeTrip>();
		this.queueDirection = Direction.STAY;
		this.elevatorCurrentState = new ElevatorState(
				elevatorStartFloorLocation,
				currentElevatorFloorLocation,
				currentElevatorDirection,
				currentElevatorStatus,
				currentElevatorDoorStatus,
				totalNumberOfFloors,
				timeBetweenFloors, 
				passengerWaitTime, 
				doorOperationTime);
	}
	
	/**
	 * This method is responsible for updating the elevator's direction.
	 * 
	 * @param direction the direction of the elevator motion.
	 */
	public void updateElevatorDirection(Direction direction) {
		this.elevatorCurrentState.setDirection(direction);
	}
	
	/**
	 * This method is responsible for updating the elevator's door status.
	 * 
	 * @param doorStatus the elevator current door status
	 */
	public void updateElevatorDoorStatus(ElevatorCurrentDoorStatus doorStatus) {
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
	public void updateElevatorStatus(ElevatorCurrentStatus status) {
		this.elevatorCurrentState.setStatus(status);
	}

	/**
	 * This method is responsible for getting the elevator's status.
	 * 
	 * @return the elevator current status
	 */
	public ElevatorCurrentStatus getElevatorStatus() {
		return this.elevatorCurrentState.getCurrentStatus();
	}
	
	
	/**
	 * This method is responsible for getting the elevator's direction.
	 * 
	 * @return the direction in which the elevator is moving
	 */
	public Direction getElevatorDirection() {
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
	public Direction getQueueDirection() {
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
	/**
	 * 
	 * @return the passenger wait time
	 */
	public Integer getPassengerWaitTime() {
		return this.elevatorCurrentState.getPassengerWaitTime();
	}
/**
 * 
 * @return the time for door open and close
 */
	public Integer getDoorOperationTime() {
		return this.elevatorCurrentState.getDoorOperationTime();
	}
/**
 * 
 * @return the time for travel between floors
 */
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
	public Integer estimatePickupTime(MakeTrip tripRequest) {
		int averageTravelTimePerFloor = 5;
		int averageTimePerStop = 9;
		
		//Ensure that if the elevator is out of service, that it can not have any trips assigned to it
		if (this.elevatorCurrentState.getCurrentStatus() == ElevatorCurrentStatus.OUT_OF_SERVICE) {
			return null;
		}

		if (this.isTripQueueEmpty()) {
			return (Math.abs(this.elevatorCurrentState.getCurrentFloor() - tripRequest.getUserinitalLocation()) * averageTravelTimePerFloor);
		} else if (this.isTripEnRoute(tripRequest)){
			int interimStops = 0;
			HashSet<Integer> allFloors = new HashSet<Integer>();
			allFloors.addAll(this.pickupFloors);
			allFloors.addAll(this.destinationFloors);
			switch (this.queueDirection) {
				case UP:
					//Check if any of the scheduled floor stops are in between the elevator's current floor and the tripRequests floor, if so this is an interim stop
					for (Integer floor : allFloors) {
						if ((this.elevatorCurrentState.getCurrentFloor() < floor) && (floor < tripRequest.getUserinitalLocation())) {
							interimStops++;
						}
					}
					break;
				case DOWN:
					//Check if any of the scheduled floor stops are in between the elevator's current floor and the tripRequests floor, if so this is an interim stop
					for (Integer floor : allFloors) {
						if ((this.elevatorCurrentState.getCurrentFloor() > floor) && (floor < tripRequest.getUserinitalLocation())) {
							interimStops++;
						}
					}
					break;
			}
			return ( (Math.abs(this.elevatorCurrentState.getCurrentFloor() - tripRequest.getUserinitalLocation()) * averageTravelTimePerFloor) + (interimStops * averageTimePerStop));
		}
		return null;
	}
	
	/**
	 * This method checks if the queues are empty or not
	 * 
	 * @return returns false if the pickup and destination queues are empty
	 */
	public boolean isTripQueueEmpty() {
		//if (this.pickupFloors.isEmpty() && this.destinationFloors.isEmpty() && this.queue.isEmpty()) {
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
		//If either, the floor is a destination stop AND the queue is in service (the elevator direction matches the queue direction)
		//OR, if the queue is not in service (idle), and the elevator is at it's starting floor.
		//OR, if the floor is a pickup floor and the elevator's 
		if ((this.isDestinationFloor(floor) && (this.elevatorCurrentState.getDirection() == this.queueDirection)) 
				|| ((this.queueDirection ==  Direction.STAY) && (this.elevatorCurrentState.getCurrentFloor() == this.elevatorCurrentState.getStartFloor()))
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
		if (this.queueDirection != Direction.STAY) {
			for (MakeTrip tripRequest : this.requestInQueue) {
				if (tripRequest.getUserinitalLocation() == this.elevatorCurrentState.getCurrentFloor()) {
					if (tripRequest.hasDestination() == false) {
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
	public Direction getNextElevatorDirection() {
		Direction nextDirection = null;
		
		//If there are no more trips left, the elevator's next direction is IDLE (as far as the tripRequestQueue is concerned)
		//If there are no more trip requests in the queue, then determine whether the elevator needs to move to get back to its starting floor.
		if (this.isTripQueueEmpty()) {
			if (this.elevatorCurrentState.getCurrentFloor() > this.elevatorCurrentState.getStartFloor()) {
				nextDirection = Direction.DOWN;
			} else if (this.elevatorCurrentState.getCurrentFloor() < this.elevatorCurrentState.getStartFloor()){
				nextDirection = Direction.UP;
			} else {
				nextDirection = Direction.STAY;
			}
		} else {
			if (!this.pickupFloors.isEmpty() || !this.destinationFloors.isEmpty()) {
				switch (this.queueDirection) {
					case UP:
							//This would be true of the elevator is traveling (in opposite direction) to the start of the queue
							if (this.elevatorCurrentState.getCurrentFloor() > this.getLowestScheduledFloor()){
								nextDirection = Direction.DOWN;
							} else {
								nextDirection = Direction.UP;
							}
						break;
					case DOWN:
							//This would be true if the elevator is traveling (in opposite direction) to the start of the queue
							if (this.elevatorCurrentState.getCurrentFloor() < this.getHighestScheduledFloor()){
								nextDirection = Direction.UP;
							} else {
								nextDirection = Direction.DOWN;
							}
						break;
				}
			} else {
				//Since the trip request queue is not empty, and there are no pickupfloors or destination floors in queue, we must be stopped at a pickup floor, next direction is the queue direction
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
		if (this.elevatorCurrentState.getCurrentStatus() == ElevatorCurrentStatus.OUT_OF_SERVICE) {
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
		for (MakeTrip tripRequest : requestInQueue) {
			if ((tripRequest.getUserinitalLocation() == pickupFloor) && (!tripRequest.hasDestination())) {
				tripRequest.setDestinationFloor(destinationFloor);
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
	public boolean addTripRequest(MakeTrip tripRequest) {
		//Ensure that if the elevator is out of service, that it can not have any trips assigned to it
		if (this.elevatorCurrentState.getCurrentStatus() == ElevatorCurrentStatus.OUT_OF_SERVICE) {
			return false;
		}
		if (this.isTripQueueEmpty()) {
			return this.addFirstTripRequest(tripRequest);
		} else {
			return this.addEnRouteTripRequest(tripRequest);
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
	
	@SuppressWarnings("incomplete-switch")
	private boolean isTripEnRoute(MakeTrip tripRequest) {
		//Check the following gating criteria first before considering adding this tripRequest to the queue
		// 1 - If the tripRequest is in the same direction as the queue direction and the elevator is moving in the same direction as the tripRequestQueue 
		// OR 2 - If the elevator's NEXT direction will be (this will allow an elevator to take any pending requests that start at AT LEAST at the same pickup floor and go the same direction)
		// OR 3 - If the tripRequest is in the same direction as the queue and it's pickupFloor is already in this elevators pickupFloors queue
		if ((this.queueDirection == tripRequest.getElevatorDirection()) && ((this.elevatorCurrentState.getDirection() == this.queueDirection) 
				|| (this.getNextElevatorDirection() == this.queueDirection)) 
				|| ((this.queueDirection == tripRequest.getElevatorDirection()) && (this.pickupFloors.contains(tripRequest.getUserinitalLocation())))) {	
			
			//If the pickup floor of the request is where the elevator is, only accept the trip if the elevator is stopped and doors are still open
			if (this.elevatorCurrentState.getCurrentFloor() == tripRequest.getUserinitalLocation()) {
				
				//If either the elevator is not stopped or the door status is not open then do not accept this trip
				if ((this.elevatorCurrentState.getCurrentStatus() != ElevatorCurrentStatus.STOP) || (this.elevatorCurrentState.getDoorStatus() != ElevatorCurrentDoorStatus.OPEN)){
					return false;
				}
			} else {
				//If the elevator is in service of the queue, or next direction is to service the queue, then check if it has passed the tripRequest's pickup floor
				if (this.queueDirection == this.elevatorCurrentState.getDirection() ||  (this.getNextElevatorDirection() == this.queueDirection)) {
					//Depending on the direction of the queue, determine whether the elevator has already passed the pickup floor of the tripRequest 
					switch(this.queueDirection) {
						case UP:
							//If this elevator is already passed the pickup floor then the elevator would have to backtrack to take this tripRequest, do not accept this trip.
							if (this.elevatorCurrentState.getCurrentFloor() > tripRequest.getUserinitalLocation()) {
								return false;
							}
							break;
						case DOWN:
							//If this elevator is already passed the pickup floor then the elevator would have to backtrack to take this tripRequest, do not accept this trip.
							if (this.elevatorCurrentState.getCurrentFloor() < tripRequest.getUserinitalLocation()) {
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
	 * Add a first trip to the queue. This is to add a trip to an idle elevator.
	 * @param tripRequest
	 * @return
	 */
	private boolean addFirstTripRequest(MakeTrip tripRequest) {
		if (this.isTripQueueEmpty()) {
			requestInQueue.add(tripRequest);
			this.queueDirection = tripRequest.getElevatorDirection();
			if (this.elevatorCurrentState.getCurrentFloor() != tripRequest.getUserinitalLocation()) {
				this.pickupFloors.add(tripRequest.getUserinitalLocation());
			}
			return true;
		}
		return false;
	}
	
	/**
	 * Attempt to add an en-route trip request to the trip request queue. 
	 * Queue must be empty for an en-route trip to be added. Check to ensure that the new tripRequest is in the same direction as the tripRequestQueue.
	 * Also needs to leverage the current elevator direction, to know whether this trip can be accommodated as an en-route request (only if the elevatorDirection matches the trip request queue's direction)
	 * Also checks the current elevator floor location to determine whether or not the trip Request can be accommodated. (if the pickup for the new tripRequest has been passed, depending on the direction)
	 * Duplicate requests are ignored (as sets are used).
	 * 
	 * @param tripRequest
	 * @return
	 */
	private boolean addEnRouteTripRequest(MakeTrip tripRequest) {
		if (!this.isTripQueueEmpty() && (this.isTripEnRoute(tripRequest))) {
			//The trip is accepted.
			requestInQueue.add(tripRequest);
			
			//If the elevator is at the pickup floor it does not need to be added to the pickupFloors queue.
			if (this.elevatorCurrentState.getCurrentFloor() != tripRequest.getUserinitalLocation()) {
				this.pickupFloors.add(tripRequest.getUserinitalLocation());
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
		
		//IS this stop a destination? If so, this destination floor can be removed from the destination queue (this removes the tripRequest as well from the tripRequestQueue, marks as successfully compelted)
		if (this.isDestinationFloor(this.elevatorCurrentState.getCurrentFloor())) {
			if (this.removeDestinationFloor(this.elevatorCurrentState.getCurrentFloor())) {
				completedTrips = this.removeTripsWithDestinationFloor(this.elevatorCurrentState.getCurrentFloor());
			}
		
			//Update each completedTrip to reflect it's current endTime
			for (MakeTrip completedTrip : completedTrips) {
				completedTrip.setCompleted();
			}
			
			//Update the queue direction to IDLE if there are no more trips left in the queue
			if (this.isTripQueueEmpty()) {
				this.queueDirection = Direction.STAY;
			}	
		}
		
		//Is this stop a pickup? IF so, this pickup Floor can be removed from the pickup queue (this does not mark a trip as successfully completed in the tripRequestQueue)
		if (this.isPickupFloor(this.elevatorCurrentState.getCurrentFloor())){
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
			this.elevatorCurrentState.toggleLamp(floor, false);
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
		
		//An iterator is used instead of a simple foreach over the set because in a foreach elements cannot be removed from a hashset properly.
		Iterator<MakeTrip> iterator = requestInQueue.iterator();
		while (iterator.hasNext()) {
			MakeTrip tripRequest = iterator.next();
			if (destination == tripRequest.getUserFinalLocation()) {
				this.tripRequestSuccess.add(tripRequest);
				completedTrips.add(tripRequest);
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
	 * Create a string output that contains the elevator state and the state of the queue's.
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
