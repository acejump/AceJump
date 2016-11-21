
import com.intellij.testFramework.EditorActionTestCase
import com.intellij.testFramework.fixtures.JavaCodeInsightFixtureTestCase
import com.intellij.testFramework.fixtures.LightCodeInsightFixtureTestCase

/**
 * @see JavaCodeInsightFixtureTestCase
 * @see LightCodeInsightFixtureTestCase
 */
class  AceTest: EditorActionTestCase() {
  override fun getActionId() = "AceJumpAction"

}
