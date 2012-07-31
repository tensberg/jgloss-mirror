<?xml version="1.0" encoding="UTF-8"?>

<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">
  
  <xsl:output method="text" encoding="UTF-8" />

  <xsl:param name="document-filename" />
  <xsl:param name="document-title" />
  <xsl:param name="generation-time" />
  <xsl:param name="font-size" />
  <xsl:param name="longest-word" />
  <xsl:param name="longest-reading" />

  <xsl:template match="/">
    <xsl:apply-templates select="jgloss/body" />
  </xsl:template>
  <xsl:template match="body">
    <xsl:text>%encoding: UTF-8
%short-description: LaTeX-CJK - translations on page
%description
% Generate a latex input file which uses the CJK macro package.
% Translations are put in a footnote on the page where the
% word appears.
%end description
% This is a UTF-8 encoded Japanese LaTeX document generated
% by JGloss from </xsl:text><xsl:value-of select="$document-filename" /><xsl:text> on </xsl:text><xsl:value-of select="$generation-time" /><xsl:text>.
% The CJK macro package must be installed to process this file.
%
% template-author: Michael Koch &lt;tensberg@gmx.net&gt;
% The template file may be freely redistributed unmodified
% or modified.
% $Id$

\documentclass[</xsl:text><xsl:value-of select="$font-size" /><xsl:text>pt]{article}

\usepackage{CJKutf8}
\usepackage[overlap,CJK]{ruby}

\renewcommand{\thefootnote}{}

\begin{document}
\begin{CJK*}{UTF8}{min}
\CJKtilde

\renewcommand{\rubysize}{0.5}
\renewcommand{\rubysep}{-0.3ex}

\newlength{\ww}
\settowidth{\ww}{</xsl:text><xsl:value-of select="$longest-word" /><xsl:text>--}
\newlength{\rw}
\settowidth{\rw}{</xsl:text><xsl:value-of select="$longest-reading" /><xsl:text>--}

\newlength{\tw}
\setlength{\tw}{\textwidth}
\addtolength{\tw}{-1.0\ww}
\addtolength{\tw}{-1.0\rw}
% I don't know where the -22pt come from, but if they are not subtracted,
% latex complains about overfull \hboxes
\addtolength{\tw}{-22pt}

\pagestyle{myheadings}
\markright{</xsl:text><xsl:value-of select="$document-title" /><xsl:text>}
\newcommand{\fn}[3]{\footnotetext{\makebox[\ww][l]{#1} \makebox[\rw][l]{#2} \parbox[t]{\tw}{#3}}}

</xsl:text><xsl:apply-templates mode="document-1" /><xsl:text>
\end{CJK*}

\end{document}
</xsl:text>
  </xsl:template>

    <!-- pattern definitions for document text 1 -->

    <xsl:template match="div" mode="document-1">
      <xsl:text></xsl:text>
      <xsl:apply-templates mode="document-1" />
      <xsl:text>

\bigskip

</xsl:text>
    </xsl:template>

    <xsl:template match="p" mode="document-1">
      <xsl:apply-templates mode="document-1" />
      <xsl:text>\\
</xsl:text>
    </xsl:template>

    <xsl:template match="anno" mode="document-1">
      <xsl:apply-templates mode="document-1" />

      <xsl:variable name="base">
        <xsl:apply-templates select="." mode="base" />
      </xsl:variable>

      <xsl:variable name="basere">
        <xsl:apply-templates select="." mode="basere" />
      </xsl:variable>

      <xsl:text>\fn{</xsl:text><xsl:value-of select="$base" /><xsl:text>}{</xsl:text><xsl:value-of select="$basere" /><xsl:text>}{</xsl:text><xsl:value-of select="@tr" /><xsl:text>}</xsl:text>
    </xsl:template>

    <xsl:template match="rbase" mode="document-1">
      <xsl:text>\ruby{</xsl:text><xsl:value-of select="." /><xsl:text>}{</xsl:text><xsl:value-of select="@re" /><xsl:text>}</xsl:text>
    </xsl:template>

  <!-- Determine the base text of the word -->
  <xsl:template match="anno" mode="base">
    <xsl:choose>
      <xsl:when test="string-length(@base)>0">
        <!-- base form in attribute -->
        <xsl:value-of select="@base" />
      </xsl:when>
      <xsl:otherwise>
        <!-- determine annotation text -->
        <xsl:for-each select="rbase|text()">
          <xsl:value-of select="." />
        </xsl:for-each>
      </xsl:otherwise>
    </xsl:choose>
  </xsl:template>

  <!-- Determine the base reading of the word, which is stored in the attribute @basere, or is equal
  to the reading of the annotation text if @basere is empty -->
  <xsl:template match="anno" mode="basere">
    <xsl:choose>
      <xsl:when test="string-length(@basere)>0">
        <!-- base form in attribute -->
        <xsl:value-of select="@basere" />
      </xsl:when>
      <xsl:otherwise>
        <!-- determine annotation text reading -->
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
  </xsl:template>

</xsl:stylesheet>
