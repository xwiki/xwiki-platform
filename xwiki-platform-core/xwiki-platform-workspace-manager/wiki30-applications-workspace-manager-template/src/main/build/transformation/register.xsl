<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">

<xsl:output method="xml" encoding="ISO-8859-1"/>

<xsl:param name="translations"></xsl:param>
<xsl:param name="skin">XWiki.DefaultSkin</xsl:param>

<xsl:template match="@* | node()">

   <xsl:copy>
   <xsl:apply-templates select="@* | node()" />
   </xsl:copy>
</xsl:template>

<xsl:template match="property/documentBundles">
  <documentBundles><xsl:value-of select="$translations"/></documentBundles>
</xsl:template>

<xsl:template match="property/skin">
  <skin><xsl:value-of select="$skin"/></skin>
</xsl:template>

</xsl:stylesheet>