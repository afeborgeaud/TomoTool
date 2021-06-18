package io.github.afeborgeaud.tomotool.data;

import java.io.IOException;
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

import io.github.afeborgeaud.tomotool.graphic.RecordSectionWindow;
import io.github.kensuke1984.kibrary.timewindow.TimewindowInformation;
import io.github.kensuke1984.kibrary.timewindow.TimewindowInformationFile;
import io.github.kensuke1984.kibrary.util.Trace;
import io.github.kensuke1984.kibrary.util.globalcmt.GlobalCMTID;
import io.github.kensuke1984.kibrary.util.sac.SACFileName;

public class AlignWindows {
	List<TimewindowInformation> timewindows;
	
	List<TimewindowInformation> alignedTimewindows;
	
	List<PolarizedShift> polarizedShifts;
	
	List<Double> amplitudes;
	
	Path sacpath;
	
	Map<GlobalCMTID, RealVector> eventWaveletMap;
	
	final double samplingHz = 20;
	
	final double buffer = 10;
	
	final double minCc; //typical value minCc = 0.8
	
	final double timeBeforePeak;
	
	final double timeAfterPeak;
	
	public AlignWindows(List<TimewindowInformation> timewindows, Path sacpath, double timeBeforePeak, double timeAfterPeak, double minCc) {
		this.timewindows = timewindows;
		alignedTimewindows = new ArrayList<TimewindowInformation>();
		this.sacpath = sacpath;
		this.eventWaveletMap = new HashMap<GlobalCMTID, RealVector>();
		this.minCc = minCc;
		this.timeBeforePeak = timeBeforePeak;
		this.timeAfterPeak = timeAfterPeak;
	}
	
