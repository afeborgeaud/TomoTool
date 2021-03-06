package io.github.afeborgeaud.tomotool.math;

import java.util.List;


public class Sph {
	
	private static double ylm(int l, int m, double phi, double theta) {
		double y = xlm(l, Math.abs(m), theta);
		if (m < 0)
			y *= Math.sqrt(2) * Math.sin(Math.abs(m) * phi);
		else if (m > 0)
			y *= Math.sqrt(2) * Math.cos(Math.abs(m) * phi);
		return y;
	}
	
	public static double eval(double phi, double theta, List<List<Double>> coeffs) {
		double y = 0;
		for (int l = 0; l < coeffs.size(); l++) {
			for (int im = 0; im < coeffs.get(l).size(); im++) {
				int m = im - l;
				y += coeffs.get(l).get(im) * ylm(l, m, phi, theta);
			}
		}
		return -y;
	}
	
	private static final double xlm(int l, int m, double theta) {
		if (m < 0 || m > l)
			throw new RuntimeException("Invalid m");
		double x = Math.cos(theta);
		double fact = 1.;
		if (m != 0)
			for (int i = l - m + 1; i <= l + m; i++)
				fact *= i;

		double coef = Math.sqrt((2 * l + 1) / fact);
		double xlm = coef * plgndr(l, m, x);
		return xlm;
	}

	private static final double plgndr(int l, int m, double x) {
		if (m < 0 || l < m || x < -1 || 1 < x)
			throw new IllegalArgumentException("l, m, x are invalid.");
		double pmm = 1;
		if (0 < m) {
			double somx2 = Math.sqrt((1 - x) * (1 + x));
			double fact = 1;
			for (int i = 1; i <= m; i++) {
				pmm *= -fact * somx2;
				fact += 2;
			}
		}
		if (l == m)
			return pmm;

		double pmmp1 = x * (2 * m + 1) * pmm;
		if (l == (m + 1))
			return pmmp1;

		for (int ll = m + 2; ll <= l; ll++) {
			double pll = (x * (2 * ll - 1) * pmmp1 - (ll + m - 1) * pmm) / (ll - m);
			pmm = pmmp1;
			pmmp1 = pll;
		}
		return pmmp1;
	}
}
