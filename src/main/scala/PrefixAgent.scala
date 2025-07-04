object PrefixAgent {
  import scala.collection.mutable

  val prefixMap: mutable.Map[String, String] = mutable.LinkedHashMap(
    "rdf"  -> "http://www.w3.org/1999/02/22-rdf-syntax-ns#",
    "rdfs" -> "http://www.w3.org/2000/01/rdf-schema#",
    "owl"  -> "http://www.w3.org/2002/07/owl#",
    "ex"   -> "http://example.org/",
    "prov" -> "http://www.w3.org/ns/prov#",
    "xsd"  -> "http://www.w3.org/2001/XMLSchema#"
  )
}
