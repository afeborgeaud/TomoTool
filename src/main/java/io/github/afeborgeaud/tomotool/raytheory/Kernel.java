package io.github.afeborgeaud.tomotool.raytheory;

import io.github.kensuke1984.kibrary.dsminformation.PolynomialStructure;

public class Kernel {
	
	public final static double core_radius = 3480.;
	
	private PolynomialStructure structure;
	
	private double vpMantle;
	
	private double vsMantle;
	
	private double vpCore;
	
	public Kernel(PolynomialStructure structure) {
		this.structure = structure;
		vpMantle = structure.getVphAt(3480.01);
		vsMantle = structure.getVshAt(3480.01);
		vpCore = structure.getVphAt(3479.99);
	}
	
	public double undersideReflection(double rayparam) throws IllegalArgumentException {
		double tmp = core_radius * core_radius / (vpCore * vpCore) - rayparam * rayparam;
		if (tmp < 0)
			throw new IllegalArgumentException("No solution for the input ray parameter and slowness");
		return 2. / core_radius * Math.sqrt(tmp);
	}
	
	public double topReflection(double rayparam, WaveType wavetype) throws IllegalArgumentException {
		double vMantleDown = 0;
		double vMantleUp = 0;
		if (wavetype.equals(WaveType.P)) {
			vMantleDown = vpMantle;
			vMantleUp = vMantleDown;
		}
		else if (wavetype.equals(WaveType.S)) {
			vMantleDown = vsMantle;
			vMantleUp = vMantleDown;
		}
		else if (wavetype.equals(WaveType.PS)) {
			vMantleDown = vpMantle;
			vMantleUp = vsMantle;
		}
		double tmpDown = core_radius * core_radius / (vMantleDown * vMantleDown) - rayparam * rayparam;
		double tmpUp = core_radius * core_radius / (vMantleUp * vMantleUp) - rayparam * rayparam;
		if (tmpDown < 0 || tmpUp < 0)
			throw new IllegalArgumentException("No solution for the input ray parameter and slowness");
		return -1 / core_radius * (Math.sqrt(tmpDown) + Math.sqrt(tmpUp));
	}
	
	public double transmission(double rayparam, WaveType wavetype) throws IllegalArgumentException {
		double vMantle = 0;
		if (wavetype.equals(WaveType.P))
			vMantle = vpMantle;
		else if (wavetype.equals(WaveType.S))
			vMantle = vsMantle;
		double tmp1 = core_radius * core_radius / (vMantle * vMantle) - rayparam * rayparam;
		double tmp2 = core_radius * core_radius / (vpCore * vpCore) - rayparam * rayparam;
		if (tmp1 < 0 || tmp2 < 0)
			throw new IllegalArgumentException("No solution for the input ray parameter and slowness");
		return -1. / core_radius * (Math.sqrt(tmp1) - Math.sqrt(tmp2));
	}
	
}