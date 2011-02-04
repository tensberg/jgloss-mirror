<?xml version="1.0" ?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="1.0"
                xmlns:fo="http://www.w3.org/1999/XSL/Format"
                exclude-result-prefixes="#default">

  <xsl:import href="../build/stylesheet.pdf.xsl" />

  <xsl:param name="japanesefont" select="Kochi-Mincho" />

  <xsl:variable name="toc.section.depth" select="2" />
  <xsl:variable name="chunk.section.depth" select="1" />
  <xsl:variable name="paper.type" select="'A4'" />
  <xsl:variable name="alignment" select="'left'" />
  <xsl:variable name="draft.mode" select="'no'" />

  <xsl:template match="foreignphrase[@lang='ja']">
    <fo:inline font-family="{$japanesefont}">
      <xsl:call-template name="inline.charseq" />
    </fo:inline>
  </xsl:template>
</xsl:stylesheet>
