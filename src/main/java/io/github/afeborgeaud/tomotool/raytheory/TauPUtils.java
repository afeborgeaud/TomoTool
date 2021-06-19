package io.github.afeborgeaud.tomotool.raytheory;

import java.awt.Point;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import edu.sc.seis.TauP.Arrival;
import edu.sc.seis.TauP.SeismicPhase;
import edu.sc.seis.TauP.SphericalCoords;
import edu.sc.seis.TauP.TauModel;
import edu.sc.seis.TauP.TauModelException;
import edu.sc.seis.TauP.TauP_Path;
import edu.sc.seis.TauP.TauP_Time;
import edu.sc.seis.TauP.TimeDist;
import io.github.kensuke1984.kibrary.util.HorizontalPosition;
import io.github.kensuke1984.kibrary.util.Location;

public class TauPUtils {
	
	public final static double core_depth = 2891.;
	
	public final static double earth_radius = 6371.;
	
	private TauP_Time timetool;
	
	private Location sourceLoc;
	
	private String[] phaseNames;
	
	private List<Ray> rays;
	
	public TauPUtils(String model) {
		try {
			timetool = new TauP_Time(model);
		} catch (TauModelException e) {
			e.printStackTrace();
		}
	}
	
	public void parsePhasesList(String phasesString) {
		timetool.parsePhaseList(phasesString);
		phaseNames = timetool.getPhaseNames();
	}
	
	public void setCMTLocation(Location sourceLoc) {
		timetool.setSourceDepth(6371. - sourceLoc.getR());
		this.sourceLoc = sourceLoc;
	}
	
	public double getEpicentralDistance(HorizontalPosition staPos) {
		return Math.toDegrees(sourceLoc.getGeographicalDistance(staPos));
	}
	
	public void calculate(HorizontalPosition staPos) {
//		double geographicalDistance = Math.toDegrees(sourceLoc.getGeographicalDistance(staPos));
		double distance = Math.toDegrees(sourceLoc.getEpicentralDistance(staPos));
		double azimuthtmp = Math.toDegrees(sourceLoc.getGeographicalAzimuth(staPos));
		
		
		rays = new ArrayList<TauPUtils.Ray>();
		
		Set<SeismicPhase> processedPhases = new HashSet<>();
		
		boolean majorarc = false;
		try {
			timetool.calculate(distance);
			List<Arrival> arrivals = timetool.getArrivals();
			
//			int count = 0;
			for (Arrival arrival : arrivals) {
				if (processedPhases.contains(arrival.getPhase())) continue;
				processedPhases.add(arrival.getPhase());
//				count++;
//				if (count > 1)
//					continue;
			//TODO include the case of triplications
//			for (Arrival arrival : timetool.getArrivals()) {
				if (arrival.getDistDeg() > distance + 1.)
					majorarc = true;
				else
					majorarc = false;
				
				final double azimuth = majorarc ? azimuthtmp - 180. : azimuthtmp;
//				final double azimuth = azimuthtmp;
				
				List<HorizontalPosition> scatterPoints = Arrays.stream(arrival.getPierce())
					.filter(td -> Math.abs(td.getDepth() - core_depth) < 0.0001)
					.map(td -> {
//						System.out.println(td.getDepth() + " " + td.getDistDeg());
						double lat = SphericalCoords.latFor(sourceLoc.getLatitude(), sourceLoc.getLongitude(), td.getDistDeg(), azimuth);
						double lon = SphericalCoords.lonFor(sourceLoc.getLatitude(), sourceLoc.getLongitude(), td.getDistDeg(), azimuth);
						return new HorizontalPosition(lat, lon);
					}).collect(Collectors.toList());
				String puristPhaseName = arrival.getPuristName();
				if (majorarc) puristPhaseName += "m";
				
				process(scatterPoints, arrival.getRayParam(), arrival.getPath(), staPos, puristPhaseName);
				
			}
		} catch (TauModelException e) {
			e.printStackTrace();
		} catch (Exception e) {
			System.err.println(e.getMessage());
		}
	}
	

//	}
	
