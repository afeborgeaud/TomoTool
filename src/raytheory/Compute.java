package raytheory;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import edu.sc.seis.TauP.TauModelException;
import edu.sc.seis.TauP.TauP_Time;
import io.github.kensuke1984.anisotime.Phase;
import io.github.kensuke1984.kibrary.datacorrection.StaticCorrection;
import io.github.kensuke1984.kibrary.datacorrection.StaticCorrectionFile;
import io.github.kensuke1984.kibrary.timewindow.TimewindowInformation;
import io.github.kensuke1984.kibrary.timewindow.TimewindowInformationFile;
import io.github.kensuke1984.kibrary.util.HorizontalPosition;
import io.github.kensuke1984.kibrary.util.Station;
import io.github.kensuke1984.kibrary.util.globalcmt.GlobalCMTID;
import io.github.kensuke1984.kibrary.util.sac.SACComponent;
import topoModel.LLNLG3DJPS;
import topoModel.SEMUCBWM1;
import topoModel.Seismic3Dmodel;
import topoModel.TK10;
import utilities.ReadUtils;

public class Compute {

	public static void main(String[] args) throws IOException {
		if (args.length == 0)
			Compute_SmKS();
		
		Path timewindowPath = Paths.get(args[0]);
//		Path timewindowPath = Paths.get("/work/anselme/CA_ANEL_NEW/syntheticPREM/filtered_stf_12.5-200s/timewindow_additional.dat");
		
		String threeDmodel = args[1].trim().toLowerCase();
		
		String phase = args[2].trim().toLowerCase();
		if (phase.equals("pcp")) {
			System.out.println("Compute PcP");
			Compute_PcP(timewindowPath, threeDmodel);
		}
		else if (phase.equals("scs")) {
			System.out.println("Compute ScS");
			Compute_ScS(timewindowPath, threeDmodel);
		}
		else if (phase.equals("test")) {
			System.out.println("test");
			try {
				test(timewindowPath);
			} catch (TauModelException e) {
				e.printStackTrace();
			}
		}
	}
	
