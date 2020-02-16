package tests;

import static org.junit.Assert.*;

import org.junit.Before;
import org.junit.Test;

import enums.SystemEnumTypes;
import requests.FloorLampRequest;

public class FloorLampRequestTest {
	
	private FloorLampRequest floorLampReq;
	
	@Before
	public void setUp() throws Exception {
		floorLampReq = new FloorLampRequest(SystemEnumTypes.Direction.DOWN, SystemEnumTypes.FloorDirectionLampStatus.ON);
	}

	@Test
	public void testSetDirection() {
		assertEquals(floorLampReq.getDirection(), SystemEnumTypes.Direction.DOWN);
		floorLampReq.setDirection(SystemEnumTypes.Direction.UP);
		assertEquals(floorLampReq.getDirection(), SystemEnumTypes.Direction.UP);
	}

}
