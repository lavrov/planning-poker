package client

import scala.scalajs.js
import scala.scalajs.js.annotation.JSImport

object JsImport {

  @JSImport("!style-loader!css-loader!../../classes/main.css", JSImport.Default ) @js.native
  object main extends js.Any

  @JSImport("!style-loader!css-loader!bootstrap/dist/css/bootstrap.css", JSImport.Default ) @js.native
  object bootstrap extends js.Any

  @JSImport("!style-loader!css-loader!animate.css/animate.css", JSImport.Default ) @js.native
  object animate extends js.Any
}