	public void process(List<HorizontalPosition> scatterPoints, double rayparam, TimeDist[] pathPoints,
			HorizontalPosition staPos, String phaseName) {
		//Process scatter points
		List<ScatterPoint> scatterList = new ArrayList<>();
		switch (phaseName) {
		case "SKS":
			if (scatterPoints.size() != 2)
				throw new RuntimeException("Unexpected number for " + phaseName + " " + scatterPoints.size());
			scatterList.add(new ScatterPoint(scatterPoints.get(0), rayparam, ScatterType.transmission, WaveType.S));
			scatterList.add(new ScatterPoint(scatterPoints.get(1), rayparam, ScatterType.transmission, WaveType.S));
			break;
		case "SKKS":
			if (scatterPoints.size() != 3)
				throw new RuntimeException("Unexpected number for " + phaseName + " " + scatterPoints.size());
			scatterList.add(new ScatterPoint(scatterPoints.get(0), rayparam, ScatterType.transmission, WaveType.S));
			scatterList.add(new ScatterPoint(scatterPoints.get(1), rayparam, ScatterType.reflection_under, WaveType.S));
			scatterList.add(new ScatterPoint(scatterPoints.get(2), rayparam, ScatterType.transmission, WaveType.S));
			break;
		case "SKKKS":
			if (scatterPoints.size() != 4)
				throw new RuntimeException("Unexpected number for " + phaseName + " " + scatterPoints.size());
			scatterList.add(new ScatterPoint(scatterPoints.get(0), rayparam, ScatterType.transmission, WaveType.S));
			scatterList.add(new ScatterPoint(scatterPoints.get(1), rayparam, ScatterType.reflection_under, WaveType.S));
			scatterList.add(new ScatterPoint(scatterPoints.get(2), rayparam, ScatterType.reflection_under, WaveType.S));
			scatterList.add(new ScatterPoint(scatterPoints.get(3), rayparam, ScatterType.transmission, WaveType.S));
			break;
		case "PKP":
			if (scatterPoints.size() != 2)
				throw new RuntimeException("Unexpected number for " + phaseName + " " + scatterPoints.size());
			scatterList.add(new ScatterPoint(scatterPoints.get(0), rayparam, ScatterType.transmission, WaveType.P));
			scatterList.add(new ScatterPoint(scatterPoints.get(1), rayparam, ScatterType.transmission, WaveType.P));
			break;
		case "PKKP":
			if (scatterPoints.size() != 3)
				throw new RuntimeException("Unexpected number for " + phaseName + " " + scatterPoints.size());
			scatterList.add(new ScatterPoint(scatterPoints.get(0), rayparam, ScatterType.transmission, WaveType.P));
			scatterList.add(new ScatterPoint(scatterPoints.get(1), rayparam, ScatterType.reflection_under, WaveType.P));
			scatterList.add(new ScatterPoint(scatterPoints.get(2), rayparam, ScatterType.transmission, WaveType.P));
			break;
		case "PKKKP":
			if (scatterPoints.size() != 4)
				throw new RuntimeException("Unexpected number for " + phaseName + " " + scatterPoints.size());
			scatterList.add(new ScatterPoint(scatterPoints.get(0), rayparam, ScatterType.transmission, WaveType.P));
			scatterList.add(new ScatterPoint(scatterPoints.get(1), rayparam, ScatterType.reflection_under, WaveType.P));
			scatterList.add(new ScatterPoint(scatterPoints.get(2), rayparam, ScatterType.reflection_under, WaveType.P));
			scatterList.add(new ScatterPoint(scatterPoints.get(3), rayparam, ScatterType.transmission, WaveType.P));
			break;
		case "PKPm":
			if (scatterPoints.size() != 2)
				throw new RuntimeException("Unexpected number for " + phaseName + " " + scatterPoints.size());
			scatterList.add(new ScatterPoint(scatterPoints.get(0), rayparam, ScatterType.transmission, WaveType.P));
			scatterList.add(new ScatterPoint(scatterPoints.get(1), rayparam, ScatterType.transmission, WaveType.P));
			break;
		case "PKKPm":
			if (scatterPoints.size() != 3)
				throw new RuntimeException("Unexpected number for " + phaseName + " " + scatterPoints.size());
			scatterList.add(new ScatterPoint(scatterPoints.get(0), rayparam, ScatterType.transmission, WaveType.P));
			scatterList.add(new ScatterPoint(scatterPoints.get(1), rayparam, ScatterType.reflection_under, WaveType.P));
			scatterList.add(new ScatterPoint(scatterPoints.get(2), rayparam, ScatterType.transmission, WaveType.P));
			break;
		case "PKKKPm":
			if (scatterPoints.size() != 4)
				throw new RuntimeException("Unexpected number for " + phaseName + " " + scatterPoints.size());
			scatterList.add(new ScatterPoint(scatterPoints.get(0), rayparam, ScatterType.transmission, WaveType.P));
			scatterList.add(new ScatterPoint(scatterPoints.get(1), rayparam, ScatterType.reflection_under, WaveType.P));
			scatterList.add(new ScatterPoint(scatterPoints.get(2), rayparam, ScatterType.reflection_under, WaveType.P));
			scatterList.add(new ScatterPoint(scatterPoints.get(3), rayparam, ScatterType.transmission, WaveType.P));
			break;
		case "PKKKKPm":
			if (scatterPoints.size() != 5)
				throw new RuntimeException("Unexpected number for " + phaseName + " " + scatterPoints.size());
			scatterList.add(new ScatterPoint(scatterPoints.get(0), rayparam, ScatterType.transmission, WaveType.P));
			scatterList.add(new ScatterPoint(scatterPoints.get(1), rayparam, ScatterType.reflection_under, WaveType.P));
			scatterList.add(new ScatterPoint(scatterPoints.get(2), rayparam, ScatterType.reflection_under, WaveType.P));
			scatterList.add(new ScatterPoint(scatterPoints.get(3), rayparam, ScatterType.reflection_under, WaveType.P));
			scatterList.add(new ScatterPoint(scatterPoints.get(4), rayparam, ScatterType.transmission, WaveType.P));
			break;
		case "PcP":
			if (scatterPoints.size() != 1)
				throw new RuntimeException("Unexpected number for " + phaseName + " " + scatterPoints.size());
			scatterList.add(new ScatterPoint(scatterPoints.get(0), rayparam, ScatterType.reflection_top, WaveType.P));
			break;
		case "ScS":
			if (scatterPoints.size() != 1)
				throw new RuntimeException("Unexpected number for " + phaseName + " " + scatterPoints.size());
			scatterList.add(new ScatterPoint(scatterPoints.get(0), rayparam, ScatterType.reflection_top, WaveType.S));
			break;
		case "PcS":
			if (scatterPoints.size() != 1)
				throw new RuntimeException("Unexpected number for " + phaseName + " " + scatterPoints.size());
			scatterList.add(new ScatterPoint(scatterPoints.get(0), rayparam, ScatterType.reflection_top, WaveType.PS));
			break;
		case "ScSScS":
			if (scatterPoints.size() != 2)
				throw new RuntimeException("Unexpected number for " + phaseName + " " + scatterPoints.size());
			scatterList.add(new ScatterPoint(scatterPoints.get(0), rayparam, ScatterType.reflection_top, WaveType.S));
			scatterList.add(new ScatterPoint(scatterPoints.get(1), rayparam, ScatterType.reflection_top, WaveType.S));
			break;
		case "ScSScSScS":
			if (scatterPoints.size() != 3)
				throw new RuntimeException("Unexpected number for " + phaseName + " " + scatterPoints.size());
			scatterList.add(new ScatterPoint(scatterPoints.get(0), rayparam, ScatterType.reflection_top, WaveType.S));
			scatterList.add(new ScatterPoint(scatterPoints.get(1), rayparam, ScatterType.reflection_top, WaveType.S));
			scatterList.add(new ScatterPoint(scatterPoints.get(2), rayparam, ScatterType.reflection_top, WaveType.S));
			break;
		case "ScSScSScSScS":
			if (scatterPoints.size() != 4)
				throw new RuntimeException("Unexpected number for " + phaseName + " " + scatterPoints.size());
			scatterList.add(new ScatterPoint(scatterPoints.get(0), rayparam, ScatterType.reflection_top, WaveType.S));
			scatterList.add(new ScatterPoint(scatterPoints.get(1), rayparam, ScatterType.reflection_top, WaveType.S));
			scatterList.add(new ScatterPoint(scatterPoints.get(2), rayparam, ScatterType.reflection_top, WaveType.S));
			scatterList.add(new ScatterPoint(scatterPoints.get(3), rayparam, ScatterType.reflection_top, WaveType.S));
			break;
		case "SKKSm":
			if (scatterPoints.size() != 3)
				throw new RuntimeException("Unexpected number for " + phaseName + " " + scatterPoints.size());
			scatterList.add(new ScatterPoint(scatterPoints.get(0), rayparam, ScatterType.transmission, WaveType.S));
			scatterList.add(new ScatterPoint(scatterPoints.get(1), rayparam, ScatterType.reflection_under, WaveType.S));
			scatterList.add(new ScatterPoint(scatterPoints.get(2), rayparam, ScatterType.transmission, WaveType.S));
			break;
		case "PKiKP":
			if (scatterPoints.size() != 2)
				throw new RuntimeException("Unexpected number for " + phaseName + " " + scatterPoints.size());
			scatterList.add(new ScatterPoint(scatterPoints.get(0), rayparam, ScatterType.transmission, WaveType.P));
			scatterList.add(new ScatterPoint(scatterPoints.get(1), rayparam, ScatterType.transmission, WaveType.P));
			break;
		case "SKSm":
		case "SKKKSm":
		case "P":
		case "S":
		case "ScSm":
		case "ScSScSm":
		case "ScSScSScSm":
			break;
		default:
			throw new RuntimeException(phaseName + " phase not implemented yet");
		}
		
		//Process raypath
		double azimuth = Math.toDegrees(sourceLoc.getGeographicalAzimuth(staPos));
		Location[] raypath = new Location[pathPoints.length];
		for (int i = 0; i < pathPoints.length; i++) {
			TimeDist td = pathPoints[i];
			double lat = SphericalCoords.latFor(sourceLoc.getLatitude(), sourceLoc.getLongitude(), td.getDistDeg(), azimuth);
			double lon = SphericalCoords.lonFor(sourceLoc.getLatitude(), sourceLoc.getLongitude(), td.getDistDeg(), azimuth);
			raypath[i] = new Location(lat, lon, earth_radius - td.getDepth());
		}
		
		Ray ray = new Ray(phaseName, scatterList, raypath);
		rays.add(ray);
	}
	
	private String toPhaseName(String puristPhaseName) {
		if (puristPhaseName.endsWith("m"))
			return puristPhaseName.substring(0, puristPhaseName.length() - 1);
		else
			return puristPhaseName;
	}
	
	public List<Ray> getRays() {
		return rays;
	}
	
	public String[] getPhaseNames() {
		return phaseNames;
	}
	
	public static class Ray {
		List<ScatterPoint> scatterPoints;
		
		Location[] raypath;
		
		String phaseName;
		
		public Ray(String phaseName, List<ScatterPoint> scatterPoints, Location[] raypath) {
			this.phaseName = phaseName;
			this.scatterPoints = scatterPoints;
			this.raypath = raypath;
		}
	}
	
}
