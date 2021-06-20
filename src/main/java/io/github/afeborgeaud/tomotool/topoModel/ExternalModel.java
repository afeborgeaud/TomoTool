package io.github.afeborgeaud.tomotool.topoModel;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOError;
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
	
	private Seismic3Dmodel mantleModel;
	
	private int LMAX;
	
	private String modelName;
	
	
	/**
	 * @param modelPath path to the file that describes the CMB topography model
	 * @param modelName identifier for the topography model
	 * @param mantleModelName name of the 3D background model (s20rts, semucb, sh18cex, or llnlg3d)
	 */
	public ExternalModel(String modelPath, String modelName, String mantleModelName) {
		rmin = 0.;
		rmax = 6371.;
		
		if (ReadUtils.isSphFile(modelPath)) coeffs = ReadUtils.readSphFile(modelPath);
		else if (ReadUtils.isSphSpecfemFile(modelPath)) coeffs = ReadUtils.readSphFile_specfem(modelPath);
		else throw new RuntimeException("File format not supported");
		
		LMAX = coeffs.size() - 1;
		this.modelName = modelName;
		
		switch (mantleModelName.toLowerCase()) {
			case "s20rts":
				mantleModel = new S20RTS();
				break;
			case "semucb":
			case "semucbwm1":
			case "semucb-wm1":
				mantleModel = new SEMUCBWM1();
			case "sh18cex":
				mantleModel = new SH18CEX();
			case "llnlg3d":
			case "llnlg3djps":
				mantleModel = new LLNLG3DJPS();
			default:
				throw new RuntimeException("mantleModelName should be s20rts, semucb, llnlg3d, or sh18cex");
		}
	}
	
	/**
	 * Get the CMB elevation at a position in km 
	 * @param position
	 */
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
	
	/**
	 * Write the CMB elevation map to file with 2 degree lat, lon increments
	 */
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
	
	/**
	 * Get Vp anomaly at location loc in percent
	 */
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
	
	public void initVelocityGrid() {
		mantleModel.initVelocityGrid();
	}
}
