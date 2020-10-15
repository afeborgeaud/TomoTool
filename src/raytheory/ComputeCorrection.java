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
import topoModel.S20RTS;
import topoModel.SEMUCBWM1;
import topoModel.Seismic3Dmodel;
import topoModel.TK10;
import utilities.ReadUtils;

public class ComputeCorrection {

	public static void main(String[] args) throws IOException {
		Path inputPath = Paths.get(args[0]);
		String threeDmodel = args[1].trim().toLowerCase();
		String phase = args[2].trim().toLowerCase();
		
		List<RaypathInformation> raypathInformations;
		try {
			raypathInformations = RaypathInformation.readRaypathFromTimewindows(inputPath);
		} catch (Exception e) {
			raypathInformations = RaypathInformation.readRaypathInformation(inputPath);
		}
		
		if (phase.equals("pcp")) {
			System.out.println("Compute PcP");
			Compute_PcP(raypathInformations, threeDmodel);
		}
		else if (phase.equals("scs")) {
			System.out.println("Compute ScS");
			Compute_ScS(raypathInformations, threeDmodel);
		}
		else if (phase.equals("test")) {
			System.out.println("test");
			try {
				test(raypathInformations);
			} catch (TauModelException e) {
				e.printStackTrace();
			}
		}
		
	}
	
	public static void Compute_PcP(List<RaypathInformation> raypathInformations, String threeDmodel) throws IOException {
		String modelName = "prem";
		
		Seismic3Dmodel seismic3Dmodel = null;
		switch (threeDmodel) {
		case "semucb":	
			seismic3Dmodel = new SEMUCBWM1();
			break;
		case "llnlg3d":
			seismic3Dmodel = new LLNLG3DJPS();
			break;
		case "s20rts":
			seismic3Dmodel = new S20RTS();
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
			if (!(phases.contains("PcP") && phases.contains("P"))) continue;
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
	
	public static void Compute_ScS(List<RaypathInformation> raypathInformations, String threeDmodel) throws IOException {
		String modelName = "prem";
		
		Seismic3Dmodel seismic3Dmodel = null;
		switch (threeDmodel) {
		case "semucb":	
			seismic3Dmodel = new SEMUCBWM1();
			break;
		case "llnlg3d":
			seismic3Dmodel = new LLNLG3DJPS();
			break;
		case "s20rts":
			seismic3Dmodel = new S20RTS();
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
	
	public static void test(List<RaypathInformation> raypathInformations) throws IOException, TauModelException {
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

}
