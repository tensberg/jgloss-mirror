<?xml version="1.0" encoding="UTF-8"?>

<!--

 Copyright (C) 2012 Michael Koch (tensberg@gmx.net)

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
<!-- Transforms a JGloss 1 XML document to a JGloss 2 document -->

<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">

    <xsl:output method="xml" indent="no" encoding="UTF-8" />

    <xsl:template match="html">
        <jgloss>
            <xsl:apply-templates />
        </jgloss>
    </xsl:template>

    <xsl:template match="head">
        <head>
            <xsl:apply-templates />
        </head>
    </xsl:template>

    <xsl:template match="title">
        <title><xsl:apply-templates /></title>
    </xsl:template>

    <xsl:template match="meta[@name='generator']">
        <generator><xsl:value-of select="@content" /> (converted to JGloss 2)</generator>
    </xsl:template>
    
    <xsl:template match="body">
        <body>
            <div>
                <xsl:apply-templates />
            </div>
        </body>
    </xsl:template>

    <xsl:template match="p">
        <p>
            <xsl:apply-templates />
        </p>
    </xsl:template>

    <xsl:template match="anno">
        <anno>            
            <xsl:attribute name="tr"><xsl:value-of select="trans" /></xsl:attribute>
            
            <xsl:apply-templates />
        </anno>
    </xsl:template>

    <xsl:template match="word">
        <xsl:apply-templates />
    </xsl:template>
    
    <xsl:template match="rb">
        <rbase>
            <xsl:attribute name="re"><xsl:value-of select="reading" /></xsl:attribute>
            <xsl:value-of select="bt" />
        </rbase>
    </xsl:template>

    <xsl:template match="trans">
        <!-- skip content -->
    </xsl:template>

</xsl:stylesheet>