package client

class Endpoints(baseUrl: String) {
  object session {
    val create = s"http://$baseUrl/session"
    def ws(id: String) = s"ws://$baseUrl/session/$id/ws"
  }
}
