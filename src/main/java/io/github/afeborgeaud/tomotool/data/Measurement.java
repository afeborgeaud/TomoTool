package io.github.afeborgeaud.tomotool.data;

import io.github.kensuke1984.kibrary.timewindow.TimewindowInformation;

public class Measurement {
	private TimewindowInformation timewindow;
	
	private double timeshift;
	
	private double amplitudeRatio;
	
	private double crosscorrelationAtBestShift;
	
	double sn0;
	
	double sn1;
	
	public Measurement(TimewindowInformation timewindow, double timeshift, double amplitudeRation, double crosscorrelationAtBestShift, double sn0, double sn1) {
		this.timewindow = timewindow;
		this.timeshift = timeshift;
		this.amplitudeRatio = amplitudeRation;
		this.crosscorrelationAtBestShift = crosscorrelationAtBestShift;
		this.sn0 = sn0;
		this.sn1 = sn1;
	}
	
	public Measurement(TimewindowInformation timewindow, double timeshift, double amplitudeRation, double crosscorrelationAtBestShift) {
		this.timewindow = timewindow;
		this.timeshift = timeshift;
		this.amplitudeRatio = amplitudeRation;
		this.crosscorrelationAtBestShift = crosscorrelationAtBestShift;
		this.sn0 = 0.;
		this.sn1 = 1.;
	}
	
	public TimewindowInformation getTimewindow() {
		return timewindow;
	}
	
	public double getAmplitudeRatio() {
		return amplitudeRatio;
	}
	
	public double getTimeshift() {
		return timeshift;
	}
	
	public double getCrosscorrelationAtBestShift() {
		return crosscorrelationAtBestShift;
	}
	
	public double getSn0() {
		return sn0;
	}
	
	public double getSn1() {
		return sn1;
	}
	
	@Override
	public String toString() {
		return String.format("%.4f %.4f %.4f %.4f %.4f", timeshift, amplitudeRatio, crosscorrelationAtBestShift, sn0, sn1);
	}
}
