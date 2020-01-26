package elevator;

import java.util.HashMap;

public class ElevatorState {
	
      private int startFloor;
      private int currentFloor;
      private Direction direction;
      private ElevatorStatus status;
      private ElevatorDoorStatus doorStatus;
      private int totalNum;//change name
      private HashMap<Integer,Boolean> lamps;
      
      public ElevatorState(int originalFloor, int currentFloor ) {
    	  //this.startFloor = 
      }
}
