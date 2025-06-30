object LexiconAgent {
  /** Infer basic XSD datatype for a string.
    * Defaults to `xsd:string` when no lexical match is found.
    */
  private val knownSchemePattern    = "^(mailto|file|urn|gemini|at):\\S+$".r
  private val genericSchemePattern  = "^[a-zA-Z][a-zA-Z0-9+.-]*://\\S+$".r
  private val booleanPattern        = "^(true|false)$".r
  private val intPattern            = "^-?\\d+$".r
  private val decimalPattern        = "^-?\\d+\\.\\d+$".r
  private val datePattern           = "\\d{4}-\\d{2}-\\d{2}".r

  def inferLiteralType(text: String): String =
    text.trim match
      case knownSchemePattern(_*)   => "xsd:anyURI"
      case genericSchemePattern(_*) => "xsd:anyURI"
      case booleanPattern()       => "xsd:boolean"
      case intPattern()           => "xsd:integer"
      case decimalPattern()       => "xsd:decimal"
      case datePattern()          => "xsd:date"
      case _                      => "xsd:string"
}