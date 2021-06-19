package io.github.afeborgeaud.tomotool.raytheory;

import java.util.List;

import io.github.kensuke1984.kibrary.util.Station;
import io.github.kensuke1984.kibrary.util.globalcmt.GlobalCMTID;

public class Measurement {
	
	private String phaseName;
	
	private double[] traveltimes;
	
	private Station station;
	
	private GlobalCMTID event;
	
	private List<ScatterPoint> scatterPoints;
	
	public Measurement(Station station, GlobalCMTID event
			, String phaseName, List<ScatterPoint> scatterPoints, double traveltimes[]) {
		this.phaseName = phaseName;
		this.traveltimes = traveltimes;
		this.station = station;
		this.event = event;
		this.scatterPoints = scatterPoints;
	}
	
	public double getEpicentralDistance() {
		return Math.toDegrees(event.getEvent().getCmtLocation().getGeographicalDistance(station.getPosition()));
	}
	
	public double getAzimuth() {
		return Math.toDegrees(event.getEvent().getCmtLocation().getGeographicalAzimuth(station.getPosition()));
	}
	
	public List<ScatterPoint> getScatterPointList() {
		return scatterPoints;
	}
	
	public double getTraveltimePerturbation() {
		return traveltimes[0];
	}
	
	public double getTraveltimePerturbationToPREM() {
		return getAbsoluteTraveltime3D() - getAbsoluteTraveltimePREM();
	}
	
	public double getAbsoluteTraveltimeRef() {
		return traveltimes[1];
	}
	
	public double getAbsoluteTraveltime3D() {
		return getAbsoluteTraveltimeRef() + getTraveltimePerturbation();
	}
	
	public double getAbsoluteTraveltimePREM() {
		return traveltimes[2];
	}
	
	public boolean isSameRecord(Measurement o) {
		return o.event.equals(event) && o.station.equals(station);
	}
	
	public String getPhaseName() {
		return phaseName;
	}
	
	@Override
	public int hashCode() {
		return station.hashCode() + event.hashCode() + phaseName.hashCode();
	}
	
	@Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        Measurement m = (Measurement) o;
        return phaseName.equals(m.phaseName)
        	&& station.equals(m.station)
        	&& event.equals(m.event);
    }
	
	@Override
	public String toString(){
		return station +  " " + event + " " + phaseName + " " + String.format("%.5f", traveltimes[0]);
	}
	
	public String getHashableID() {
		return station.getStringID() + event.toString() + phaseName;
	}
	
	public Station getStation() {
		return station;
	}
	
	public GlobalCMTID getGlobalCMTID() {
		return event;
	}
}
