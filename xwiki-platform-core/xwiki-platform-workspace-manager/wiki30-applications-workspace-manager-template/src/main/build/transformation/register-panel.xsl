<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">

<xsl:output method="xml" encoding="ISO-8859-1"/>

<xsl:param name="panel"></xsl:param>

<xsl:template match="@* | node()">

   <xsl:copy>
   <xsl:apply-templates select="@* | node()" />
   </xsl:copy>
</xsl:template>

<xsl:template match="property/rightPanels">
  <rightPanels><xsl:value-of select="$panel"/>,<xsl:value-of select="."/></rightPanels>
</xsl:template>

</xsl:stylesheet>