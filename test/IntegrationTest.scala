import org.junit._
import play.mvc._
import play.test._
import play.libs.F._
import play.test.Helpers._
import org.fest.assertions.Assertions._
import org.fluentlenium.core.filter.FilterConstructor._

class IntegrationTest {
  /**
   * add your integration test here
   * in this example we just check if the welcome page is being shown
   */
  @Test def test() {
//    running(testServer(3333, fakeApplication(inMemoryDatabase)), HTMLUNIT, new F.Callback[TestBrowser] {
//      def invoke(browser: TestBrowser) {
//        browser.goTo("http://localhost:3333")
//        assertThat(browser.pageSource).contains("Your new application is ready.")
//      }
//    })
  }
}