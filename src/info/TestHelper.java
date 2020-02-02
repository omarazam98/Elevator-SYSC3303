package info;
import main.requests.Helper;
public class TestHelper {
	
	private Helper helper = new Helper();
	
	
	@Before
	public void setup(){
		
		
		static DirectionLampRequest dirLR = new DirectionLampRequest(Direction.UP, Direction.ON);
		static ElevatorArrivalRequest elAR = new ElevatorArrivalRequest("elevator_1", "1st_floor");
		static ElevatorDoorRequest elDR = new ElevatorDoorRequest("elevator_1", ElevatorDoorStatus.OPENED);
		static ElevatorLampRequest elLR = new ElevatorLampRequest("button", LampStatus.ON);
		static ElevatorMotorRequest elMR = new ElevatorMotorRequest("elevator_1", Direction.UP);
		static FloorButtonRequest flBR = new FloorButtonRequest("time", "1st_floor", Direction.UP, "2nd_floor");
		static FloorLampRequest flLR = new FloorLampRequesst(direction.UP, LampStatus.ON);
		
	}
	
	
	@Test
	public void testCreateAndParseRequest(){
		PacketdirLR = helper.CreateRequest(dirLR);
		PacketelAR = helper.CreateRequest(elAR);
		PacketelDR = helper.CreateRequest(elDR);
		PacketelLR = helper.CreateRequest(elLR);
		PacketelMR = helper.CreateRequest(elMR);
		PacketflBR = helper.CreateRequest(flBR);
		PacketflLR = helper.CreateRequest(flLR);
		
		assertEquals("Datagram creation and parsing experienced data loss", dirLR, helper.ParseRequest());
		
	}
	
}
