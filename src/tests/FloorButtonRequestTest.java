package tests;

import static org.junit.Assert.assertEquals;

import java.text.SimpleDateFormat;
import java.util.Date;

import org.junit.Before;
import org.junit.Test;

import enums.SystemEnumTypes;
import requests.FloorButtonRequest;

public class FloorButtonRequestTest {
	private FloorButtonRequest floorBR;
	private String time;
	
	@Before
	public void setUp() throws Exception {
		Date date = new Date();
		SimpleDateFormat sdf = new SimpleDateFormat("hh:mm:ss.mmm");
		time = sdf.format(date).toString();
		floorBR = new FloorButtonRequest(time, "2", SystemEnumTypes.Direction.DOWN, "7");
	}

	@Test
	public void testSetButtonPressTime() {
		System.out.println(time);
		System.out.println(floorBR.getButtonPressTime());
		assertEquals(floorBR.getButtonPressTime(), time);
	}

	@Test
	public void testSetFloorName() {
		assertEquals(floorBR.getFloorName(), "2");
	}

	@Test
	public void testSetDirection() {
		assertEquals(floorBR.getDirection(), SystemEnumTypes.Direction.DOWN);
	}

	@Test
	public void testSetDestinationFloor() {
		assertEquals(floorBR.getDestinationFloor(), "7");
	}

}

