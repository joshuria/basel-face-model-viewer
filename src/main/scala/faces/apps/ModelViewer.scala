/*
 * Copyright University of Basel, Graphics and Vision Research Group
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
 package faces.apps

 import java.awt.Dimension
 import java.io.{File, IOException}

 import javax.swing._
 import javax.swing.event.{ChangeEvent, ChangeListener}
 import breeze.linalg.min
 import scalismo.faces.gui.{GUIBlock, GUIFrame, ImagePanel}
 import scalismo.faces.gui.GUIBlock._
 import scalismo.faces.parameters.RenderParameter
 import scalismo.faces.io.{MeshIO, MoMoIO, PixelImageIO, RenderParameterIO}
 import scalismo.faces.sampling.face.MoMoRenderer
 import scalismo.faces.color.RGB
 import scalismo.faces.image.PixelImage
 import scalismo.utils.Random
 import scalismo.faces.color.RGBA
 import scalismo.faces.momo.MoMo

 import scala.reflect.io.Path
 import scala.util.{Failure, Try}

object ModelViewer extends App {
  final val DEFAULT_DIR = new File(".")
  final val DefaultConfigPath = "./config.json";

  //! Load config file
  private val cfg: Config =
  try { Config.loadFromFile(DefaultConfigPath) }
  catch {
    case e: IOException =>
      e.printStackTrace()
      null
  }
  if (cfg == null)
    println("Fail to load config file: " + DefaultConfigPath)
    None

  val modelFile: Option[File] = getModelFile(args)
  modelFile.map(SimpleModelViewer(_, cfg))

  private def getModelFile(args: Seq[String]): Option[File] = {
    if (args.nonEmpty) {
      val path = Path(args.head)
      if (path.isFile) return Some(path.jfile)
      if (path.isDirectory) return askUserForModelFile(path.jfile)
    }

    println("Loading model file...")
    if (cfg.getModelPath.trim().isEmpty)
      askUserForModelFile(DEFAULT_DIR)
    else
      Some(new File(cfg.getModelPath))
  }

  private def askUserForModelFile(dir: File): Option[File] = {
    val jFileChooser = new JFileChooser(dir)
    if (jFileChooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
      Some(jFileChooser.getSelectedFile)
    } else {
      println("No model select...")
      None
    }
  }
}

case class SimpleModelViewer(
  modelFile: File,
  cfg: Config,
//  imageWidth: Int = 512,
//  imageHeight: Int = 512,
  maximalSliderValue: Int = 2,
  maximalShapeRank: Option[Int] = None,
  maximalColorRank: Option[Int] = None,
  maximalExpressionRank: Option[Int] = None
) {

  scalismo.initialize()
  val seed: Int = cfg.getRandomSeed
  implicit val rnd: Random = Random(seed)


  val model: MoMo = MoMoIO.read(modelFile, "").get
  var showExpressionModel: Boolean = model.hasExpressions

  val shapeRank: Int = maximalShapeRank match {
    case Some(rank) => min(model.neutralModel.shape.rank, rank)
    case _ => model.neutralModel.shape.rank
  }

  val colorRank: Int = maximalColorRank match {
    case Some(rank) => min(model.neutralModel.color.rank, rank)
    case _ => model.neutralModel.color.rank
  }

  val expRank: Int = maximalExpressionRank match {
    case Some(rank) => try{min(model.expressionModel.get.expression.rank, rank)} catch {case _: Exception => 0}
    case _ => try{model.expressionModel.get.expression.rank} catch {case _: Exception => 0}
  }

  var renderer: MoMoRenderer = MoMoRenderer(model, RGBA.BlackTransparent).cached(5)

  val initDefault: RenderParameter = RenderParameter.defaultSquare.fitToImageSize(cfg.getWidth, cfg.getHeight)
  val init10: RenderParameter = initDefault.copy(
    momo = initDefault.momo.withNumberOfCoefficients(shapeRank, colorRank, expRank)
  )
  var init: RenderParameter = init10

  var changingSliders = false

  val sliderSteps = 1000
  var maximalSigma: Int = maximalSliderValue
  var maximalSigmaSpinner: JSpinner = {
    val spinner = new JSpinner(new SpinnerNumberModel(maximalSigma,0,999,1))
    spinner.addChangeListener( new ChangeListener() {
      override def stateChanged(e: ChangeEvent): Unit = {
        val newMaxSigma = spinner.getModel.asInstanceOf[SpinnerNumberModel].getNumber.intValue()
        maximalSigma = math.abs(newMaxSigma)
        setShapeSliders()
        setColorSliders()
        setExpSliders()
      }
    })
    spinner.setToolTipText("maximal slider value")
    spinner
  }


  def sliderToParam(value: Int): Double = {
    maximalSigma * value.toDouble/sliderSteps
  }

  def paramToSlider(value: Double): Int = {
    (value / maximalSigma * sliderSteps).toInt
  }

  val bg = PixelImage(cfg.getWidth, cfg.getHeight, (_, _) => RGBA.Black)

  val imageWindow = ImagePanel(renderWithBG(init))

  //--- SHAPE -----
  val shapeSlider: IndexedSeq[JSlider] = for (n <- 0 until shapeRank) yield {
    GUIBlock.slider(-sliderSteps, sliderSteps, 0, f => {
      updateShape(n, f)
      updateImage()
    })
  }

  val shapeSliderView: JPanel = GUIBlock.shelf(shapeSlider.zipWithIndex.map(s => GUIBlock.stack(s._1, new JLabel("" + s._2))): _*)
  val shapeScrollPane = new JScrollPane(shapeSliderView)
  val shapeScrollBar: JScrollBar = shapeScrollPane.createVerticalScrollBar()
  shapeScrollPane.setSize(800, 300)
  shapeScrollPane.setPreferredSize(new Dimension(800, 300))

  val rndShapeButton: JButton = GUIBlock.button("random", {
    randomShape(); updateImage()
  })
  val resetShapeButton: JButton = GUIBlock.button("reset", {
    resetShape(); updateImage()
  })
  rndShapeButton.setToolTipText("draw each shape parameter at random from a standard normal distribution")
  resetShapeButton.setToolTipText("set all shape parameters to zero")

  def updateShape(n: Int, value: Int): Unit = {
    init = init.copy(momo = init.momo.copy(shape = {
      val current = init.momo.shape
      current.zipWithIndex.map { case (v, i) => if (i == n) sliderToParam(value) else v }
    }))
  }

  def randomShape(): Unit = {
    init = init.copy(momo = init.momo.copy(shape = {
      val current = init.momo.shape
      current.zipWithIndex.map {
        case (_, _) =>
          rnd.scalaRandom.nextGaussian
      }

    }))
    setShapeSliders()
  }

  def resetShape(): Unit = {
    init = init.copy(momo = init.momo.copy(
      shape = IndexedSeq.fill(shapeRank)(0.0)
    ))
    setShapeSliders()
  }

  def setShapeSliders(): Unit = {
    changingSliders = true
    (0 until shapeRank).foreach(i => {
      shapeSlider(i).setValue(paramToSlider(init.momo.shape(i)))
    })
    changingSliders = false
  }

  //--- COLOR -----
  val colorSlider: IndexedSeq[JSlider] = for (n <- 0 until colorRank) yield {
    GUIBlock.slider(-sliderSteps, sliderSteps, 0, f => {
      updateColor(n, f)
      updateImage()
    })
  }

  val colorSliderView: JPanel = GUIBlock.shelf(colorSlider.zipWithIndex.map(s => GUIBlock.stack(s._1, new JLabel("" + s._2))): _*)
  val colorScrollPane = new JScrollPane(colorSliderView)
  val colorScrollBar: JScrollBar = colorScrollPane.createHorizontalScrollBar()
  colorScrollPane.setSize(800, 300)
  colorScrollPane.setPreferredSize(new Dimension(800, 300))

  val rndColorButton: JButton = GUIBlock.button("random", {
    randomColor(); updateImage()
  })

  val resetColorButton: JButton = GUIBlock.button("reset", {
    resetColor(); updateImage()
  })
  rndColorButton.setToolTipText("draw each color parameter at random from a standard normal distribution")
  resetColorButton.setToolTipText("set all color parameters to zero")

  def updateColor(n: Int, value: Int): Unit = {
    init = init.copy(momo = init.momo.copy(color = {
      val current = init.momo.color
      current.zipWithIndex.map { case (v, i) => if (i == n) sliderToParam(value) else v }
    }))
  }

  def randomColor(): Unit = {
    init = init.copy(momo = init.momo.copy(color = {
      val current = init.momo.color
      current.zipWithIndex.map {
        case (_, _) =>
          rnd.scalaRandom.nextGaussian
      }

    }))
    setColorSliders()
  }

  def resetColor(): Unit = {
    init = init.copy(momo = init.momo.copy(
      color = IndexedSeq.fill(colorRank)(0.0)
    ))
    setColorSliders()
  }

  def setColorSliders(): Unit = {
    changingSliders = true
    (0 until colorRank).foreach(i => {
      colorSlider(i).setValue(paramToSlider(init.momo.color(i)))
    })
    changingSliders = false
  }

  //--- EXPRESSION -----
  val expSlider: IndexedSeq[JSlider] = for (n <- 0 until expRank)yield {
    GUIBlock.slider(-sliderSteps, sliderSteps, 0, f => {
      updateExpression(n, f)
      updateImage()
    })
  }

  val expSliderView: JPanel = GUIBlock.shelf(expSlider.zipWithIndex.map(s => GUIBlock.stack(s._1, new JLabel("" + s._2))): _*)
  val expScrollPane = new JScrollPane(expSliderView)
  val expScrollBar: JScrollBar = expScrollPane.createVerticalScrollBar()
  expScrollPane.setSize(800, 300)
  expScrollPane.setPreferredSize(new Dimension(800, 300))

  val rndExpButton: JButton = GUIBlock.button("random", {
    randomExpression(); updateImage()
  })
  val resetExpButton: JButton = GUIBlock.button("reset", {
    resetExpression(); updateImage()
  })

  rndExpButton.setToolTipText("draw each expression parameter at random from a standard normal distribution")
  resetExpButton.setToolTipText("set all expression parameters to zero")

  def updateExpression(n: Int, value: Int): Unit = {
    init = init.copy(momo = init.momo.copy(expression = {
      val current = init.momo.expression
      current.zipWithIndex.map { case (v, i) => if (i == n) sliderToParam(value) else v }
    }))
  }

  def randomExpression(): Unit = {
    init = init.copy(momo = init.momo.copy(expression = {
      val current = init.momo.expression
      current.zipWithIndex.map {
        case (_, _) =>
          rnd.scalaRandom.nextGaussian
      }

    }))
    setExpSliders()
  }

  def resetExpression(): Unit = {
    init = init.copy(momo = init.momo.copy(
      expression = IndexedSeq.fill(expRank)(0.0)
    ))
    setExpSliders()
  }

  def setExpSliders(): Unit = {
    changingSliders = true
    (0 until expRank).foreach(i => {
      expSlider(i).setValue(paramToSlider(init.momo.expression(i)))
    })
    changingSliders = false
  }


  //--- ALL TOGETHER -----
  val randomButton: JButton = GUIBlock.button("random", {
    randomShape(); randomColor(); randomExpression(); updateImage()
  })
  val resetButton: JButton = GUIBlock.button("reset", {
    resetShape(); resetColor(); resetExpression(); updateImage()
  })

  val toggleExpressionButton: JButton = GUIBlock.button("expressions off", {
    if ( model.hasExpressions ) {
      if ( showExpressionModel ) renderer = MoMoRenderer(model.neutralModel, RGBA.BlackTransparent).cached(5)
      else renderer = MoMoRenderer(model, RGBA.BlackTransparent).cached(5)

      showExpressionModel = !showExpressionModel
      updateToggleExpressionButton()
      addRemoveExpressionTab()
      updateImage()
    }
  })

  def updateToggleExpressionButton(): Unit = {
    if ( showExpressionModel ) toggleExpressionButton.setText("expressions off")
    else toggleExpressionButton.setText("expressions on")
  }

  randomButton.setToolTipText("draw each model parameter at random from a standard normal distribution")
  resetButton.setToolTipText("set all model parameters to zero")
  toggleExpressionButton.setToolTipText("toggle expression part of model on and off")

  //function to export the current shown face as a .ply file
  def exportShape (): Try[Unit] ={

    def askToOverwrite(file: File): Boolean = {
      val dialogButton = JOptionPane.YES_NO_OPTION
      JOptionPane.showConfirmDialog(null, s"Would you like to overwrite the existing file: $file?","Warning",dialogButton) == JOptionPane.YES_OPTION
    }

    val VCM3D = if (model.hasExpressions && !showExpressionModel) model.neutralModel.instance(init.momo.coefficients)
    else model.instance(init.momo.coefficients)

    val fc = new JFileChooser()
    fc.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES)
    fc.setDialogTitle("Select a folder to store the .ply file and name it")
    if (fc.showSaveDialog(null) == JFileChooser.APPROVE_OPTION) {
      var file = fc.getSelectedFile
      if (file.isDirectory) file = new File(file,"instance.ply")
      if ( !file.getName.endsWith(".ply")) file = new File( file+".ply")
      if (!file.exists() || askToOverwrite(file)) {
        MeshIO.write(VCM3D, file)
      } else {
        Failure(new IOException(s"Something went wrong when writing to file the file $file."))
      }
    } else {
      Failure(new Exception("User aborted save dialog."))
    }
  }

  //function to export the current shown face as a .ply file
  def exportImage (): Try[Unit] ={

    def askToOverwrite(file: File): Boolean = {
      val dialogButton = JOptionPane.YES_NO_OPTION
      JOptionPane.showConfirmDialog(null, s"Would you like to overwrite the existing file: $file?","Warning",dialogButton) == JOptionPane.YES_OPTION
    }

    val img = renderer.renderImage(init)

    val fc = new JFileChooser()
    fc.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES)
    fc.setDialogTitle("Select a folder to store the .png file and name it")
    if (fc.showSaveDialog(null) == JFileChooser.APPROVE_OPTION) {
      var file = fc.getSelectedFile
      if (file.isDirectory) file = new File(file,"instance.png")
      if ( !file.getName.endsWith(".png")) file = new File( file+".png")
      if (!file.exists() || askToOverwrite(file)) {
        PixelImageIO.write(img, file)
      } else {
        Failure(new IOException(s"Something went wrong when writing to file the file $file."))
      }
    } else {
      Failure(new Exception("User aborted save dialog."))
    }
  }

  //exportShape button and its tooltip
  val exportShapeButton: JButton = GUIBlock.button("export PLY",
    {
      exportShape()
    }
  )
  exportShapeButton.setToolTipText("export the current shape and texture as .ply")

  //exportImage button and its tooltip
  val exportImageButton: JButton = GUIBlock.button("export PNG",
    {
      exportImage()
    }
  )
  exportImageButton.setToolTipText("export the current image as .png")


  //loads parameters from file
  //TODO: load other parameters than the momo shape, expr and color

  def askUserForRPSFile(dir: File): Option[File] = {
    val jFileChooser = new JFileChooser(dir)
    if (jFileChooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
      Some(jFileChooser.getSelectedFile)
    } else {
      println("No Parameters select...")
      None
    }
  }

  def resizeParameterSequence(params: IndexedSeq[Double], length: Int, fill: Double): IndexedSeq[Double] = {
    val zeros = IndexedSeq.fill[Double](length)(fill)
    (params ++ zeros).slice(0, length) //brute force
  }

  def updateModelParameters(params: RenderParameter): Unit = {
    val newShape = resizeParameterSequence(params.momo.shape, shapeRank, 0)
    val newColor = resizeParameterSequence(params.momo.color, colorRank, 0)
    val newExpr = resizeParameterSequence(params.momo.expression, expRank, 0)
    println("Loaded Parameters")

    init = init.copy(momo = init.momo.copy(shape = newShape, color = newColor, expression = newExpr))
    setShapeSliders()
    setColorSliders()
    setExpSliders()
    updateImage()
  }

  val loadButton: JButton = GUIBlock.button(
    "load RPS",
    {
      for {rpsFile <- askUserForRPSFile(new File("."))
           rpsParams <- RenderParameterIO.read(rpsFile)} {
        val maxSigma = (rpsParams.momo.shape ++ rpsParams.momo.color ++ rpsParams.momo.expression).map(math.abs).max
        if ( maxSigma > maximalSigma ) {
          maximalSigma = math.ceil(maxSigma).toInt
          maximalSigmaSpinner.setValue(maximalSigma)
          setShapeSliders()
          setColorSliders()
          setExpSliders()
        }
        updateModelParameters(rpsParams)
      }
    }
  )


  //---- update the image
  def updateImage(): Unit = {
    if (!changingSliders)
      imageWindow.updateImage(renderWithBG(init))
  }

  def renderWithBG(init: RenderParameter): PixelImage[RGB] = {
    val fg = renderer.renderImage(init)
    fg.zip(bg).map { case (f, b) => b.toRGB.blend(f) }
    //    fg.map(_.toRGB)
  }

  //--- COMPOSE FRAME ------
  val controls = new JTabbedPane()
  controls.addTab("color", GUIBlock.stack(colorScrollPane, GUIBlock.shelf(rndColorButton, resetColorButton)))
  controls.addTab("shape", GUIBlock.stack(shapeScrollPane, GUIBlock.shelf(rndShapeButton, resetShapeButton)))
  if ( model.hasExpressions)
    controls.addTab("expression", GUIBlock.stack(expScrollPane, GUIBlock.shelf(rndExpButton, resetExpButton)))
  def addRemoveExpressionTab(): Unit = {
    if ( showExpressionModel ) {
      controls.addTab("expression", GUIBlock.stack(expScrollPane, GUIBlock.shelf(rndExpButton, resetExpButton)))
    } else {
      val idx = controls.indexOfTab("expression")
      if ( idx >= 0) controls.remove(idx)
    }
  }

  val guiFrame: GUIFrame = GUIBlock.stack(
    GUIBlock.shelf(imageWindow,
      GUIBlock.stack(controls,
        if (model.hasExpressions) {
          GUIBlock.shelf(maximalSigmaSpinner, randomButton, resetButton, toggleExpressionButton, loadButton, exportShapeButton, exportImageButton)
        } else {
          GUIBlock.shelf(maximalSigmaSpinner, randomButton, resetButton, loadButton, exportShapeButton, exportImageButton)
        }
      )
    )
  ).displayIn("MoMo-Viewer")



  //--- ROTATION CONTROLS ------

  import java.awt.event._

  var lookAt = false
  imageWindow.requestFocusInWindow()

  imageWindow.addKeyListener(new KeyListener {
    override def keyTyped(e: KeyEvent): Unit = {
    }

    override def keyPressed(e: KeyEvent): Unit = {
      if (e.getKeyCode == KeyEvent.VK_CONTROL) lookAt = true
    }

    override def keyReleased(e: KeyEvent): Unit = {
      if (e.getKeyCode == KeyEvent.VK_CONTROL) lookAt = false
    }
  })

  imageWindow.addMouseListener(new MouseListener {
    override def mouseExited(e: MouseEvent): Unit = {}

    override def mouseClicked(e: MouseEvent): Unit = {
      imageWindow.requestFocusInWindow()
    }

    override def mouseEntered(e: MouseEvent): Unit = {}

    override def mousePressed(e: MouseEvent): Unit = {}

    override def mouseReleased(e: MouseEvent): Unit = {}
  })

  imageWindow.addMouseMotionListener(new MouseMotionListener {
    override def mouseMoved(e: MouseEvent): Unit = {
      if (lookAt) {
        val x = e.getX
        val y = e.getY
        val yawPose = math.Pi / 2 * (x - cfg.getWidth * 0.5) / (cfg.getWidth / 2)
        val pitchPose = math.Pi / 2 * (y - cfg.getHeight * 0.5) / (cfg.getHeight / 2)

        init = init.copy(pose = init.pose.copy(yaw = yawPose, pitch = pitchPose))
        updateImage()
      }
    }

    override def mouseDragged(e: MouseEvent): Unit = {}
  })


  //! Run automation task defined in our config file
  private final val maxColorDim = math.min(colorRank, cfg.getColorMaxDimension)
  private final val maxShapeDim = math.min(shapeRank, cfg.getShapeMaxDimension)
  private final val maxExpressionDim = math.min(expRank, cfg.getExpressionMaxDimension)
  private final val colorSamples: Int = if (cfg.isFixColor) 1 else
    if (cfg.getColorRandomCount > 0) cfg.getColorRandomCount else maxColorDim * cfg.getColorSamplePerDimension
  private final val shapeSamples: Int = if (cfg.isFixShape) 1 else
    if (cfg.getShapeRandomCount > 0) cfg.getShapeRandomCount else maxShapeDim * cfg.getShapeSamplePerDimension
  private final val expSamples: Int = if (cfg.isFixExpression) 1 else
    if (cfg.getExpressionMaxDimension > 0) cfg.getExpressionMaxDimension else maxExpressionDim * cfg.getExpressionSamplePerDimension

  import scala.collection.JavaConverters._
  import scala.collection.mutable.ListBuffer

  private final var colorSampleList = Sampler.sampleByCount(cfg.getColorSamplePerDimension).asScala
  private final var shapeSampleList = Sampler.sampleByCount(cfg.getShapeSamplePerDimension).asScala
  private final var expSampleList = Sampler.sampleByCount(cfg.getExpressionSamplePerDimension).asScala

  private var colorSampleIndex = ListBuffer.fill[Int](maxColorDim)(0)
  private var shapeSampleIndex = ListBuffer.fill[Int](maxShapeDim)(0)
  private var expSampleIndex = ListBuffer.fill[Int](maxExpressionDim)(0)

  var color = 0
  var shape = 0
  var express = 0
  for (color <- 0 until colorSamples) {
    if (!cfg.isFixColor) {
      //! Do random
      if (cfg.getColorRandomCount > 0)  randomColor()
      //! Do sampling
      else {
        //! Update value
        init = init.copy(momo = init.momo.copy(color = {
          val current = init.momo.color
          current.zipWithIndex.map {
            case (k, v) =>
              if (v < colorSampleIndex.size)
                colorSampleList(colorSampleIndex(v)).asInstanceOf[Double] * maximalSigma
              else
                k
          }
        }))
        //! Increase index
        var i = 0
        var carry = 0
        colorSampleIndex(maxColorDim - 1) = colorSampleIndex(maxColorDim - 1) + 1
        for (i <- maxColorDim - 1 to 0 by -1) {
          val v = colorSampleIndex(i) + carry
          carry = v / cfg.getColorSamplePerDimension
          colorSampleIndex(i) = v % cfg.getColorSamplePerDimension
        }
      }
    }
    for (shape <- 0 until shapeSamples) {
      if (!cfg.isFixShape) {
        //! Do random
        if (cfg.getShapeRandomCount > 0) randomShape()
        //! Do sampling
        else {
          //! Update value
          init = init.copy(momo = init.momo.copy(shape = {
            val current = init.momo.shape
            current.zipWithIndex.map { case (k, v) =>
              if (v < shapeSampleIndex.size)
                shapeSampleList(shapeSampleIndex(v)).asInstanceOf[Double] * maximalSigma
              else
                k
            }
          }))
          //! Increase index
          var i = 0
          var carry = 0
          shapeSampleIndex(maxShapeDim - 1) = shapeSampleIndex(maxShapeDim - 1) + 1
          for (i <- maxShapeDim - 1 to 0 by -1) {
            val v = shapeSampleIndex(i) + carry
            carry = v / cfg.getShapeSamplePerDimension
            shapeSampleIndex(i) = v % cfg.getShapeSamplePerDimension
          }
        }
      }
      for (express <- 0 until expSamples) {
        if (!cfg.isFixExpression) {
          //! Do random
          if (cfg.getExpressionRandomCount > 0) randomExpression()
          //! Do sampling
          else {
            //! Update value
            init = init.copy(momo = init.momo.copy(expression = {
              val current = init.momo.expression
              current.zipWithIndex.map { case (k, v) =>
                if (v < expSampleIndex.size)
                  expSampleList(expSampleIndex(v)).asInstanceOf[Double] * maximalSigma
                else
                  k
              }
            }))
            //! Increase index
            var i = 0
            var carry = 0
            expSampleIndex(maxShapeDim - 1) = expSampleIndex(maxExpressionDim - 1) + 1
            for (i <- maxExpressionDim - 1 to 0 by -1) {
              val v = expSampleIndex(i) + carry
              carry = v / cfg.getExpressionSamplePerDimension
              expSampleIndex(i) = v % cfg.getExpressionSamplePerDimension
            }
          }
        }
        //! Update image
        if (cfg.needUpdateImage())  updateImage()
        //! Write statistics csv
        ;
        //! Write to file
        ;
      }
    }
  }
}
