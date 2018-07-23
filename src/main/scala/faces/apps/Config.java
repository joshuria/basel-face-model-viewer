package faces.apps;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;

/**Config json file mapping class.*/
@JsonIgnoreProperties(ignoreUnknown = true) class Config {
    public static final int DefaultSeed = 89797;
    public static final int DefaultWidth = 512;
    private static final int DefaultHeight = 512;

    private final int _seed;
    private final int _width, _height;
    private final String _modelPath;
    private final String _csvOutputPath;
    private final String _meshOutputRootPath;
    private final boolean _updateImage;
    private final boolean _closeAfterDone;
    private final boolean _saveImage;

    private final boolean _fixColor;
    private final boolean _fixShape;
    private final boolean _fixExpression;

    private final int _colorRandomCount;
    private final int _colorMaxDimension;
    private final int _colorSamplePerDimension;

    private final int _shapeRandomCount;
    private final int _shapeMaxDimension;
    private final int _shapeSamplePerDimension;

    private final int _expressionRandomCount;
    private final int _expressionMaxDimension;
    private final int _expressionSamplePerDimension;

    /**Random seed specified in config file.*/
    public int getRandomSeed() { return _seed; }
    /**Window width (also color image width).*/
    public int getWidth() { return _width; }
    /**Window height (also color image height).*/
    public int getHeight() { return _height; }
    /**Shape model (*.h5) model's path.*/
    public String getModelPath() { return _modelPath; }
    /**Statistics CSV file output path.*/
    public String getCSVOutputPath() { return _csvOutputPath; }
    /**Root folder path of for storing output mesh files.*/
    public String getMeshOutputRootPath() { return _meshOutputRootPath; }
    /**Update image after adjust parameters or not.*/
    public boolean needUpdateImage() { return _updateImage; }
    /**Is close app after all output done.*/
    public boolean closeAfterDone() { return _closeAfterDone; }
    /**Also save image related to enumerated parameters or not.
     @implSpec the images will be saved to {@link #getMeshOutputRootPath()}.*/
    public boolean isSaveImage() { return _saveImage; }
    /**Is face color (texture) fixed.*/
    public boolean isFixColor() { return _fixColor; }
    /**Is face shape (vertex position) fixed.*/
    public boolean isFixShape() { return _fixShape; }
    /**Is face expression fixed.*/
    public boolean isFixExpression() { return _fixExpression; }

    /**Get # of random samples to be used in color features.
     <p>If this value is greater than 0, the system will use random sampling on color features and
     ignore fixed range sampling ({@link #getColorMaxDimension()} and {@link #getColorSamplePerDimension()}).</p>*/
    public int getColorRandomCount() { return _colorRandomCount; }
    /**Get max # of dimension to be used on color features by fix sampling.
     <p>Total number of output is {@code {@link #getColorMaxDimension()} x {@link #getColorSamplePerDimension()}}.</p>*/
    public int getColorMaxDimension() { return _colorMaxDimension; }
    /**Get # of samples to be used per color dimension.
     <p>Total number of output is {@code {@link #getColorMaxDimension()} x {@link #getColorSamplePerDimension()}}.</p>*/
    public int getColorSamplePerDimension() { return _colorSamplePerDimension; }

    /**Get # of random samples to be used in shape features.
     <p>If this value is greater than 0, the system will use random sampling on shape features and
     ignore fixed range sampling ({@link #getShapeMaxDimension()} and {@link #getShapeSamplePerDimension()}).</p>*/
    public int getShapeRandomCount() { return _shapeRandomCount; }
    /**Get max # of dimension to be used on shape features by fix sampling.
     <p>Total number of output is {@code {@link #getShapeMaxDimension()} x {@link #getShapeSamplePerDimension()}}.</p>*/
    public int getShapeMaxDimension() { return _shapeMaxDimension; }
    /**Get # of samples to be used per shape dimension.
     <p>Total number of output is {@code {@link #getShapeMaxDimension()} x {@link #getShapeSamplePerDimension()}}.</p>*/
    public int getShapeSamplePerDimension() { return _shapeSamplePerDimension; }

    /**Get # of random samples to be used in expression features.
     <p>If this value is greater than 0, the system will use random sampling on expression features and
     ignore fixed range sampling ({@link #getExpressionMaxDimension()} and {@link #getExpressionSamplePerDimension()}).</p>*/
    public int getExpressionRandomCount() { return _expressionRandomCount; }
    /**Get max # of dimension to be used on expression features by fix sampling.
     <p>Total number of output is {@code {@link #getExpressionMaxDimension()} x {@link #getExpressionSamplePerDimension()}}.</p>*/
    public int getExpressionMaxDimension() { return _expressionMaxDimension; }
    /**Get # of samples to be used per expression dimension.
     <p>Total number of output is {@code {@link #getExpressionMaxDimension()} x {@link #getExpressionSamplePerDimension()}}.</p>*/
    public int getExpressionSamplePerDimension() { return _expressionSamplePerDimension; }

