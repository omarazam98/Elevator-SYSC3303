package test;

import static org.junit.Assert.*;

import enums.SystemEnumTypes;
import scheduler.Monitor;
import org.junit.Before;
import org.junit.Test;

public class MonitorTest {

	private Monitor monitor;
	
	@Before
	public void setUp() throws Exception {
		monitor = new Monitor("elevator", 3, 3, SystemEnumTypes.Direction.DOWN,
				SystemEnumTypes.ElevatorCurrentStatus.MOVE,
				SystemEnumTypes.ElevatorCurrentDoorStatus.CLOSE, 4);
	}
	
	@Test
	public void TestgetElevatorStatus() {
		assertEquals(SystemEnumTypes.ElevatorCurrentStatus.MOVE, monitor.getElevatorStatus());
	}

	@Test
	public void TestisEmpty() {
		assertEquals(monitor.isEmpty(), true);
	}
	
	@Test
	public void TestGetElevatorFloorLocation() {
		assertTrue(monitor.getElevatorFloorLocation()==3);
	}
	
	@Test
	public void TestGetNextElevatorDirection() {
		monitor.updateElevatorFloorLocation(8);
		assertEquals(monitor.getNextElevatorDirection(), SystemEnumTypes.Direction.DOWN);
	}
	
	

}
