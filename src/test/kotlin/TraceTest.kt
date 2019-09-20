import javafx.application.Application
import javafx.event.EventHandler
import javafx.scene.Scene
import javafx.scene.canvas.Canvas
import javafx.scene.canvas.GraphicsContext
import javafx.scene.input.KeyCode.ESCAPE
import javafx.scene.layout.Pane
import javafx.scene.paint.Color
import javafx.stage.Screen
import javafx.stage.Stage
import javafx.stage.StageStyle.TRANSPARENT
import org.bytedeco.javacpp.IntPointer
import org.bytedeco.javacv.Java2DFrameConverter
import org.bytedeco.javacv.LeptonicaFrameConverter
import org.bytedeco.leptonica.PIX
import org.bytedeco.leptonica.global.lept
import org.bytedeco.tesseract.ResultIterator
import org.bytedeco.tesseract.TessBaseAPI
import org.bytedeco.tesseract.global.tesseract.*
import org.junit.Ignore
import org.junit.Test
import java.awt.*
import java.net.URI
import java.net.URLEncoder
import kotlin.system.exitProcess
import kotlin.system.measureTimeMillis

class TraceTest : Application() {
  private val api = TessBaseAPI()
  private val robot = Robot()
  init {
    if (api.Init("src/main/resources", "eng") != 0) {
      System.err.println("Could not initialize Tesseract")
      exitProcess(1)
    }
  }

  private val results = mutableListOf<Target>()

  @Throws(Exception::class)
  override fun start(stage: Stage) {
    val bounds = Screen.getPrimary().visualBounds

    stage.initStyle(TRANSPARENT)

    val canvas = Canvas(bounds.width, bounds.height)
    val gc = canvas.graphicsContext2D

    for (i in 0..10) println(measureTimeMillis { paintTargets(gc!!) })
    val pane = Pane()
    pane.children.add(canvas)

    val scene = Scene(pane, bounds.width, bounds.height)
    scene.onMouseClicked = EventHandler { e ->
      val tg = results.firstOrNull { it.isPointInMap(e.x.toInt(), e.y.toInt()) }
      if (tg != null)
        try {
          val desktop = Desktop.getDesktop()
          val encodedQuery = URLEncoder.encode(tg.string, "UTF-8")
          val oURL = URI("https://stackoverflow.com/search?q=$encodedQuery")
          desktop.browse(oURL)
        } catch (e: Exception) {
          e.printStackTrace()
        }

//      println("Received click event at: (x: ${e.x} y: ${e.y})")
    }
    scene.onKeyPressed = EventHandler { e ->
      if (e.code == ESCAPE) stage.close()
//      else if(e.code == H) {
//        gc.clearRect(0.0, 0.0, bounds.width, bounds.height)
//      }
    }

    scene.fill = Color.TRANSPARENT
    stage.scene = scene
    stage.x = 0.0
    stage.y = 0.0

    stage.show()
  }

  private fun getScreenContents(): PIX {
    val screenRect = Rectangle(Toolkit.getDefaultToolkit().screenSize)
    val capture = robot.createScreenCapture(screenRect)
    val j2d = Java2DFrameConverter().convert(capture)
    return LeptonicaFrameConverter().convert(j2d)
  }

  private fun paintTarget(resultIt: ResultIterator, gc: GraphicsContext) {
    val outText = resultIt.GetUTF8Text(RIL_WORD)
    val conf = resultIt.Confidence(RIL_WORD)
//      println("${outText.string} ($conf)")

    val left = IntPointer(1)
    val top = IntPointer(1)
    val right = IntPointer(1)
    val bottom = IntPointer(1)
    val pageIt = TessResultIteratorGetPageIterator(resultIt)
    TessPageIteratorBoundingBox(pageIt, RIL_WORD, left, top, right, bottom)

    val x = left.get().toDouble()
    val y = top.get().toDouble()
    val width = (right.get() - left.get()).toDouble()
    val height = (bottom.get() - top.get()).toDouble()
//      println("x: $x, y: $y, width: $width, height: $height")

    gc.fill = Color(0.0, 1.0, 0.0, 0.4)
    if (conf > 1 && outText.string.length > 3) {
      gc.fillRoundRect(x, y, width, height, 10.0, 10.0)
      results.add(Target(outText.string,
        x.toInt(), y.toInt(), right.get(), bottom.get()))
    }

    outText.deallocate()
    arrayOf(left, top, right, bottom).forEach { it.deallocate() }
  }

  private fun paintTargets(gc: GraphicsContext) {
    var start = System.currentTimeMillis()
    val image = getScreenContents()
    results.clear()
    api.SetImage(image)
    println("Setup time: ${System.currentTimeMillis() - start}")
    start = System.currentTimeMillis()
    api.Recognize(TessMonitorCreate())
    println("Recognize time: ${System.currentTimeMillis() - start}")
    start = System.currentTimeMillis()
    val resultIt = api.GetIterator()
    if (resultIt != null) do {
      paintTarget(resultIt, gc)
    } while (resultIt.Next(RIL_WORD))
    println("Read time: ${System.currentTimeMillis() - start}")
    start = System.currentTimeMillis()
    lept.pixDestroy(image)
    println("Cleanup time: ${System.currentTimeMillis() - start}")
  }

  @Ignore
  @Test
  fun traceTest() = launch()

  inner class Target(val string: String = "",
    val x1: Int = 0, val y1: Int = 0, val x2: Int = 0, val y2: Int = 0) {
    fun isPointInMap(x: Int, y: Int) = x in x1..x2 && y in y1..y2
  }
}