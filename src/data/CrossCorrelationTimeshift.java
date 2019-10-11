package data;


import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.apache.commons.math3.linear.ArrayRealVector;
import org.apache.commons.math3.linear.RealVector;

import io.github.kensuke1984.kibrary.timewindow.TimewindowInformation;
import io.github.kensuke1984.kibrary.timewindow.TimewindowInformationFile;
import io.github.kensuke1984.kibrary.util.Trace;
import io.github.kensuke1984.kibrary.util.sac.SACFileName;
import math.Interpolate;
import raytheory.Compute;
import raytheory.ScatterPoint;
import topoModel.Seismic3Dmodel;
import topoModel.TK10;

public class CrossCorrelationTimeshift {
	
	private List<TimewindowInformation> timewindows;
	
	private List<SACFileName> obsNames;
	
	private List<SACFileName> synNames;
	
	private boolean convolute;
	
	public static void main(String args[]) throws IOException {
//		computePKKKP();
//		computeScS();
		
		Path timewindowPath = Paths.get(args[0]);
		String phaseName = args[1].trim();
		String modelRefName = args[2].trim();
		String modelName = args[3].trim();
		System.out.println(String.format("Windows: %s, Phase: %s, ModelRef: %s, Model: %s", timewindowPath.getFileName(), phaseName, modelRefName, modelName));
		List<TimewindowInformation> timewindows = TimewindowInformationFile.read(timewindowPath).stream().collect(Collectors.toList());
		compute_phase(timewindows, modelRefName, modelName, phaseName);
	}
	
	public CrossCorrelationTimeshift(List<TimewindowInformation> timewindows, Path obsPath, Path synPath, boolean convolute) {
		obsNames = new ArrayList<>();
		synNames = new ArrayList<>();
		this.timewindows = timewindows;
		for (TimewindowInformation window : timewindows) {
			String obsString = window.getGlobalCMTID() + "/" + window.getStation().getStationName() + "." + window.getGlobalCMTID() 
				+ "." + window.getComponent() + "sc";
//			String synString = obsString + "s";
//			if (convolute) synString += "c";
			String synString = obsString;
			Path obs = obsPath.resolve(obsString);
			Path syn = synPath.resolve(synString);
			this.obsNames.add(new SACFileName(obs));
			this.synNames.add(new SACFileName(syn));
		}
	}
	
