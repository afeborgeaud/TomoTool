package io.github.afeborgeaud.tomotool.topoModel;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;

import io.github.kensuke1984.kibrary.dsminformation.PolynomialStructure;
import io.github.kensuke1984.kibrary.util.HorizontalPosition;
import io.github.kensuke1984.kibrary.util.Location;

public class SEMUCBWM1 implements Seismic3Dmodel {
	
//	private final static double ddepth = 10.;
//	private final static double dlon = 1.;
//	private final static double dlat = 1.;
//	private final static int nlon = 360;
//	private final static int nlat = 181;
//	private final static int ndepth = 284;
	private final static double ddepth = 20.;
	private final static double dlon = 2.;
	private final static double dlat = 2.;
	private static int nlon = 181;
	private static int nlat = 91;
	private static int ndepth = 143;
	
	private double rmin;
	private double rmax;

	private double[][][] modeldlnvs;
	
	private double[] latitudes;
	
	private double[] longitudes;
	
	private double[] depths;
	
	public static void main(String[] args) {
		SEMUCBWM1 model = new SEMUCBWM1();
		double r = Double.parseDouble(args[0]);
		Path outpath = Paths.get(String.format("model_semucb_%.0f.txt", r));
		
		try {
			model.outputLayer(outpath, r);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public SEMUCBWM1() {
		System.out.println("Loading model SEMUCBWM1");
		try {
//			InputStream is = SEMUCBWM1.class.getResourceAsStream("/resources/model_semucb_wholemantle.dat"); // for non-runnable JAR files
			InputStream is = SEMUCBWM1.class.getResourceAsStream("/model_semucb_wholemantle.dat"); // for runnable JAR files
			BufferedReader bufferedReader = 
			            new BufferedReader(new InputStreamReader(is));
			
//			Path resourcePath =  new File("resources").getAbsoluteFile().toPath();
//			Path modelPath = resourcePath.resolve("model_semucb_wholemantle.dat");
//			BufferedReader bufferedReader = Files.newBufferedReader(modelPath);
	        
			rmin = 0.;
			rmax = 6371.;
			
			String line = null;
			
			String[] ss = bufferedReader.readLine().trim().split("\\s+");
			nlon = Integer.parseInt(ss[1]);
			ss = bufferedReader.readLine().trim().split("\\s+");
			nlat = Integer.parseInt(ss[1]);
			ss = bufferedReader.readLine().trim().split("\\s+");
			ndepth = Integer.parseInt(ss[1]);
			
			modeldlnvs = new double[ndepth][nlon][nlat];
			depths = new double[ndepth];
			latitudes = new double[nlat];
			longitudes = new double[nlon];
			
			for (int i = 0; i < nlon; i++)
				longitudes[i] = Double.parseDouble(bufferedReader.readLine());
			for (int i = 0; i < nlat; i++)
				latitudes[i] = Double.parseDouble(bufferedReader.readLine());
			for (int i = 0; i < ndepth; i++)
				depths[i] = Double.parseDouble(bufferedReader.readLine());
			
			int ilat = 0;
			int ilon = 0;
			int idepth = 0;
			
	        while((line = bufferedReader.readLine()) != null) {
	            ss = line.trim().split("\\s+");
	            double dvs = Double.parseDouble(ss[0]);
	            modeldlnvs[idepth][ilon][ilat] = dvs * 0.01;
	            
	            ilon++;
	            if (ilon == nlon) {
	            	ilon = 0;
	            	ilat++; 
	            }
	            if (ilat == nlat) {
	            	ilat = 0;
	            	idepth++;
	            }
	        }   

	        bufferedReader.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	private int getDepthIndex(double depth) {
		int iDepth = (int) ((depth - 40.) / ddepth);
		if (iDepth >= ndepth)
			iDepth = ndepth - 1;
		if (iDepth < 0)
			iDepth = 0;
		return iDepth;
	}
	
	private int getLongitudeIndex(double longitude) {
		double tmplon = longitude > 180 ? longitude - 360. : longitude;
		int iLon = (int) ((tmplon + 180.) / dlon);
		if (iLon >= nlon)
			iLon = nlon - 1;
		return iLon;
	}
	
	private int getLatitudeIndex(double latitude) {
		int iLat = (int) ((latitude + 90.) / dlat);
		if (iLat == nlat)
			iLat = nlat - 1;
		return iLat;
	}
	
	@Override
	public double getCMBElevation(HorizontalPosition position) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public void writeCMBElevationMap(Path outpath, StandardOpenOption... options) throws IOException {
		// TODO Auto-generated method stub
	}

	@Override
	public double getdlnVp(Location loc) {
		return getdlnVs(loc) / 2.;
	}

	@Override
	public double getdlnVs(Location loc) {
		if (loc.getR() < rmin || loc.getR() > rmax)
			return 0.;
		
		double depth = 6371 - loc.getR();
		int iLat = getLatitudeIndex(loc.getLatitude());
		int iLon = getLongitudeIndex(loc.getLongitude());
		int iDepth = getDepthIndex(depth);
		
		return modeldlnvs[iDepth][iLon][iLat];
	}

	@Override
	public double getVs(double radius) {
		return RefSEMUCB.getVsh(radius);
	}

	@Override
	public double getVp(double radius) {
		return RefSEMUCB.getVph(radius);
	}
	
	@Override
	public void setTruncationRange(double rmin, double rmax) {
		System.out.println("Set effective range to " + rmin + " " + rmax);
		this.rmin = rmin;
		this.rmax = rmax;
	}
	
	@Override
	public void outputLayer(Path outputpath, double r) throws IOException {
		PrintWriter pw = new PrintWriter(outputpath.toFile());
		int iDepth = getDepthIndex(6371. - r);
		for (int ilat = 0; ilat < nlat; ilat++) {
			for (int ilon = 0; ilon < nlon; ilon++) {
				pw.printf("%.3f %.3f %.3f %.3f\n", longitudes[ilon], latitudes[ilat],
						depths[iDepth], modeldlnvs[iDepth][ilon][ilat]*100);
			}
		}
		pw.close();
	}
	
	@Override
	public String getName() {
		return "semucbwm1";
	}
	
	public void initVelocityGrid() {
	}
	
}
