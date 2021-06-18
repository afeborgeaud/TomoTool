package io.github.afeborgeaud.tomotool.math;

public class Interpolate {
	
	public static double[] simple(double[] y, int sampling) {
		double[] yInterp = new double[(y.length - 1) * sampling + 1];
		for (int i = 0; i < y.length - 1; i++) {
			yInterp[i * sampling] = y[i];
			for (int j = 1; j < sampling; j++) {
				yInterp[i * sampling + j] = y[i] + (double) j / sampling * (y[i+1] - y[i]);
			}
		}
		yInterp[yInterp.length - 1] = y[y.length - 1];
		return yInterp;
	}
	
}
