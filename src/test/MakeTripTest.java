package test;

import static org.junit.Assert.*;

import org.junit.Before;
import org.junit.Test;

import enums.SystemEnumTypes;
import scheduler.MakeTrip;

public class MakeTripTest {
	
	private MakeTrip tripReq;
	
	@Before
	public void setUp() throws Exception {
		tripReq = new MakeTrip(2,7);
	}

	@Test
	public void testMakeTrip() {
		assertEquals(tripReq.getUserinitalLocation(), 2);
		assertEquals(tripReq.getUserFinalLocation(), 7);
		assertEquals(tripReq.getElevatorDirection(), SystemEnumTypes.Direction.UP);
	}
}