	public static void Compute_SmKS() throws IOException {
//		Path workdir = Paths.get("/work/anselme/TOPO");
//		Path resourcePath =  new File("resources").getAbsoluteFile().toPath();
//		Path eventFile = resourcePath.resolve("events_java.inf"); //events.inf
//		Path stationFile = resourcePath.resolve("station_usarray.inf"); // station_usarray.inf station_fnet.inf
		
		//test Wen-che
//		eventFile = resourcePath.resolve("events_wenche_1.inf");// events_wenche_1.inf events_wenche_small.inf
//		stationFile = resourcePath.resolve("stations_test_wenche.inf");
//		Set<Station> stationSet = ReadUtils.readStationFile(stationFile);
//		Set<GlobalCMTID> eventSet = ReadUtils.readEventFile(eventFile);
//		
//		workdir = Paths.get("/Users/navy/Dropbox/Frederic/project/topography_CMB/calculations");
		
		// ---
		Path workdir = Paths.get("/work/anselme/TOPO/ETH/dataset");
		Path eventFile = workdir.resolve("evt_dataset_1.inf");
//		Path eventFile = resourcePath.resolve("evt_dataset_tonga.inf");
		Path stationFile = workdir.resolve("sta_dataset_1.inf");
		Set<Station> stationSet = ReadUtils.readStationFile(stationFile);
		Set<GlobalCMTID> eventSet = ReadUtils.readSimpleEventFile(eventFile);
		
		double minDistance = 90;
		double maxDistance = 140;
		
		//Select raypaths
		Set<RaypathInformation> raypathInformations = new HashSet<>();
		for (GlobalCMTID event : eventSet) {
			for (Station station : stationSet) {
				RaypathInformation raypathInformation = new RaypathInformation(station, event);
				double distance = raypathInformation.getDistanceDegree();
				boolean userecord = true;
				if (distance < minDistance || distance > maxDistance)
					userecord = false;
				if (userecord) {
					raypathInformations.add(raypathInformation);
				}
			}
		}
		
		String modelName = "prem";
		
//		Seismic3Dmodel seismic3Dmodel = new SEMUCBWM1();
		Seismic3Dmodel seismic3Dmodel = new TK10();
		
		Traveltime traveltimetool = new Traveltime(raypathInformations, modelName, seismic3Dmodel, "SKS, SKKS, SKKKS");
		
		traveltimetool.setIgnoreMantle(true);
		traveltimetool.setIgnoreCMBElevation(false);
		
		traveltimetool.run();
		List<List<Measurement>> measurements = traveltimetool.getMeasurements();
		
		List<Measurement> measurements_SKS = new ArrayList<>();
		List<Measurement> measurements_SKKS = new ArrayList<>();
		List<Measurement> measurements_S3KS = new ArrayList<>();
		for (List<Measurement> record : measurements) {
			Set<String> phases = record.stream().map(p -> p.getPhaseName()).collect(Collectors.toSet());
			if (!(phases.contains("SKS") && phases.contains("SKKS") && phases.contains("SKKKS"))) {
				System.err.println(record);
				continue;
			}
			for (Measurement m : record) {
				if (m.getPhaseName().equals("SKS"))
					measurements_SKS.add(m);
				else if (m.getPhaseName().equals("SKKS"))
					measurements_SKKS.add(m);
				else if (m.getPhaseName().equals("SKKKS"))
					measurements_S3KS.add(m);
			}
		}
		
//		Path modelOutPath = workdir.resolve("sh18cex.dat");
//		traveltimetool.getSeismic3Dmodel().writeCMBElevationMap(modelOutPath);
		
//		Path eventPath = workdir.resolve("events.dat");
//		traveltimetool.writeEventInformation(eventPath);
		
//		Path stationPath = workdir.resolve("stations.dat");
//		traveltimetool.writeStationInformation(stationPath);
		
		Path pierceSKSPath = workdir.resolve("piercepoints_SKS.dat");
		Path undersideSKKSPath = workdir.resolve("undersidepoints_SKKS.dat");
		Path pierceSKKSPath = workdir.resolve("piercepoints_SKKS.dat");
//		Path timeSKKS = workdir.resolve("traveltime_SKKS-SKS_mantleOnly_dataset1.dat");
//		Path timeS3KS = workdir.resolve("traveltime_S3KS-SKKS_mantleOnly_dataset1.dat");
		Path timeSKKS = workdir.resolve("traveltime_SKKS-SKS_dataset1.dat");
		Path timeS3KS = workdir.resolve("traveltime_S3KS-SKKS_dataset1.dat");
		PrintWriter pwSKS = new PrintWriter(pierceSKSPath.toFile());
		PrintWriter pwSKKS = new PrintWriter(undersideSKKSPath.toFile());
		PrintWriter pwPierceSKKS = new PrintWriter(pierceSKKSPath.toFile());
		PrintWriter pwTimeSKKS = new PrintWriter(timeSKKS.toFile());
		PrintWriter pwTimeS3KS = new PrintWriter(timeS3KS.toFile());
		for (int i = 0; i < measurements_SKS.size(); i++) {
			Measurement mSKS = measurements_SKS.get(i);
			Measurement mSKKS = measurements_SKKS.get(i);
			Measurement mS3KS = measurements_S3KS.get(i);
			mSKS.getScatterPointList().forEach(p -> pwSKS.println(p.getPosition().getLongitude() + " " + p.getPosition().getLatitude()));
			mSKKS.getScatterPointList().stream().filter(p -> p.getType().equals(ScatterType.reflection_under)).forEach(p -> pwSKKS.println(p.getPosition().getLongitude() + " " + p.getPosition().getLatitude()));
			mSKKS.getScatterPointList().stream().filter(p -> p.getType().equals(ScatterType.transmission)).forEach(p -> pwPierceSKKS.println(p.getPosition().getLongitude() + " " + p.getPosition().getLatitude()));
			HorizontalPosition reflectionPointSKKS = mSKKS.getScatterPointList().stream().filter(p -> p.getType().equals(ScatterType.reflection_under)).map(p ->p.getPosition()).findAny().get();
			pwTimeSKKS.println(reflectionPointSKKS + " " + mSKKS.getEpicentralDistance() + " " + (mSKKS.getTraveltimePerturbation() - mSKS.getTraveltimePerturbation()));
			pwTimeS3KS.println(reflectionPointSKKS + " " + mS3KS.getEpicentralDistance() + " " + (mS3KS.getTraveltimePerturbation() - mSKKS.getTraveltimePerturbation()));
		}
		pwSKS.close();
		pwSKKS.close();
		pwTimeSKKS.close();
		pwPierceSKKS.close();
		pwTimeS3KS.close();
	}
	
	public static void Compute_PmKP() throws IOException {
		
	}
	
