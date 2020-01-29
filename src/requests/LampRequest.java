package requests;

import enums.SystemEnumTypes;

public class LampRequest extends Request {

	/**
	 * The action or status of the lamp
	 */
	private SystemEnumTypes.FloorDirectionLampStatus CurrentStatus;

	public LampRequest(SystemEnumTypes.FloorDirectionLampStatus status) {
		this.CurrentStatus = status;
	}

	public SystemEnumTypes.FloorDirectionLampStatus getCurrentStatus() {
		return CurrentStatus;
	}

	public void setCurrentStatus(SystemEnumTypes.FloorDirectionLampStatus currentStatus) {
		CurrentStatus = currentStatus;
	}
}