	public List<Measurement> calculate() {
		List<Measurement> timeshifts = new ArrayList<>();
		for (int i = 0; i < timewindows.size(); i++) {
			try {
				Trace obsTrace = obsNames.get(i).read().createTrace()
						.cutWindow(timewindows.get(i).getStartTime() - 5, timewindows.get(i).getEndTime() + 5);
				Trace synTrace = synNames.get(i).read().createTrace().cutWindow(timewindows.get(i));
				
				obsTrace = interpolate(obsTrace, 50);
				synTrace = interpolate(synTrace, 50);
				
				double timeshift = obsTrace.findBestShift(synTrace);
				Trace shiftObsTrace = obsTrace
						.cutWindow(timewindows.get(i).getStartTime() + timeshift, timewindows.get(i).getEndTime() + timeshift);
				double amplitudeRatio = shiftObsTrace.getYVector().getLInfNorm() / synTrace.getYVector().getLInfNorm();
				double crosscorrelationAtBestShift = crossCorrelation(synTrace, shiftObsTrace);
				
				timeshifts.add(new Measurement(timewindows.get(i), timeshift, amplitudeRatio, crosscorrelationAtBestShift));
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		return timeshifts;
	}
	
	private Trace interpolate(Trace trace, int sampling) {
		double[] yInterp = Interpolate.simple(trace.getY(), sampling);
		double[] xInterp = Interpolate.simple(trace.getX(), sampling);
		return new Trace(xInterp, yInterp);
	}
	
	private double crossCorrelation(Trace trace1, Trace trace2) {
		RealVector v1 = trace1.getYVector();
		RealVector v2 = trace2.getYVector();
		double cc = 0;
		if (v1.getDimension() < v2.getDimension()) {
			RealVector v = v2.getSubVector(0, v1.getDimension());
			cc = v.dotProduct(v1) / v.getNorm() / v1.getNorm();
		}
		else if (v1.getDimension() > v2.getDimension()) {
			RealVector v = v1.getSubVector(0, v2.getDimension());
			cc = v.dotProduct(v2) / v.getNorm() / v2.getNorm();
		}
		else {
			cc = v1.dotProduct(v2) / v1.getNorm() / v2.getNorm();
		}
		return cc;
	}
	
	public static void computePKKKP() {
		String model = "1D";
		Path workDir = Paths.get("/work/anselme/TOPO/ETH/synthetics");
		Path timewindowInformationPath = Paths.get("/work/anselme/TOPO/ETH/synthetics/timewindow_PKKKKP.dat");
		Path obsPath = workDir.resolve(model);
		Path synPath = obsPath;
		boolean convolute = true;
		
		try {
			List<TimewindowInformation> timewindows 
				= TimewindowInformationFile.read(timewindowInformationPath).stream().collect(Collectors.toList());
			
			CrossCorrelationTimeshift ccShift = new CrossCorrelationTimeshift(timewindows
					, obsPath, synPath, convolute);
			List<Measurement> measurements = ccShift.calculate();
			
			Seismic3Dmodel seismic3Dmodel = new TK10();
			List<raytheory.Measurement> rayMearuements = Compute.compute_phase(timewindows, seismic3Dmodel, "PKKKKP");
			
			Path outpath = Paths.get("/work/anselme/TOPO/ETH/synthetics/correlationTimeshift_topo_PKKKKP.dat");
			PrintWriter pw = new PrintWriter(outpath.toFile());
			for (int i = 0; i < timewindows.size(); i++) {
				Measurement m = measurements.get(i);
				raytheory.Measurement mRay = rayMearuements.get(i);
				ScatterPoint point = mRay.getScatterPointList().get(0);
				pw.println(mRay.getTraveltimePerturbation() + " " + m.getTimeshift() 
					+ " " + m.getAmplitudeRatio() + " " + m.getCrosscorrelationAtBestShift()
					+ " " + point.getPosition());
			}
			pw.close();
			
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public static void computeScS() {
		String modelSyn = "1D";
		String modelObs = "1Dtopo";
		Path workDir = Paths.get("/work/anselme/TOPO/ETH_local/synthetics");
		Path timewindowInformationPath = Paths.get("/work/anselme/TOPO/ETH_local/synthetics/timewindow_ScS.dat");
		Path obsPath = workDir.resolve(modelObs);
		Path synPath = workDir.resolve(modelSyn);
		boolean convolute = true;
		
		try {
			List<TimewindowInformation> timewindows 
				= TimewindowInformationFile.read(timewindowInformationPath).stream().collect(Collectors.toList());
			
			CrossCorrelationTimeshift ccShift = new CrossCorrelationTimeshift(timewindows
					, obsPath, synPath, convolute);
			List<Measurement> measurements = ccShift.calculate();
			
			Seismic3Dmodel seismic3Dmodel = new TK10();
			List<raytheory.Measurement> rayMearuements = Compute.compute_phase(timewindows, seismic3Dmodel, "ScS");
			
			Path outpath = Paths.get("/work/anselme/TOPO/ETH_local/synthetics/correlationTimeshift_topo_ScS.dat");
			PrintWriter pw = new PrintWriter(outpath.toFile());
			for (int i = 0; i < timewindows.size(); i++) {
				Measurement m = measurements.get(i);
				raytheory.Measurement mRay = rayMearuements.get(i);
				ScatterPoint point = mRay.getScatterPointList().get(0);
				pw.println(mRay.getTraveltimePerturbation() + " " + -m.getTimeshift() 
					+ " " + m.getAmplitudeRatio() + " " + m.getCrosscorrelationAtBestShift()
					+ " " + point.getPosition());
			}
			pw.close();
			
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public static void compute_phase(List<TimewindowInformation> timewindows, String modelRefName, String modelName, String phaseName) {
		String modelSyn = modelRefName;
		String modelObs = modelName;
		Path workDir = Paths.get("/work/anselme/TOPO/ETH_local/synthetics");
		Path obsPath = workDir.resolve(modelObs);
		Path synPath = workDir.resolve(modelSyn);
		boolean convolute = true;
		
		Path outpath = workDir.resolve("correlationTimeshift_" + modelSyn + "_" + modelObs + "_" + phaseName + ".dat");
		
		try {
			CrossCorrelationTimeshift ccShift = new CrossCorrelationTimeshift(timewindows
					, obsPath, synPath, convolute);
			List<Measurement> measurements = ccShift.calculate();
			
			Seismic3Dmodel seismic3Dmodel = new TK10();
			
			List<raytheory.Measurement> rayMearuements = Compute.compute_phase(timewindows, seismic3Dmodel, phaseName);
			
			PrintWriter pw = new PrintWriter(outpath.toFile());
			for (int i = 0; i < timewindows.size(); i++) {
				Measurement m = measurements.get(i);
				raytheory.Measurement mRay = rayMearuements.get(i);
				ScatterPoint point = mRay.getScatterPointList().get(0);
				pw.println(mRay.getTraveltimePerturbation() + " " + -m.getTimeshift() 
					+ " " + m.getAmplitudeRatio() + " " + m.getCrosscorrelationAtBestShift()
					+ " " + point.getPosition() + mRay.getPhaseName() + " " + mRay.getStation().getStationName() + " " + mRay.getGlobalCMTID());
			}
			pw.close();
			
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
}
