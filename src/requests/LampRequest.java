package requests;

import enums.SystemEnumTypes;

/**
 * This class deals with the LampRequest
 *
 */
public class LampRequest extends Request {

	// lamp current status
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
