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
%short-description: LaTeX-CJK - transl. at end - hyperlinks
%description
% Generate a latex/pdflatex input file which uses the CJK and hyperref
% macro packages. Translations are put in a list after the main text.
% Hyperlinks between text paragraphs and
% vocabulary paragraphs are inserted, and paragraphs are bookmarked.
%
% Run latex twice on the document to get the bookmarks right.
%end description
% This is a UTF-8 encoded Japanese LaTeX document generated
% by JGloss from </xsl:text><xsl:value-of select="$document-filename" /><xsl:text> on </xsl:text><xsl:value-of select="$generation-time" /><xsl:text>.
% The CJK macro package must be installed to process this file.
%
% template-author: Heinrich Kuensting &lt;heinrich.kuensting@gmx.net&gt;
% The template file may be freely redistributed unmodified
% or modified.
% $Id$

\newif\ifpdf
\ifx\pdfoutput\undefined
   \pdffalse
\else
   \pdfoutput=1
   \pdftrue
\fi

\documentclass[</xsl:text><xsl:value-of select="$font-size" /><xsl:text>pt]{article}

\ifpdf
   \usepackage[pdftex]{graphicx}
   \usepackage[pdfpagemode={UseOutlines},        %FullScreen, None
               %pdfauthor={Your name here},
               pdfsubject={Annotated Japanese Document},
               pdfkeywords={JGloss, PDF, PDFLatex, Japan, Japanese},
               pdfstartview={FitBH},
               pdfview={FitBH},
               plainpages={false},
               pdftex,
               colorlinks,
               bookmarks,
               bookmarksopen,
               bookmarksnumbered,
               urlcolor={blue}]{hyperref}
   \pdfcompresslevel=9
\else
   \usepackage{epsf}
   \usepackage[plainpages={false}]{hyperref}
\fi

\usepackage{CJKutf8}
\usepackage[overlap,CJK]{ruby}

\renewcommand{\thefootnote}{}
\newcommand{\fn}[3]{\noindent \makebox[\ww][l]{#1} \makebox[\rw][l]{#2} \parbox[t]{\tw}{#3}}
\newcommand{\para}[1]{\noindent \textbf{Paragraph #1}}

\begin{document}
\begin{CJK*}{UTF8}{min}
\CJKtilde

\renewcommand{\rubysize}{0.5}
\renewcommand{\rubysep}{-0.3ex}

\pagestyle{myheadings}
\markright{</xsl:text><xsl:value-of select="$document-title" /><xsl:text>}
    
</xsl:text><xsl:apply-templates mode="document-1" /><xsl:text>
\newpage
\small
\markright{</xsl:text><xsl:value-of select="$document-title" /><xsl:text> --- Vocabulary List}
\newlength{\ww}
\settowidth{\ww}{</xsl:text><xsl:value-of select="$longest-word" /><xsl:text>--}
\newlength{\rw}
\settowidth{\rw}{</xsl:text><xsl:value-of select="$longest-reading" /><xsl:text>--}

\newlength{\tw}
\setlength{\tw}{\textwidth}
\addtolength{\tw}{-1.0\ww}
\addtolength{\tw}{-1.0\rw}
\addtolength{\tw}{-5pt}

\hypertarget{Vocabulary}{}
\pdfbookmark[1]{Vocabulary}{Vocabulary}

</xsl:text><xsl:apply-templates select="./div" mode="annotationlist-1" /><xsl:text>
\end{CJK*}

\end{document}
</xsl:text>
  </xsl:template>

    <!-- pattern definitions for document text 1 -->

    <xsl:template match="div" mode="document-1">
      <xsl:text>\hypertarget{text_para </xsl:text><xsl:number count="div" /><xsl:text>}{}
\pdfbookmark[1]{Text paragraph </xsl:text><xsl:number count="div" /><xsl:text>}{text_para </xsl:text><xsl:number count="div" /><xsl:text>}
\noindent\hyperlink{voc_para </xsl:text><xsl:number count="div" /><xsl:text>}{\tiny$\diamondsuit$}
</xsl:text>
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

      <xsl:text></xsl:text>
    </xsl:template>

    <xsl:template match="rbase" mode="document-1">
      <xsl:text>\ruby{</xsl:text><xsl:value-of select="." /><xsl:text>}{</xsl:text><xsl:value-of select="@re" /><xsl:text>}</xsl:text>
    </xsl:template>

    <!-- pattern definitions for annotation list 1 -->

    <xsl:template match="div" mode="annotationlist-1">
      <xsl:text>\hypertarget{voc_para </xsl:text><xsl:number count="div" /><xsl:text>}{\para{</xsl:text><xsl:number count="div" /><xsl:text>}}
\pdfbookmark[2]{Vocabulary </xsl:text><xsl:number count="div" /><xsl:text>}{voc_para </xsl:text><xsl:number count="div" /><xsl:text>}
\noindent\hyperlink{text_para </xsl:text><xsl:number count="div" /><xsl:text>}{\tiny$\diamondsuit$}

</xsl:text>
      <xsl:apply-templates select=".//anno" mode="annotationlist-1" />
      <xsl:text>
\bigskip

</xsl:text>
    </xsl:template>

    <xsl:template match="anno" mode="annotationlist-1">
      <xsl:variable name="base">
        <xsl:apply-templates select="." mode="base" />
      </xsl:variable>

      <xsl:variable name="basere">
        <xsl:apply-templates select="." mode="basere" />
      </xsl:variable>

      <xsl:text>\fn{</xsl:text><xsl:value-of select="$base" /><xsl:text>}{</xsl:text><xsl:value-of select="$basere" /><xsl:text>}{</xsl:text><xsl:value-of select="@tr" /><xsl:text>}
</xsl:text>
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
