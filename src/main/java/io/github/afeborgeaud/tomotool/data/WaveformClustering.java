package io.github.afeborgeaud.tomotool.data;

import java.awt.BasicStroke;
import java.awt.Color;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import javax.swing.JFrame;

import org.apache.commons.math3.linear.ArrayRealVector;
import org.apache.commons.math3.linear.RealVector;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.annotations.XYTextAnnotation;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.data.xy.XYDataset;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;

import de.lmu.ifi.dbs.elki.algorithm.clustering.hierarchical.PointerHierarchyRepresentationResult;
import de.lmu.ifi.dbs.elki.algorithm.clustering.hierarchical.SLINK;
import de.lmu.ifi.dbs.elki.data.Cluster;
import de.lmu.ifi.dbs.elki.data.Clustering;
import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.data.model.DendrogramModel;
import de.lmu.ifi.dbs.elki.data.type.TypeUtil;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.database.ids.DBIDIter;
import de.lmu.ifi.dbs.elki.database.ids.DBIDRange;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.distance.distancefunction.DistanceFunction;
import de.lmu.ifi.dbs.elki.utilities.datastructures.iterator.It;
import io.github.kensuke1984.kibrary.timewindow.TimewindowInformation;
import io.github.kensuke1984.kibrary.timewindow.TimewindowInformationFile;
import io.github.kensuke1984.kibrary.util.sac.SACFileName;
import io.github.afeborgeaud.tomotool.ml.clustering.WaveformDatabase;
import io.github.afeborgeaud.tomotool.ml.clustering.WaveformDistanceFunction;
import de.lmu.ifi.dbs.elki.algorithm.clustering.hierarchical.extraction.CutDendrogramByHeight;

public class WaveformClustering {
	final RealVector[] waveforms;
	
	Clustering<DendrogramModel> dendrogram;
	
	Database db;
	
	final double cutHeight;
	
