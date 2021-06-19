package io.github.afeborgeaud.tomotool.topoModel;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import io.github.kensuke1984.kibrary.dsminformation.PolynomialStructure;
import io.github.kensuke1984.kibrary.util.HorizontalPosition;
import io.github.kensuke1984.kibrary.util.Location;
import io.github.afeborgeaud.tomotool.math.Sph_specfem;
import io.github.afeborgeaud.tomotool.utilities.ReadUtils;

public class S20RTS implements Seismic3Dmodel {
	
	private final static double rsp = 2.; //dlnVs to dlnVp ratio
	
	private double rmin;
	
	private double rmax;
	
	private final double MAX_DLNVS_CMB_20 = 0.017668 ; //at degree 20 0.017668 0.0210415
	private final double MAX_DLNVS_CMB_4 = 0.0167915; //at degree 4
	
	private final double dh_max_negativeanomaly;
	
	private final double dh_max_positiveanomaly;
	
	private double[][][] dlnvsGrid;
	
	private double[][][] dlnvpGrid;
	
	private double[] latGrid, lonGrid, depthGrid;
	
	private double dlat_grid, dlon_grid, dr_grid;
	
	private boolean useVelGrid;
	
	public static void main(String[] args) {
		double dh_max = 0.;
		S20RTS s20rts = new S20RTS(dh_max);
		
		double r = Double.parseDouble(args[0]);
		double lon = Double.parseDouble(args[1]);
		double lat = Double.parseDouble(args[2]);
		Location loc = new Location(lat, lon, r);
		System.out.println(s20rts.getdlnVs(loc));
		s20rts.initVelocityGrid();
		System.out.println(s20rts.getdlnVs(loc));
	}
	
	public S20RTS() {
		this(0);
	}
	
	public S20RTS(double dh_max) {
		rmin = 0.;
		rmax = 6371.;
		
		this.dh_max_negativeanomaly = dh_max;
		this.dh_max_positiveanomaly = dh_max;
		
		this.useVelGrid = false;
		
		read_model_s20rts();
	}
	
	public S20RTS(double dh_max_positiveanomaly, double dh_max_negativeanomaly) {
		rmin = 0.;
		rmax = 6371.;
		
		this.dh_max_negativeanomaly = dh_max_negativeanomaly;
		this.dh_max_positiveanomaly = dh_max_positiveanomaly;
		
		read_model_s20rts();
	}
	
	public void filter(int NLMAX) {
		this.NLMAX = NLMAX;
	}
	
	public void initVelocityGrid() {
		System.out.println("Init velocity Grid for S20RTS");
		useVelGrid = false; // just to be sure
		
		dlat_grid = 2.;
		dlon_grid = 2.;
		int nLat = (int) (180. / dlat_grid);
		int nLon = (int) (360. / dlon_grid);
		dlat_grid = 180. / nLat;
		dlon_grid = 360. / nLon;
		nLat += 1;
		nLon += 1;
		latGrid = new double[nLat];
		lonGrid = new double[nLon];
		
		dr_grid = 10.;
		int nR = (int) (2891. / dr_grid);
		dr_grid = 2891. / nR;
		nR = nR + 1;
		depthGrid = new double[nR];
		
		dlnvsGrid = new double[nR][nLon][nLat];
		
		for (int idepth = 0; idepth < nR; idepth++)
			depthGrid[idepth] = idepth * dr_grid;
		for (int ilon = 0; ilon < nLon; ilon++)
			lonGrid[ilon] = -180. + ilon * dlon_grid;
		for (int ilat = 0; ilat < nLat; ilat++)
			latGrid[ilat] = -90 + ilat * dlat_grid;
		
		System.out.println(depthGrid[nR - 1]);
		depthGrid[nR - 1] = 2891.;
		
//		for (double depth : depthGrid) {
		Arrays.stream(depthGrid).parallel().forEach(depth -> {
//			System.out.println(depth);
			for (double lon : lonGrid) {
				for (double lat : latGrid) {
					Location loc = new Location(lat, lon, 6371. - depth);
					int[] idx = locIndices(loc);
					dlnvsGrid[idx[0]][idx[1]][idx[2]] = getdlnVs(loc);
				}
			}
//		}
		});
		
		useVelGrid = true;
		System.out.println("Done");
	}
	
