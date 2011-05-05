<?xml version="1.0" encoding="iso-8859-1"?>
<!-- Prepares an XHTML document to be converted to an office format -->
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="1.0" xmlns="http://www.w3.org/1999/xhtml"
  xmlns:xhtml="http://www.w3.org/1999/xhtml" exclude-result-prefixes="xhtml">
  <!-- OpenOffice does not support XHTML so we remove the XML marker and let it parse the output as if it was HTML content -->
  <xsl:output method="html" encoding="UTF-8" omit-xml-declaration="yes" doctype-public="-//W3C//DTD XHTML 1.0 Strict//EN"
    doctype-system="http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd" indent="no" />

  <xsl:template name="addPageBreakBefore">
    <p style="page-break-before: always" />
    <xsl:copy>
      <xsl:apply-templates select="@*|node()" />
    </xsl:copy>
  </xsl:template>

  <!-- Copy all nodes by default -->
  <xsl:template match="@*|node()">
    <xsl:copy>
      <xsl:apply-templates select="@*|node()" />
    </xsl:copy>
  </xsl:template>

  <!-- Remove the id attribute from the body element because the office conversion fails when it is present -->
  <xsl:template match="//xhtml:body/@id" />

  <!-- Table of contents -->
  <xsl:template match="*[@class = 'pdftoc']">
    <xsl:variable name="withToc">
      <xsl:value-of select="//xhtml:body/@pdftoc" />
    </xsl:variable>
    <xsl:if test="$withToc = '1'">
      <!-- Add page break before the table of contents -->
      <xsl:call-template name="addPageBreakBefore" />
    </xsl:if>
  </xsl:template>
  <xsl:template match="*[@class = 'pdftoc']/*[last()]">
    <xsl:copy>
      <xsl:apply-templates select="@*|node()" />
    </xsl:copy>
    <!-- Write the table of contents after the last child -->
    <xsl:for-each select="//*[@id = 'xwikimaincontainer']/*[local-name() = 'h1' or local-name() = 'h2' or local-name() = 'h3']">
      <!-- Transforming the flat headings structure into an hierarchical structure based on nested ordered lists is hard 
        to do with XSLT 1.0 (a complete solution would have to generate empty list items when heading levels are missing: when there 
        is a level 3 heading after a level 1 heading). A solution using XSLT 2.0 is described here http://www.xmlplease.com/tocxhtml 
        . Let's use the simplest solution for now: indenting -->
      <xsl:element name="div">
        <xsl:attribute name="style">margin-left: <xsl:value-of select="(round(substring(local-name(), 2)) - 1) * 12" />pt</xsl:attribute>
        <xsl:element name="a">
          <xsl:attribute name="href">#<xsl:value-of select="@id" /></xsl:attribute>
          <xsl:value-of select="." />
        </xsl:element>
      </xsl:element>
    </xsl:for-each>
  </xsl:template>

  <!-- Add page break before the content -->
  <xsl:template match="*[@class = 'pdftoc' or @id = 'xwikimaincontainer']">
    <xsl:call-template name="addPageBreakBefore" />
  </xsl:template>
</xsl:stylesheet>
