<?xml version="1.0" encoding="UTF-8"?>

<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">
  
  <xsl:output method="html" indent="no" encoding="UTF-8" />
  <!-- if indent is set to "yes", Internet Explorer does not render the annotation correctly -->

  <xsl:variable name="writetr" 
    select="/jgloss-export/parameters/writetranslations='true'" />
  <xsl:variable name="writere" 
    select="/jgloss-export/parameters/writereadings='true'" />

  <xsl:template match="/">
    <xsl:apply-templates select="jgloss-export/jgloss" />
  </xsl:template>

  <xsl:template match="jgloss">
    <html>
      <xsl:apply-templates />
    </html>
  </xsl:template>

  <xsl:template match="generator">
    <meta name="generator" value="{.}" />
  </xsl:template>

  <xsl:template match="anno">
    <xsl:choose>
      <xsl:when test="$writetr and string-length(@tr)>0">
        <span title="{@tr}">
          <xsl:apply-templates />
        </span>
      </xsl:when>
      <xsl:otherwise>
        <xsl:apply-templates />
      </xsl:otherwise>
    </xsl:choose>
  </xsl:template>

  <xsl:template match="rbase">
    <xsl:choose>
      <xsl:when test="$writere and string-length(@re)>0">
        <ruby>
          <xsl:if test="$writetr and string-length(../@tr)>0">
            <xsl:attribute name="title"><xsl:value-of select="../@tr" /></xsl:attribute>
          </xsl:if>
          <rb><xsl:value-of select="." /></rb>
          <rp>《</rp><rt><xsl:value-of select="@re" /></rt><rp>》</rp>
        </ruby>
      </xsl:when>
      <xsl:otherwise>
        <xsl:value-of select="." />
      </xsl:otherwise>
    </xsl:choose>
  </xsl:template>

  <xsl:template match="*">
    <xsl:copy>
      <xsl:apply-templates />
    </xsl:copy>
  </xsl:template>

</xsl:stylesheet>
