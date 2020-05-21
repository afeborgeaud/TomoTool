# TomoTool

Java tools for computation of travel-time perturbations due to the CMB topography.<br>
DISCLAMER: these tools are not yet fully intended for general usage (lack of documentation, lack of user-friendly interface), but a simple usage for computing travel-time perturbations from an input raypath information file is described bellow

## Dependenciejfreechars
- jfreechart
- jcommon
- Apache Commons io
- Apache Commons Math
- Apache Commons Lang
- Apache Commons cli
- Apache Commons net
- Apache Commons email
- javax.mail
- activation


## Installation
- Dowload tomoTool.jar file (located in bin/) to your local machine
- add the following line to your .bashrc
```bash
export CLASSPATH=$CLASSPATH:path/to/tomoTool.jar
```

## Usage
A template raypath information file is in resources/example/raypath_informations.txt.<br>
You can compute travel-time perturbations for topo and mantle for the model tk10 (Tanaka, 2010) for the ScS phase using
```java
java raytheory.Compute raypath_informations.txt tk10 ScS
```
