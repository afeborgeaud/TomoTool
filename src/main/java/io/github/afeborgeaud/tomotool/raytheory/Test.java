package io.github.afeborgeaud.tomotool.raytheory;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import io.github.kensuke1984.kibrary.timewindow.TimewindowInformation;
import io.github.kensuke1984.kibrary.timewindow.TimewindowInformationFile;
import io.github.kensuke1984.kibrary.util.HorizontalPosition;
import io.github.kensuke1984.kibrary.util.Station;
import io.github.kensuke1984.kibrary.util.globalcmt.GlobalCMTID;
import io.github.afeborgeaud.tomotool.topoModel.TK10;
import io.github.afeborgeaud.tomotool.topoModel.TestModel;

public class Test {
	
	public static void main(String[] args) throws IOException {
//		testFromSynEvent();
		
		testFromTimewindow(Paths.get("/home/anselme/Dropbox/topo_eth_local/synthetics/timewindow_PKKP_nocut.dat"));
	}
	
	private static void testFromSynEvent() {
		List<RaypathInformation> raypathInformations = new ArrayList<>();
		
		RaypathInformation info = new RaypathInformation(new Station("SYN", new HorizontalPosition(70, 180.), "SYN"),
				new GlobalCMTID("200001010000A")); 
		raypathInformations.add(info);
		
		Traveltime timetool = new Traveltime(raypathInformations, "prem", new TK10(), "PKKP");
		timetool.setIgnoreCMBElevation(false);
		timetool.setIgnoreMantle(true);
		timetool.run();
		for (List<Measurement> measurements : timetool.getMeasurements()) {
			for (Measurement m : measurements) {
				System.out.println("Station: " + m.getStation().getPosition());
				System.out.println("Event: " + m.getGlobalCMTID().getEvent().getCmtLocation());
				System.out.println("Distance: " + m.getEpicentralDistance());
				m.getScatterPointList().forEach(System.out::println);
				System.out.println(m);
			}
		}
	}
	
	private static void testFromTimewindow(Path timewindowPath) throws IOException {
		Set<TimewindowInformation> timewindows = TimewindowInformationFile.read(timewindowPath);
		List<RaypathInformation> raypathInformations = timewindows.stream().limit(3)
				.map(tw -> new RaypathInformation(tw.getStation(), tw.getGlobalCMTID()))
				.collect(Collectors.toList());
		
		Traveltime timetool = new Traveltime(raypathInformations, "prem", new TK10(), "PKKP");
		timetool.setIgnoreCMBElevation(false);
		timetool.setIgnoreMantle(true);
		timetool.run();
		for (List<Measurement> measurements : timetool.getMeasurements()) {
			for (Measurement m : measurements) {
				System.out.println("Station: " + m.getStation().getPosition());
				System.out.println("Event: " + m.getGlobalCMTID().getEvent().getCmtLocation());
				System.out.println("Distance: " + m.getEpicentralDistance());
				m.getScatterPointList().forEach(System.out::println);
				System.out.println(m);
			}
		}
	}
}
