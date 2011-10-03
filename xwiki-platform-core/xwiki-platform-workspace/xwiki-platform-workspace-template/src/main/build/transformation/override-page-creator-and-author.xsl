<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">

<xsl:output method="xml" encoding="ISO-8859-1"/>

<xsl:param name="creatorAndAuthor">xwiki:XWiki.Admin</xsl:param>

<xsl:template match="@* | node()">

   <xsl:copy>
   <xsl:apply-templates select="@* | node()" />
   </xsl:copy>
</xsl:template>

<xsl:template match="xwikidoc/creator">
  <creator><xsl:value-of select="$creatorAndAuthor"/></creator>
</xsl:template>

<xsl:template match="xwikidoc/author">
  <author><xsl:value-of select="$creatorAndAuthor"/></author>
</xsl:template>

<xsl:template match="xwikidoc/contentAuthor">
  <contentAuthor><xsl:value-of select="$creatorAndAuthor"/></contentAuthor>
</xsl:template>

</xsl:stylesheet>