package ca.ualberta.physics.cssdp.domain;

import org.joda.time.DateTime;
import org.joda.time.format.ISODateTimeFormat;

import ca.ualberta.physics.cssdp.domain.ServiceStats.ServiceName;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;

/**
 * A struct to hold service info for CANARIE service registry
 * 
 * @author rpotter
 * 
 */
@JsonAutoDetect(fieldVisibility = Visibility.ANY)
public class ServiceInfo {

	public ServiceName name;
	public String synopsis;
	public String version = "1.0";
	public String institution = "University of Alberta, Department of Physics, Space Physics";
	// ISO8601
	public String releaseTime = ISODateTimeFormat.dateTimeNoMillis().withZoneUTC().print(
			new DateTime(2013, 06, 30, 01, 01));

}