<?xml version="1.0" ?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                version="1.0"
                xmlns="http://www.w3.org/TR/xhtml1/transitional"
                exclude-result-prefixes="#default">

  <xsl:import href="../build/stylesheet.html.xsl" />

  <xsl:variable name="html.ext" select="'.html'" />
  <xsl:variable name="root.filename" select="'index'" />

  <xsl:variable name="toc.section.depth" select="2" />
  <xsl:variable name="chunk.section.depth" select="1" />
</xsl:stylesheet>
