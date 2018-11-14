import com.intellij.testFramework.EditorActionTestCase

/**
 * @see com.intellij.testFramework.fixtures.LightCodeInsightFixtureTestCase
 */
class AceTest : EditorActionTestCase() {
  override fun getActionId() = "AceAction"

  // TODO: Improve test coverage, https://github.com/acejump/AceJump/issues/139
  fun testSomething() {
    assert(true)
  }
}