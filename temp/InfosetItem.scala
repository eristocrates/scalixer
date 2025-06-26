// In a separate file, ideally
sealed trait InfosetItem

case class ElementInfo(
  qualifiedName: String,
  localName: String,
  prefix: Option[String],
  namespaceUri: Option[String],
  attributes: List[AttributeInfo]
) extends InfosetItem

case class AttributeInfo(
  qualifiedName: String,
  localName: String,
  prefix: Option[String],
  namespaceUri: Option[String],
  value: String
) extends InfosetItem

case class CharacterInfo(text: String) extends InfosetItem

case class CommentInfo(comment: String) extends InfosetItem

case object EndElementInfo extends InfosetItem

