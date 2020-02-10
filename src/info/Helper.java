package info;

import java.net.DatagramPacket;
import java.util.Arrays;

import enums.SystemEnumTypes;
import requests.DirectionLampRequest;
import requests.ElevatorArrivalRequest;
import requests.ElevatorDestinationRequest;
import requests.ElevatorDoorRequest;
import requests.ElevatorLampRequest;
import requests.ElevatorMotorRequest;
import requests.ElevatorWaitRequest;
import requests.FloorButtonRequest;
import requests.FloorLampRequest;
import requests.LampRequest;
import requests.Request;

public final class Helper {

	public static final int buffer_size = 1024; // Max information to be contained in a datagram packet

	/**
	 * Creates a datagram packet from a request class
	 * 
	 * @param request Request create a packet for
	 * @return Datagram packet containing the data, ready to send (does not contain
	 *         host or port)
	 * @throws InvalidRequestException In case the request contains null
	 *                                 information, it will be invalid
	 */
	public static DatagramPacket CreateRequest(Request request) {
		MutInt counter = new MutInt(0);
		byte[] data = new byte[buffer_size];

		/* Populate a general request */
		// add initial 0 byte
		data[counter.getAndIncrement()] = 0;
		// populate optional params
		PopulateSourceDest(data, request, counter);
		// Populate type
		PopulateType(data, request, counter);
		// Populate based on Type
		PopulateOnType(data, request, counter);

		DatagramPacket packet = new DatagramPacket(data, counter.intValue());
		return packet;

	}

	/**
	 * Parses a request class from a datagram packet's data
	 * 
	 * @param packet packet with data to parse
	 * @return a generic Request object. Can be checked using instanceof to find the
	 *         corresponding request
	 * @throws InvalidRequestException The data in the array was corrupt and could
	 *                                 not fit parse criteria
	 */
	public static Request ParseRequest(DatagramPacket packet) {
		byte[] data = packet.getData();
		// int data_length = packet.getLength();
		// if(data.length != data_length) throw Invalid();
		MutInt counter = new MutInt(0);
		if (data[counter.getAndIncrement()] != 0) {
			System.out.println("Could not parse data. Invalid request.");
		}

		String[] SrcDests = ParseSrcDest(data, counter);

		byte[] RequestType = ParseType(data, counter);
		Request request = ParseOnType(data, RequestType, counter);
		IncludeParams(SrcDests, request);
		return request;
	}

	private static void IncludeParams(String[] arr, Request request) {
		if (arr[0] != "")
			request.Sender = arr[0];
		if (arr[1] != "")
			request.Receiver = arr[1];
	}

	private static String[] ParseSrcDest(byte[] data, MutInt counter) {
		boolean IncludeSrcName = RTF(data[counter.getAndAdd(2)]), IncludeDestName = RTF(data[counter.getAndAdd(2)]);

		String SourceName = "";
		String DestinationName = "";

		if (IncludeSrcName) {
			SourceName = ParseString(data, counter);
		}

		if (IncludeDestName) {
			DestinationName = ParseString(data, counter);
		}

		String[] res = new String[] { SourceName, DestinationName };
		return res;
	}

