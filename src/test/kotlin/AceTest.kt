import com.intellij.testFramework.EditorActionTestCase

/**
 * @see com.intellij.testFramework.fixtures.LightCodeInsightFixtureTestCase
 */
class AceTest : EditorActionTestCase() {
  override fun getActionId() = "AceAction"

  fun testSomething() {
    assert(true)
  }
}