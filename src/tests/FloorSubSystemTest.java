package tests;

import static org.junit.Assert.*;

import java.util.HashMap;

import org.junit.Before;
import org.junit.Test;

import client.FloorSubSystem;


public class FloorSubSystemTest {

	private FloorSubSystem floorss;
	
	@Before 
	public void setUp() {
		HashMap<String, HashMap<String, String>> tempConfig = new HashMap<String, HashMap<String, String>>();
		HashMap<String, String> tempEle = new HashMap<String, String>();
		tempEle.put("port", "2000");
		tempConfig.put("E1", tempEle);
		
		floorss = new FloorSubSystem("1", 2, 3, "3", tempConfig);
	}
	
	
	@Test
	public void testGetName() {
		assertEquals(floorss.getName(), "1");
	}
	
}