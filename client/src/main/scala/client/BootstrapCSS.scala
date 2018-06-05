package client

import scala.scalajs.js
import scala.scalajs.js.annotation.JSImport

@JSImport("!style-loader!css-loader!bootstrap/dist/css/bootstrap.css", JSImport.Default )
@js.native
object BootstrapCSS extends js.Object

@JSImport("!style-loader!css-loader!../../classes/main.css", JSImport.Default )
@js.native
object CSSLoader extends js.Object