	public static void Compute_PcP(Path timewindowPath, String threeDmodel) throws IOException {
		Set<TimewindowInformation> timewindows = TimewindowInformationFile.read(timewindowPath);
		
		//Select raypaths
		Set<RaypathInformation> raypathInformations = timewindows.stream()
				.map(tw -> new RaypathInformation(tw.getStation(), tw.getGlobalCMTID()))
				.collect(Collectors.toSet());
		
		String modelName = "prem";
		
		Seismic3Dmodel seismic3Dmodel = null;
		switch (threeDmodel) {
		case "semucb":	
			seismic3Dmodel = new SEMUCBWM1();
			break;
		case "llnlg3d":
			seismic3Dmodel = new LLNLG3DJPS();
			break;
		default:
			throw new RuntimeException("Error: 3D model " + threeDmodel + " not implemented yet");
		}
		seismic3Dmodel.setTruncationRange(3881., 6371.);
//		seismic3Dmodel.setTruncationRange(3971., 6371.);
//		seismic3Dmodel.setTruncationRange(3481., 6371.);
		
		Traveltime traveltimetool = new Traveltime(raypathInformations, modelName, seismic3Dmodel, "P, PcP");
		
		traveltimetool.setIgnoreMantle(false);
		traveltimetool.setIgnoreCMBElevation(true);
		
		traveltimetool.run();
		List<List<Measurement>> measurements = traveltimetool.getMeasurements();
		
		List<Measurement> measurements_PcP = new ArrayList<>();
		List<Measurement> measurements_P = new ArrayList<>();
		Set<StaticCorrection> corrections = new HashSet<>();
		
		
		String[] pwString = new String[] {""};
		
		for (List<Measurement> record : measurements) {
//		measurements.stream().parallel().forEach(record -> {
			Set<String> phases = record.stream().map(p -> p.getPhaseName()).collect(Collectors.toSet());
			if (!(phases.contains("PcP") && phases.contains("P"))) {
				System.err.println(record);
				return;
			}
			Measurement mP = null;
			Measurement mPcP = null;
			for (Measurement m : record) {
				if (m.getPhaseName().equals("P")) {
					mP = m;
					measurements_P.add(m);
				}
				else if (m.getPhaseName().equals("PcP")) {
					mPcP = m;
					measurements_PcP.add(m);
				}
			}
			
//			double shift = -(mPcP.getTraveltimePerturbation() - mP.getTraveltimePerturbation());
			double shift = -(mPcP.getTraveltimePerturbationToPREM() - mP.getTraveltimePerturbationToPREM());
			
			pwString[0] += mPcP.getScatterPointList().get(0) + " " + shift + "\n";
			
			StaticCorrection correction = new StaticCorrection(mP.getStation(), mP.getGlobalCMTID()
					, SACComponent.Z, 0., shift, 1., new Phase[] {Phase.PcP});
			corrections.add(correction);
//		});
		}
		
		Path bouncepointPath = Paths.get("bouncepointPcP.dat");
		PrintWriter pw = new PrintWriter(bouncepointPath.toFile());
		pw.print(pwString[0]);
		pw.close();
		
		Path outpath = Paths.get("mantleCorrection_P-PcP.dat");
		StaticCorrectionFile.write(corrections, outpath);
	}
	
