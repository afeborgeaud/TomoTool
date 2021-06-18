package io.github.afeborgeaud.tomotool.ml.clustering;

import org.apache.commons.math3.linear.ArrayRealVector;
import org.apache.commons.math3.linear.RealVector;

import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.distance.distancefunction.AbstractNumberVectorDistanceFunction;

public class WaveformDistanceFunction extends AbstractNumberVectorDistanceFunction {
	
	@Override
	public double distance(NumberVector o1, NumberVector o2) {
		RealVector v1 = new ArrayRealVector(o1.toArray());
		RealVector v2 = new ArrayRealVector(o2.toArray());
//		return distanceResidual(v1, v2);
//		return distanceZeroLagCC(v1, v2);
		return distanceMaxCC(v1, v2);
	}
	
	public double distanceZeroLagCC(RealVector v1, RealVector v2) {
		if (Double.isNaN(v1.getNorm()) || Double.isNaN(v2.getNorm()))
			throw new RuntimeException("Vector is NaN");
		double num = v1.dotProduct(v2);
		double denom = (v1.getNorm() * v2.getNorm());
		double cc1 = num / denom;
		return 1 - Math.abs(cc1) ;
	}
	
	public double distanceMaxCC(RealVector v1, RealVector v2) {
		if (Double.isNaN(v1.getNorm()) || Double.isNaN(v2.getNorm()))
			throw new RuntimeException("Vector is NaN");
		int deltaPointShift = 4;
		int rangePointShift = 100;
		double maxCC = Double.MIN_VALUE;
		double minCC = Double.MAX_VALUE;
		for (int i = 0; i < rangePointShift; i += deltaPointShift) {
			RealVector v1cut = v1.getSubVector(i, v1.getDimension() - i);
			RealVector v2cut = v2.getSubVector(0, v2.getDimension() - i);
			double cc = v1cut.dotProduct(v2cut) / (v1cut.getNorm() * v2cut.getNorm());
			if (cc > maxCC) maxCC = cc;
			if (cc < minCC) minCC = cc;
		}
		for (int i = 0; i < rangePointShift; i += deltaPointShift) {
			RealVector v2cut = v2.getSubVector(i, v2.getDimension() - i);
			RealVector v1cut = v1.getSubVector(0, v1.getDimension() - i);
			double cc = v1cut.dotProduct(v2cut) / (v1cut.getNorm() * v2cut.getNorm());
			if (cc > maxCC) maxCC = cc;
			if (cc < minCC) minCC = cc;
		}
		return 1 - Math.max(maxCC, -minCC);
	}
	
	public double distanceResidual(RealVector v1, RealVector v2) {
		if (Double.isNaN(v1.getNorm()) || Double.isNaN(v2.getNorm()))
			throw new RuntimeException("Vector is NaN");
		return 2 * v1.subtract(v2).getNorm() / (v1.getNorm() + v2.getNorm());
	}
	
}
