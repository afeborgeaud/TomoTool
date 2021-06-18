package io.github.afeborgeaud.tomotool.ml.clustering;

import java.util.Arrays;

import de.lmu.ifi.dbs.elki.algorithm.AbstractDistanceBasedAlgorithm;
import de.lmu.ifi.dbs.elki.data.Cluster;
import de.lmu.ifi.dbs.elki.data.Clustering;
import de.lmu.ifi.dbs.elki.data.model.Model;
import de.lmu.ifi.dbs.elki.data.type.TypeInformation;
import de.lmu.ifi.dbs.elki.data.type.TypeUtil;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.database.ids.ArrayDBIDs;
import de.lmu.ifi.dbs.elki.database.ids.ArrayModifiableDBIDs;
import de.lmu.ifi.dbs.elki.database.ids.DBIDArrayIter;
import de.lmu.ifi.dbs.elki.database.ids.DBIDUtil;
import de.lmu.ifi.dbs.elki.database.ids.DBIDs;
import de.lmu.ifi.dbs.elki.database.ids.ModifiableDBIDs;
import de.lmu.ifi.dbs.elki.database.query.distance.DistanceQuery;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.distance.distancefunction.DistanceFunction;
import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.logging.progress.FiniteProgress;
import de.lmu.ifi.dbs.elki.result.Result;
import it.unimi.dsi.fastutil.ints.Int2ReferenceMap;
import it.unimi.dsi.fastutil.ints.Int2ReferenceOpenHashMap;

public class HierarchicalClustering<O> extends AbstractDistanceBasedAlgorithm<O, Result> {
	
	  public HierarchicalClustering(DistanceFunction<? super O> distanceFunction, int numclusters) {
		  super(distanceFunction);
		  this.numclusters = numclusters;
	  }
	  
	  private static final Logging LOG = Logging.getLogger(HierarchicalClustering.class);
	  
	  /**
	   * Threshold, how many clusters to extract.
	   */
	  int numclusters;
	  
	  @Override
	  public TypeInformation[] getInputTypeRestriction() {
	    return TypeUtil.array(getDistanceFunction().getInputTypeRestriction());
	  }
	
	  @Override
	  protected Logging getLogger() {
	    return LOG;
	  }
	  
	  public Result run(Database db, Relation<O> relation) {
		  DistanceQuery<O> dq = db.getDistanceQuery(relation, getDistanceFunction());
		  
		  ArrayDBIDs ids = DBIDUtil.ensureArray(relation.getDBIDs());
		  final int size = ids.size();
		  
		  double[][] matrix = new double[size][size];
		  DBIDArrayIter ix = ids.iter(), iy = ids.iter();
		  for (int x = 0; ix.valid(); x++, ix.advance()) {
			  iy.seek(0);
			  for (int y = 0; y < x; y++, iy.advance()) {
				  final double dist = dq.distance(ix, iy);
				  matrix[x][y] = dist;
				  matrix[y][x] = dist;
			  }
		  }
		  
		// Initialize space for result:
	    double[] height = new double[size];
	    Arrays.fill(height, Double.POSITIVE_INFINITY);
	    // Parent node, to track merges
	    // have every object point to itself initially
	    ArrayModifiableDBIDs parent = DBIDUtil.newArray(ids);
	    // Active clusters, when not trivial.
	    Int2ReferenceMap<ModifiableDBIDs> clusters = new Int2ReferenceOpenHashMap<>();
	    
	    final int stop = size - numclusters;
	    FiniteProgress prog = LOG.isVerbose() ?
	      new FiniteProgress("Agglomerative clustering", stop, LOG)
	      : null;
	    for(int i = 0; i < stop; i++) {
	      // TODO: find clusters to merge
	        double min = Double.POSITIVE_INFINITY;
	        int minx = -1, miny = -1;
	        for(int x = 0; x < size; x++) {
	          if(height[x] < Double.POSITIVE_INFINITY) {
	            continue;
	          }
	          for(int y = 0; y < x; y++) {
	            if(height[y] < Double.POSITIVE_INFINITY) {
	              continue;
	            }
	            if(matrix[x][y] < min) {
	              min = matrix[x][y];
	              minx = x;
	              miny = y;
	            }
	          }
	        }

	      // TODO: store the merge in auxillary data
	     // Avoid allocating memory, by reusing existing iterators:
	        ix.seek(minx);
	        iy.seek(miny);
	        // Perform merge in data structure: x -> y
	        // Since y < x, prefer keeping y, dropping x.
	        height[minx] = min;
	        parent.set(minx, iy);
	        
	        // Merge into cluster
	        ModifiableDBIDs cx = clusters.get(minx);
	        ModifiableDBIDs cy = clusters.get(miny);
	        if(cy == null) {
	          cy = DBIDUtil.newHashSet();
	          cy.add(iy);
	        }
	        if(cx == null) {
	          cy.add(ix);
	        }
	        else {
	          cy.addDBIDs(cx);
	          clusters.remove(minx);
	        }
	        clusters.put(miny, cy);

	      // TODO: update distance matrix
	     // Update distance matrix for y:
	        for(int j = 0; j < size; j++) {
	          matrix[j][miny] = Math.min(matrix[j][minx], matrix[j][miny]);
	          matrix[miny][j] = Math.min(matrix[minx][j], matrix[miny][j]);
	        }
	        
	      if(prog != null) {
	        prog.incrementProcessed(LOG);
	      }
	    }
	    if(prog != null) {
	      prog.ensureCompleted(LOG);
	    }
	    
	    final Clustering<Model> dendrogram = new Clustering<>(
      	      "Hierarchical-Clustering", "hierarchical-clustering");
      	    for(int x = 0; x < size; x++) {
      	      if(height[x] < Double.POSITIVE_INFINITY) {
      	        DBIDs cids = clusters.get(x);
      	        // For singleton objects, this may be null.
      	        if(cids == null) {
      	          ix.seek(x);
      	          cids = DBIDUtil.deref(ix);
      	        }
      	        Cluster<Model> cluster = new Cluster<>("Cluster", cids);
      	        dendrogram.addToplevelCluster(cluster);
      	      }
      	    }
      	    return dendrogram;
	  }
}
