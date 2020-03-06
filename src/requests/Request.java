package requests;

/**
 * This class deals with the requests that are being made
 *
 */
public class Request {
//	private Date time;
	private int floor;
	private String direction;
	private int carButton;
	private String destination;
	private byte[] requestType;
	private String source;
	public String Receiver;

	public String Sender;
	private long startTime, endTime, elapsedTime;

	protected Request() {

	}

	protected Request(String source, String destination) {
		this.destination = destination;
		this.source = source;
	}
	/*
	 * public Request(Date time, int floor, String direction, int carButton) {
	 * this.time = time; this.floor = floor; this.direction = direction;
	 * this.carButton = carButton; }
	 */

	public void setFloor(int floor) {
		this.floor = floor;
	}

	protected void setRequestType(byte[] RequestType) {
		RequestType = new byte[] { RequestType[0], RequestType[1] };
		this.requestType = RequestType;
	}

	public String getSender() {
		return Sender;
	}

	public void setSender(String destinationName) {
		Sender = destinationName;
	}

	public byte[] IGetRequestType() {
		return this.requestType;
	}

	public void setDirec(String direction) {
		if (direction.equals("UP") || direction.equals("DOWN")) {
			this.direction = direction;
		}
	}

	public void setCarButton(int carButton) {
		this.carButton = carButton;
	}

	public int getFloor() {
		return floor;
	}

	public String getDirec() {
		return direction;
	}

	public int getCarButton() {
		return carButton;
	}

	public String getDestination() {
		return this.destination;
	}

	public void setDestination(String destinationName) {
		this.destination = destinationName;
	}

	public String getSource() {
		return this.source;
	}

	public void setSource(String sourceName) {
		source = sourceName;
	}

	/*
	 * public Date getTime() { return time; }
	 */
	public void setStartTime() {
		this.startTime = System.nanoTime();
	}

	public void setEndTime() {
		this.endTime = System.nanoTime();
		this.elapsedTime = this.endTime - this.startTime;
	}

	public double getElapsedTime() {
		return (double) this.elapsedTime / 1000000;
	}

}