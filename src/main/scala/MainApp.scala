import cats.effect.{IO, IOApp, ExitCode}

object MainApp extends IOApp {

  def run(args: List[String]): IO[ExitCode] = {
    args.headOption match {
      case Some("rdf") =>
        println("Running RDF lift...")
        XmlToRdf.run.as(ExitCode.Success)

      case Some("xml") =>
        println("Running XML lowering...")
        RdfToXml.run(args.tail).as(ExitCode.Success)

      case Some("infer") =>
        println("Running inference and lexicon generation...")
        XmlToRdf.runInferAndLexicon.as(ExitCode.Success)

      // case Some("lift") =>
      //   XmlToRdf.runFinalLift.as(ExitCode.Success)

      case _ =>
        IO.println("Usage: sbt \"run [rdf|xml|infer]\"") *> IO.pure(ExitCode(2))
    }
  }
}
