<?xml version="1.0" encoding="UTF-8"?>

<!--

  Written by Michael Koch <tensberg@gmx.net>

  This file is in the public domain and may be distributed freely unmodified or modified.

  $Id$

-->

<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">
  
  <xsl:output method="text" encoding="UTF-8" />

  <xsl:param name="writetr" />
  <xsl:param name="writere" />
  <xsl:param name="voclist" />

  <xsl:template match="/">
    <xsl:apply-templates select="jgloss/body" />
  </xsl:template>

  <xsl:template match="jgloss">
    <xsl:apply-templates />
    <xsl:if test="$voclist">
      <xsl:text>
        
Vocabulary List
---------------

      </xsl:text>
      <xsl:apply-templates select=".//anno" mode="voclist" />
    </xsl:if>
  </xsl:template>

  <xsl:template match="anno">
    <xsl:apply-templates />
    <xsl:if test="$writetr and string-length(@tr)>0">
      <xsl:text>(</xsl:text><xsl:value-of select="@tr" /><xsl:text>)</xsl:text>
    </xsl:if>
  </xsl:template>

  <xsl:template match="rbase">
    <xsl:value-of select="."/>
    <xsl:if test="$writere and string-length(@re)>0">
      <xsl:text>《</xsl:text><xsl:value-of select="@re" /><xsl:text>》</xsl:text>
    </xsl:if>
  </xsl:template>

  <xsl:template match="anno" mode="voclist">
    <xsl:choose>
      <xsl:when test="string-length(@base)>0">
        <xsl:value-of select="@base" />
      </xsl:when>
      <xsl:otherwise>
        <xsl:for-each select="rbase|text()">
          <xsl:value-of select="." />
        </xsl:for-each>
      </xsl:otherwise>
    </xsl:choose>

    <xsl:if test="string-length(@basere)>0 or count(rbase)>0">
      <xsl:text> 《</xsl:text>
      <xsl:choose>
        <xsl:when test="string-length(@basere)>0">
          <xsl:value-of select="@basere" />
        </xsl:when>
        <xsl:otherwise>
          <xsl:for-each select="rbase|text()">
            <xsl:choose>
              <xsl:when test="string-length(@re)>0">
                <xsl:value-of select="@re" />
              </xsl:when>
              <xsl:otherwise>
                <xsl:value-of select="." />
              </xsl:otherwise>
            </xsl:choose>
          </xsl:for-each>
        </xsl:otherwise>
      </xsl:choose>
      <xsl:text>》</xsl:text>
    </xsl:if>

    <xsl:if test="string-length(@tr)>0">
      <xsl:text> </xsl:text>
      <xsl:value-of select="@tr" />
    </xsl:if>

    <xsl:text>
    </xsl:text>
  </xsl:template>

</xsl:stylesheet>
