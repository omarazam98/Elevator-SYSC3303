package tests;

import static org.junit.Assert.*;

import org.junit.Before;
import org.junit.Test;

import enums.SystemEnumTypes;
import scheduler.MakeTrip;
/**
 * The test case for maketrip class
 * 
 *
 */
public class MakeTripTest {
	
	private MakeTrip tripReq;
	
	@Before//initial setup for maketrip request
	public void setUp() throws Exception {
		tripReq = new MakeTrip(2, SystemEnumTypes.Direction.UP);
	}

	@Test//test initial setup for maketrip request work properly
	public void testMakeTrip() {
		assertEquals(tripReq.getUserinitalLocation(), 2);
		assertEquals(tripReq.getElevatorDirection(), SystemEnumTypes.Direction.UP);
	}
}