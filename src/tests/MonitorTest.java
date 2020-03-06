package tests;

import static org.junit.Assert.*;

import enums.SystemEnumTypes;
import scheduler.Monitor;
import org.junit.Before;
import org.junit.Test;
/**
 * The test class for Monitor class 
 *
 */
public class MonitorTest {

	private Monitor monitor;
	
	@Before//initial setup
	public void setUp() throws Exception {
		monitor = new Monitor("elevator", 3, 3, SystemEnumTypes.Direction.DOWN,
				SystemEnumTypes.ElevatorCurrentStatus.MOVE,
				SystemEnumTypes.ElevatorCurrentDoorStatus.CLOSE, 4);
	}
	
	@Test//test getElevatorStatus() 
	public void TestgetElevatorStatus() {
		assertEquals(SystemEnumTypes.ElevatorCurrentStatus.MOVE, monitor.getElevatorStatus());
	}

	@Test//test isEmpty()
	public void TestisEmpty() {
		assertEquals(monitor.isTripQueueEmpty(), true);
	}
	
	@Test//test GetElevatorFloorLocation()
	public void TestGetElevatorFloorLocation() {
		assertTrue(monitor.getElevatorFloorLocation()==3);
	}
	
	@Test//test GetNextElevatorDirection()
	public void TestGetNextElevatorDirection() {
		monitor.updateElevatorFloorLocation(8);
		assertEquals(monitor.getNextElevatorDirection(), SystemEnumTypes.Direction.DOWN);
	}
	
	

}