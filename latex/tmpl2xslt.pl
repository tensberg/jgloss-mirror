#!/usr/bin/perl -w

# Convert a LaTeX export template compatible with JGloss 1.0 to
# a XSLT style sheet usable with the JGloss 2 export mechanism.

# Author: Michael Koch
# This file is in the public domain

# $Id$

use English;

my $header = <<'END_HEADER';
<?xml version="1.0" encoding="UTF-8"?>

<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">
  
  <xsl:output method="text" encoding="EUC-JP" />

  <xsl:param name="document-filename" />
  <xsl:param name="document-title" />
  <xsl:param name="generation-time" />
  <xsl:param name="font-size" />
  <xsl:param name="longest-word" />
  <xsl:param name="longest-reading" />

  <xsl:template match="/">
    <xsl:apply-templates select="jgloss/body" />
  </xsl:template>
END_HEADER

my $body_start = <<'END_BODY';
  <xsl:template match="body">
    <xsl:text>
END_BODY
chop $body_start;

my $body_end = <<'END_BODY';
</xsl:text>
  </xsl:template>

END_BODY

my $footer = <<'END_FOOTER';
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
END_FOOTER

my %replacements = ();
$replacements{'document-title'} = <<'END';
</xsl:text><xsl:value-of select="/jgloss/head/title" /><xsl:text>
END
$replacements{'document-filename'} = <<'END';
</xsl:text><xsl:value-of select="$document-filename" /><xsl:text>
END
$replacements{'document-title'} = <<'END';
</xsl:text><xsl:value-of select="$document-title" /><xsl:text>
END
$replacements{'generation-time'} = <<'END';
</xsl:text><xsl:value-of select="$generation-time" /><xsl:text>
END
$replacements{'font-size'} = <<'END';
</xsl:text><xsl:value-of select="$font-size" /><xsl:text>
END
$replacements{'longest-word'} = <<'END';
</xsl:text><xsl:value-of select="$longest-word" /><xsl:text>
END
$replacements{'longest-reading'} = <<'END';
</xsl:text><xsl:value-of select="$longest-reading" /><xsl:text>
END
$replacements{'word'} = <<'END';
</xsl:text><xsl:value-of select="." /><xsl:text>
END
$replacements{'reading'} = <<'END';
</xsl:text><xsl:value-of select="@re" /><xsl:text>
END
$replacements{'dictionary-word'} = <<'END';
</xsl:text><xsl:value-of select="$base" /><xsl:text>
END
$replacements{'dictionary-reading'} = <<'END';
</xsl:text><xsl:value-of select="$basere" /><xsl:text>
END
$replacements{'translation'} = <<'END';
</xsl:text><xsl:value-of select="@tr" /><xsl:text>
END
$replacements{'paragraph-number'} = <<'END';
</xsl:text><xsl:number count="div" /><xsl:text>
END

foreach (keys(%replacements)) {
    chomp $replacements{$_};
}

sub main {
    print $header;
    
    print $body_start;

    my $documentCounter = 0;
    my $annotationListCounter = 0;
    my @matchers = ();
    
    while (<>) {
        $line = $_;
        $line = escapeSpecialChars($line);
        $line = substituteVariables($line);

        if ($line =~ m/^\%document-text/) {
            push @matchers, handleDocumentText(++$documentCounter);
        }
        elsif ($line =~ m/^\%annotation-list/) {
            push @matchers, handleAnnotationList(++$annotationListCounter);
        }
        else {
            handleTemplateLine($line);
        }
    }

    print $body_end;

    print @matchers;

    print $footer;
}


sub substituteVariables {
    my $line = shift(@_);
    
    if ($line =~ m/\%(.+?)(?=\%)/) {
        if (exists($replacements{$1})) {
            $line = $PREMATCH . $replacements{$1} . substituteVariables(substr($POSTMATCH, 1));
        }
        else {
            $line = "$PREMATCH" . "\%$1" . substituteVariables($POSTMATCH);
        }
    }
    
    return $line;
}

