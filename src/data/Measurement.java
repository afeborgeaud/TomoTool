package data;

import io.github.kensuke1984.kibrary.timewindow.TimewindowInformation;

public class Measurement {
	private TimewindowInformation timewindow;
	
	private double timeshift;
	
	private double amplitudeRatio;
	
	private double crosscorrelationAtBestShift;
	
	public Measurement(TimewindowInformation timewindow, double timeshift, double amplitudeRation, double crosscorrelationAtBestShift) {
		this.timewindow = timewindow;
		this.timeshift = timeshift;
		this.amplitudeRatio = amplitudeRation;
		this.crosscorrelationAtBestShift = crosscorrelationAtBestShift;
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
	
	@Override
	public String toString() {
		return String.format("%.4f %.4f %.4f", timeshift, amplitudeRatio, crosscorrelationAtBestShift);
	}
}
