package io.github.afeborgeaud.tomotool.ml.clustering;

import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.database.StaticArrayDatabase;
import de.lmu.ifi.dbs.elki.datasource.ArrayAdapterDatabaseConnection;
import de.lmu.ifi.dbs.elki.datasource.DatabaseConnection;


public class WaveformDatabase {
	
	public static Database getDatabase(double[][] waveformdata) {
		DatabaseConnection dbc = new ArrayAdapterDatabaseConnection(waveformdata);
		return new StaticArrayDatabase(dbc, null);
	}
	
}
