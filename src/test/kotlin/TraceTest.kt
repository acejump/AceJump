import javafx.application.Application
import javafx.event.EventHandler
import javafx.scene.Scene
import javafx.scene.canvas.Canvas
import javafx.scene.input.MouseEvent
import javafx.scene.layout.Pane
import javafx.scene.paint.Color
import javafx.scene.text.Font
import javafx.stage.*
import org.acejump.label.Solver
import org.bytedeco.javacpp.IntPointer
import org.bytedeco.javacv.Java2DFrameConverter
import org.bytedeco.javacv.LeptonicaFrameConverter
import org.bytedeco.leptonica.PIX
import org.bytedeco.leptonica.global.lept
import org.bytedeco.leptonica.global.lept.pixScale
import org.bytedeco.tesseract.ResultIterator
import org.bytedeco.tesseract.TessBaseAPI
import org.bytedeco.tesseract.global.tesseract.*
import org.jnativehook.GlobalScreen
import org.jnativehook.NativeHookException
import org.jnativehook.keyboard.NativeKeyEvent
import org.jnativehook.keyboard.NativeKeyListener
import org.junit.Test
import java.awt.*
import java.net.URLEncoder
import kotlin.system.exitProcess
import kotlin.system.measureTimeMillis

class TraceTest : Application() {
  private lateinit var stage: Stage
  private val api = TessBaseAPI()
  private val robot = Robot()
  private var query = ""
  @Volatile var resultMap: Map<String, Target> = mapOf()
  @Volatile var activated = false
  @Volatile lateinit var scene: Scene

  init {
    if (api.Init("src/main/resources", "eng") != 0) {
      System.err.println("Could not initialize Tesseract")
      exitProcess(1)
    }
    registerKeyListener()
    Thread {
      while (!activated) {
        println("Activated: $activated")
        resultMap = findTargets()
        scene = paintScene()
      }
    }.start()
  }

  fun paintScene(): Scene {
    val canvas = Screen.getPrimary().visualBounds.run { Canvas(width, height) }
    println(measureTimeMillis { paintTargets(canvas, resultMap) })
    val pane = Pane().apply { children.add(canvas) }
    val scene = Scene(pane, canvas.width, canvas.height).apply {
      onMouseClicked = makeClickHandler(resultMap.values)
//      onKeyPressed = makeEventHandler(stage)
      fill = Color.TRANSPARENT
    }

    return scene
  }

  fun registerKeyListener() {
    try {
      GlobalScreen.registerNativeHook()
    } catch (ex: NativeHookException) {
      System.err.println(ex.message)
      exitProcess(1)
    }

    GlobalScreen.addNativeKeyListener(object : NativeKeyListener {
      @Volatile
      var ctrlDown = false

      override fun nativeKeyPressed(p0: NativeKeyEvent) {
        activated = true
//        if (p0.keyCode == NativeKeyEvent.VC_SEMICOLON) ctrlDown = true
//        else if (p0.keyCode == NativeKeyEvent.VC_SEMICOLON && ctrlDown) {
//          activated = true
//          println("Activated!")
//        }
      }

      override fun nativeKeyReleased(p0: NativeKeyEvent) {
        if (p0.keyCode == NativeKeyEvent.CTRL_MASK) ctrlDown = false
      }

      override fun nativeKeyTyped(e: NativeKeyEvent) {
        if (!e.keyChar.isLetterOrDigit()) exitProcess(0)
        else {
          query += e.keyChar
          resultMap[query.takeLast(2)]?.run { jumpTo(this); query = "" }
        }
      }
    })
  }

  fun makeClickHandler(results: Collection<Target>) =
    EventHandler<MouseEvent> { e ->
      val tg = results.firstOrNull { it.isPointInMap(e.x, e.y) }
      if (tg != null) jumpTo(tg)
      // println("Received click event at: (x: ${e.x} y: ${e.y})")
    }

  override fun start(stage: Stage) {
    this.stage = stage
    stage.initStyle(StageStyle.TRANSPARENT)
    while (!activated) { Thread.sleep(10) }
    stage.scene = scene
    stage.x = 0.0
    stage.y = 0.0
    stage.show()
  }

  val queryPrefix = "https://stackoverflow.com/search?q="

  fun jumpTo(tg: Target) {
    try {
      val encodedQuery = URLEncoder.encode(tg.string, "UTF-8")
      hostServices.showDocument("$queryPrefix$encodedQuery")
//      robot.mouseMove(tg.x1.toInt(), tg.y1.toInt())
//      robot.mousePress(InputEvent.BUTTON1_DOWN_MASK)
//      robot.mouseRelease(InputEvent.BUTTON1_DOWN_MASK)
    } catch (e: Exception) {
      e.printStackTrace()
    }
  }

