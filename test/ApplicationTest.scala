import org.junit._
import play.mvc._
import play.test.Helpers._
import org.fest.assertions.Assertions._

/**
 *
 * Simple (JUnit) tests that can call all parts of a play app.
 * If you are interested in mocking a whole application, see the wiki for more details.
 *
 */
class ApplicationTest {
  @Test def simpleCheck() {
    val a: Int = 1 + 1
    assertThat(a).isEqualTo(2)
  }

  @Test def renderTemplate() {
//    val html: Content = views.html.index.render("Your new application is ready.",session)
//    assertThat(contentType(html)).isEqualTo("text/html")
//    assertThat(contentAsString(html)).contains("Your new application is ready.")
  }

  @Test def authenticated() {
    val result: Result = callAction(controllers.routes.ref.Application.index(), fakeRequest.withSession("sendr@dlocalhost", "klJJS13j#k"))
    //assertEquals(200, status(result))
  }
}