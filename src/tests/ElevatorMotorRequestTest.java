import static org.junit.Assert.*;

import org.junit.Before;
import org.junit.Test;

import enums.SystemEnumTypes;
import requests.ElevatorMotorRequest;

public class ElevatorMotorRequestTest {

	private ElevatorMotorRequest ElevatorMotorReq;

	@Before
	public void setUp() throws Exception {
		ElevatorMotorReq = new ElevatorMotorRequest('Name', SystemEnumTypes.Direction.UP);
	}

	@Test
	public void testGetSetDirection() {
		assertEquals(ElevatorLampReq.Direction(), SystemEnumTypes.Direction.UP);
		ElevatorMotorReq.setRequestAction(SystemEnumTypes.Direction.DOWN);
		assertEquals(ElevatorLampReq.Direction(), SystemEnumTypes.Direction.DOWN);
	}
	@Test
    public void testGetSetName() {
    		assertEquals(getElevatorName.getElevatorName(), 'Name');
    		ElevatorMotorReq.setElevatorName('3303');
    		assertEquals(getElevatorName.getElevatorName(), '3303');
    	}
}