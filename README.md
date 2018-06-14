## Dev environment

Client is built with scalajs-bundler plugin so nodejs and npm must be available.

Enter the sbt console and run:
```sh
sbt:planning-poker> project client
sbt:client> fastOptJS/webpack
sbt:client> startWorkbenchServer
sbt:client> ~fastOptJS
```
Open `http://localhost:12345/`

## Deploy

$ sbt client/ghpagesPushSite
