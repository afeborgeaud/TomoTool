package io.github.afeborgeaud.tomotool.graphic;

import java.awt.BasicStroke;
import java.awt.Color;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

import javax.swing.JFrame;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.data.xy.XYDataset;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;
import io.github.afeborgeaud.tomotool.data.AlignWindows.PolarizedShift;
import io.github.kensuke1984.kibrary.timewindow.TimewindowInformation;
import io.github.kensuke1984.kibrary.util.Trace;
import io.github.kensuke1984.kibrary.util.sac.SACFileName;

public class RecordSectionWindow {
	
	private final double datasamplinghz = 20;
	
	private XYSeriesCollection dataset;
	
	public RecordSectionWindow(List<TimewindowInformation> timewindows, Path sacpath) {
		dataset = createDataset(timewindows, sacpath);
	}
	
	public RecordSectionWindow(List<TimewindowInformation> timewindows, List<PolarizedShift> polarizedShifts, Path sacpath) {
		dataset = createDataset(timewindows, polarizedShifts, sacpath);
	}
	
	public void show() {
		displayResultInRecordSection(dataset);
	}
	
	public void addTraceAtDistance(Trace trace, double distance) {
		XYSeries serie = new XYSeries(dataset.getSeriesCount() + 1);
		double norm = trace.getYVector().getLInfNorm();
	    for (int k = 0; k < trace.getLength(); k++)
	    	serie.add(k / datasamplinghz, trace.getYAt(k) / norm + distance);
	    serie.setDescription("wavelet");
	    dataset.addSeries(serie);
	}
	
	private XYSeriesCollection createDataset(List<TimewindowInformation> timewindows, List<PolarizedShift> polarizedShifts, Path sacpath) {
		XYSeriesCollection dataset = new XYSeriesCollection();
		
		for (int i = 0; i < timewindows.size(); i++) {
			TimewindowInformation window = timewindows.get(i);
			String sacnameString = window.getGlobalCMTID() + "/" + window.getStation().getName() + "." + window.getGlobalCMTID() 
				+ "." + window.getComponent() + "sc";
			Path syn = sacpath.resolve(sacnameString);
			
			Trace trace = null;
			try {
				trace = new SACFileName(syn).read().createTrace().cutWindow(window).multiply(polarizedShifts.get(i).getPolarization());
			} catch (IOException e) {
				e.printStackTrace();
			}
			double norm = trace.getYVector().getLInfNorm();
			double distance = window.getDistanceDegree();
			
			XYSeries serie = new XYSeries(i);
			serie.setDescription("");
		    for (int k = 0; k < trace.getLength(); k++)
		    	serie.add(k / datasamplinghz, trace.getYAt(k) / norm + distance);
		    dataset.addSeries(serie);
		}
		
		return dataset;
	}
	
	private XYSeriesCollection createDataset(List<TimewindowInformation> timewindows, Path sacpath) {
		XYSeriesCollection dataset = new XYSeriesCollection();
		
		for (int i = 0; i < timewindows.size(); i++) {
			TimewindowInformation window = timewindows.get(i);
			String sacnameString = window.getGlobalCMTID() + "/" + window.getStation().getName() + "." + window.getGlobalCMTID() 
				+ "." + window.getComponent() + "sc";
			Path syn = sacpath.resolve(sacnameString);
			
			Trace trace = null;
			try {
				trace = new SACFileName(syn).read().createTrace().cutWindow(window);
			} catch (IOException e) {
				e.printStackTrace();
			}
			double norm = trace.getYVector().getLInfNorm();
			double distance = window.getDistanceDegree();
			
			XYSeries serie = new XYSeries(i);
			serie.setDescription("");
		    for (int k = 0; k < trace.getLength(); k++)
		    	serie.add(k / datasamplinghz, trace.getYAt(k) / norm + distance);
		    dataset.addSeries(serie);
		}
		
		return dataset;
	}
	
   private JFreeChart createChart(XYDataset dataset) {
	  boolean legend = false;
      JFreeChart chart = ChartFactory.createXYLineChart("Time series", "Time from origin time (s)", "Distance (deg)", dataset
    		  , PlotOrientation.VERTICAL, legend, true, false);
      
      final XYPlot plot = chart.getXYPlot();
      XYLineAndShapeRenderer renderer = new XYLineAndShapeRenderer();
      Color[] colors = new Color[] {Color.BLACK, Color.GRAY, Color.BLUE, Color.CYAN, Color.GREEN, Color.YELLOW, Color.PINK, Color.ORANGE, Color.RED, Color.MAGENTA};
      for (int i = 0; i < dataset.getSeriesCount(); i++) {
	      renderer.setSeriesPaint(i, colors[0] );
	      renderer.setSeriesStroke(i, new BasicStroke(0.5f));
      }
      renderer.setBaseShapesVisible(false);
      plot.setRenderer(renderer);
      plot.setDomainGridlinePaint(Color.WHITE);
      plot.setRangeGridlinePaint(Color.WHITE);
      plot.setBackgroundPaint(Color.WHITE);
      
      return chart;
   }
   
  
   public void displayResultInRecordSection(XYDataset dataset) {
      JFreeChart chart = createChart(dataset);
      ChartPanel chartPanel = new ChartPanel(chart);
      chartPanel.setPreferredSize( new java.awt.Dimension( 560 , 370 ) );         
      chartPanel.setMouseZoomable( true , false );         
      JFrame f = new JFrame("Record section");
      f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
      f.add(chartPanel);
      f.pack();
      f.setLocationRelativeTo(null);
      f.setVisible(true);
   }
}
