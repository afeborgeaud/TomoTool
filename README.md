# TomoTool

Java tools for computation of travel-time perturbations due to the CMB topography.<br>
DISCLAMER: these tools are not yet fully intended for general usage (lack of documentation, lack of user-friendly interface), but a simple usage for computing travel-time perturbations from an input raypath information file is described bellow

## DEPENDENCIES
- activation
- jfreechart
- jcommon
- Apache Commons io
- Apache Commons Math
- Apache Commons Lang
- Apache Commons cli
- Apache Commons net
- Apache Commons email
- javax.mail
- elki-bundle
- epsgraphics
- javax.mail
- jcommon
- jfreechart
- log4j
- netcdfAll
- seedCodec
- seisFile
- slf4j
- TauP
- [Kibrary](https://www.dropbox.com/s/6mm0cfk259x8l25/kibrary-anselme.jar?dl=0)


## INSTALLATION
1. Download the dependencies
2. Download [tomoTool.jar](https://www.dropbox.com/sh/03ksrnmnr5zbh02/AADj0Lli8DRbfyxkf3kfZBfQa?dl=0)
3. Add ```tomoTool.jar``` to your CLASSPATH (in ~/.bashrc)
```bash
export CLASSPATH=$CLASSPATH:path/to/tomoTool.jar
```
4. Add the dependencies to your ```CLASSPATH```

## USAGE
A template raypath information file is in *resources/example/raypath_informations.txt*<br>

### Computation of differential PcP-P or ScS-S travel-times
You can compute differential PcP-P travel-time perturbations due to the 3-D mantle using
```java
java raytheory.ComputeCorrection raypath_informations.txt semucb pcp
```
The outputs are ```bouncepointPcP.dat``` (travel-time perturbations at PcP bouncing points) and ```mantleCorrection_P-PcP.dat``` (a binary kibrary StaticCorrectionFile). Currently, the following 3-D models are available:
- semucb (SEMUCB-WM1; French and Romanowicz 2014)
- llnlg3d (LLNLG3DJPS; )
- s20rts (S20RTS & SP12; Ritsema et al. 2000)

### Computation of travel-time perturbations due to CMB topo and 3-D mantle
You can compute travel-time perturbations for topo and mantle for the model tk10 (Tanaka, 2010) for the ScS phase using
```java
java raytheory.Compute raypath_informations.txt tk10 ScS
```
