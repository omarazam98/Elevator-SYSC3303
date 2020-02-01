package info;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import elevator.Direction;
import java.util.Date;

/**
 * Test class for info.Request
 * 
 * @author abdullaal-wazzan
 *
 */
public class RequestTest {

	private Request req1 = null;
	private Request req2 = null;

	@BeforeEach
	void setUp() throws Exception {
		req1 = new Request(new Date(), 1, "UP", 3);
		req2 = new Request(new Date(), 2, "DOWN", 4);
	}

	@Test
	void testRequest() {
		Request testReq = new Request(new Date(), 1, "UP", 3);
		assertTrue(testReq instanceof Request);
	}

	@Test
	void testGetFloor() {
		// req1's floor # should equal to 1
		assertEquals(req1.getFloor(), 1);
	}

	@Test
	void testSetFloor() {
		// req1's floor # should equal to 1
		assertTrue(req1.getFloor() == 1);

		// setting req1's floor # to 2
		req1.setFloor(2);

		// req1 and req2 should have same floor #, 2
		assertEquals(req1.getFloor(), req2.getFloor());

	}

	@Test
	void testGetCarButton() {
		// req1's carButton # should equal to 1
		assertEquals(req1.getCarButton(), 3);
	}

	@Test
	void testSetCarButton() {
		// req1's carButton # should equal to 1
		assertTrue(req1.getCarButton() == 3);

		// req1's carButton is set to 5
		req1.setCarButton(5);

		assertEquals(req1.getCarButton(), 5);
	}

	@Test
	void testGetTime() {
		// tests if req1's time field has been initialized
		assertTrue(req1.getTime() != null);
		assertTrue(req1.getTime() instanceof Date);
	}

	@Test
	void testGetDirec() {
		// req1's direction should be UP
		assertEquals(req1.getDirec(), "UP");
	}

	@Test
	void testSetDirec() {
		// req1's direction should be UP
		assertEquals(req1.getDirec(), "UP");

		// req1's direction set to down
		req1.setDirec("DOWN");

		// req1 and req2 should have the same direction, DOWN
		assertEquals(req1.getDirec(), req2.getDirec());
	}
}
