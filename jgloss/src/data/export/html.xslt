<?xml version="1.0" encoding="UTF-8"?>

<!--

  Written by Michael Koch <tensberg@gmx.net>

  This file is in the public domain and may be distributed freely unmodified or modified.

  $Id$

-->

<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">
  
  <xsl:output method="html" indent="no" encoding="UTF-8" />
  <!-- if indent is set to "yes", Internet Explorer does not render the annotations correctly -->

  <xsl:param name="writetr" />
  <xsl:param name="writere" />

  <xsl:template match="/">
    <xsl:apply-templates select="jgloss" />
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
