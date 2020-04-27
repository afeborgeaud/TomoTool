package work1;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.Set;

import io.github.kensuke1984.kibrary.util.HorizontalPosition;
import io.github.kensuke1984.kibrary.util.Station;

public class StationGrid {

	public static void main(String[] args) {
		Path stationFile = Paths.get("STATIONS");
		writeStation_specfem(grid1_specfem(stationFile), stationFile);
		
	}

	public static Set<Station> grid1_specfem(Path outpath) {
		double lonmin = -130;
		double lonmax = -50;
		double dlon = 4.;
		
		double latmin = -20;
		double latmax = 80;
		double dlat = 1.;
		
		int nlon = (int) ((lonmax - lonmin) / dlon) + 1;
		int nlat = (int) ((latmax - latmin) / dlat) + 1;
		
		Set<Station> stations = new HashSet<>();
		
		for (int ilon = 0; ilon < nlon; ilon++) {
			for (int ilat = 0; ilat < nlat; ilat++) {
				HorizontalPosition position = new HorizontalPosition(latmin + ilat * dlat, lonmin + ilon * dlon);
				Station station = new Station(String.format("S%04d", ilat + ilon * nlat), position, "SYN");
				stations.add(station);
			}
		}
		
		return stations;
	}
	
	public static void writeStation_specfem(Set<Station> stations, Path stationFile) {
		try (BufferedWriter writer = Files.newBufferedWriter(stationFile)) {
			for (Station sta : stations) {
				writer.write(String.format("%s %s %.3f %.3f 0.0 0.0%n"
					, sta.getStationName()
					, sta.getNetwork()
					, sta.getPosition().getLatitude()
					, sta.getPosition().getLongitude())
				);
			}
		} catch (IOException e) {
			System.err.format("IOException: %s%n", e);
		}
	}
	
}
