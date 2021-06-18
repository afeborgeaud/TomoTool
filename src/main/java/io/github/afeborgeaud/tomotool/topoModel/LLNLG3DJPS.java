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

public class LLNLG3DJPS implements Seismic3Dmodel {
	
	private final static double ddepth = 20.;
	private final static double dlon = 2.;
	private final static double dlat = 2.;
	private static int nlon = 180;
	private static int nlat = 91;
	private static int ndepth = 143;
	
	private double rmin;
	private double rmax;

	private double[][][][] modeldlnvpvs;
	
	private double[] latitudes;
	
	private double[] longitudes;
	
	private double[] depths;
	
	public static void main(String[] args) {
		LLNLG3DJPS model = new LLNLG3DJPS();
		double r = Double.parseDouble(args[0]);
		Path outpath = Paths.get(String.format("model_llnlg3djps_%.0f.txt", r));
		
		try {
			model.outputLayer(outpath, r);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public LLNLG3DJPS() {
		System.out.println("Loading model LLNLG3DJPS");
		rmin = 0.;
		rmax = 6371.;
		
		try {
			//TODO
//			InputStream is = LLNLG3DJPS.class.getResourceAsStream("/resources/model_llnl3djps_wholemantle.dat"); // for non-runnable JAR files
			InputStream is = LLNLG3DJPS.class.getResourceAsStream("/model_llnl3djps_wholemantle.dat"); // for runnable JAR files
			BufferedReader bufferedReader = 
			            new BufferedReader(new InputStreamReader(is));
			
//			Path resourcePath =  new File("resources").getAbsoluteFile().toPath();
//			Path modelPath = Paths.get("/home/navy/git/TopoCMB/resources/model_llnl3djps_wholemantle.dat");
//			BufferedReader bufferedReader = Files.newBufferedReader(modelPath);
			
			String[] ss = bufferedReader.readLine().trim().split("\\s+");
			nlon = Integer.parseInt(ss[1]);
			ss = bufferedReader.readLine().trim().split("\\s+");
			nlat = Integer.parseInt(ss[1]);
			ss = bufferedReader.readLine().trim().split("\\s+");
			ndepth = Integer.parseInt(ss[1]);
			
			modeldlnvpvs = new double[ndepth][nlon][nlat][2];
			
			depths = new double[ndepth];
			latitudes = new double[nlat];
			longitudes = new double[nlon];
			
			for (int i = 0; i < nlon; i++)
				longitudes[i] = Double.parseDouble(bufferedReader.readLine());
			for (int i = 0; i < nlat; i++)
				latitudes[i] = Double.parseDouble(bufferedReader.readLine());
			for (int i = 0; i < ndepth; i++)
				depths[i] = Double.parseDouble(bufferedReader.readLine());
			
			String line = null;
			int ilat = 0;
			int ilon = 0;
			int idepth = 0;
			
	        while((line = bufferedReader.readLine()) != null) {
	            ss = line.trim().split("\\s+");
	            double vp = Double.parseDouble(ss[0]);
	            double vs = Double.parseDouble(ss[1]);
	            
	            double depth = depths[idepth];
	            
	            double vp0 = RefLLNLG3DJPS.getVp(6371. - depth);
	            double vs0 = RefLLNLG3DJPS.getVs(6371. - depth);
	            
	            modeldlnvpvs[idepth][ilon][ilat][0] = (vp - vp0) / vp0;
	            modeldlnvpvs[idepth][ilon][ilat][1] = (vs - vs0) / vs0;
	            
	            ilat++;
	            if (ilat == nlat) {
	            	ilat = 0;
	            	ilon++; 
	            }
	            if (ilon == nlon) {
	            	ilon = 0;
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
		if (loc.getR() < rmin || loc.getR() > rmax)
			return 0.;
		
		double depth = 6371 - loc.getR();
		int iLat = getLatitudeIndex(loc.getLatitude());
		int iLon = getLongitudeIndex(loc.getLongitude());
		int iDepth = getDepthIndex(depth);
		
		return modeldlnvpvs[iDepth][iLon][iLat][0];
	}

	@Override
	public double getdlnVs(Location loc) {
		if (loc.getR() < rmin || loc.getR() > rmax)
			return 0.;
		
		double depth = 6371 - loc.getR();
		int iLat = getLatitudeIndex(loc.getLatitude());
		int iLon = getLongitudeIndex(loc.getLongitude());
		int iDepth = getDepthIndex(depth);
		
		return modeldlnvpvs[iDepth][iLon][iLat][1];
	}

	@Override
	public double getVs(double radius) {
		return RefLLNLG3DJPS.getVs(radius);
	}

	@Override
	public double getVp(double radius) {
		return RefLLNLG3DJPS.getVp(radius);
	}
	
	public void setTruncationRange(double rmin, double rmax) {
		System.out.println("Set effective range to " + rmin + " " + rmax);
		this.rmin = rmin;
		this.rmax = rmax;
	}
	
	@Override
	public void outputLayer(Path outputpath, double r) throws IOException {
		PrintWriter pw = new PrintWriter(outputpath.toFile());
		int iDepth = getDepthIndex(6371. - r);
		for (int ilon = 0; ilon < nlon; ilon++) {
			for (int ilat = 0; ilat < nlat; ilat++) {
				pw.printf("%.3f %.3f %.3f %.3f %.3f\n", longitudes[ilon], latitudes[ilat],
						depths[iDepth], modeldlnvpvs[iDepth][ilon][ilat][0]*100, modeldlnvpvs[iDepth][ilon][ilat][1]*100);
			}
		}
		pw.close();
	}
	
	@Override
	public String getName() {
		return "llnlg3djps";
	}
	
	public void initVelocityGrid() {
	}
}
