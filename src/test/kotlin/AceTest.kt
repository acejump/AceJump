import com.intellij.openapi.actionSystem.IdeActions
import com.intellij.openapi.fileTypes.PlainTextFileType
import com.intellij.testFramework.fixtures.LightCodeInsightFixtureTestCase
import com.intellij.util.ui.UIUtil
import org.acejump.control.AceAction
import org.acejump.search.Finder
import org.acejump.view.Canvas

/**
 * @see com.intellij.testFramework.fixtures.LightCodeInsightFixtureTestCase
 */
class AceTest : LightCodeInsightFixtureTestCase() {
  override fun tearDown() {
    myFixture.performEditorAction(IdeActions.ACTION_EDITOR_ESCAPE)
    UIUtil.dispatchAllInvocationEvents()
    assert(Canvas.jumpLocations.isEmpty())
    super.tearDown()
  }

  fun `test finder finds all occurrences`() {
    val code = "test test test"

    invokeAndSearch(code, "test")

    assert(Canvas.jumpLocations.size == 3)
  }

  fun `test finder finds nothing`() {
    val code = "test test test"

    invokeAndSearch(code, "qest")

    assert(Canvas.jumpLocations.isEmpty())
  }


  fun `test finder is sticky`() {
    val code = "test test test"

    invokeAndSearch(code, "tezt")

    assert(Canvas.jumpLocations.size == 3)
  }

  fun `test jump to first occurrence`() {
    val code = "<caret>testing 1234"

    invokeAndSearch(code, "1")

    myFixture.performEditorAction(IdeActions.ACTION_EDITOR_ENTER)
    // TODO: Verify that the caret has moved to "testing <caret>1234"
  }

  private fun invokeAndSearch(code: String, query: String) =
    myFixture.run {
      configureByText(PlainTextFileType.INSTANCE, code)
      testAction(AceAction())
      type(query)
      UIUtil.dispatchAllInvocationEvents()
    }
}