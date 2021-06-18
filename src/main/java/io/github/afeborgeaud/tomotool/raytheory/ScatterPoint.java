package io.github.afeborgeaud.tomotool.raytheory;

import io.github.kensuke1984.kibrary.util.HorizontalPosition;

public class ScatterPoint {
	
	private ScatterType type;
	
	private HorizontalPosition position;
	
	private double rayparameter;
	
	private WaveType wavetype;
	
	public ScatterPoint(HorizontalPosition pos, double rayparameter, ScatterType type, WaveType wavetype) {
		this.type = type;
		this.position = pos;
		this.rayparameter = rayparameter;
		this.wavetype = wavetype;
	}
	
	public ScatterType getType() {
		return type;
	}
	
	public HorizontalPosition getPosition() {
		return position;
	}
	
	public double getRayparameter() {
		return rayparameter;
	}
	
	public WaveType getWaveType() {
		return wavetype;
	}
	
	@Override
	public String toString() {
		return position + " " + rayparameter + " " + type;
	}
}
