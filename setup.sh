This is a minimal modern Scala 3 project setup suitable for starting `scalixr`, a LIXR-inspired, idiomatic XML-to-RDF transformation library.

**File: `build.sbt`**
```scala
ThisBuild / scalaVersion := "3.4.1"
ThisBuild / version := "0.1.0"
ThisBuild / organization := "org.scalixr"

lazy val root = (project in file(".")).
  settings(
    name := "scalixr",
    libraryDependencies ++= Seq(
      "org.scala-lang.modules" %% "scala-xml" % "2.2.0",
      "org.apache.jena" % "apache-jena-libs" % "4.9.0" pomOnly(),
      "org.typelevel" %% "cats-core" % "2.10.0"
    )
  )
```

---

**File: `project/plugins.sbt`**
```scala
// optional: sbt plugin version pinning
```

---

**File: `project/build.properties`**
```properties
sbt.version=1.9.8
```

---

**File: `src/main/scala/org/scalixr/Main.scala`**
```scala
package org.scalixr

import scala.xml._

object Main:
  def main(args: Array[String]): Unit =
    println("Scalixr is alive!")
```

---

**File: `.gitignore`**
```gitignore
target/
.idea/
.vscode/
project/target/
project/project/
.DS_Store
```

---

This gives you a clean **Scala 3** project structure with:
- XML parsing via `scala-xml`
- RDF support via Apache Jena
- Functional tools via Cats

You can build this with `sbt compile`, run with `sbt run`, and test with `sbt test` (once tests are added).

Let me know when you're ready to:
- Add a module for Infoset abstraction (`.xfo`)
- Add SAX and StAX streaming support
- Add RDF lowering with Jena
- Set up SHACL/SHEX integration
