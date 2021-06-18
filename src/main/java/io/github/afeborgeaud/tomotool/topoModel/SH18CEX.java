package io.github.afeborgeaud.tomotool.topoModel;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;

import io.github.kensuke1984.kibrary.dsminformation.PolynomialStructure;
import io.github.kensuke1984.kibrary.util.HorizontalPosition;
import io.github.kensuke1984.kibrary.util.Location;

public class SH18CEX implements Seismic3Dmodel {
	
	private final static double rsp = 2.; //dlnVs to dlnVp ratio
	
	private double rmin;
	
	private double rmax;
	
	public static void main(String[] args) throws IOException {
		SH18CEX model = new SH18CEX();
		Path outpath = Paths.get("sh18cex.dat");
		model.writeCMBElevationMap(outpath);
	}
	
	public SH18CEX() {
		rmin = 0.;
		rmax = 6371.;
	}
	
	public double getCMBElevation(HorizontalPosition position) {
		double dv = io.github.kensuke1984.kibrary.external.SH18CEX.getV(position.toLocation(3480.1));
		return dVsToElevation(dv);
	}
	
	private double dVsToElevation(double dvs) {
		double elevation = 0;
		if (dvs < -1.)
			elevation = -2.;
//		elevation = -2.;
		return elevation;
	}
	
	public void writeCMBElevationMap(Path outpath, StandardOpenOption... options) throws IOException {
		PrintWriter pw = new PrintWriter(outpath.toFile());
		for (int ilon = -180; ilon <= 180; ilon += 2) {
			for (int ilat = -90; ilat <= 90; ilat += 2) {
				HorizontalPosition position = new HorizontalPosition(ilat, ilon);
				pw.println(position.getLongitude() + " " + position.getLatitude() + " " + getCMBElevation(position));
			}
		}
		pw.close();
	}
	
	public double getdlnVp(Location loc) {
		return getdlnVs(loc) / rsp;
	}
	
	public double getdlnVs(Location loc) {
		if (loc.getR() < rmin || loc.getR() > rmax)
			return 0.;
		
		double dlnvs = io.github.kensuke1984.kibrary.external.SH18CEX.getV(loc) * 0.01;
		if (Double.isNaN(dlnvs))
			dlnvs = 0.;
		return dlnvs;
	}
	
	public double getVs(double radius) {
		return PolynomialStructure.PREM.getVshAt(radius);
	}
	
	public double getVp(double radius) {
		return PolynomialStructure.PREM.getVphAt(radius);
	}
	
	@Override
	public void setTruncationRange(double rmin, double rmax) {
		this.rmin = rmin;
		this.rmax = rmax;
	}
	
	@Override
	public void outputLayer(Path outputpath, double r) throws IOException {
		
	}
	
	@Override
	public String getName() {
		return "sh18cex";
	}
	
	public void initVelocityGrid() {
	}
}
