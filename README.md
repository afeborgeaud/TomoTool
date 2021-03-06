# TomoTool
Java tools for computation of travel-time perturbations due to the Earth's core-mantle boundary topography and 3-D mantle. TomoTool uses several classes from the seismic waveform inversion tool [Kibrary](https://github.com/kensuke1984/Kibrary).<br><br>
DISCLAMER: TomoTool is not yet intended for general usage (lack of documentation, lack of user-friendly interface), but simple usages for computing travel-time perturbations from an input raypath information file are given bellow.

# INSTALLATION
1. [Install Apache Maven](https://maven.apache.org/download.cgi)
```bash
# On macOS, maven can be installed using brew
brew install maven
````
2. Clone the TomoTool respository to your local machine
```bash
git clone git@github.com:afeborgeaud/TomoTool.git
```
4. Change to the TomoTool directory, and build TomoTool using Maven
```
cd TomoTool
mvn package assembly:single
```
4. Add the generated JAR file ```TomoTool-1.0-SNAPSHOT-jar-with-dependencies.jar``` to your CLASSPATH (in ~/.bashrc)
```bash
# replace /path/to/TomoTool/dir/ by the path to the TomoTool directory cloned in step 3
echo "# TomoTool\nexport CLASSPATH=/path/to/TomoTool/dir/target/TomoTool-1.0-SNAPSHOT-jar-with-dependencies.jar:$CLASSPATH" >> ~/.bashrc
source ~/.bashrc
```
5. To check that the installation is succesfull, run:
```bash
java io.github.afeborgeaud.tomotool.About
```

# USAGE
A template raypath information file is in ```src/main/resources/example/raypath_informations.txt```<br>

## Computation of travel-time perturbations due to CMB topo and 3-D mantle
You can compute travel time perturbations for CMB topography and 3-D mantle using:
```java
java io.github.afeborgeaud.tomotool.raytheory.Compute -ri path/to/raypath/info/file -m modelName -p phaseName
```
The full list of CLI arguments can be printed using:
```bash
java io.github.afeborgeaud.tomotool.raytheory.Compute --help
usage: Compute
 -h,--help                  print this message
 -m,--model <arg>           name of the mantle/topo model, or path to a
                            custom model file
 -n,--model-name <arg>      name of the mantle/topo model (for custom
                            models)
 -p,--phase <arg>           seismic phase
 -ri,--raypath-info <arg>   path to a raypath information file
```
<br>

For instance, you can compute travel-time perturbations for topo and mantle for the model tk10 (Tanaka, 2010) for the ScS phase using:
```java
java io.github.afeborgeaud.tomotool.raytheory.Compute -ri raypath_informations.txt -m tk10 -p ScS
```
Currently, the following CMB topography models are implemented:
- tk10 (Tanaka, 2010)
- a user-defined CMB topography model using spherical harmonics coefficients<br>

Currently accepted phases are:
- S, ScS, ScSScS, ScSScSScS, SKS, SKKS, SKKKS, SKKKKS
- P, PcP, PcS, PKP, PKKP, PKKKP, PKKKKP, PKiKP<br>

For instance, the same computation as above but for PKiKP can be done using:
```java
java io.github.afeborgeaud.tomotool.raytheory.Compute -ri raypath_informations.txt -m tk10 -p PKiKP
```

Exemples of custom spherical harmonics topography model are in ``src/main/resources``:
- dh89.sph
- md87.sph
- sh03.sph
- gauss_ca_1000_8_20.ylm (in SPECFEM format)

## Computation of differential PcP-P or ScS-S travel-times
You can compute differential PcP-P travel-time perturbations due to the 3-D mantle using
```java
java io.github.afeborgeaud.tomotool.raytheory.ComputeCorrection raypath_informations.txt semucb pcp
```
Note: path to a Kibrary TimewindowInformationFile binary file can also be used instead of ```raypath_information.txt```.<br>
The outputs are ```bouncepointPcP.dat``` (travel-time perturbations at PcP bouncing points) and ```mantleCorrection_P-PcP.dat``` (a binary kibrary StaticCorrectionFile). Currently, the following 3-D models are available:
- semucb (SEMUCB-WM1; [French and Romanowicz 2014](https://academic.oup.com/gji/article/199/3/1303/612270))
- llnlg3d (LLNLG3DJPS; [Simmons et al. 2012](https://agupubs.onlinelibrary.wiley.com/doi/full/10.1029/2012JB009525))
- s20rts (S20RTS & SP12; Ritsema et al. 2000)
