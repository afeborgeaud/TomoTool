package work1;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import edu.sc.seis.TauP.SphericalCoords;
import edu.sc.seis.TauP.TauModelException;
import edu.sc.seis.TauP.TauP_Time;
import edu.sc.seis.TauP.TimeDist;
import io.github.kensuke1984.kibrary.util.HorizontalPosition;
import io.github.kensuke1984.kibrary.util.Station;
import io.github.kensuke1984.kibrary.util.globalcmt.GlobalCMTID;
import raytheory.RaypathInformation;

public class RaypathDistribution {
	
	Path rootPath;
	
	double pierceDepth;
	
	List<RaypathInformation> rays;
	
	List<List<HorizontalPosition>> piercePoints;
	
	public static void main(String[] args) {
		Path rootPath = Paths.get("/home/anselme/Dropbox/topo_eth_local/eventsmetadata/DATALESS");
		double pierceDepth = 2491.;
		
		RaypathDistribution rd = new RaypathDistribution(rootPath, pierceDepth);
		
		rd.run();
		
		Path outpath = rootPath.resolve("raypaths_ScS.inf");
		Path outpathEvent = rootPath.resolve("events.inf");
		Path outpathStation = rootPath.resolve("stations_ScS.inf");
		Path outpathSamplingMap = rootPath.resolve("sampling_ScS.dat");
		Path outpathInfo = rootPath.resolve("rayinfo_ScS.inf");
		try {
			rd.write(outpath);
			rd.writeStations(outpathStation);
			rd.writeEvents(outpathEvent);
			rd.writeSamplingMap(outpathSamplingMap);
			RaypathInformation.writeRaypathInformation(rd.getRays(), outpathInfo);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public RaypathDistribution(Path rootPath, double pierceDepth) {
		this.rootPath = rootPath;
		this.pierceDepth = pierceDepth;
		
		rays = new ArrayList<>();
		piercePoints = new ArrayList<>();
	}
	
	public void run() {
		TauP_Time timetool = null;
		try {
			timetool = new TauP_Time("prem");
		} catch (TauModelException e1) {
			e1.printStackTrace();
		}
		timetool.parsePhaseList("ScS");
		
		try {
			List<Path> paths = Files.list(rootPath).filter(f -> f.toString().endsWith("stations"))
					.collect(Collectors.toList());
			for (Path path : paths) {
				GlobalCMTID id = new GlobalCMTID(path.getFileName().toString().split("\\.")[0]);
				List<Station> stations = Files.readAllLines(path).stream().map(line -> {
					String[] ss = line.split("\\s+");
					Station station = new Station(ss[0], new HorizontalPosition(Double.parseDouble(ss[2]), Double.parseDouble(ss[3]))
							, ss[1]);
					return station;
				}).collect(Collectors.toList());
				
				timetool.setSourceDepth(6371. - id.getEvent().getCmtLocation().getR());
				
				for (Station station : stations) {
					RaypathInformation ray = new RaypathInformation(station, id);
					timetool.calculate(ray.getDistanceDegree());
					List<Double> pierceDists =  new ArrayList<Double>();
					if (timetool.getNumArrivals() == 0) {
						System.err.println(ray + " " + ray.getDistanceDegree());
						continue;
					}
					TimeDist[] raypath = timetool.getArrival(0).getPath();
					for (int i = 0; i < raypath.length - 1; i++) {
						if ((raypath[i].getDepth() - pierceDepth) * (raypath[i+1].getDepth() - pierceDepth) < 0) {
							pierceDists.add((raypath[i].getDistDeg() + raypath[i+1].getDistDeg()) / 2.);
						}
					}
					if (pierceDists.size() != 2) throw new RuntimeException(ray.toString());
					double inLat = SphericalCoords.latFor(id.getEvent().getCmtLocation().getLatitude(), id.getEvent().getCmtLocation().getLongitude()
							, pierceDists.get(0), ray.getAzimuthDegree());
					double inLon = SphericalCoords.lonFor(id.getEvent().getCmtLocation().getLatitude(), id.getEvent().getCmtLocation().getLongitude()
							, pierceDists.get(0), ray.getAzimuthDegree());
					double outLat = SphericalCoords.latFor(id.getEvent().getCmtLocation().getLatitude(), id.getEvent().getCmtLocation().getLongitude()
							, pierceDists.get(1), ray.getAzimuthDegree());
					double outLon = SphericalCoords.lonFor(id.getEvent().getCmtLocation().getLatitude(), id.getEvent().getCmtLocation().getLongitude()
							, pierceDists.get(1), ray.getAzimuthDegree());
					
					List<HorizontalPosition> pierces = new ArrayList<>();
					pierces.add(new HorizontalPosition(inLat, inLon));
					pierces.add(new HorizontalPosition(outLat, outLon));
					piercePoints.add(pierces);
					
					rays.add(ray);
				}
			}
			
		} catch (IOException e) {
			e.printStackTrace();
		} catch (TauModelException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public void write(Path outpath) throws IOException {
		PrintWriter pw = new PrintWriter(outpath.toFile());
		for (int i = 0; i < rays.size(); i++) {
			RaypathInformation ray = rays.get(i);
			pw.println(ray.getEvent() + " " + ray.getStation().getStationName() + " " + ray.getStation().getNetwork() + " "
					+ piercePoints.get(i).get(0) + " " + piercePoints.get(i).get(1));
		}
		pw.close();
	}
	
	public void writeStations(Path outpath) throws IOException {
		PrintWriter pw = new PrintWriter(outpath.toFile());
		rays.stream().map(ray -> ray.getStation()).collect(Collectors.toSet()).forEach(station -> {
			pw.println(station.getStationName() + " " + station.getNetwork() + " " + station.getPosition());
		});
		pw.close();
	}
	
	public void writeEvents(Path outpath) throws IOException {
		PrintWriter pw = new PrintWriter(outpath.toFile());
		rays.stream().map(ray -> ray.getEvent()).collect(Collectors.toSet()).forEach(event -> {
			pw.println(event + " " + event.getEvent().getCmtLocation());
		});
		pw.close();
	}
	
	public void writeSamplingMap(Path outpath) throws IOException {
		List<HorizontalPosition> grid = new ArrayList<>();
		Map<HorizontalPosition, Integer> sampling = new HashMap<HorizontalPosition, Integer>();
		double dl = 2;
		int nlat = (int) (180 / dl);
		int nlon = (int) (360 / dl);
		for (int i = 0; i < nlat; i++) {
			for (int j = 0; j < nlon; j++) {
				grid.add(new HorizontalPosition(-90 + i * dl, -180 + j * dl));
			}
		}
		grid.stream().forEach(p -> sampling.put(p, 0));
		for (List<HorizontalPosition> pierces : piercePoints) {
			grid.parallelStream().filter(p -> {
				double b = pierces.get(1).getEpicentralDistance(p);
				double c2 = pierces.get(1).getEpicentralDistance(pierces.get(0));
				double a2 = pierces.get(0).getEpicentralDistance(p);
				double cosA = (Math.cos(a2) - Math.cos(b) * Math.cos(c2)) / (Math.sin(b) * Math.sin(c2));
				if (cosA <= 0)
					return false;
				double A = Math.acos(cosA);
				double a = Math.abs(Math.asin(Math.sin(A) * Math.sin(b)));
				double c = Math.abs(Math.acos(Math.cos(b) / Math.cos(a)));
				if (c > c2)
					return false;
				c = Math.toDegrees(c);
				double azimuth = Math.toDegrees(pierces.get(1).getAzimuth(pierces.get(0)));
				double lat = SphericalCoords.latFor(pierces.get(1).getLatitude(), pierces.get(1).getLongitude(), c, azimuth);
				double lon = SphericalCoords.lonFor(pierces.get(1).getLatitude(), pierces.get(1).getLongitude(), c, azimuth);
				if (Double.isNaN(lat) || Double.isNaN(lon)) {
					System.err.println(p + " " + c2);
					return false;
				}
				HorizontalPosition pp = new HorizontalPosition(lat, lon);
				if (Math.toDegrees(p.getEpicentralDistance(pp)) <= dl) 
					return true;
				else 
					return false;
			})
			.forEach(p -> {
				int n = sampling.get(p);
				n += 1;
				sampling.replace(p, n);
			});
		}
		
		PrintWriter pw = new PrintWriter(outpath.toFile());
		for (HorizontalPosition p : grid) {
			pw.println(p + " " + sampling.get(p));
		}
		pw.close();
	}
	
	public List<List<HorizontalPosition>> getPiercePoints() {
		return piercePoints;
	}
	
	public List<RaypathInformation> getRays() {
		return rays;
	}

}
