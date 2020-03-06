package info;

import java.net.DatagramPacket;
import java.util.Arrays;

import requests.DirectionLampRequest;
import requests.ElevatorArrivalRequest;
import requests.ElevatorDestinationRequest;
import requests.ElevatorDoorRequest;
import requests.ElevatorLampRequest;
import requests.ElevatorMotorRequest;
import requests.ElevatorWaitRequest;
import requests.FloorButtonRequest;
import requests.FloorLampRequest;
import requests.Request;
import enums.SystemEnumTypes;

public class Parser {
	private byte[] data;
	private MutInt counter;
	
	public Parser(byte[] data, MutInt counter) {
		this.data = data;
		this.counter = counter;
	}
	
	public Parser () {
		Clear();
	}
	
	public Request ParseRequest(DatagramPacket packet) {
		data = packet.getData();
		counter = new MutInt(0);
		
		if(data[counter.getAndIncrement()] != 0){
			System.out.println("Could not parse data. Invalid request.");
		}

		String[] SrcDests = ParseSrcDest();


		byte[] RequestType = ParseType();
		Request request = ParseOnType(RequestType);
		IncludeParams(SrcDests, request);
		Clear();
		return request;
	}
	
	private void Clear() {
		this.data = null;
		this.counter = null;
	}
	
	private void IncludeParams(String[] arr, Request request) {
		if(arr[0] != "")
			request.Sender = arr[0];
		if(arr[1] != "")
			request.Receiver = arr[1];
	}

	private String[] ParseSrcDest() {
		boolean IncludeSrcName = RTF(data[counter.getAndAdd(2)]),
				IncludeDestName = RTF(data[counter.getAndAdd(2)]);


		String SourceName = "";
		String DestinationName = "";


		if(IncludeSrcName){
			SourceName = ParseString();
		}

		if(IncludeDestName){
			DestinationName = ParseString();
		}

		String[] res = new String[] {SourceName, DestinationName};
		return res;
	}

