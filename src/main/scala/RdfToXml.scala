import cats.effect.{IO}
import scala.xml.{XML, Elem, Node => XmlNode, Text, UnprefixedAttribute, PrefixedAttribute, MetaData, Null => XmlNull}
import java.nio.file.{Paths, Path}
import scala.collection.mutable

import PrefixAgent.prefixMap

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
    val attrKeyMap   = mutable.Map.empty[String, String] // attrNodeId -> key
    val attrValueMap = mutable.Map.empty[String, String] // attrNodeId -> value

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

      // Track attribute_key and attribute_value for separate attr nodes
      for e <- desc.child.collect { case el: scala.xml.Elem => el } do
        val resAttr = e.attribute(rdfNS, "resource").map(_.text)
        val q       = Option(e.prefix).map(_ + ":" + e.label).getOrElse(e.label)
        q match
          case "rdf:type" | "rdfs:member" => // handled elsewhere
          case "ex:attribute_key" =>
            attrKeyMap(about) = e.text
          case "ex:attribute_value" =>
            attrValueMap(about) = e.text
          case "ex:attribute" =>
            val attrId = resAttr.getOrElse("")
            if attrId.nonEmpty then
              // Delay real insertion to build time
              n.attrs(attrId) = "__ATTR_PLACEHOLDER__"
          case "ex:sourceProfile" =>
            n.sourceProfile = Some(e.text)
          case "rdfs:label" =>
            n.text = Some(e.text)
          case "ex:xmlString" =>
            n.text = Some(e.text)

          case other =>
            val attrName = attrNameFromProp(e.label)
            n.attrs(attrName) = e.text

      // rdfs:member children
      for m <- desc \ "member" do
        val child = (m \ s"@{${rdfNS}}resource").text
        if child.nonEmpty then n.children += child

    val referenced = nodes.values.flatMap(_.children).toSet
    val rootId = nodes.collectFirst { case (id, nd) if nd.tagName.nonEmpty && !referenced(id) => id }.get

    def build(id: String): XmlNode =
      val nd      = nodes(id)
      val metadata = nd.attrs.foldLeft(XmlNull: MetaData) {
        case (acc, (attrId, "__ATTR_PLACEHOLDER__")) =>
          val key = attrKeyMap.getOrElse(attrId, "unknown")
          val value = attrValueMap.getOrElse(attrId, "")
          new UnprefixedAttribute(key, value, acc)
        case (acc, (k, v)) =>
          new UnprefixedAttribute(k, v, acc)
      }

      val childNodes = nd.children.map(build) ++ nd.text.map(Text(_))
      Elem(null, nd.tagName.getOrElse(""), metadata, scala.xml.TopScope, minimizeEmpty = false, childNodes* )

    val xmlRootBase = build(rootId)
    val nsMeta = prefixMap.foldLeft(xmlRootBase.attributes) {
      case (acc, ("", uri)) => new UnprefixedAttribute("xmlns", uri, acc)
      case (acc, (p, uri))   => new PrefixedAttribute("xmlns", p, uri, acc)
    }
    val xmlRoot = xmlRootBase match
      case e: Elem => e.copy(attributes = nsMeta)
      case n       => n
    XML.save("lowered.xml", xmlRoot, "UTF-8", xmlDecl = true)
  }
