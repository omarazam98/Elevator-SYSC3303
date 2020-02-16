
package tests;

import static org.junit.Assert.*;

import org.junit.Before;
import org.junit.Test;

import info.MutInt;

/**Test class for  MutInt class
 * @author Nrx03
 *
 */
public class MutIntTest {
	private MutInt mit;

	@Before//initial setup()
	public void setUp() throws Exception {
		mit = new MutInt(3);
	}

	@Test//test MutIntMutInt()
	public void testMutIntMutInt() {
		MutInt mit2 = new MutInt(mit);
		assertTrue(mit2.intValue() == 3);
	}

	@Test//test SetValue
	public void testSetValue() {
		assertTrue(mit.intValue() == 3);
		MutInt mit2 = new MutInt(8);
		mit.setValue(mit2);
		assertTrue(mit.intValue() == 8);

	}

	@Test//test GetAndAdd()
	public void testGetAndAdd() {
		mit.getAndAdd(2);
		assertTrue(mit.intValue() == 5);
	}

	@Test//test Increment()
	public void testIncrement() {
		mit.getAndIncrement();
		assertTrue(mit.intValue() == 4);
	}

	@Test//test Add()
	public void testAdd() {
		mit.add(9);
		assertTrue(mit.intValue() == 12);
	}

}