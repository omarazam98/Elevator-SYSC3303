  
package tests;
import org.junit.runner.RunWith;
//test all the test class
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;
@RunWith(Suite.class)
@SuiteClasses({
MonitorTest.class,
RequestTest.class,
MutIntTest.class,
MakeTripTest.class,
ElevatorStateTest.class,
FloorSubSystemTest.class,
})

public class Tests {}