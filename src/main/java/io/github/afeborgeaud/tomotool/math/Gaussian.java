package io.github.afeborgeaud.tomotool.math;

import io.github.kensuke1984.kibrary.util.HorizontalPosition;

public class Gaussian {
	
	double amplitude;
	
	double sigmaKm;
	
	HorizontalPosition center;
	
	public static void main(String[] args) {
		Gaussian gaussian = new Gaussian(new HorizontalPosition(10, -90), 8., 1000 / 2.);
		HorizontalPosition pos = new HorizontalPosition(20, -90);
		System.out.println(gaussian.getY(pos) + " " + gaussian.getSlopeDeg(pos));
	}
	
	public Gaussian(HorizontalPosition center, double amplitude, double sigmaKm) {
		this.center = center;
		this.amplitude = amplitude;
		this.sigmaKm = sigmaKm;
	}
	
	public double getY(HorizontalPosition x) {
		double d = x.getEpicentralDistance(center) * 3480.;
		return amplitude * Math.exp(-d * d / (2 * sigmaKm * sigmaKm));
	}
	
	public double getDerivativeY(HorizontalPosition x) {
		double d = x.getEpicentralDistance(center) * 3480.;
		return -d / (sigmaKm * sigmaKm) * amplitude * Math.exp(-d * d / (2 * sigmaKm * sigmaKm));
	}
	
	public double getSlopeDeg(HorizontalPosition x) {
		return Math.toDegrees(Math.atan(getDerivativeY(x)));
	}
}
