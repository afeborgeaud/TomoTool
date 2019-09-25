package raytheory;

import io.github.kensuke1984.kibrary.util.HorizontalPosition;
import io.github.kensuke1984.kibrary.util.Location;
import io.github.kensuke1984.kibrary.util.Station;
import io.github.kensuke1984.kibrary.util.globalcmt.GlobalCMTID;

public class RaypathInformation {
	
	private Station station;
	
	private GlobalCMTID event;
	
	public RaypathInformation(Station station, GlobalCMTID event) {
		this.station = station;
		this.event = event;
	}
	
	public double getDistanceDegree() {
		return Math.toDegrees(event.getEvent().getCmtLocation().getEpicentralDistance(station.getPosition()));
	}
	
	public Location getCmtLocation() {
		return event.getEvent().getCmtLocation();
	}
	
	public HorizontalPosition getStationPosition() {
		return station.getPosition();
	}
	
	public Station getStation() {
		return station;
	}
	
	public GlobalCMTID getEvent() {
		return event;
	}
}
