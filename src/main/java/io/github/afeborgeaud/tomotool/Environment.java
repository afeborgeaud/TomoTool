package io.github.afeborgeaud.tomotool;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

import io.github.afeborgeaud.tomotool.raytheory.RaypathInformation;

public class Environment {
	
	public static final String SAMPLE_RAYPATH_INFO = "example/raypath_informations.txt";
	
	public static List<RaypathInformation> sampleRaypathInfo() {
		Path resourcePath =  new File("src/main/resources").getAbsoluteFile().toPath();
		Path raypathInformationPath = resourcePath.resolve(SAMPLE_RAYPATH_INFO);
		List<RaypathInformation> rayInfo = null;
		try {
			rayInfo = RaypathInformation.readRaypathInformation(raypathInformationPath);
		} catch (IOException e) {
			e.printStackTrace();
		}
		return rayInfo;
	}
	
}
