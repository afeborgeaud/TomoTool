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

public class RaypathDistributionSimple {
	
	Path rootPath;
	
	List<RaypathInformation> rays;
	
	double distanceMin, distanceMax;
	
	public static void main(String[] args) {
		Path rootPath = Paths.get("/home/anselme/Dropbox/topo_eth_local/eventsmetadata/DATALESS");
		
		double distanceMin = 90.;
		double distanceMax = 140.;
		
		RaypathDistributionSimple rd = new RaypathDistributionSimple(rootPath, distanceMin, distanceMax);
		
		rd.run();
		
		Path outpathEvent = rootPath.resolve(String.format("events_dist_%.0f_%.0f.inf", distanceMin, distanceMax));
		Path outpathStation = rootPath.resolve(String.format("stations_dist_%.0f_%.0f.inf", distanceMin, distanceMax));
		Path outpathInfo = rootPath.resolve(String.format("rayinfo_dist_%.0f_%.0f.inf", distanceMin, distanceMax));
		try {
			rd.writeStations(outpathStation);
			rd.writeEvents(outpathEvent);
			RaypathInformation.writeRaypathInformation(rd.getRays(), outpathInfo);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public RaypathDistributionSimple(Path rootPath, double distanceMin, double distanceMax) {
		this.rootPath = rootPath;
		this.distanceMin = distanceMin;
		this.distanceMax = distanceMax;
		rays = new ArrayList<>();
	}
	
	public void run() {
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
				
				for (Station station : stations) {
					RaypathInformation ray = new RaypathInformation(station, id);
					double distance = ray.getDistanceDegree();
					if (distance >= distanceMin && distance <= distanceMax)
						rays.add(ray);
				}
			}
			
		} catch (IOException e) {
			e.printStackTrace();
		}
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
	
	public List<RaypathInformation> getRays() {
		return rays;
	}

}
