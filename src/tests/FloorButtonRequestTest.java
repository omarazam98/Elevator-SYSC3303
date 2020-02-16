package tests;

import static org.junit.Assert.assertEquals;

import java.text.SimpleDateFormat;
import java.util.Date;

import org.junit.Before;
import org.junit.Test;

import enums.SystemEnumTypes;
import requests.FloorButtonRequest;

/**
 * 
 * This test is to check if floorButtonRequest can provide/store 
 * correct information
 *
 */
public class FloorButtonRequestTest {
	private FloorButtonRequest floorBR;
	private String time;
	
	@Before
	public void setUp() throws Exception {
		Date date = new Date();
		SimpleDateFormat sdf = new SimpleDateFormat("hh:mm:ss.mmm");
		//the standard form of time
		time = sdf.format(date).toString();
		floorBR = new FloorButtonRequest(time, "2", SystemEnumTypes.Direction.DOWN, "7");
		//initialize floorButtonRequest
	}
	
	/**
	 * test the get and set of buttonPressedTime
	 */
	@Test
	public void testSetButtonPressTime() {
		assertEquals(floorBR.getButtonPressTime(), time);
		Date newDate = new Date();
		SimpleDateFormat sdf = new SimpleDateFormat("hh:mm:ss.mmm");
		time = sdf.format(newDate).toString();
		floorBR.setButtonPressTime(time);
		assertEquals(floorBR.getButtonPressTime(), time);
	}
	
	/**
	 * test the get and set of floorName
	 */
	@Test
	public void testSetFloorName() {
		assertEquals(floorBR.getFloorName(), "2");
		floorBR.setFloorName("3");
		assertEquals(floorBR.getFloorName(), "3");
	}
	
	/**
	 * test the set and get of direction
	 */
	@Test
	public void testSetDirection() {
		assertEquals(floorBR.getDirection(), SystemEnumTypes.Direction.DOWN);
		floorBR.setDirection(SystemEnumTypes.Direction.UP);
		assertEquals(floorBR.getDirection(), SystemEnumTypes.Direction.UP);
	}
	
	/**
	 * test the set and get of destination floor
	 */
	@Test
	public void testSetDestinationFloor() {
		assertEquals(floorBR.getDestinationFloor(), "7");
		floorBR.setDestinationFloor("9");
		assertEquals(floorBR.getDestinationFloor(), "9");
	}

}

