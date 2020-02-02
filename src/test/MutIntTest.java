/**
 * 
 */
package test;

import static org.junit.Assert.*;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import info.MutInt;

/**
 * @author Nrx03
 *
 */
public class MutIntTest {
	private MutInt mit;
	
	@Before
	public void setUp() throws Exception {
		mit = new MutInt(3);
	}

	@Test
	public void testMutIntMutInt() {
		MutInt mit2 = new MutInt(mit);
		assertTrue(mit2.intValue() == 3);
	}

	@Test
	public void testSetValue() {
		assertTrue(mit.intValue() == 3);
		MutInt mit2 = new MutInt(8);
		mit.setValue(mit2);
		assertTrue(mit.intValue() == 8);
		
	}

	@Test
	public void testGetAndAdd() {
		mit.getAndAdd(2);
		assertTrue(mit.intValue() == 5);
	}

	@Test
	public void testIncrement() {
		mit.getAndIncrement();
		assertTrue(mit.intValue() == 4);
	}

	@Test
	public void testAdd() {
		mit.add(9);
		assertTrue(mit.intValue() == 12);
	}

}
