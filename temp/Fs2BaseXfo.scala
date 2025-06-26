import cats.effect.{IO, IOApp}
import fs2.data.xml._
import fs2.data.xml.XmlEvent.*
import fs2.data.text._
import fs2.Stream
import java.io.InputStream

object Fs2SaxLike extends IOApp.Simple {

  def run: IO[Unit] = {
    val in: InputStream = getClass.getResourceAsStream("/sample.xml")
    if (in == null) IO.raiseError(new IllegalArgumentException("Missing sample.xml"))
    else {
      fs2.io.readInputStream(IO.pure(in), 4096)
        .through(fs2.text.utf8.decode)
        .through(events[IO, String]())
        .evalMap {
          case StartTag(name, _, _) =>
            IO.println(s"Start Element: <${name.local}>")
          case EndTag(name) =>
            IO.println(s"End Element: </${name.local}>")
          case XmlString(s, _) if s.trim.nonEmpty =>
            IO.println(s"Text chunk: '${s.trim}'")
          case Comment(c) =>
            IO.println(s"Comment: $c")
          case _ =>
            IO.unit
        }
        .compile
        .drain
    }
  }
}
