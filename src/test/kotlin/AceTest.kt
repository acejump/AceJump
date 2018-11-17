import com.intellij.openapi.actionSystem.IdeActions
import com.intellij.openapi.fileTypes.PlainTextFileType
import com.intellij.testFramework.fixtures.LightCodeInsightFixtureTestCase
import com.intellij.util.ui.UIUtil
import org.acejump.control.AceAction
import org.acejump.search.Finder

/**
 * @see com.intellij.testFramework.fixtures.LightCodeInsightFixtureTestCase
 */
class AceTest : LightCodeInsightFixtureTestCase() {
  override fun tearDown() {
    myFixture.performEditorAction(IdeActions.ACTION_EDITOR_ESCAPE)
    assert(Finder.results.isEmpty())
    UIUtil.dispatchAllInvocationEvents()
    super.tearDown()
  }

  fun `test finder finds text`() {
    val code = "test test test"

    invokeAndSearch(code, "test")

    assert(Finder.results.size == 3)
  }

  fun `test finder finds nothing`() {
    val code = "test test test"

    invokeAndSearch(code, "qezt")

    assert(Finder.results.size == 0)
  }


  fun `test finder is sticky`() {
    val code = "test test test"

    invokeAndSearch(code, "tezt")

    assert(Finder.results.size == 3)
  }

  private fun invokeAndSearch(code: String, query: String) =
    myFixture.run {
      configureByText(PlainTextFileType.INSTANCE, code)
      testAction(AceAction())
      type(query)
    }
}