	private static Request ParseOnType(byte[] data, byte[] rt, MutInt counter) {
		Request request = null;
		if (Arrays.equals(rt, ElevatorLampRequest.getRequestType())) {
			/* Parse based on SystemEnumTypes.Direction Lamp Request */
			SystemEnumTypes.Direction direction = (SystemEnumTypes.Direction) ParseEnum(data,
					SystemEnumTypes.Direction.class, counter);
			SystemEnumTypes.FloorDirectionLampStatus status = (SystemEnumTypes.FloorDirectionLampStatus) ParseEnum(data,
					SystemEnumTypes.FloorDirectionLampStatus.class, counter);
			request = new DirectionLampRequest(direction, status);

		} else if (Arrays.equals(rt, ElevatorArrivalRequest.getRequestType())) {
			/* Parse based on Elevator Arrival Request */
			String ElevatorName = ParseString(data, counter);
			String FloorName = ParseString(data, counter);
			SystemEnumTypes.Direction dir = (SystemEnumTypes.Direction) ParseEnum(data, SystemEnumTypes.Direction.class,
					counter);
			request = new ElevatorArrivalRequest(ElevatorName, FloorName, dir);
		} else if (Arrays.equals(rt, ElevatorDoorRequest.getRequestType())) {
			/* Parse based on Elevator Door Request */
			String ElevatorName = ParseString(data, counter);
			SystemEnumTypes.ElevatorCurrentDoorStatus Action = (SystemEnumTypes.ElevatorCurrentDoorStatus) ParseEnum(
					data, SystemEnumTypes.ElevatorCurrentDoorStatus.class, counter);
			request = new ElevatorDoorRequest(ElevatorName, Action);
		} else if (Arrays.equals(rt, ElevatorLampRequest.getRequestType())) {
			/* Parse based on Elevator Lamp Request */
			String ElevatorName = ParseString(data, counter);
			SystemEnumTypes.FloorDirectionLampStatus status = (SystemEnumTypes.FloorDirectionLampStatus) ParseEnum(data,
					SystemEnumTypes.FloorDirectionLampStatus.class, counter);
			request = new ElevatorLampRequest(ElevatorName, status);

		} else if (Arrays.equals(rt, ElevatorMotorRequest.getRequestType())) {
			/* Parse based on DElevator Motor Request */
			String ElevatorName = ParseString(data, counter);
			SystemEnumTypes.Direction Action = (SystemEnumTypes.Direction) ParseEnum(data,
					SystemEnumTypes.Direction.class, counter);
			request = new ElevatorMotorRequest(ElevatorName, Action);
		} else if (Arrays.equals(rt, FloorButtonRequest.getRequestType())) {
			/* Parse based on Floor Button Request */
			String DateString = ParseString(data, counter);
			String FloorName = ParseString(data, counter);
			SystemEnumTypes.Direction direction = (SystemEnumTypes.Direction) ParseEnum(data,
					SystemEnumTypes.Direction.class, counter);
			String Destination = ParseString(data, counter);
			request = new FloorButtonRequest(DateString, FloorName, direction, Destination);

		} else if (Arrays.equals(rt, FloorLampRequest.getRequestType())) {
			/* Parse based on Floor Lamp Request */
			SystemEnumTypes.Direction Direction = (SystemEnumTypes.Direction) ParseEnum(data,
					SystemEnumTypes.Direction.class, counter);
			SystemEnumTypes.FloorDirectionLampStatus status = (SystemEnumTypes.FloorDirectionLampStatus) ParseEnum(data,
					SystemEnumTypes.FloorDirectionLampStatus.class, counter);
			request = new FloorLampRequest(Direction, status);
		} else if (Arrays.equals(rt, ElevatorDestinationRequest.getRequestType())) {
			/* Parse based on Elevator Destination Request */
			String PickupFloor = ParseString(data, counter);
			String DestFloor = ParseString(data, counter);
			String ElevatorName = ParseString(data, counter);
			request = new ElevatorDestinationRequest(PickupFloor, DestFloor, ElevatorName);
		} else if (Arrays.equals(rt, ElevatorWaitRequest.getRequestType())) {
			/* Parse based on Elevator Wait Request */
			String elevatorName = ParseString(data, counter);
			request = new ElevatorWaitRequest(elevatorName);
		}
		return request;
	}

	private static byte[] ParseType(byte[] data, MutInt counter) {
		byte[] array = new byte[] { data[counter.getAndIncrement()], data[counter.getAndIncrement()] };
		if (data[counter.intValue()] == 0)
			counter.getAndIncrement();
		else
			System.out.println("Could not parse type of request. Data was invalid.");
		return array;
	}

	private static String ParseString(byte[] data, MutInt counter) {
		String ret = "";
		if (data[counter.intValue()] != 0) {
			// System.out.println("data: "+data[counter.intValue()]);
			if (data[counter.intValue()] == (byte) -1) {
				counter.add(2);
				return ret;
			}
			// attempt to parse data
			MutInt temp_counter = new MutInt(counter);
			while (temp_counter.intValue() != data.length && data[temp_counter.getAndIncrement()] != 0)
				;

			ret = new String(Arrays.copyOfRange(data, counter.intValue(), temp_counter.intValue() - 1));
			counter.setValue(temp_counter);
		}
		return ret;
	}

	private static <T extends Enum<T>> Enum<?> ParseEnum(byte[] data, Class<T> clazz, MutInt counter) {
		Enum<?>[] enums = clazz.getEnumConstants();
		if ((((int) data[counter.intValue()]) - 1) < enums.length) {
			return enums[((int) data[counter.getAndAdd(2)]) - 1];
		} else
			System.out.println("Could not parse Enum; Invalid data or Enum does not exist.");
		return null;
	}

	private static void PopulateSourceDest(byte[] data, Request request, MutInt counter) {

		boolean IncludeSrcName = request.Sender != null && !request.Sender.isEmpty(),
				IncludeDestName = request.Receiver != null && !request.Receiver.isEmpty();

		Populate(data, TF(IncludeSrcName), counter);
		Populate(data, TF(IncludeDestName), counter);

		// populate sender name with a string
		if (IncludeSrcName) {
			String SenderName = request.Sender;
			Populate(data, SenderName, counter);
		}
		// Populate Receiver name
		if (IncludeDestName) {
			String ReceiverName = request.Receiver;
			Populate(data, ReceiverName, counter);
		}
	}

