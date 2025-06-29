import cats.effect.{IO}
import scala.xml.{XML, Elem, Node => XmlNode, Text, UnprefixedAttribute, MetaData, Null => XmlNull}
import java.nio.file.{Paths, Path}
import scala.collection.mutable

object RdfToXml:

  private case class Node(
      var tagName: Option[String] = None,
      attrs: mutable.Map[String, String] = mutable.Map.empty,
      var text: Option[String] = None,
      children: mutable.ListBuffer[String] = mutable.ListBuffer.empty,
      var sourceProfile: Option[String] = None
  )

  private def attrNameFromProp(prop: String): String =
    val base = prop.stripPrefix("has")
    if base == "Xmlns" then "xmlns"
    else if base.length <= 3 then s"xmlns:${base.toLowerCase}"
    else base.head.toLower + base.tail

  private val rdfNS  = "http://www.w3.org/1999/02/22-rdf-syntax-ns#"
  private val rdfsNS = "http://www.w3.org/2000/01/rdf-schema#"
  private val exNS   = "http://example.org/"

  def run(args: List[String]): IO[Unit] = IO {
    val targetProfile =
      args.sliding(2).collectFirst { case List("--targetProfile", p) => p }
    targetProfile.foreach(p => println(s"Target profile: $p"))

    val rdfPath = Paths.get("example.rdf")
    val rdf     = XML.loadFile(rdfPath.toFile)

    val nodes = mutable.Map.empty[String, Node]
    def node(id: String): Node = nodes.getOrElseUpdate(id, Node())

    for desc <- (rdf \ "Description") do
      val about = (desc \ s"@{${rdfNS}}about").text
      val n     = node(about)

      // tag name from rdf:type ..._Tag
      for t <- desc \ "type" do
        val res = (t \ s"@{${rdfNS}}resource").text
        if res.endsWith("_Tag") then
          val name = res.split('/').last.stripSuffix("_Tag")
          n.tagName = Some(name)

      // rdfs:member children
      for m <- desc \ "member" do
        val child = (m \ s"@{${rdfNS}}resource").text
        if child.nonEmpty then n.children += child

      // other elements
      for e <- desc.child.collect { case el: scala.xml.Elem => el } do
        val q = Option(e.prefix).map(_ + ":" + e.label).getOrElse(e.label)
        q match
          case "rdf:type" | "rdfs:member" =>
          case "rdfs:label"                 => n.text = Some(e.text)
          case "ex:sourceProfile"           => n.sourceProfile = Some(e.text)
          case other =>
            val resAttr = e.attribute(rdfNS, "resource").map(_.text)
            resAttr match
              case Some(childId) if childId.nonEmpty =>
                n.children += childId
              case _ =>
                val attrName = attrNameFromProp(e.label)
                n.attrs(attrName) = e.text

    val referenced = nodes.values.flatMap(_.children).toSet
    val rootId = nodes.collectFirst { case (id, nd) if nd.tagName.nonEmpty && !referenced(id) => id }.get

    def build(id: String): XmlNode =
      val nd      = nodes(id)
      val metadata = nd.attrs.foldLeft(XmlNull: MetaData) { case (acc, (k, v)) =>
        new UnprefixedAttribute(k, v, acc)
      }
      val childNodes = nd.children.map(build) ++ nd.text.map(Text(_))
      Elem(null, nd.tagName.getOrElse(""), metadata, scala.xml.TopScope, minimizeEmpty = false, childNodes* )

    val xmlRoot = build(rootId)
    XML.save("lowered.xml", xmlRoot, "UTF-8", xmlDecl = true)
  }
