<?xml version="1.0" ?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="1.0"
                xmlns:fo="http://www.w3.org/1999/XSL/Format"
                xmlns:docbook="http://docbook.org/ns/docbook" 
                exclude-result-prefixes="docbook">

    <xsl:import href="urn:docbkx:stylesheet"/>

    <xsl:template match="docbook:foreignphrase">
        <fo:inline font-family="JapaneseFont">
            <xsl:call-template name="inline.charseq" />
        </fo:inline>
    </xsl:template>
</xsl:stylesheet>
