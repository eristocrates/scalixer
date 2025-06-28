<?xml version="1.0" encoding="UTF-8"?>
<sch:schema xmlns:sch="http://purl.oclc.org/dsdl/schematron">
   <sch:title>ISO Schematron file created from example.xml.dtd</sch:title>
   <sch:ns prefix="mml" uri="http://www.w3.org/1998/Math/MathML"/>
   <sch:ns prefix="xsi" uri="http://www.w3.org/2001/XMLSchema-instance"/>
   <sch:ns prefix="xlink" uri="http://www.w3.org/1999/xlink"/>
   <sch:pattern id="heading">
      <sch:rule context="/">
         <sch:report test="*">Report date: <sch:value-of select="current-dateTime()"/>
         </sch:report>
      </sch:rule>
   </sch:pattern>
   <sch:pattern id="elements">
      <sch:title>Element Checks</sch:title>
   </sch:pattern>
   <sch:pattern id="dtd-attributes">
      <sch:title>DTD Attribute Checks</sch:title>
      <sch:rule context="book">
         <sch:assert test="@id">@id is a required attribute for &lt; book &gt;</sch:assert>
      </sch:rule>
      <sch:rule context="title/@xmlns">
         <sch:assert test=". = ''">@xmlns is a fixed attribute for &lt;title&gt; and must equal ""</sch:assert>
      </sch:rule>
      <sch:rule context="book/@xmlns">
         <sch:assert test=". = ''">@xmlns is a fixed attribute for &lt;book&gt; and must equal ""</sch:assert>
      </sch:rule>
      <sch:rule context="catalog/@xmlns">
         <sch:assert test=". = ''">@xmlns is a fixed attribute for &lt;catalog&gt; and must equal ""</sch:assert>
      </sch:rule>
      <sch:rule context="genre/@xmlns">
         <sch:assert test=". = ''">@xmlns is a fixed attribute for &lt;genre&gt; and must equal ""</sch:assert>
      </sch:rule>
      <sch:rule context="author/@xmlns">
         <sch:assert test=". = ''">@xmlns is a fixed attribute for &lt;author&gt; and must equal ""</sch:assert>
      </sch:rule>
      <sch:rule context="price/@xmlns">
         <sch:assert test=". = ''">@xmlns is a fixed attribute for &lt;price&gt; and must equal ""</sch:assert>
      </sch:rule>
      <sch:rule context="description/@xmlns">
         <sch:assert test=". = ''">@xmlns is a fixed attribute for &lt;description&gt; and must equal ""</sch:assert>
      </sch:rule>
      <sch:rule context="publish_date/@xmlns">
         <sch:assert test=". = ''">@xmlns is a fixed attribute for &lt;publish_date&gt; and must equal ""</sch:assert>
      </sch:rule>
   </sch:pattern>
</sch:schema>
