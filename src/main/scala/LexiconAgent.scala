object LexiconAgent {
  /** Infer basic XSD datatype for a string.
    * Defaults to `xsd:string` when no lexical match is found.
    */
  def inferLiteralType(text: String): String =
    text.trim match {
      case s if s.matches("^-?\\d+$")       => "xsd:integer"
      case s if s.matches("^-?\\d+\\.\\d+$") => "xsd:decimal"
      case s if s.matches("^(true|false)$")  => "xsd:boolean"
      case s if s.matches("\\d{4}-\\d{2}-\\d{2}") => "xsd:date"
      case _                                 => "xsd:string"
    }
}
