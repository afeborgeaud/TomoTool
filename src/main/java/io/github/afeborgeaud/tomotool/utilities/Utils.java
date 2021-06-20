package io.github.afeborgeaud.tomotool.utilities;

import io.github.afeborgeaud.tomotool.topoModel.ExternalModel;
import io.github.afeborgeaud.tomotool.topoModel.GaussianPointPerturbation;
import io.github.afeborgeaud.tomotool.topoModel.LLNLG3DJPS;
import io.github.afeborgeaud.tomotool.topoModel.S20RTS;
import io.github.afeborgeaud.tomotool.topoModel.SEMUCBWM1;
import io.github.afeborgeaud.tomotool.topoModel.Seismic3Dmodel;
import io.github.afeborgeaud.tomotool.topoModel.TK10;
import org.apache.commons.cli.*;

public class Utils {
	
	public static Seismic3Dmodel parse3DModel(String modelName, String mantleModelName) {
		Seismic3Dmodel seismic3Dmodel = null;
		switch (modelName.toLowerCase()) {
		case "semucb":
		case "semucbwm1":
		case "semucb_wm1":
			seismic3Dmodel = new SEMUCBWM1();
			break;
		case "llnlg3d":
			seismic3Dmodel = new LLNLG3DJPS();
			break;
		case "tanaka10":
		case "tk10":
			seismic3Dmodel = new TK10();
			break;
		case "gauss":
			seismic3Dmodel = new GaussianPointPerturbation();
			break;
		case "s20rts":
			seismic3Dmodel = new S20RTS();
			break;
		default:
			try {
				seismic3Dmodel = new ExternalModel(modelName, "custom", mantleModelName);
			} catch (Exception e) {
				throw new RuntimeException("Error: 3D model " + modelName + " not implemented yet");
			}
		}
		return seismic3Dmodel;
	}
	
	public static Seismic3Dmodel parse3DModel(String modelName) {
		return parse3DModel(modelName, "s20rts");
	}
	
}
