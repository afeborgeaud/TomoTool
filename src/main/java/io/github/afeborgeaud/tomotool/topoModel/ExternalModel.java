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

import org.apache.commons.math3.linear.ArrayRealVector;

public class ExternalModel implements Seismic3Dmodel {
	
	private final static double rsp = 2.; //dlnVs to dlnVp ratio
	
	private double rmin;
	
	private double rmax;
	
	private List<List<Double>> coeffs;
	
	private S20RTS mantleModel;
	
	private int LMAX;
	
	private String modelName;
	
	public static void main(String[] args) throws IOException {
		String model_path = "/home/anselme/Dropbox/topo_eth_local/models/geodynamics/CMBtopo-T1_SHcoef_rot_l20.ylm";
		ExternalModel model = new ExternalModel(model_path, "T1", false);
		
		ArrayRealVector topos = new ArrayRealVector(180*360);
		int i = 0;
		for (int lon = -180; lon < 180; lon +=2) {
			for (int lat = -90; lat < 90; lat += 2) {
				double topo = model.getCMBElevation(new HorizontalPosition(lat, lon));
				topos.setEntry(i, topo);
				i++;
			}
		}
		
		System.out.println(topos.getLInfNorm());
	}
	
	public ExternalModel(String model_path, String model_name, boolean useGrid) {
//		System.out.println("Loading model GaussianPointPerturbation");
		rmin = 0.;
		rmax = 6371.;
		
		coeffs = ReadUtils.readSphFile_specfem(model_path);
		LMAX = coeffs.size();
		this.modelName = model_name;
		
		mantleModel = new S20RTS();
		
		if (useGrid)
			mantleModel.initVelocityGrid();
	}
	
	public double getCMBElevation(HorizontalPosition position) {
		double lon = position.getLongitude();
		if (lon > 180.)
			lon -= 360.;
		double phi = position.getLongitude() + 180.;
		phi = Math.toRadians(phi);
		double theta = Math.toRadians(90. - position.getLatitude());
		double dr = Sph_specfem.eval(phi, theta, coeffs, LMAX);
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
	
	public void filter(int LMAX) {
		this.LMAX = LMAX;
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
		return modelName;
	}
}