	private Request ParseOnType(byte[] rt) {
		Request request = null;
		if(Arrays.equals(rt, DirectionLampRequest.getRequestType())){
			/* Parse based on Direction Lamp Request */
			SystemEnumTypes.Direction direction = (SystemEnumTypes.Direction) ParseEnum(SystemEnumTypes.Direction.class);
			SystemEnumTypes.FloorDirectionLampStatus status = (SystemEnumTypes.FloorDirectionLampStatus) ParseEnum(SystemEnumTypes.FloorDirectionLampStatus.class);
			request = new DirectionLampRequest(direction, status);


		} else if(Arrays.equals(rt, ElevatorArrivalRequest.getRequestType())){
			/* Parse based on Elevator Arrival Request */
			String ElevatorName = ParseString();
			String FloorName = ParseString();
			SystemEnumTypes.Direction dir = (SystemEnumTypes.Direction) ParseEnum(SystemEnumTypes.Direction.class);
			request = new ElevatorArrivalRequest(ElevatorName, FloorName, dir);
		} else if(Arrays.equals(rt, ElevatorDoorRequest.getRequestType())){
			/* Parse based on Elevator Door Request */
			String ElevatorName = ParseString();
			SystemEnumTypes.ElevatorCurrentDoorStatus Action = (SystemEnumTypes.ElevatorCurrentDoorStatus) ParseEnum(SystemEnumTypes.ElevatorCurrentDoorStatus.class);
			request = new ElevatorDoorRequest(ElevatorName, Action);
		} else if(Arrays.equals(rt, ElevatorLampRequest.getRequestType())){
			/* Parse based on Elevator Lamp Request */
			String ElevatorName = ParseString();
			SystemEnumTypes.FloorDirectionLampStatus status = (SystemEnumTypes.FloorDirectionLampStatus) ParseEnum(SystemEnumTypes.FloorDirectionLampStatus.class);
			request = new ElevatorLampRequest(ElevatorName, status);

		} else if(Arrays.equals(rt, ElevatorMotorRequest.getRequestType())){
			/* Parse based on DElevator Motor Request */
			String ElevatorName = ParseString();
			SystemEnumTypes.Direction Action = (SystemEnumTypes.Direction) ParseEnum(SystemEnumTypes.Direction.class);
			request = new ElevatorMotorRequest(ElevatorName, Action);
		} else if(Arrays.equals(rt, FloorButtonRequest.getRequestType())){
			/* Parse based on Floor Button Request */
			String DateString = ParseString();
			String FloorName = ParseString();
			SystemEnumTypes.Direction Direction = (SystemEnumTypes.Direction) ParseEnum(SystemEnumTypes.Direction.class);
			String Destination = ParseString();
		//	Fault fault = (Fault) ParseOptionalEnum(Fault.class);

		// request = new FloorButtonRequest(DateString,FloorName, Direction, Destination, fault);

			request = new FloorButtonRequest(DateString,FloorName, Direction, Destination);

		} else if(Arrays.equals(rt, FloorLampRequest.getRequestType())){
			/* Parse based on Floor Lamp Request */
			SystemEnumTypes.Direction Direction = (SystemEnumTypes.Direction) ParseEnum(SystemEnumTypes.Direction.class);
			SystemEnumTypes.FloorDirectionLampStatus status = (SystemEnumTypes.FloorDirectionLampStatus) ParseEnum(SystemEnumTypes.FloorDirectionLampStatus .class);
			request = new FloorLampRequest(Direction, status);
		} else if(Arrays.equals(rt, ElevatorDestinationRequest.getRequestType())){
			/* Parse based on Elevator Destination Request */
			String PickupFloor = ParseString();
			String DestFloor = ParseString();
			String ElevatorName = ParseString();
		//	Fault fault = (Fault) ParseOptionalEnum(Fault.class);
		//	request = new ElevatorDestinationRequest(PickupFloor,DestFloor,ElevatorName, fault);
			request = new ElevatorDestinationRequest(PickupFloor,DestFloor,ElevatorName);
		} else if(Arrays.equals(rt, ElevatorWaitRequest.getRequestType())){
			/* Parse based on Elevator Wait Request */
			String elevatorName = ParseString();
			request = new ElevatorWaitRequest(elevatorName);
		} 
		return request;
	}
/*
	private <T extends Enum<T>> Enum<?> ParseOptionalEnum(Class<T> clazz) {
		boolean parseEnum = RTF(data[counter.getAndAdd(2)]); //checks if fault was included before parsing
		if (parseEnum) {
			return ParseEnum(clazz);
		}
		return null;
	}
*/
	private byte[] ParseType() {
		byte[] array = new byte[] {data[counter.getAndIncrement()], data[counter.getAndIncrement()]};
		if(data[counter.intValue()] == 0) 
			counter.getAndIncrement();
		else {
			System.out.println("Could not parse type of request. Data was invalid.");
		}
		
		return array;
	}

	private String ParseString() {
		String ret = "";
		if(data[counter.intValue()] != 0){
			//System.out.println("data: "+data[counter.intValue()]);
			if(data[counter.intValue()] == (byte) -1){
				counter.add(2);
				return ret;
			}
			//attempt to parse data
			MutInt temp_counter = new MutInt(counter) ;
			while(temp_counter.intValue() != data.length && data[temp_counter.getAndIncrement()]!=0);

			ret = new String(Arrays.copyOfRange(data, counter.intValue(),temp_counter.intValue() - 1));
			counter.setValue(temp_counter);
		}
		return ret;
	}

	private <T extends Enum<T>> Enum<?> ParseEnum(Class<T> clazz) {
		Enum<?>[] enums = clazz.getEnumConstants();
		if((((int)data[counter.intValue()]) - 1) < enums.length){
			return enums[((int) data[counter.getAndAdd(2)]) - 1];
		}
		else {
			System.out.println("Could not parse Enum; Invalid data or Enum does not exist.");
			return null;
		} 
	}

	private boolean RTF(byte tf){
		if(tf == 'T') return true;
		else return false;
	}

}
