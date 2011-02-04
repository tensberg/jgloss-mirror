<?xml version='1.0'?>
<xsl:stylesheet
  xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
  xmlns:fo="http://www.w3.org/1999/XSL/Format" version='1.0'>

  <xsl:template match="jgloss">
    <fo:root>

      <fo:layout-master-set>
        <fo:simple-page-master master-name="page"
          page-height="297mm"
          page-width="210mm"
          margin-top="20mm"
          margin-bottom="10mm"
          margin-left="25mm"
          margin-right="25mm">
          <fo:region-body margin-top="0mm" margin-bottom="15mm" margin-left="0mm" margin-right="0mm"/>
        </fo:simple-page-master>
      </fo:layout-master-set>

      <fo:page-sequence master-reference="page" font-family="Gothic">

        <fo:flow flow-name="xsl-region-body">
          <xsl:apply-templates select="body" />
        </fo:flow>
      </fo:page-sequence>

    </fo:root>
  </xsl:template>

  <xsl:template match="p">
    <fo:block>
      <xsl:apply-templates />
    </fo:block>
  </xsl:template>

  <xsl:template match="anno">
    <xsl:text>

    </xsl:text>
    <fo:inline text-indent="0mm" last-line-end-indent="0mm"
      start-indent="0mm" end-indent="0mm">
      <fo:table table-layout="fixed">
        <fo:table-column column-width="proportional-column-width(1)" />
        <fo:table-body>
          <fo:table-row>
            <fo:table-cell>
              <fo:block><xsl:apply-templates select="word/rb/reading"/></fo:block>
            </fo:table-cell>
          </fo:table-row>
          <fo:table-row>
            <fo:table-cell>
              <fo:block><xsl:apply-templates select="word/rb/bt"/></fo:block>
            </fo:table-cell>
          </fo:table-row>
          <fo:table-row>
            <fo:table-cell>
              <fo:block><xsl:apply-templates select="trans"/></fo:block>
            </fo:table-cell>
          </fo:table-row>
        </fo:table-body>
      </fo:table>
    </fo:inline>
    <xsl:text>

    </xsl:text>
  </xsl:template>

</xsl:stylesheet>
