 # Simple Morphable Model Viewer
 
 Simple tool to view the [Basel Face Model 2017](http://faces.cs.unibas.ch/bfm/bfm2017.html).
 
 ![Simple Morphable Model Viewer](Screenshot.png)
 
 For further information about the model and the surrounding theory and software please visit [http://gravis.dmi.unibas.ch/PMM](http://gravis.dmi.unibas.ch/PMM)

This repository migrates sbt building system to maven and support output images and meshes by given
configuration file.

## Requirements
- installed [Java](http://www.oracle.com/technetwork/java/javase/downloads/index.html) (Version 8.0 or higher recommended)

## Run (precompiled):
- download `model-viewer.jar` under `release`
- run `java -jar model-viewer.jar -Xmx2g`

## Usage:
- upper random and reset button will update color/shape/expression parameters for active tab
- lower random and reset button will update all model parameters
- the button neutralModel will remove the expression part of the model
- the button `load RPS` will load rendering parameters from a .rps file (currently only shape, color and expression)
- the button `write PLY` enables writing the current instance as a mesh in the ply file format (thanks to ImageSynthTue)
- the button `write PNG` enables writing the current instance as an image in the png file format
- sliders are ordered according to the principal components
- the maximal parameter value corresponding to the sliders can be adjusted
- press `Ctrl` to move pose with mouse (first click on face to activate the frame)
- use `config.json` to change app behavior and automatically generate meshes and images.

## Configuration file

 - *seed*: **integer** random seed.
 - *modelPath*: **string** model's (\*.h5) path.
 - *csvOutputPath*: **string** path to save statistics CSV file.
 - *meshOutputRootPath*: **string** root folder path to store output mesh and image files.
 - *closeAfterDone*: **boolean** close app after automation job done.
 - *saveImage*: **boolean** also save image (mesh view generated by scalismo) to mesh output root
    path or not.
 - *width*: **integer** width of image size.
 - *height*: **integer** height of image size.
 - *updateImage*: **boolean** specify whether update image view in UI once when parameter changes.
 
 - *colorFix*: **boolean** is color parameters fixed no change. If this value is **true**, color
    parameters will be all zero and not be changed during automation.
 - *colorRandomCount*: **integer** the number of random samples on color parameters.
 - *colorMaxDimension*: **integer** the number of dimension (from coarse/low to detail/high) to be
    used for fixed sampling.
 - *colorSamplePerDimension**: **integer** the number of samples to be used for each color dimension.

 - *shapeFix*: **boolean** is shape parameters fixed no change. If this value is **true**, shape
    parameters will be all zero and not be changed during automation.
 - *shapeRandomCount*: **integer** the number of random samples on shape parameters.
 - *shapeMaxDimension*: **integer** the number of dimension (from coarse/low to detail/high) to be
    used for fixed sampling.
 - *shapeSamplePerDimension**: **integer** the number of samples to be used for each shape dimension.

 - *expressionFix*: **boolean** is expression parameters fixed no change. If this value is **true**,
    expression parameters will be all zero and not be changed during automation.
 - *expressionRandomCount*: **integer** the number of random samples on expression parameters.
 - *expressionMaxDimension*: **integer** the number of dimension (from coarse/low to detail/high) to
    be used for fixed sampling.
 - *expressionSamplePerDimension**: **integer** the number of samples to be used for each expression
    dimension.
 
Note that when *xxxFix* is **true**, the related parameters will fixed on **0** and never change
during automation task.  
If *xxxFix** is **false**, and *xxxRandomCount* is `N` and is greater than **0**, then the random
sampling on related parameters will be used, and will generate `N` output.  
If *xxxFix* is **false**, *xxxRandomCount* is less than **0**, then the fixed range sampling will
be used. The fixed range sampling will sample on related parameters from coarse (i.e. low dimension)
to detail (i.e. high dimension) iteratively, and totally generate `xxxMaxDimension x xxxSamplePerDimension`
outputs. The correct way to do fixed range sampling will generate `xxxSamplePerDimension ^ xxxMaxDimension`
outputs, and this is obviously too many, so I modify this sampling method to *linear* one.
 
## For Developers:
- clone repository
- install maven and JDK 8

## Maintainer

- Bernhard Egger <bernhard.egger@unibas.ch>
- Andreas Morel-Forster <andreas.forster@unibas.ch>

## Dependencies

- [scalismo-faces](https://github.com/unibas-gravis/scalismo-faces) `0.9.2+`
- scalismo_2.12 `0.16.1`
- org.scalanlp.breeze_2.12 `0.13.2`
- scala `2.12.6`
- jackson `2.9.6`
