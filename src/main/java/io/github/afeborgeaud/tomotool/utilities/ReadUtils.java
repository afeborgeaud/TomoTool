package io.github.afeborgeaud.tomotool.utilities;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import io.github.kensuke1984.kibrary.util.HorizontalPosition;
import io.github.kensuke1984.kibrary.util.Station;
import io.github.kensuke1984.kibrary.util.globalcmt.GlobalCMTID;
import io.github.kensuke1984.kibrary.util.globalcmt.GlobalCMTSearch;

public class ReadUtils {
	
	public static Set<GlobalCMTID> readGlobalCMTIDFile(Path path) throws IOException {
		return Files.readAllLines(path).stream().map(line -> new GlobalCMTID(line.trim()))
			.collect(Collectors.toSet());
	}
	
	public static Set<GlobalCMTID> readEventFile(Path path) throws IOException {
		return Files.readAllLines(path).stream().map(line -> {
			String[] ss = line.split("\\s+");
			int year = Integer.parseInt(ss[0].substring(0, 4));
			int month = Integer.parseInt(ss[0].substring(4, 6));
			int day = Integer.parseInt(ss[0].substring(6, 8));
			int hour = Integer.parseInt(ss[0].substring(9, 11));
			int minute = Integer.parseInt(ss[0].substring(11, 13));
			double lat = Double.parseDouble(ss[1]);
			double lon = Double.parseDouble(ss[2]);
			double depth = Double.parseDouble(ss[3]);
			
			GlobalCMTSearch cmtSea = new GlobalCMTSearch(LocalDate.of(year, month, day), LocalDate.of(year, month, day));
			cmtSea.setLatitudeRange(Math.max(-90, lat - 3), Math.min(90, lat + 3));
			cmtSea.setLongitudeRange(Math.max(-180, lon - 3), Math.min(359.999, lon + 3));
			cmtSea.setDepthRange(Math.max(0., depth - 35), depth + 35);
			GlobalCMTID id = null;
			try {
				id = cmtSea.search().stream().filter(tmp -> tmp.getEvent().getCMTTime().getHour() - hour == 0 
						&& Math.abs(tmp.getEvent().getCMTTime().getMinute() - minute) <= 1)
						.findAny().get();
			} catch (RuntimeException e) {
				System.out.println("No id found for " + line);
			}
			
			return id;
		}).filter(id -> id != null).collect(Collectors.toSet());
	}
	
	public static Set<GlobalCMTID> readSimpleEventFile(Path path) throws IOException {
		return Files.readAllLines(path).stream().map(line -> {
			String[] ss = line.split("\\s+");
			return new GlobalCMTID(ss[0].trim());
		}).collect(Collectors.toSet());
	}
	
	public static Set<Station> readStationFile(Path path) throws IOException {
		return Files.readAllLines(path).stream().map(line -> {
			String[] ss = line.split("\\s+");
			return new Station(ss[0].trim(), new HorizontalPosition(Double.parseDouble(ss[2]), Double.parseDouble(ss[3])), ss[1].trim());
		}).collect(Collectors.toSet());
	}
	
	public static boolean isSphFile(String path) {
		try {
			int[] nCoeffs = Files.readAllLines(Paths.get(path)).stream()
					.filter(line -> !line.startsWith("#"))
					.mapToInt(line -> line.trim().split("\\s+").length).toArray();
			for (int i = 0; i < nCoeffs.length; i++) {
				if (nCoeffs[i] != 2*i + 1) return false;
			}
		} catch (Exception e) {
			return false;
		}
		return true;
	}
	
	public static boolean isSphSpecfemFile(String path) {
		try {
			BufferedReader bufferedReader = 
		            new BufferedReader(new FileReader(path));
			double lmax = Double.parseDouble(bufferedReader.readLine().trim());
			bufferedReader.close();
			
			int[] nCoeffs = Files.readAllLines(Paths.get(path)).stream()
					.filter(line -> !line.startsWith("#"))
					.mapToInt(line -> line.trim().split("\\s+").length).toArray();
			if (nCoeffs[0] != 1) return false;
			if (nCoeffs.length != lmax + 2) return false;
			for (int i = 1; i < nCoeffs.length; i++) {
				if (nCoeffs[i] != lmax + 1) return false;
			}
		} catch (Exception e) {
			return false;
		}
		return true;
	}
	
	public static List<List<Double>> readSphFile(String path) {
		List<List<Double>> coeffs = new ArrayList<>();
		try {
			for (String line : Files.readAllLines(Paths.get(path))) {
				coeffs.add(Arrays.stream(line.trim().split(" ")).map(s -> Double.parseDouble(s)).collect(Collectors.toList()));
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		return coeffs;
	}
	
	public static List<List<Double>> readSphFile(InputStream is) {
		BufferedReader bufferedReader = 
	            new BufferedReader(new InputStreamReader(is));
		List<List<Double>> coeffs = new ArrayList<>();
		String line = "";
		try {
		while((line = bufferedReader.readLine()) != null) {
			coeffs.add(Arrays.stream(line.trim().split(" ")).map(s -> Double.parseDouble(s)).collect(Collectors.toList()));
		}
		} catch (IOException e) {
			e.printStackTrace();
		}
		return coeffs;
	}
	
	public static List<List<Double>> readSphFile_specfem(InputStream is) {
		BufferedReader bufferedReader = 
	            new BufferedReader(new InputStreamReader(is));
		double lmax = 0;
		List<Double> tmps = new ArrayList<>();
		try {
			lmax = Double.parseDouble(bufferedReader.readLine().trim());
			String line = "";
			while((line = bufferedReader.readLine()) != null) {
				Arrays.stream(line.trim().split(" ")).forEach(s -> tmps.add(Double.parseDouble(s)));
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		List<List<Double>> coeffs = new ArrayList<>();
		int count = 0;
		for (int l = 0; l < lmax + 1; l++) {
			List<Double> cs = new ArrayList<Double>();
			for (int m = 0; m < 2*l+1; m++) {
				cs.add(tmps.get(count));
				count++;
			}
			coeffs.add(cs);
		}
		return coeffs;
	}
	
	public static List<List<Double>> readSphFile_specfem(String model_path) {
		double lmax = 0;
		List<Double> tmps = new ArrayList<>();
		try (BufferedReader bufferedReader = 
		            new BufferedReader(new FileReader(model_path))) {
			lmax = Double.parseDouble(bufferedReader.readLine().trim());
			String line = "";
			while((line = bufferedReader.readLine()) != null) {
				Arrays.stream(line.trim().split(" ")).forEach(s -> tmps.add(Double.parseDouble(s)));
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		List<List<Double>> coeffs = new ArrayList<>();
		int count = 0;
		for (int l = 0; l < lmax + 1; l++) {
			List<Double> cs = new ArrayList<Double>();
			for (int m = 0; m < 2*l+1; m++) {
				cs.add(tmps.get(count));
				count++;
			}
			coeffs.add(cs);
		}
		return coeffs;
	}
}
