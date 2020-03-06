package tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Date;

import org.junit.Before;
import org.junit.Test;

import requests.Request;

/**
 * Test class for info.Request
 *
 * @author abdullaal-wazzan
 *
 */
public class RequestTest {

    private Request req1 = null;
    private Request req2 = null;

    @Before//initial setup()
    public void setUp() throws Exception {
        req1 = new Request(new Date(), 1, "UP", 3);
        req2 = new Request(new Date(), 2, "DOWN", 4);
    }

    @Test//test Request() default constructor
    public void testRequest() {
        Request testReq = new Request(new Date(), 1, "UP", 3);
        assertTrue(testReq instanceof Request);
    }

    @Test//test GetFloor()
    public void testGetFloor() {
        // req1's floor # should equal to 1
        assertEquals(req1.getFloor(), 1);
    }

    @Test//test SetFloor()
    public void testSetFloor() {
        // req1's floor # should equal to 1
        assertTrue(req1.getFloor() == 1);

        // setting req1's floor # to 2
        req1.setFloor(2);

        // req1 and req2 should have same floor #, 2
        assertEquals(req1.getFloor(), req2.getFloor());

    }

    @Test//test GetCarButton() 
    public void testGetCarButton() {
        // req1's carButton # should equal to 1
        assertEquals(req1.getCarButton(), 3);
    }

    @Test//test SetCarButton()
    public void testSetCarButton() {
        // req1's carButton # should equal to 1
        assertTrue(req1.getCarButton() == 3);

        // req1's carButton is set to 5
        req1.setCarButton(5);

        assertEquals(req1.getCarButton(), 5);
    }

    @Test//test GetTime()
    public void testGetTime() {
        // tests if req1's time field has been initialized
        assertTrue(req1.getTime() != null);
        assertTrue(req1.getTime() instanceof Date);
    }

    @Test//test GetDirec()
    public void testGetDirec() {
        // req1's direction should be UP
        assertEquals(req1.getDirec(), "UP");
    }

    @Test//test SetDirec()
    public void testSetDirec() {
        // req1's direction should be UP
        assertEquals(req1.getDirec(), "UP");

        // req1's direction set to down
        req1.setDirec("DOWN");

        // req1 and req2 should have the same direction, DOWN
        assertEquals(req1.getDirec(), req2.getDirec());
    }
}