	private static String TF(boolean tf) {
		if (tf)
			return "T";
		else
			return "F";
	}

	private static boolean RTF(byte tf) {
		if (tf == 'T')
			return true;
		else
			return false;
	}

	private static void PopulateEnum(byte[] data, Enum<?> E, MutInt counter) {
		data[counter.getAndIncrement()] = (byte) (E.ordinal() + 1); // add 1 to avoid 0-ordinal values
		data[counter.getAndIncrement()] = 0;
	}

	private static void PopulateOnType(byte[] data, Request request, MutInt counter) {
		if (request instanceof ElevatorLampRequest) {
			/* SystemEnumTypes.Direction Lamp Request is of the form 0DIR0STATUS0ACTION */
			ElevatorLampRequest req = (ElevatorLampRequest) request;
			PopulateEnum(data, req.getCurrentStatus(), counter);
			PopulateEnum(data, req.getCurrentStatus(), counter);

		} else if (request instanceof ElevatorArrivalRequest) {
			/* Elevator Arrival Request is of form 0E_NAME0FLOOR_NAME0 */
			ElevatorArrivalRequest req = (ElevatorArrivalRequest) request;
			Populate(data, req.getElevatorName(), counter);
			Populate(data, req.getFloorName(), counter);
			PopulateEnum(data, req.getDirection(), counter);

		} else if (request instanceof ElevatorDoorRequest) {
			/* Elevator Door Request is of form 0E_NAME0ACTION0 */
			ElevatorDoorRequest req = (ElevatorDoorRequest) request;
			if (req.getRequestAction() == null)
				System.out.println("The request's action is null. Could not populate.");
			Populate(data, req.getElevatorName(), counter);
			PopulateEnum(data, req.getRequestAction(), counter);
		} else if (request instanceof ElevatorLampRequest) {
			/* Elevator Lamp Request is of the form 0E_NAME0E_BUTTON0STATUS0ACTION */
			ElevatorLampRequest req = (ElevatorLampRequest) request;

			Populate(data, req.getElevatorButton(), counter);
			PopulateEnum(data, req.getCurrentStatus(), counter);
		} else if (request instanceof ElevatorMotorRequest) {
			/* Elevator Motor Request is of the form 0E_NAME0ACTION0 */
			ElevatorMotorRequest req = (ElevatorMotorRequest) request;
			Populate(data, req.getElevatorName(), counter);
			PopulateEnum(data, req.getRequestAction(), counter);
		} else if (request instanceof FloorButtonRequest) {
			/* Floor Button Request is of the form 0DATE0FLOOR0DIRECTION0DESTINATION0 */
			FloorButtonRequest req = (FloorButtonRequest) request;
			Populate(data, req.getButtonPressTime(), counter);
			Populate(data, req.getFloorName(), counter);
			PopulateEnum(data, req.getDirection(), counter);
			Populate(data, req.getDestinationFloor(), counter);
		} else if (request instanceof FloorLampRequest) {
			/* Floor Button Request is of the form 0DIRECTION0ACTION0 */
			FloorLampRequest req = (FloorLampRequest) request;
			PopulateEnum(data, req.getDirection(), counter);
			PopulateEnum(data, req.getCurrentStatus(), counter);
		} else if (request instanceof ElevatorDestinationRequest) {
			/* Floor Button Request is of the form 0FLOOR0 */
			ElevatorDestinationRequest req = (ElevatorDestinationRequest) request;
			Populate(data, req.getPickupFloor(), counter);
			Populate(data, req.getDestinationFloor(), counter);
			Populate(data, req.getElevatorName(), counter);
		} else if (request instanceof ElevatorWaitRequest) {
			/* Floor Button Request is of the form 0DIRECTION0ACTION0 */
			ElevatorWaitRequest req = (ElevatorWaitRequest) request;
			Populate(data, req.getElevatorName(), counter);
		}
	}

	/**
	 * 
	 * @param data
	 * @param request
	 * @param counter
	 */
	private static void PopulateType(byte[] data, Request request, MutInt counter) {
		byte[] TypeCode = request.IGetRequestType();
		data[counter.getAndIncrement()] = TypeCode[0];
		data[counter.getAndIncrement()] = TypeCode[1];
		data[counter.getAndIncrement()] = 0;

	}

	private static void Populate(byte[] array, String array2, MutInt pos) {
		if (array2 == null) {
			array[pos.getAndIncrement()] = (byte) -1;
		} else {
			CopyArray(array, array2.getBytes(), pos);
		}
		array[pos.getAndIncrement()] = 0;
	}

	private static void CopyArray(byte[] array, byte[] array2, MutInt pos) {
		for (int i = 0; i < array2.length; i++) {
			array[i + pos.intValue()] = array2[i];
		}
		pos.add(array2.length);
	}

}