	public static void main(String[] args) {
		if (args.length != 2) System.err.println("Usage: WaveformClustering.java path_to_timewindow_file path_to_sac_file_dir");
		Path timewindowPath = Paths.get(args[0]);
		Path sacpath = Paths.get(args[1]);
		
		double timeBeforePeak = 20;
		double timeAfterPeak = 20;
		
		timeBeforePeak = 50.;
		timeAfterPeak = 50.;
		
		double minCc = 0.6;
		
		List<TimewindowInformation> timewindows = null;
		try {
			timewindows = TimewindowInformationFile.read(timewindowPath).stream().collect(Collectors.toList());
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		AlignWindows alignwindows = new AlignWindows(timewindows, sacpath, timeBeforePeak, timeAfterPeak, minCc);
		alignwindows.run();
		
		alignwindows.displayInitialRecordSection();
		
		alignwindows.displayAlignedRecordSection();
		
		Path outpath = Paths.get("/home/anselme/Dropbox/topo_eth_local/synthetics/timewindow_PKKP_nocut_longer_shifted.dat");
		try {
			TimewindowInformationFile.write(alignwindows.getAlignedTimewindows().stream().collect(Collectors.toSet()), outpath);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public void displayInitialRecordSection() {
		RecordSectionWindow rswindow = new RecordSectionWindow(timewindows, sacpath);
		rswindow.show();
	}
	
	public void displayAlignedRecordSection() {
		RecordSectionWindow rswindow = new RecordSectionWindow(alignedTimewindows, polarizedShifts, sacpath);
		rswindow.show();
	}
	
	private void runAndDisplay() {
		GlobalCMTID id = new GlobalCMTID("200602021248A");
		
		initialize();
		RecordSectionWindow rswindow0 = new RecordSectionWindow(timewindows, sacpath);
		rswindow0.show();
		
		runOneIteration();
		RecordSectionWindow rswindow1 = new RecordSectionWindow(getAlignedTimewindows(), getPolarizedShifts(), sacpath);
		double[] wavelet = eventWaveletMap.get(id).toArray();
		rswindow1.addTraceAtDistance(new Trace(new double[wavelet.length], wavelet), -1.);
		rswindow1.show();
		
		runOneIteration();
		RecordSectionWindow rswindow2 = new RecordSectionWindow(getAlignedTimewindows(), getPolarizedShifts(), sacpath);
		wavelet = eventWaveletMap.get(id).toArray();
		rswindow2.addTraceAtDistance(new Trace(new double[wavelet.length], wavelet), -1.);
		rswindow2.show();
	}
	
	public void run() {
		System.err.println("Shift timewindows for 3D reference model");
		initialize();
		
		runOneIteration();
		
		runOneIteration();
		
		computeAlignTimewindows();
		
//		removeWindowsBasedOnAmplitude();
		
		removeBadWindows();
		
		initializePhase2();
		
		runPhase2Iteration();
		
		computePhase2AlignTimewindows();
		
		removeBadWindows();
		
		System.err.println("Done");
	}
	
	private void initialize() {
		Set<GlobalCMTID> events = timewindows.parallelStream().map(tw -> tw.getGlobalCMTID()).collect(Collectors.toSet());
		events.stream().forEach(event -> eventWaveletMap.put(event, null));
		
//		polarizedShifts = new ArrayList<>();
//		for (int i = 0; i < timewindows.size(); i++)
//			polarizedShifts.add(new PolarizedShift(0, 1, 1));
		
		for (GlobalCMTID event : events) {
			List<TimewindowInformation> eventTimewindows = timewindows.parallelStream().filter(tw -> tw.getGlobalCMTID().equals(event))
					.collect(Collectors.toList());
			RealVector wavelet = new ArrayRealVector((int) (eventTimewindows.get(0).getLength() * samplingHz) + 1);
			eventWaveletMap.put(event, wavelet);
			
			for (int i = 0; i < eventTimewindows.size(); i++) {
				TimewindowInformation window = eventTimewindows.get(i);
				String sacnameString = window.getGlobalCMTID() + "/" + window.getStation().getName() + "." + window.getGlobalCMTID() 
					+ "." + window.getComponent() + "sc";
				Path syn = sacpath.resolve(sacnameString);
				try {
					RealVector tmpWaveform = new SACFileName(syn).read().createTrace().cutWindow(window.getStartTime(), window.getEndTime()).removeTrend().getYVector();
					wavelet = truncateAndAdd(wavelet, tmpWaveform);
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
			
			wavelet = wavelet.mapDivide(eventTimewindows.size());
			eventWaveletMap.replace(event, wavelet);
		}
	}
	
	private void initializePhase2() {
		Set<GlobalCMTID> events = alignedTimewindows.parallelStream().map(tw -> tw.getGlobalCMTID()).collect(Collectors.toSet());
		
//		polarizedShifts = new ArrayList<>();
//		for (int i = 0; i < timewindows.size(); i++)
//			polarizedShifts.add(new PolarizedShift(0, 1, 1));
		
		for (GlobalCMTID event : events) {
			List<TimewindowInformation> eventTimewindows = alignedTimewindows.parallelStream().filter(tw -> tw.getGlobalCMTID().equals(event))
					.collect(Collectors.toList());
			RealVector wavelet = new ArrayRealVector((int) (eventTimewindows.get(0).getLength() * samplingHz) + 1);
			
			for (int i = 0; i < eventTimewindows.size(); i++) {
				TimewindowInformation window = eventTimewindows.get(i);
				String sacnameString = window.getGlobalCMTID() + "/" + window.getStation().getName() + "." + window.getGlobalCMTID() 
					+ "." + window.getComponent() + "sc";
				Path syn = sacpath.resolve(sacnameString);
				try {
					RealVector tmpWaveform = new SACFileName(syn).read().createTrace().cutWindow(window.getStartTime(), window.getEndTime()).removeTrend().getYVector();
					wavelet = truncateAndAdd(wavelet, tmpWaveform);
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
			
			wavelet = wavelet.mapDivide(eventTimewindows.size());
			eventWaveletMap.replace(event, wavelet);
		}
	}
	
	private void runOneIteration() {
		for (GlobalCMTID event : eventWaveletMap.keySet()) {
			List<TimewindowInformation> eventTimewindows = timewindows.parallelStream().filter(tw -> tw.getGlobalCMTID().equals(event))
					.collect(Collectors.toList());
			RealVector wavelet0 = eventWaveletMap.get(event);
			RealVector wavelet = new ArrayRealVector(wavelet0.getDimension());
			
			for (int i = 0; i < eventTimewindows.size(); i++) {
				TimewindowInformation window = eventTimewindows.get(i);
				String sacnameString = window.getGlobalCMTID() + "/" + window.getStation().getName() + "." + window.getGlobalCMTID() 
					+ "." + window.getComponent() + "sc";
				Path syn = sacpath.resolve(sacnameString);
				try {
					RealVector tmpWaveform = new SACFileName(syn).read().createTrace()
							.cutWindow(window.getStartTime() - buffer, window.getEndTime() + buffer).removeTrend().getYVector();
					PolarizedShift pointShift = findBestShiftParallel(wavelet0, tmpWaveform);
//					System.out.println(pointShift + " " + wavelet0.getDimension() + " " + tmpWaveform.getDimension());
					int tmppointshift = Math.max(0, pointShift.getShift() + (int) (buffer*samplingHz) - 1);
					RealVector shiftedWaveform = tmpWaveform.getSubVector(tmppointshift, wavelet0.getDimension())
							.mapMultiply(pointShift.getPolarization());
					wavelet = truncateAndAdd(wavelet, shiftedWaveform);
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
			
			wavelet = wavelet.mapDivide(eventTimewindows.size());
			eventWaveletMap.replace(event, wavelet);
		}
	}
	
	private void runPhase2Iteration() {
		for (GlobalCMTID event : eventWaveletMap.keySet()) {
			List<TimewindowInformation> eventTimewindows = alignedTimewindows.parallelStream().filter(tw -> tw.getGlobalCMTID().equals(event))
					.collect(Collectors.toList());
			RealVector wavelet0 = eventWaveletMap.get(event);
			RealVector wavelet = new ArrayRealVector(wavelet0.getDimension());
			
			for (int i = 0; i < eventTimewindows.size(); i++) {
				TimewindowInformation window = eventTimewindows.get(i);
				String sacnameString = window.getGlobalCMTID() + "/" + window.getStation().getName() + "." + window.getGlobalCMTID() 
					+ "." + window.getComponent() + "sc";
				Path syn = sacpath.resolve(sacnameString);
				try {
					RealVector tmpWaveform = new SACFileName(syn).read().createTrace()
							.cutWindow(window.getStartTime() - buffer, window.getEndTime() + buffer).removeTrend().getYVector();
					PolarizedShift pointShift = findBestShiftParallel(wavelet0, tmpWaveform);
					RealVector shiftedWaveform = tmpWaveform.getSubVector(pointShift.getShift() + (int) (buffer*samplingHz), wavelet0.getDimension())
							.mapMultiply(pointShift.getPolarization());
					wavelet = truncateAndAdd(wavelet, shiftedWaveform);
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
			
			wavelet = wavelet.mapDivide(eventTimewindows.size());
			eventWaveletMap.replace(event, wavelet);
		}
	}
	
	private void removeBadWindows() {
		List<PolarizedShift> tmpshifts = new ArrayList<>();
		List<TimewindowInformation> tmpwindows = new ArrayList<>();
		for (int i = 0; i < alignedTimewindows.size(); i++) {
			if (polarizedShifts.get(i).getCc() >= minCc) {
				tmpshifts.add(polarizedShifts.get(i));
				tmpwindows.add(alignedTimewindows.get(i));
			}
		}
		alignedTimewindows = tmpwindows;
		polarizedShifts = tmpshifts;
	}
	
	
	private void computeAlignTimewindows() {
		alignedTimewindows = new ArrayList<TimewindowInformation>();
		polarizedShifts = new ArrayList<>();
		
		for (int i = 0; i < timewindows.size(); i++) {
			TimewindowInformation window = timewindows.get(i);
			String sacnameString = window.getGlobalCMTID() + "/" + window.getStation().getName() + "." + window.getGlobalCMTID() 
				+ "." + window.getComponent() + "sc";
			Path syn = sacpath.resolve(sacnameString);
			
			RealVector wavelet = eventWaveletMap.get(window.getGlobalCMTID());
			
			int iOfmax = 0;
			int globalPolarization = 1;
			if (wavelet.getMaxValue() > -wavelet.getMinValue()) iOfmax = wavelet.getMaxIndex();
			else {
				iOfmax = wavelet.getMinIndex();
				globalPolarization = -1;
			}
			double tOfmax = (double) iOfmax / samplingHz;
			
			try {
				RealVector tmpWaveform = new SACFileName(syn).read().createTrace()
						.cutWindow(window.getStartTime() - buffer, window.getEndTime() + buffer).removeTrend().getYVector();
				PolarizedShift tmpPointShift = findBestShiftParallel(wavelet, tmpWaveform);
				PolarizedShift pointShift = new PolarizedShift(tmpPointShift.getShift(),
						tmpPointShift.getPolarization() * globalPolarization, tmpPointShift.getCc());
				double timeshift = (double) pointShift.getShift() / samplingHz;
				double t0 = window.getStartTime() + timeshift + tOfmax - timeBeforePeak;
				double t1 = t0 + timeBeforePeak + timeAfterPeak;
				TimewindowInformation tmpwindow = new TimewindowInformation(t0, t1,
						window.getStation(), window.getGlobalCMTID(),
						window.getComponent(), window.getPhases());
				alignedTimewindows.add(tmpwindow);
				polarizedShifts.add(pointShift);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
	
	private void computePhase2AlignTimewindows() {
		for (int i = 0; i < alignedTimewindows.size(); i++) {
			TimewindowInformation window = alignedTimewindows.get(i);
			String sacnameString = window.getGlobalCMTID() + "/" + window.getStation().getName() + "." + window.getGlobalCMTID() 
				+ "." + window.getComponent() + "sc";
			Path syn = sacpath.resolve(sacnameString);
			
			RealVector wavelet = eventWaveletMap.get(window.getGlobalCMTID());
			
			int iOfmax = 0;
			int globalPolarization = 1;
			if (wavelet.getMaxValue() > -wavelet.getMinValue()) iOfmax = wavelet.getMaxIndex();
			else {
				iOfmax = wavelet.getMinIndex();
				globalPolarization = -1;
			}
			double tOfmax = (double) iOfmax / samplingHz;
			
			try {
				RealVector tmpWaveform = new SACFileName(syn).read().createTrace()
						.cutWindow(window.getStartTime() - buffer, window.getEndTime() + buffer).removeTrend().getYVector();
				PolarizedShift tmpPointShift = findBestShiftParallel(wavelet, tmpWaveform);
				PolarizedShift pointShift = new PolarizedShift(tmpPointShift.getShift(),
						tmpPointShift.getPolarization() * globalPolarization, tmpPointShift.getCc());
				double timeshift = (double) pointShift.getShift() / samplingHz;
				double t0 = window.getStartTime() + timeshift + tOfmax - timeBeforePeak;
				double t1 = t0 + timeBeforePeak + timeAfterPeak;
				TimewindowInformation tmpwindow = new TimewindowInformation(t0, t1,
						window.getStation(), window.getGlobalCMTID(),
						window.getComponent(), window.getPhases());
				alignedTimewindows.set(i, tmpwindow);
				polarizedShifts.set(i, pointShift);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
	
	public List<TimewindowInformation> getAlignedTimewindows() {
		return alignedTimewindows;
	}
	
	public List<PolarizedShift> getPolarizedShifts() {
		return polarizedShifts;
	}
	
	private RealVector truncateAndAdd(RealVector v1, RealVector v2) {
		if (v1.getDimension() != v2.getDimension()) {
			int n = Math.min(v1.getDimension(), v2.getDimension());
			return v1.getSubVector(0, n).add(v2.getSubVector(0, n));
		}
		else
			return v1.add(v2);
	}
	
	private PolarizedShift findBestShiftParallel(RealVector v, RealVector vStatic) {
        int gapLength = vStatic.getDimension() - v.getDimension();
        if (gapLength <= 0) throw new IllegalArgumentException("Input vector must be shorter.");
        double compY2 = v.getNorm();
        int[] shifts = new int[gapLength + 1];
        double[] cors = new double[gapLength + 1];
        IntStream.range(0, gapLength + 1).parallel().forEach(i -> {
            double cor = v.dotProduct(vStatic.getSubVector(i, v.getDimension()));
            double y2 = vStatic.getSubVector(i, v.getDimension()).getNorm();
            cor /= y2 * compY2;
            shifts[i] = i;
            cors[i] = cor;
        });
        
        RealVector tmpv = new ArrayRealVector(cors);
        PolarizedShift polarizedShift = null;
        
        if (tmpv.getMaxValue() >= -tmpv.getMinValue()) {
        	polarizedShift = new PolarizedShift(tmpv.getMaxIndex() - gapLength / 2, 1, tmpv.getMaxValue());
        }
        else {
        	polarizedShift = new PolarizedShift(tmpv.getMinIndex() - gapLength / 2, -1, -tmpv.getMinValue());
        }
        
        return polarizedShift;
    }
	
	public static class PolarizedShift {
		final int shift;
		final int polarization;
		final double cc;
		
		public PolarizedShift(int shift, int polarization, double cc) {
			this.shift = shift;
			if (Math.abs(polarization) != 1)
				throw new RuntimeException("Polarization must be 1 or -1");
			this.polarization = polarization;
			this.cc = cc;
		}
		
		public int getShift() {
			return shift;
		}
		
		public int getPolarization() {
			return polarization;
		}
		
		public double getCc() {
			return cc;
		}
		
		@Override
		public String toString() {
			return String.format("%d %d %.2f", shift, polarization, cc);
		}
	}
}