sub handleDocumentText {
    my $documentCount = shift(@_);
    my $mode = "document-$documentCount";

    my %patterns =
        ( 'reading'         => '',
          'translation'     => '',
          'line-break'      => "\\\\\n",
          'paragraph-start' => '',
          'paragraph-end'   => "\n\n\n"
          );
    
    readPatternDefinitions(\%patterns);

    print "</xsl:text><xsl:apply-templates mode=\"$mode\" /><xsl:text>";

    # output XSLT definition
    return <<"END_XSLT";
    <!-- pattern definitions for document text $documentCount -->

    <xsl:template match="div" mode="$mode">
      <xsl:text>$patterns{'paragraph-start'}</xsl:text>
      <xsl:apply-templates mode="$mode" />
      <xsl:text>$patterns{'paragraph-end'}</xsl:text>
    </xsl:template>

    <xsl:template match="p" mode="$mode">
      <xsl:apply-templates mode="$mode" />
      <xsl:text>$patterns{'line-break'}</xsl:text>
    </xsl:template>

    <xsl:template match="anno" mode="$mode">
      <xsl:apply-templates mode="$mode" />

      <xsl:variable name="base">
        <xsl:apply-templates select="." mode="base" />
      </xsl:variable>

      <xsl:variable name="basere">
        <xsl:apply-templates select="." mode="basere" />
      </xsl:variable>

      <xsl:text>$patterns{'translation'}</xsl:text>
    </xsl:template>

    <xsl:template match="rbase" mode="$mode">
      <xsl:text>$patterns{'reading'}</xsl:text>
    </xsl:template>

END_XSLT
}

sub handleAnnotationList {
    my $annotationListCount = shift(@_);
    my $mode = "annotationlist-$annotationListCount";

    my %patterns =
        ( 'reading'         => '',
          'translation'     => '',
          'line-break'      => "\\\\\n",
          'paragraph-start' => '',
          'paragraph-end'   => "\n\n\n"
          );
    
    readPatternDefinitions(\%patterns);

    print "</xsl:text><xsl:apply-templates select=\"./div\" mode=\"$mode\" /><xsl:text>";

    # output XSLT definition
    return <<"END_XSLT";
    <!-- pattern definitions for annotation list $annotationListCount -->

    <xsl:template match="div" mode="$mode">
      <xsl:text>$patterns{'paragraph-start'}</xsl:text>
      <xsl:apply-templates select=".//anno" mode="$mode" />
      <xsl:text>$patterns{'paragraph-end'}</xsl:text>
    </xsl:template>

    <xsl:template match="anno" mode="$mode">
      <xsl:variable name="base">
        <xsl:apply-templates select="." mode="base" />
      </xsl:variable>

      <xsl:variable name="basere">
        <xsl:apply-templates select="." mode="basere" />
      </xsl:variable>

      <xsl:text>$patterns{'translation'}</xsl:text>
    </xsl:template>
END_XSLT
}

sub escapeSpecialChars {
    my $line = shift(@_);

    $line =~ s/&/&amp;/g;
    $line =~ s/</&lt;/g;
    $line =~ s/>/&gt;/g;

    return $line;
}

sub handleTemplateLine {
    print @_;
}

sub unescapePattern {
    my $pattern = shift(@_);

    $pattern =~ s/(\G|[^\\](\\\\)*)\\n/$1\n/g;
    $pattern =~ s/(\G|[^\\](\\\\)*)\\t/$1\t/g;
    $pattern =~ s/(\G|[^\\](\\\\)*)\\r/$1\r/g;
    $pattern =~ s/\\\\/\\/g;

    return $pattern;
}

sub readPatternDefinitions {
    my $patterns = shift(@_); # reference to hash of pattern definitions

    while (<>) {
        if (m/^\%end/i) {
            last;
        }

        if (m/^\%(.+?):\s*(.*)/) {
            setPattern($patterns, $1, $2);
        }
        else {
            warn "Unknown line: $_\n";
        }
    }
}

sub setPattern {
    my $patterns = shift(@_); # reference to hash of pattern definitions
    my $patternName = shift(@_);
    my $pattern = shift(@_);

    if (!exists($patterns->{$patternName})) {
        warn "Unknown pattern definition: $patternName\n";
    }
    else {
        $pattern = escapeSpecialChars($pattern);
        $pattern = substituteVariables($pattern);
        $pattern = unescapePattern($pattern);
        $patterns->{$patternName} = $pattern;
    }
}

main();
