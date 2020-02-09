package info;

/**
 * This class is responsible for creating mutable int's
 * 
 * @author JCS
 *
 */
public class MutInt {
	private int value;

	/**
	 * Create a mutable int using another mutable int's value
	 * 
	 * @param mutint other mutable int
	 */
	public MutInt(MutInt mutint) {
		this.value = mutint.intValue();
	}

	/**
	 * Create a mutable int using an int
	 * 
	 * @param i initial int value
	 */
	public MutInt(int i) {
		this.value = i;
	}

	/**
	 * Returns the int and increments it afterwards
	 * 
	 * @return value++
	 */
	public int getAndIncrement() {
		int ret_val = value;
		this.value = this.value + 1;
		return ret_val;
	}

	/**
	 * 
	 * @return the int value
	 */
	public int intValue() {
		return value;
	}

	/**
	 * sets the int value
	 */
	public void setValue(MutInt temp_counter) {
		this.value = temp_counter.value;
	}

	/**
	 * gets the value and increments it
	 */
	public int getAndAdd(int i) {
		int ret_val = value;
		this.value = this.value + i;
		return ret_val;
	}

	/**
	 * Increments the value
	 */
	public void increment() {
		this.value = this.value + 1;
	}

	/**
	 * adds a value
	 */
	public void add(int val) {
		this.value = this.value + val;
	}

}
