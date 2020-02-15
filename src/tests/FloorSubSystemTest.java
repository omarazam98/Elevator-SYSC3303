package tests;

import static org.junit.Assert.*;

import org.junit.Before;
import org.junit.Test;

import client.FloorSubSystem;


public class FloorSubSystemTest {

	private FloorSubSystem floorss;
	
	@Before 
	public void setUp() {
		floorss = new FloorSubSystem("1", 2, 3, null);
	}
	
	
	@Test
	public void testGetName() {
		assertEquals("get failed", "1", floorss.getName());
	}
	
}