	public static void test(Path timewindowPath) throws IOException, TauModelException {
		Set<TimewindowInformation> timewindows = TimewindowInformationFile.read(timewindowPath);
		timewindows = timewindows.stream().limit(10).collect(Collectors.toSet());
		
		//Select raypaths
		Set<RaypathInformation> raypathInformations = timewindows.stream()
				.map(tw -> new RaypathInformation(tw.getStation(), tw.getGlobalCMTID()))
				.collect(Collectors.toSet());
		
		String modelName = "prem";
		
		Seismic3Dmodel seismic3Dmodel = new SEMUCBWM1();
		
		Traveltime traveltimetool = new Traveltime(raypathInformations, modelName, seismic3Dmodel, "P, PcP");
		
		traveltimetool.setIgnoreMantle(false);
		traveltimetool.setIgnoreCMBElevation(true);
		
		traveltimetool.run();
		List<List<Measurement>> measurements = traveltimetool.getMeasurements();
		
		List<Measurement> measurements_PcP = new ArrayList<>();
		List<Measurement> measurements_P = new ArrayList<>();
		
//		TauP_Time tauptime = new TauP_Time("/usr/local/share/TauP-2.4.5/StdModels/PREM_1000.taup");
		TauP_Time tauptime = new TauP_Time("prem");
		tauptime.parsePhaseList("P, PcP");
		
		for (List<Measurement> record : measurements) {
			Set<String> phases = record.stream().map(p -> p.getPhaseName()).collect(Collectors.toSet());
			if (!(phases.contains("PcP") && phases.contains("P"))) {
				System.err.println(record);
				continue;
			}
			Measurement mP = null;
			Measurement mPcP = null;
			for (Measurement m : record) {
				if (m.getPhaseName().equals("P")) {
					mP = m;
					measurements_P.add(m);
				}
				else if (m.getPhaseName().equals("PcP")) {
					mPcP = m;
					measurements_PcP.add(m);
				}
			}
			
			tauptime.setSourceDepth(6371. - mP.getGlobalCMTID().getEvent().getCmtLocation().getR());
			tauptime.calculate(mP.getEpicentralDistance());
			double taup_P = tauptime.getArrival(0).getTime();
			double taup_PcP = tauptime.getArrival(1).getTime();
			
			System.out.println(mPcP.getAbsoluteTraveltimeRef() + " " + mPcP.getAbsoluteTraveltimePREM() + " " + taup_PcP 
					+ " " + mPcP.getTraveltimePerturbation() + " " + mPcP.getTraveltimePerturbationToPREM());
		}
	}
	
	public static void Compute_ScS(Path timewindowPath, String threeDmodel) throws IOException {
		Set<TimewindowInformation> timewindows = TimewindowInformationFile.read(timewindowPath);
		
		//Select raypaths
		Set<RaypathInformation> raypathInformations = timewindows.stream()
				.map(tw -> new RaypathInformation(tw.getStation(), tw.getGlobalCMTID()))
				.collect(Collectors.toSet());
		
		String modelName = "prem";
		
		Seismic3Dmodel seismic3Dmodel = null;
		switch (threeDmodel) {
		case "semucb":	
			seismic3Dmodel = new SEMUCBWM1();
			break;
		case "llnlg3d":
			seismic3Dmodel = new LLNLG3DJPS();
			break;
		default:
			throw new RuntimeException("Error: 3D model " + threeDmodel + " not implemented yet");
		}
		seismic3Dmodel.setTruncationRange(3881., 6371.);
//		seismic3Dmodel.setTruncationRange(3971., 6371.);
//		seismic3Dmodel.setTruncationRange(3481., 6371.);
		
		Traveltime traveltimetool = new Traveltime(raypathInformations, modelName, seismic3Dmodel, "S, ScS");
		
		traveltimetool.setIgnoreMantle(false);
		traveltimetool.setIgnoreCMBElevation(true);
		
		traveltimetool.run();
		List<List<Measurement>> measurements = traveltimetool.getMeasurements();
		
		List<Measurement> measurements_ScS = new ArrayList<>();
		List<Measurement> measurements_S = new ArrayList<>();
		Set<StaticCorrection> corrections = new HashSet<>();
		
		Path bouncepointPath = Paths.get("bouncepointScS.dat");
		PrintWriter pw = new PrintWriter(bouncepointPath.toFile());
		
		for (List<Measurement> record : measurements) {
			Set<String> phases = record.stream().map(p -> p.getPhaseName()).collect(Collectors.toSet());
			if (!(phases.contains("ScS") && phases.contains("S"))) {
				System.err.println(record);
				continue;
			}
			Measurement mS = null;
			Measurement mScS = null;
			for (Measurement m : record) {
				if (m.getPhaseName().equals("ScS")) {
					mScS = m;
					measurements_ScS.add(m);
				}
				else if (m.getPhaseName().equals("S")) {
					mS = m;
					measurements_S.add(m);
				}
			}
			
//			double shift = -(mScS.getTraveltimePerturbation() - mS.getTraveltimePerturbation());
			double shift = -(mScS.getTraveltimePerturbationToPREM() - mS.getTraveltimePerturbationToPREM());
			
			pw.println(mScS.getScatterPointList().get(0) + " " + shift);
			
			StaticCorrection correction = new StaticCorrection(mS.getStation(), mS.getGlobalCMTID()
					, SACComponent.T, 0., shift, 1., new Phase[] {Phase.ScS});
			corrections.add(correction);
		}
		pw.close();
		
		Path outpath = Paths.get("mantleCorrection_S-ScS.dat");
		StaticCorrectionFile.write(corrections, outpath);
	}

}