	public static void main(String[] args) {
		if (args.length != 2)
			throw new RuntimeException("Usage: WaveformClustering.java path_to_timewindow_file path_to_sac_file_dir");
		Path timewindowPath = Paths.get(args[0]);
		Path sacpath = Paths.get(args[1]);
		
		List<TimewindowInformation> timewindows;
		try {
			timewindows = TimewindowInformationFile.read(timewindowPath).stream()
					.collect(Collectors.toList());
			WaveformClustering clustering = new WaveformClustering(timewindows, sacpath, 0.015);
			clustering.run();
			clustering.printResult();
			clustering.displayResultInRecordSection();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
	}
	
	public WaveformClustering(RealVector[] waveforms, double cutHeight) {
		this.waveforms = waveforms;
		this.cutHeight = cutHeight;
	}
	
	public WaveformClustering(List<TimewindowInformation> timewindows, Path sacpath, double cutHeight) {
		this.waveforms = new RealVector[timewindows.size()];
		setWaveforms(timewindows, sacpath);
		datasamplinghz = 20;
		this.cutHeight = cutHeight;
	}
	
	public WaveformClustering(List<TimewindowInformation> timewindows, Path sacpath) {
		this.waveforms = new RealVector[timewindows.size()];
		setWaveforms(timewindows, sacpath);
		datasamplinghz = 20;
		this.cutHeight = 0.02;
	}
	
	public void run() {
		double[][] waveformdata = new double[waveforms.length][];
		int maxwaveformlength = -1;
		for (RealVector waveform : waveforms) {
			if (waveform.getDimension() > maxwaveformlength)
				maxwaveformlength = waveform.getDimension();
		}
		for (int i = 0; i < waveforms.length; i++) {
			waveformdata[i] = Arrays.copyOf(waveforms[i].toArray(), maxwaveformlength);
		}
		
		db = WaveformDatabase.getDatabase(waveformdata);
		db.initialize();
		
		DistanceFunction<NumberVector> dist = new WaveformDistanceFunction();
		
		SLINK<NumberVector> slink = new SLINK<NumberVector>(dist);
		PointerHierarchyRepresentationResult result = slink.run(db);

		CutDendrogramByHeight cut = new CutDendrogramByHeight(slink, cutHeight, true);
		dendrogram = cut.run(result);
		
	}
	
	private void printResult() {
		Relation<NumberVector> rel = db.getRelation(TypeUtil.NUMBER_VECTOR_FIELD);
		DBIDRange ids = (DBIDRange) rel.getDBIDs();
		
		Cluster<DendrogramModel> largestCluster = getLargestCluster();
	
		System.out.println(largestCluster.getModel().getDistance());
		  System.out.println("#" + 0 + ": " + largestCluster.getNameAutomatic());
		  System.out.println("Size: " + largestCluster.size());
		  System.out.println("Center: " + largestCluster.getModel().toString());
		  System.out.println(dendrogram.getClusterHierarchy().numChildren(largestCluster));
		  System.out.println("-----------------------------------------------------");
		  
		int i = 0;
		for (It<Cluster<DendrogramModel>> it = dendrogram.getClusterHierarchy().iterAncestors(largestCluster); it.valid(); it.advance()) {
			Cluster<DendrogramModel> clu0 = it.get();
			  System.out.println(dendrogram.getClusterHierarchy().numChildren(clu0));
			for (It<Cluster<DendrogramModel>> it2 = dendrogram.getClusterHierarchy().iterChildren(it.get()); it2.valid(); it2.advance()) {
			Cluster<DendrogramModel> clu = it2.get();
			  System.out.println(dendrogram.getClusterHierarchy().numChildren(clu));
			  System.out.println("#" + i + ": " + clu.getNameAutomatic());
			  System.out.println("Size: " + clu.size());
			  System.out.println("Center: " + clu.getModel().toString());
			  // Iterate over objects:
			  System.out.print("Objects: ");
			  for(DBIDIter itdb = clu.getIDs().iter(); itdb.valid(); itdb.advance()) {
			NumberVector v = rel.get(itdb);
			
			final int offset = ids.getOffset(itdb);
			System.out.print(" " + offset);
			  }
			  System.out.println();
			  ++i;
			}
			 System.out.println("-----------------------------------------------------");
		}
	}
	
	public Cluster<DendrogramModel> getLargestCluster() {
		int indexOfMax = -1;
		int nmax = -1;
		List<Cluster<DendrogramModel>> clusters = dendrogram.getAllClusters();
		for (int i = 0; i < clusters.size(); i++) {
			int n = clusters.get(i).getIDs().size();
			if (n > nmax) {
				nmax = n;
				indexOfMax = i;
			}
		}
		
		return clusters.get(indexOfMax);
	}
	
	public List<Integer> getIndicesOfLargestCluster() {
		List<Integer> indices = new ArrayList<>();
		Cluster<DendrogramModel> cluster = getLargestCluster();
		Relation<NumberVector> rel = db.getRelation(TypeUtil.NUMBER_VECTOR_FIELD);
		DBIDRange ids = (DBIDRange) rel.getDBIDs();
		for(DBIDIter it = cluster.getIDs().iter(); it.valid(); it.advance())
			indices.add(ids.getOffset(it));
		return indices;
	}
	
	private void setWaveforms(List<TimewindowInformation> timewindows, Path sacpath) {
		for (int i = 0; i < timewindows.size(); i++) {
			TimewindowInformation window = timewindows.get(i);
			String sacnameString = window.getGlobalCMTID() + "/" + window.getStation().getName() + "." + window.getGlobalCMTID() 
				+ "." + window.getComponent() + "sc";
			Path syn = sacpath.resolve(sacnameString);
			try {
				waveforms[i] = new SACFileName(syn).read().createTrace().cutWindow(window.getStartTime(), window.getEndTime()).removeTrend().getYVector();
				if (waveforms[i].isNaN())
					throw new RuntimeException("NaN waveform");
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
	
	private int[] clusterIds;
	
	private double datasamplinghz;
	
	private XYDataset createDataset(List<Cluster<DendrogramModel>> clusters, Relation<NumberVector> rel, DBIDRange ids) {
		clusterIds = new int[ids.size()];
		XYSeriesCollection dataset = new XYSeriesCollection();
		int ic = 0;
		int iw = 0;
		for(Cluster<DendrogramModel> clu : clusters) {
		  for(DBIDIter it = clu.getIDs().iter(); it.valid(); it.advance()) {
		    // To get the vector use:
		    NumberVector v = rel.get(it);
		    
		    double norm = new ArrayRealVector(v.toArray()).getLInfNorm();

		    // Offset within our DBID range: "line number"
		    final int offset = ids.getOffset(it);
		    clusterIds[offset] = ic;
		    XYSeries serie = new XYSeries(offset);
		    serie.setDescription(String.format("%.2f", clu.getModel().getDistance()));
		    for (int i = 0; i < v.getDimensionality(); i++)
		    	serie.add(i / datasamplinghz, v.doubleValue(i) / norm + iw);
		    dataset.addSeries(serie);
		    iw++;
		  }
		  ic++;
		}
		
		return dataset;
	}
	
   private JFreeChart createChart(XYDataset dataset, List<Cluster<DendrogramModel>> clusters) {
	  boolean legend = false;
      JFreeChart chart = ChartFactory.createXYLineChart("Time series", "Time from origin time (s)", "Velocity (m/s)", dataset
    		  , PlotOrientation.VERTICAL, legend, true, false);
      
      final XYPlot plot = chart.getXYPlot();
      XYLineAndShapeRenderer renderer = new XYLineAndShapeRenderer();
      Color[] colors = new Color[] {Color.BLACK, Color.GRAY, Color.BLUE, Color.CYAN, Color.GREEN, Color.YELLOW, Color.PINK, Color.ORANGE, Color.RED, Color.MAGENTA};
      for (int i = 0; i < dataset.getSeriesCount(); i++) {
	      renderer.setSeriesPaint(i, colors[clusterIds[Integer.parseInt(dataset.getSeriesKey(i).toString())] % colors.length] );
	      renderer.setSeriesStroke(i, new BasicStroke(1.0f));
      }
      renderer.setBaseShapesVisible(false);
      plot.setRenderer(renderer);
      plot.setDomainGridlinePaint(Color.WHITE);
      plot.setRangeGridlinePaint(Color.WHITE);
      plot.setBackgroundPaint(Color.WHITE);
      
      double h = chart.getXYPlot().getRangeAxis().getUpperBound() * .5;
      
      double y = 0;
//      double x = dataset.getXValue(0, dataset.getItemCount(0) - 1);
      double x = 30;
      double dx = x * 0.02;
      for (int i = 0; i < clusters.size(); i++) {
    	  Cluster<DendrogramModel> clu = clusters.get(i);
    	  y += clu.getIDs().size() / 2.;
    	  double tmpx = i % 2 == 0 ? x + dx : x + 2 * dx;
    	  final XYTextAnnotation anno = new XYTextAnnotation(String.format("%.3f", clu.getModel().getDistance()), x + dx, y);
    	  anno.setPaint(colors[i % colors.length]);
    	  plot.addAnnotation(anno);
    	  y += clu.getIDs().size() / 2.;
      }
      
      return chart;
   }
   
  
   public void displayResultInRecordSection() {
	  Relation<NumberVector> rel = db.getRelation(TypeUtil.NUMBER_VECTOR_FIELD);
	  DBIDRange ids = (DBIDRange) rel.getDBIDs();
	   
   	  XYDataset dataset = createDataset(dendrogram.getAllClusters(), rel, ids);
      JFreeChart chart = createChart(dataset, dendrogram.getAllClusters());
      ChartPanel chartPanel = new ChartPanel(chart);
      chartPanel.setPreferredSize( new java.awt.Dimension( 560 , 370 ) );         
      chartPanel.setMouseZoomable( true , false );         
      JFrame f = new JFrame("Time series and Spectra");
      f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
      f.add(chartPanel);
      f.pack();
      f.setLocationRelativeTo(null);
      f.setVisible(true);
   }
	
}
