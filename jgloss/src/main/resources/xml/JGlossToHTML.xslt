<?xml version="1.0" encoding="UTF-8"?>

<!--

 Copyright (C) 2002-2013 Michael Koch (tensberg@gmx.net)

 This file is part of JGloss.

 JGloss is free software; you can redistribute it and/or modify
 it under the terms of the GNU General Public License as published by
 the Free Software Foundation; either version 2 of the License, or
 (at your option) any later version.

 JGloss is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with JGloss; if not, write to the Free Software
 Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA

 $Id$

-->

<!-- 
  Transforms a JGloss XML document to a modified HTML document readable by
  JGlossHTMLDoc/JGlossEditorKit.
 -->

<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">

<xsl:output method="html" indent="no" encoding="UTF-8" />

  <xsl:template match="jgloss">
    <html>
      <xsl:apply-templates />
    </html>
  </xsl:template>

  <xsl:template match="anno">
    <anno>

      <xsl:if test="string-length(@base)>0">
        <xsl:attribute name="base"><xsl:value-of select="@base" /></xsl:attribute>
      </xsl:if>

      <xsl:if test="string-length(@basere)>0">
        <xsl:attribute name="basere"><xsl:value-of select="@basere" /></xsl:attribute>
      </xsl:if>

      <xsl:if test="string-length(@type)>0">
        <xsl:attribute name="type"><xsl:value-of select="@type" /></xsl:attribute>
      </xsl:if>

      <word>
        <xsl:choose>
          <xsl:when test="count(rbase)=0">
            <rb>
              <reading><xsl:text> </xsl:text></reading>
              <bt>
                <xsl:value-of select="." />
              </bt>
            </rb>
          </xsl:when>
          <xsl:otherwise>
            <xsl:for-each select="*|text()">
              <xsl:choose>
                <xsl:when test="contains(name(.),'rbase')">
                  <xsl:apply-templates select="." />
                </xsl:when>
                <xsl:otherwise>
                  <bt>
                    <xsl:value-of select="." />
                  </bt>
                </xsl:otherwise>
              </xsl:choose>
            </xsl:for-each>
          </xsl:otherwise>
        </xsl:choose>
      </word>
      <trans>
        <xsl:choose>
          <xsl:when test="string-length(@tr)>0">
            <xsl:value-of select="@tr" />
          </xsl:when>
          <xsl:otherwise>
            <xsl:text> </xsl:text>
          </xsl:otherwise>
        </xsl:choose>
      </trans>
    </anno>
  </xsl:template>

  <xsl:template match="rbase">
    <rb>

      <xsl:if test="string-length(@docre)>0">
        <xsl:attribute name="docre"><xsl:value-of select="@docre" /></xsl:attribute>
      </xsl:if>

      <reading>
        <xsl:choose>
          <xsl:when test="string-length(@re)>0">
            <xsl:value-of select="@re" />
          </xsl:when>
          <xsl:otherwise>
            <xsl:text> </xsl:text>
          </xsl:otherwise>
        </xsl:choose>
      </reading>
      <bt>
        <xsl:value-of select="." />
      </bt>
    </rb>
  </xsl:template>

  <xsl:template match="*">
    <xsl:copy><xsl:apply-templates /></xsl:copy>
  </xsl:template>

</xsl:stylesheet>