    private Config(
        @JsonProperty("seed") int seed,
        @JsonProperty("width") int width, @JsonProperty("height") int height,
        @JsonProperty("modelPath") String modelPath,
        @JsonProperty("csvOutputPath") String csvOutputPath,
        @JsonProperty("meshOutputRootPath") String meshOutputRootPath,
        @JsonProperty("updateImage") boolean updateImage,
        @JsonProperty("closeAfterDone") boolean closeAfterDone,
        @JsonProperty("saveImage") boolean saveImage,
        @JsonProperty("colorFix") boolean colorFix,
        @JsonProperty("colorRandomCount") int colorRandomCount,
        @JsonProperty("colorMaxDimension") int colorMaxDimension,
        @JsonProperty("colorSamplePerDimension") int colorSamplePerDimension,
        @JsonProperty("shapeFix") boolean shapeFix,
        @JsonProperty("shapeRandomCount") int shapeRandomCount,
        @JsonProperty("shapeMaxDimension") int shapeMaxDimension,
        @JsonProperty("shapeSamplePerDimension") int shapeSamplePerDimension,
        @JsonProperty("expressionFix") boolean expressionFix,
        @JsonProperty("expressionRandomCount") int expressionRandomCount,
        @JsonProperty("expressionMaxDimension") int expressionMaxDimension,
        @JsonProperty("expressionSamplePerDimension") int expressionSamplePerDimension
    ) {
        _seed = seed > 0 ? seed : DefaultSeed;
        _width = width > 0 ? width : DefaultWidth;
        _height = height > 0 ? height : DefaultHeight;
        _modelPath = modelPath;
        _csvOutputPath = csvOutputPath;
        _meshOutputRootPath = meshOutputRootPath.trim().isEmpty() ? "./" : meshOutputRootPath.trim();
        _updateImage = updateImage;
        _closeAfterDone = closeAfterDone;
        _saveImage = saveImage;
        _fixColor = colorFix;   _fixShape = shapeFix;   _fixExpression = expressionFix;
        if (colorFix) {
            _colorRandomCount = 0;
            _colorMaxDimension = Integer.MAX_VALUE;
            _colorSamplePerDimension = 1;
        }
        else {
            _colorRandomCount = colorRandomCount >= 0 ? colorRandomCount : 0;
            if (_colorRandomCount > 0) {
                _colorMaxDimension = 0;
                _colorSamplePerDimension = 0;
            }
            else {
                _colorMaxDimension = colorMaxDimension >= 0 ? colorMaxDimension : 0;
                _colorSamplePerDimension = colorSamplePerDimension >= 0 ? colorSamplePerDimension : 0;
            }
        }
        if (shapeFix) {
            _shapeRandomCount = 0;
            _shapeMaxDimension = Integer.MAX_VALUE;
            _shapeSamplePerDimension = 1;
        }
        else {
            _shapeRandomCount = shapeRandomCount >= 0 ? shapeRandomCount : 0;
            if (_shapeRandomCount > 0) {
                _shapeMaxDimension = 0;
                _shapeSamplePerDimension = 0;
            }
            else {
                _shapeMaxDimension = shapeMaxDimension >= 0 ? shapeMaxDimension : 0;
                _shapeSamplePerDimension = shapeSamplePerDimension >= 0 ? shapeSamplePerDimension : 0;
            }
        }
        if (expressionFix) {
            _expressionRandomCount = 0;
            _expressionMaxDimension = Integer.MAX_VALUE;
            _expressionSamplePerDimension = 1;
        }
        else {
            _expressionRandomCount = expressionRandomCount >= 0 ? expressionRandomCount : 0;
            if (_expressionRandomCount > 0) {
                _expressionMaxDimension = 0;
                _expressionSamplePerDimension = 0;
            }
            else {
                _expressionMaxDimension = expressionMaxDimension >= 0 ? expressionMaxDimension : 0;
                _expressionSamplePerDimension = expressionSamplePerDimension >= 0 ? expressionSamplePerDimension : 0;
            }
        }
    }

    /**Load config from give path.
     @throws IOException if given file path cannot be read or is not a valid json file. */
    public static Config loadFromFile(String path) throws IOException {
        final String content = new String(Files.readAllBytes(Paths.get(path)), Charset.forName("utf-8"));
        final ObjectMapper om = new ObjectMapper();
        return om.readValue(content, Config.class);
    }
} // ! class Config