  fun TessBaseAPI.recognizeImage(image: PIX) {
    var start = System.currentTimeMillis()
    SetImage(image)
    println("Setup time: ${System.currentTimeMillis() - start}")
    start = System.currentTimeMillis()
    val resultCode = Recognize(TessMonitorCreate())
    if (resultCode != 0) {
      throw Exception("Recognition error: $resultCode")
    }
    println("Recognition time: ${System.currentTimeMillis() - start}")
  }

  private fun findTargets(): Map<String, Target> {
    val results = getResults(api, getScreenContents())

    val targetMap = mutableMapOf<Int, Target>()
    val text = results.fold("") { acc, t ->
      targetMap[acc.length] = t; acc + t.string + " "
    }

    return Solver(text, "", targetMap.keys).solve()
      .map { Pair(it.key, targetMap[it.value]!!) }.toMap()
  }

  val SCALE_FACTOR = 4.0f

  private fun getScreenContents(): PIX {
    val screenRect = Rectangle(Toolkit.getDefaultToolkit().screenSize)
    val capture = robot.createScreenCapture(screenRect)
    val j2d = Java2DFrameConverter().convert(capture)
    val pix = LeptonicaFrameConverter().convert(j2d)
    return pixScale(pix, SCALE_FACTOR, SCALE_FACTOR)
  }

  private fun paintTarget(canvas: Canvas, target: Target, tag: String) {
    if (target.conf <= 1 || target.string.length < 3) return
    val gc = canvas.graphicsContext2D

    gc.fill = Color(0.0, 1.0, 0.0, 0.4)
    gc.fillRoundRect(target.x1,
      target.y1,
      target.width,
      target.height,
      10.0,
      10.0)
    gc.fill = Color(1.0, 1.0, 0.0, 1.0)
    val widthOfTag = target.height * 0.7 * 2
    val startOfTag = target.x1 - widthOfTag
    gc.fillRoundRect(startOfTag,
      target.y1,
      widthOfTag,
      target.height,
      10.0,
      10.0)
    gc.fill = Color(0.0, 0.0, 0.0, 1.0)
    gc.font = Font.font("Courier")
    gc.fillText(tag.toUpperCase(), startOfTag, target.y2)
  }

  private fun readTargetFromResult(resultIt: ResultIterator): Target {
    val outTextPtr = resultIt.GetUTF8Text(RIL_WORD)
    val outText = outTextPtr.string
    outTextPtr.deallocate()

    val conf = resultIt.Confidence(RIL_WORD)

    val left = IntPointer(1)
    val top = IntPointer(1)
    val right = IntPointer(1)
    val bottom = IntPointer(1)

    val pageIt = TessResultIteratorGetPageIterator(resultIt)
    TessPageIteratorBoundingBox(pageIt, RIL_WORD, left, top, right, bottom)

    val x1 = left.get().toDouble() / SCALE_FACTOR
    val y1 = top.get().toDouble() / SCALE_FACTOR
    val x2 = right.get().toDouble() / SCALE_FACTOR
    val y2 = bottom.get().toDouble() / SCALE_FACTOR
    arrayOf(left, top, right, bottom).forEach { it.deallocate() }
    pageIt.deallocate()

    return Target(outText, conf, x1, y1, x2, y2)
  }

  private fun paintTargets(canvas: Canvas, resultMap: Map<String, Target>) {
    var start = System.currentTimeMillis()
    resultMap.forEach { paintTarget(canvas, it.value, it.key) }
    println("Read time: ${System.currentTimeMillis() - start}")
    start = System.currentTimeMillis()
    println("Cleanup time: ${System.currentTimeMillis() - start}")
  }

  private fun getResults(api: TessBaseAPI, image: PIX): MutableList<Target> {
    api.recognizeImage(image)
    val results = mutableListOf<Target>()
    val resultIt = api.GetIterator()
    if (resultIt != null) {
      do {
        results.add(readTargetFromResult(resultIt))
      } while (resultIt.Next(RIL_WORD))
      resultIt.deallocate()
    }
    lept.pixDestroy(image)
    return results
  }

  inner class Target(val string: String = "",
                     val conf: Float = 0f,
                     val x1: Double = 0.0,
                     val y1: Double = 0.0,
                     val x2: Double = 0.0,
                     val y2: Double = 0.0) {
    val width = x2 - x1
    val height = y2 - y1
    fun isPointInMap(x: Double, y: Double) = x in x1..x2 && y in y1..y2
  }

  @Test
  fun traceTest() = launch()
}