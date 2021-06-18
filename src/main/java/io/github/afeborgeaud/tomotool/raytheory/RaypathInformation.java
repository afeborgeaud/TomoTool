package io.github.afeborgeaud.tomotool.raytheory;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;

import io.github.kensuke1984.kibrary.timewindow.TimewindowInformationFile;
import io.github.kensuke1984.kibrary.util.HorizontalPosition;
import io.github.kensuke1984.kibrary.util.Location;
import io.github.kensuke1984.kibrary.util.Station;
import io.github.kensuke1984.kibrary.util.globalcmt.GlobalCMTID;

public class RaypathInformation {
	
	private Station station;
	
	private GlobalCMTID event;
	
	public static void main(String[] args) {
		Path timewindowPath = Paths.get("/work/anselme/CA_ANEL_NEW/VERTICAL/syntheticPREM_Q165/filtered_stf_12.5-200s/timewindow_SScS_60deg.dat");
		try {
			List<RaypathInformation> raypaths = readRaypathFromTimewindows(timewindowPath);
			Path outpath = Paths.get("/home/anselme/Dropbox/noise_correlation/EQs/raypaths_from_earthquakes.txt");
			writeRaypathInformation(raypaths, outpath);
		} catch (IOException e) {
			e.printStackTrace();
		}
		
	}
	
	public RaypathInformation(Station station, GlobalCMTID event) {
		this.station = station;
		this.event = event;
	}
	
	public double getDistanceDegree() {
		return Math.toDegrees(event.getEvent().getCmtLocation().getEpicentralDistance(station.getPosition()));
	}
	
	public double getAzimuthDegree() {
		return Math.toDegrees(event.getEvent().getCmtLocation().getAzimuth(station.getPosition()));
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
	
	public static List<RaypathInformation> readRaypathFromTimewindows(Path timewindowPath) throws IOException {
		return TimewindowInformationFile.read(timewindowPath).stream()
			.map(window -> new RaypathInformation(window.getStation(), window.getGlobalCMTID()))
			.collect(Collectors.toList());
	}
	
	public static List<RaypathInformation> readRaypathInformation(Path path) throws IOException {
		return Files.readAllLines(path).stream().map(line -> {
			String[] ss = line.split("\\s+");
			GlobalCMTID event = new GlobalCMTID(ss[0]);
			Station station = new Station(ss[1].trim(),
				new HorizontalPosition(Double.parseDouble(ss[3]), Double.parseDouble(ss[4])), ss[2].trim());
			return new RaypathInformation(station, event);
		}).collect(Collectors.toList());
	}
	
	public static void writeRaypathInformation(List<RaypathInformation> rays, Path outpath) throws IOException {
		PrintWriter pw = new PrintWriter(outpath.toFile());
		rays.stream().forEach(r -> pw.println(r.getEvent() + " " +
				r.getStation().getName() 
				+ " " + r.getStation().getNetwork() + " " + r.getStation().getPosition()));
		pw.close();
	}
	
	@Override
	public String toString() {
		return station.getName() + " " + event;
	}
}
