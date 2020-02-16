package tests;

import static org.junit.Assert.*;

import org.junit.Before;
import org.junit.Test;

import enums.SystemEnumTypes;
import enums.SystemEnumTypes.Direction;
import scheduler.MakeTrip;
/**
 * The test case for maketrip class
 * @author Tian
 *
 */
public class MakeTripTest {
	
	private MakeTrip tripReq;
	
	@Before//initial setup for maketrip request
	public void setUp() throws Exception {
		tripReq = new MakeTrip(2,Direction.UP);
	}

	@Test//test initial setup for maketrip request work properly
	public void testMakeTrip() {
		assertEquals(tripReq.getUserinitalLocation(), 2);
		assertEquals(tripReq.hasDestination(), false);
		assertEquals(tripReq.getElevatorDirection(), SystemEnumTypes.Direction.UP);
	}
}