package io.github.afeborgeaud.tomotool.topoModel;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.List;

import io.github.kensuke1984.kibrary.dsminformation.PolynomialStructure;
import io.github.kensuke1984.kibrary.util.HorizontalPosition;
import io.github.kensuke1984.kibrary.util.Location;
import io.github.afeborgeaud.tomotool.math.Sph;
import io.github.afeborgeaud.tomotool.math.Sph_specfem;
import io.github.afeborgeaud.tomotool.utilities.ReadUtils;

public class GaussianPointPerturbation implements Seismic3Dmodel {
	
	private final static double rsp = 2.; //dlnVs to dlnVp ratio
	
	private double rmin;
	
	private double rmax;
	
	private List<List<Double>> coeffs;
	
	private Seismic3Dmodel mantleModel = new SEMUCBWM1(); 
	
	public static void main(String[] args) throws IOException {
		GaussianPointPerturbation gauss = new GaussianPointPerturbation();
//		double dh = tk10.getCMBElevation(new HorizontalPosition(0, 0));
//		System.out.println(dh);
		Path outpath = Paths.get("gauss_ca_1000_8_40.ylm");
		gauss.writeCMBElevationMap(outpath);
	}
	
	public GaussianPointPerturbation() {
		System.out.println("Loading model GaussianPointPerturbation");
		rmin = 0.;
		rmax = 6371.;
		
		//TODO
//		String GAUSS_ = "/resources/gauss_ca_1000_8_20.ylm"; // for non-runnable JAR files
		String GAUSS_ = "/gauss_ca_1000_8_20.ylm"; // for runnable JAR files
		coeffs = ReadUtils.readSphFile_specfem(
				GaussianPointPerturbation.class.getResourceAsStream(GAUSS_));
	}
	
	public double getCMBElevation(HorizontalPosition position) {
		double lon = position.getLongitude();
		if (lon > 180.)
			lon -= 360.;
		double phi = position.getLongitude() + 180.;
		phi = Math.toRadians(phi);
		double theta = Math.toRadians(90. - position.getLatitude());
		double dr = Sph_specfem.eval(phi, theta, coeffs);
		return dr;
	}
	
	public void writeCMBElevationMap(Path outpath, StandardOpenOption... options) throws IOException {
		PrintWriter pw = new PrintWriter(outpath.toFile());
		for (int ilon = -179; ilon <= 180; ilon += 2) {
			for (int ilat = -89; ilat <= 89; ilat += 2) {
				HorizontalPosition position = new HorizontalPosition(ilat, ilon);
				pw.println(position.getLongitude() + " " + position.getLatitude() + " " + getCMBElevation(position));
			}
		}
		pw.close();
	}
	
	public double getdlnVp(Location loc) {
		return mantleModel.getdlnVp(loc);
	}
	
	public double getdlnVs(Location loc) {
		return mantleModel.getdlnVs(loc);
	}
	
	public double getVs(double radius) {
		return mantleModel.getVs(radius);
	}
	
	public double getVp(double radius) {
		return mantleModel.getVp(radius);
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
		return "gauss";
	}
	
	public void initVelocityGrid() {
	}
}