	private int[] locIndices(Location loc) {
		double depth = 6371. - loc.getR();
		double lon = loc.getLongitude(); 
		if (lon > 180.)
			lon -= 360.;
		int iDepth = (int) (depth / dr_grid);
		int iLon = (int) ((lon + 180.) / dlon_grid);
		int iLat = (int) ((loc.getLatitude() + 90.) / dlat_grid);
		if (iLon == lonGrid.length) iLon -= 1;
		if (iLat == latGrid.length) iLat -= 1;
		return new int[] {iDepth, iLon, iLat};
	}
	
	public double getCMBElevation(HorizontalPosition position) {
		double dvs = getdlnVs(position.toLocation(3480.));
		double MAX_DLNVS_CMB = NLMAX == 20 ? MAX_DLNVS_CMB_20 : MAX_DLNVS_CMB_4;
		if (dvs < 0)
			return dvs / MAX_DLNVS_CMB * dh_max_negativeanomaly;
		else
			return dvs / MAX_DLNVS_CMB * dh_max_positiveanomaly;
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
	
	public void writeLayer(double r, Path outpath, StandardOpenOption... options) throws IOException {
		PrintWriter pw = new PrintWriter(outpath.toFile());
		for (int ilon = -180; ilon < 180; ilon += 2) {
			for (int ilat = -90; ilat <= 90; ilat += 2) {
				Location loc = new Location(ilat, ilon, r);
				pw.println(loc.getLongitude() + " " + loc.getLatitude() + " " + loc.getR() 
					+ " " + getdlnVs(loc)*100 + " " + getdlnVp(loc)*100);
			}
		}
		pw.close();
	}
	
	public double getdlnVp(Location loc) {
		if (loc.getR() < rmin || loc.getR() > rmax)
			return 0.;
		
		double lon = loc.getLongitude();
		if (lon > 180.)
			lon -= 360.;
		double phi = loc.getLongitude() + 180.;
		phi = Math.toRadians(phi);
		double theta = Math.toRadians(90. - loc.getLatitude());
		
		return mantle_s20rts(loc.getR(), theta, phi)[1];
	}
	
	public double getdlnVs(Location loc) {
		if (loc.getR() < rmin || loc.getR() > rmax)
			return 0.;
		
		if (!useVelGrid)
			return mantle_s20rts_fromLoc(loc)[0];
		else {
//			System.out.println("Using grid");
//			int[] idx = locIndices(loc);
//			return dlnvsGrid[idx[0]][idx[1]][idx[2]];
			return interpolateDlnvsGrid(loc);
		}
	}
	
	private double interpolateDlnvsGrid(Location loc) {
		int[] idx = locIndices(loc);
		int idep = idx[0];
		int ilat = idx[2];
		int ilon = idx[1];
		double lat0 = latGrid[ilat];
		double lon0 = lonGrid[ilon];
		double dep0 = depthGrid[idep];
		
		int idep1 = idep+1;
		if (idep1 == depthGrid.length) idep1 = idep;
		int ilat1 = ilat == latGrid.length - 1 ? ilat : ilat+1;
		int ilon1 = ilon == lonGrid.length - 1 ? ilon : ilon+1;
		
		double lat = loc.getLatitude();
		double dep = 6371. - loc.getR();
		double lon = loc.getLongitude();
		
		double dlnvs0 = dlnvsGrid[idep][ilon][ilat];
		double dlnvs0_dep = dlnvsGrid[idep1][ilon][ilat];
		
		double dlnvs_lon = dlnvs0 + (lon - lon0)/dlon_grid * (dlnvsGrid[idep][ilon1][ilat] - dlnvs0);
		double dlnvs_lon_lat = dlnvsGrid[idep][ilon][ilat1] + (lon - lon0)/dlon_grid * (dlnvsGrid[idep][ilon1][ilat1] - dlnvsGrid[idep][ilon][ilat1]);
		double dlnvs = dlnvs_lon + (lat - lat0) / dlat_grid * (dlnvs_lon_lat - dlnvs_lon);
		
		double dlnvs_lon_dep = dlnvs0_dep + (lon - lon0)/dlon_grid * (dlnvsGrid[idep1][ilon+1][ilat] - dlnvs0_dep);
		double dlnvs_lon_lat_dep = dlnvsGrid[idep1][ilon][ilat1] + (lon - lon0)/dlon_grid * (dlnvsGrid[idep1][ilon1][ilat1] - dlnvsGrid[idep1][ilon][ilat1]);
		double dlnvs_dep = dlnvs_lon_dep + (lat - lat0) / dlat_grid * (dlnvs_lon_lat_dep - dlnvs_lon_dep);
		
		double dlnvs_ = dlnvs + (dep - dep0) / dr_grid * (dlnvs_dep - dlnvs);
		
		return dlnvs_;
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
	
	private int nk_20 = 20;
	
	private int ns_20 = 20;
	
	private double[] S20RTS_V_spknt;
	
	private double[][] S20RTS_V_qq0;
	
	private double[][][] S20RTS_V_qq;
	
	private double[][][] S20RTS_V_dvs_a;
	
	private double[][][] S20RTS_V_dvs_b;
	
	private double[][][] S20RTS_V_dvp_a;
	
	private double[][][] S20RTS_V_dvp_b;
	
	private final double RMOHO_ = 6346.4;
	private final double R_EARTH_ = 6371.;
	private final double RCMB_ = 3480.;
	private final double SCALE_RHO = 0.4;
	
	private int NLMAX = 20;
	
	private void read_model_s20rts() {
		System.out.println("Loading model S20RTS");
		S20RTS_V_dvs_a = new double[nk_20 + 1][ns_20 + 1][ns_20 + 1];
		S20RTS_V_dvs_b = new double[nk_20 + 1][ns_20 + 1][ns_20 + 1];
		S20RTS_V_dvp_a = new double[nk_20 + 1][ns_20 + 1][ns_20 + 1];
		S20RTS_V_dvp_b = new double[nk_20 + 1][ns_20 + 1][ns_20 + 1];

		//TODO
	  // for non-runnable JAR files
//		  String S20RTS_ = "/resources/S20RTS.dat";
//		  String P12_ = "/resources/P12.dat";
	  // for runnable JAR files
	  String S20RTS_ = "/S20RTS.dat";
	  String P12_ = "/P12.dat";
	  
	  String line;
	  
	  try {
		 //S20RTS degree 20 S model from Ritsema
		  BufferedReader bufferedReader = 
		            new BufferedReader(new InputStreamReader(S20RTS.class.getResourceAsStream(S20RTS_)));
		  
		  List<Double> coeffs = new ArrayList<>();
		  while((line = bufferedReader.readLine()) != null) {
				Arrays.stream(line.trim().split("\\s+")).forEach(s -> coeffs.add(Double.parseDouble(s)));
		  }
		  int i = 0;
		  for (int k = 0; k < nk_20 + 1; k++) {
			  int l = 0;
			  int j = 0;
			  do {
				  S20RTS_V_dvs_a[k][l][0] = coeffs.get(i++);
				  j++;
				  for (int m = 1; m < l + 1; m++) {
					  S20RTS_V_dvs_a[k][l][m] = coeffs.get(i++);
					  S20RTS_V_dvs_b[k][l][m] = coeffs.get(i++);
					  j += 2;
				  }
				  l++;
			  } while (j < (ns_20 + 1) * (ns_20 + 1));
		  }
		  

		 //P12 degree 12 P model from Ritsema
		  bufferedReader = 
		            new BufferedReader(new InputStreamReader(S20RTS.class.getResourceAsStream(P12_)));
		  
		  List<Double> coeffsP = new ArrayList<>();
		  while((line = bufferedReader.readLine()) != null) {
				Arrays.stream(line.trim().split("\\s+")).forEach(s -> coeffsP.add(Double.parseDouble(s)));
		  }
		  i = 0;
		  for (int k = 0; k < nk_20 + 1; k++) {
			  int l = 0;
			  int j = 0;
			  do {
				  S20RTS_V_dvp_a[k][l][0] = coeffs.get(i++);
				  j++;
				  for (int m = 1; m < l + 1; m++) {
					  S20RTS_V_dvp_a[k][l][m] = coeffs.get(i++);
					  S20RTS_V_dvp_b[k][l][m] = coeffs.get(i++);
					  j += 2;
				  }
				  l++;
			  } while (j < (ns_20 + 1) * (ns_20 + 1));
		  }
	  } catch (IOException e) {
		  e.printStackTrace();
	  }

	  //set up the splines used as radial basis functions by Ritsema
	  s20rts_splhsetup();
	}
	
	private double[] mantle_s20rts_fromLoc(Location loc) {
		double lon = loc.getLongitude();
		if (lon > 180.)
			lon -= 360.;
		double phi = loc.getLongitude() + 180.;
		phi = lon;
		phi = Math.toRadians(phi);
		double theta = Math.toRadians(90. - loc.getLatitude());
		
		return mantle_s20rts(loc.getR(), theta, phi);
	}
	
	private double[] mantle_s20rts(double radius, double theta, double phi) {
	  double dvs = 0;
	  double dvp = 0;
	  double drho = 0;
	  
	  double[] radial_basis = new double[nk_20 + 1];
	  
	  double r_moho = RMOHO_;
	  double r_cmb = RCMB_;
	  if (radius >= r_moho || radius < r_cmb) {
//		  System.out.println(radius);
		  return new double[] {0., 0., 0.};
	  }

	  double xr=-1.0+2.0*(radius-r_cmb)/(r_moho-r_cmb);
	  for (int k = 0; k < nk_20+1; k++) {
	    radial_basis[k]=s20rts_rsple(0,nk_20,S20RTS_V_spknt,S20RTS_V_qq0[nk_20-k],S20RTS_V_qq[nk_20-k],xr);
	  }

	  for (int l = 0; l<=NLMAX; l++) {
	    double[] x = new double[l + 1];
	    for (int m = 0; m < l + 1; m++)
	    	x[m] = Sph_specfem.xlm(l, m, theta);

	    double dvs_alm=0.0;
	    double dvp_alm=0.0;
	    for (int k = 0; k < nk_20 + 1; k++) {
	      dvs_alm=dvs_alm+radial_basis[k]*S20RTS_V_dvs_a[k][l][0];
	      dvp_alm=dvp_alm+radial_basis[k]*S20RTS_V_dvp_a[k][l][0];
	    }
	    dvs=dvs+dvs_alm*x[0];
	    dvp=dvp+dvp_alm*x[0];

	    for (int m = 1; m < l + 1; m++) {
	      dvs_alm=0.0;
	      dvp_alm=0.0;
	      double dvs_blm=0.0;
	      double dvp_blm=0.0;
	      for (int k = 0; k < nk_20 + 1; k++) {
	        dvs_alm=dvs_alm+radial_basis[k]*S20RTS_V_dvs_a[k][l][m];
	        dvp_alm=dvp_alm+radial_basis[k]*S20RTS_V_dvp_a[k][l][m];
	        dvs_blm=dvs_blm+radial_basis[k]*S20RTS_V_dvs_b[k][l][m];
	        dvp_blm=dvp_blm+radial_basis[k]*S20RTS_V_dvp_b[k][l][m];
	      }
	      dvs=dvs+(dvs_alm*Math.cos(m*phi)+dvs_blm*Math.sin(m*phi))*x[m];
	      dvp=dvp+(dvp_alm*Math.cos(m*phi)+dvp_blm*Math.sin(m*phi))*x[m];
		}

	  }

	  drho = SCALE_RHO*dvs;
	  
	  return new double[] {dvs, dvp, drho};
	}
	
	private void s20rts_splhsetup() {
		S20RTS_V_qq0 = new double[nk_20 + 1][nk_20 + 1];
		S20RTS_V_qq = new double[nk_20 + 1][3][ns_20 + 1];
		
		S20RTS_V_spknt = new double[21];
		  S20RTS_V_spknt[0] = -1.00000;
		  S20RTS_V_spknt[1] = -0.78631;
		  S20RTS_V_spknt[2] = -0.59207;
		  S20RTS_V_spknt[3] = -0.41550;
		  S20RTS_V_spknt[4] = -0.25499;
		  S20RTS_V_spknt[5] = -0.10909;
		  S20RTS_V_spknt[6] = 0.02353;
		  S20RTS_V_spknt[7] = 0.14409;
		  S20RTS_V_spknt[8] = 0.25367;
		  S20RTS_V_spknt[9] = 0.35329;
		  S20RTS_V_spknt[10] = 0.44384;
		  S20RTS_V_spknt[11] = 0.52615;
		  S20RTS_V_spknt[12] = 0.60097;
		  S20RTS_V_spknt[13] = 0.66899;
		  S20RTS_V_spknt[14] = 0.73081;
		  S20RTS_V_spknt[15] = 0.78701;
		  S20RTS_V_spknt[16] = 0.83810;
		  S20RTS_V_spknt[17] = 0.88454;
		  S20RTS_V_spknt[18] = 0.92675;
		  S20RTS_V_spknt[19] = 0.96512;
		  S20RTS_V_spknt[20] = 1.00000;
		  
		  for (int i = 0; i < nk_20 + 1; i++) {
			  for (int j = 0; j < nk_20 + 1; j++) {
				  if (i == j) S20RTS_V_qq0[i][j] = 1.;
				  else S20RTS_V_qq0[i][j] = 0.;
			  }
		  }
		  
		  for (int i = 0; i < nk_20 + 1; i++) {
			  S20RTS_V_qq[i] = s20rts_rspln(0, nk_20, S20RTS_V_spknt, S20RTS_V_qq0[i]);
		  }
	}
	
	private double s20rts_rsple(int I1, int I2, double[] X, double[] Y, double[][] Q, double S) {
	      double H;

	      int I = 1;
	      int II=I2-1;

	//   GUARANTEE I WITHIN BOUNDS.
	      I=Math.max(I,I1);
	      I=Math.max(I,II);
	      
	      int LABEL = 0;

	//   SEE IF X IS INCREASING OR DECREASING.
	  do {
	      if (X[I2]-X[I1] <  0) LABEL = 1;
	      if (X[I2]-X[I1] >= 0) LABEL = 2;

		//   X IS DECREASING.  CHANGE I AS NECESSARY.
		 //1
		 if (LABEL == 1) {
		 if (S-X[I] <= 0) LABEL = 3;
		 if (S-X[I] >  0) LABEL = 4;
		 }
		      
		 //4
		 if (LABEL == 4) {
		  I=I-1;
	
	      if (I-I1 <  0) LABEL = 11;
	      if (I-I1 == 0) LABEL = 6;
	      if (I-I1 >  0) LABEL = 1;
		 }
	
		if (LABEL == 3) {
		     if (S-X[I+1] <  0) LABEL = 5;
		      if (S-X[I+1] >= 0) LABEL = 6;
	
		}
		if (LABEL == 5 ) {
		    I=I+1;
	
		      if (I-II <  0) LABEL = 3;
		      if (I-II == 0) LABEL = 6;
		      if (I-II >  0) LABEL = 7;
		}
		 if (LABEL == 2) {
		//   X IS INCREASING.  CHANGE I AS NECESSARY.
		      //2
		     if (S-X[I+1] <= 0) LABEL = 8;
		      if (S-X[I+1] >  0) LABEL = 9;
		 }
		 if (LABEL == 9) {
		      //9
		    I=I+1;
	
		      if (I-II <  0) LABEL = 2;
		      if (I-II == 0) LABEL = 6;
		      if (I-II >  0) LABEL = 7;
		 }
		 if (LABEL == 8) {
		      //8
		     if (S-X[I] <  0) LABEL = 10;
		     if (S-X[I] >= 0) LABEL = 6;
		}
		if (LABEL == 10) {
		      //10
		    I=I-1;
		      if (I-I1 <  0) LABEL = 11;
		      if (I-I1 == 0) LABEL = 6;
		      if (I-I1 >  0) LABEL = 8;
		}
		if (LABEL == 7) {
		      //7
		  I=II;
		    LABEL = 6;
		  }
		 if (LABEL == 11) {
		      //11
		    I=I1;
		    LABEL = 6;
		 }
	} while (LABEL != 6);

	//   CALCULATE RSPLE USING SPLINE COEFFICIENTS IN Y AND Q.
	      //6
	  H=S-X[I];
	  return Y[I]+H*(Q[0][I]+H*(Q[1][I]+H*Q[2][I]));
	}
	
	private double[][] s20rts_rspln(int I1, int I2, double[] X, double[] Y) {
		final double small = 1e-8;
		
		double[][] Q = new double[3][X.length];
		
		double[][] F = new double[3][X.length];
		
		double[] YY = new double[] {0, 0, 0};
		
		int J1=I1+1;
		int J2 = 0;
		double Y0=0.0;
		
		//   BAIL OUT IF THERE ARE LESS THAN TWO POINTS TOTAL
	    if (I2-I1  < 0) return null;
	    
	    do  {
	      if (I2-I1  > 0) {
	    	  //8
	     double A0=X[J1-1];
	//   SEARCH FOR DISCONTINUITIES.
	      for (int i=J1; i < I2; i++) {
		      double B0=A0;
		      A0=X[i];
			      if (Math.abs((A0-B0)/Math.max(A0,B0)) < small) {
			    	  J1=J1-1;
			    	  J2=i-3;
			      }
		      }
	      }
	      else if (I2-I1 == 0) {
	    	  //17
	      J1=J1-1;
	      J2=I2-2;
	      }
	//   SEE IF THERE ARE ENOUGH POINTS TO INTERPOLATE (AT LEAST THREE).
	      if (J2+1-J1 == 0)  { //10
	    	//   ONLY TWO POINTS.  USE LINEAR INTERPOLATION.
		    J2=J2+2;
		      Y0=(Y[J2]-Y[J1])/(X[J2]-X[J1]);
		      for (int j=0; j < 3; j++) {
		        Q[j][J1]=YY[j];
		        Q[j][J2]=YY[j];
		      }
//	    		      GOTO 12
	      }
	      else if (J2+1-J1 >  0) { //11
	    	//   MORE THAN TWO POINTS.  DO SPLINE INTERPOLATION.
	    		 double A0 = 0.;
	    		      double H=X[J1+1]-X[J1];
	    		      double H2=X[J1+2]-X[J1];
	    		      Y0=H*H2*(H2-H);
	    		      H=H*H;
	    		      H2=H2*H2;
	    		//   CALCULATE DERIVATIVE AT NEAR END.
	    		      double B0=(Y[J1]*(H-H2)+Y[J1+1]*H2-Y[J1+2]*H)/Y0;
	    		      double B1=B0;

	    		//   EXPLICITLY REDUCE BANDED MATRIX TO AN UPPER BANDED MATRIX.
	    		      double HA, H2A, H3A, H2B;
	    		      for (int I=J1-1; I <J2; I++) {
	    		        H=X[I+1]-X[I];
	    		        Y0=Y[I+1]-Y[I];
	    		        H2=H*H;
	    		        HA=H-A0;
	    		        H2A=H-2.*A0;
	    		         H3A=2.*H-3.*A0;
	    		         H2B=H2*B0;
	    		        Q[1][I]=H2/HA;
	    		        Q[2][I]=-HA/(H2A*H2);
	    		        Q[3][I]=-H*H2A/H3A;
	    		        F[1][I]=(Y0-H*B0)/(H*HA);
	    		        F[2][I]=(H2B-Y0*(2.0*H-A0))/(H*H2*H2A);
	    		        F[3][I]=-(H2B-3.0*Y0*HA)/(H*H3A);
	    		        A0=Q[3][I];
	    		        B0=F[3][I];
	    		      }

	    		//   TAKE CARE OF LAST TWO ROWS.
	    		      int I=J2+1;
	    		      H=X[I+1]-X[I];
	    		      Y0=Y[I+1]-Y[I];
	    		      H2=H*H;
	    		      HA=H-A0;
	    		      H2A=H*HA;
	    		      H2B=H2*B0-Y0*(2.0*H-A0);
	    		      Q[1][I]=H2/HA;
	    		      F[1][I]=(Y0-H*B0)/H2A;
	    		      HA=X[J2]-X[I+1];
	    		      Y0=-H*HA*(HA+H);
	    		      HA=HA*HA;

	    		//   CALCULATE DERIVATIVE AT FAR END.
	    		      Y0=(Y[I+1]*(H2-HA)+Y[I]*HA-Y[J2]*H2)/Y0;
	    		      Q[3][I]=(Y0*H2A+H2B)/(H*H2*(H-2.0*A0));
	    		      Q[2][I]=F[1][I]-Q[1][I]*Q[3][I];

	    		//   SOLVE UPPER BANDED MATRIX BY REVERSE ITERATION.
	    		      for (int J=J1-1; J <J2; J++) {
	    		        int K=I-1;
	    		        Q[1][I]=F[3][K]-Q[3][K]*Q[2][I];
	    		        Q[3][K]=F[2][K]-Q[2][K]*Q[1][I];
	    		        Q[2][K]=F[1][K]-Q[1][K]*Q[3][K];
	    		        I=K;
	    		      }
	    		      Q[1][I]=B1;
	      }
	      if (J2+1-J1 <  0 || J2+1-J1 > 0) { //9
		    	//   FILL IN THE LAST POINT WITH A LINEAR EXTRAPOLATION.
		    	  J2=J2+2;
		    	  for (int j=0; j < 3; j++)
		    		  Q[j][J2]=YY[j];
		  }

	//   SEE IF THIS DISCONTINUITY IS THE LAST.
	      //12
	      if (J2-I2 >= 0)
	    	  return Q;
	      
	      J1=J2+2;
      } while (J1-I2 <= 0);
	//   NO.  GO BACK FOR MORE.
	      //6
	     

	//   THERE IS ONLY ONE POINT LEFT AFTER THE LATEST DISCONTINUITY.
	      //7
	  for (int J=0; J<3; J++)
	        Q[J][I2]=YY[J];
	  
	  return Q;
	}
	
	public void writeTopoSph(Path outpath) {
		double MAX_DLNVS_CMB = NLMAX == 20 ? MAX_DLNVS_CMB_20 : MAX_DLNVS_CMB_4;
		try (PrintWriter pw = new PrintWriter(outpath.toFile())) {
			int l = 0;
			int i = 0;
			double xr = -1.0 + 2.0 * (3480.-RCMB_)/(RMOHO_-RCMB_);
			double ck = s20rts_rsple(0,nk_20,S20RTS_V_spknt,S20RTS_V_qq0[0],S20RTS_V_qq[0],xr);
			pw.println(nk_20);
			do {
				pw.printf("%.5f ", ck * S20RTS_V_dvs_a[nk_20][l][0] / MAX_DLNVS_CMB * dh_max_negativeanomaly);
				i++;
				if (i % (nk_20+1) == 0) pw.println();
				for (int m = 1; m <= l; m++) {
					pw.printf("%.5f ", ck * S20RTS_V_dvs_a[nk_20][l][m] / MAX_DLNVS_CMB * dh_max_negativeanomaly);
					i++;
					if (i % (nk_20+1) == 0) pw.println();
					pw.printf("%.5f ", ck * S20RTS_V_dvs_b[nk_20][l][m] / MAX_DLNVS_CMB * dh_max_negativeanomaly);
					i++;
					if (i % (nk_20+1) == 0) pw.println();
				}
				l++;
			} while (i < (nk_20 + 1) * (nk_20 + 1));
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	@Override
	public String getName() {
		String s = "s20rts";
		if (NLMAX != nk_20) s += String.format("_filter%d", NLMAX);
		if (dh_max_negativeanomaly > 0) s += String.format("_plus_%.0f", dh_max_negativeanomaly);
		if (dh_max_negativeanomaly < 0) s += String.format("_minus_%.0f", -dh_max_negativeanomaly);
		return s;
	}
}
