package client

class Endpoints(baseUrl: String, secure: Boolean) {
  object session {
    val create = httpUrl("/session")
    def get(id: String) = httpUrl(s"/session/$id")
    def ws(id: String, userId: String) = wsUrl(s"/session/$id/ws/$userId")
  }
  private def httpUrl(path: String) = {
    val protocol = if (secure) "https" else "http"
    s"$protocol://$baseUrl$path"
  }
  private def wsUrl(path: String) = {
    val protocol = if (secure) "wss" else "ws"
    s"$protocol://$baseUrl$path"
  }
}
