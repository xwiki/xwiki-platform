<?xml version="1.0"?>
<!-- book_fo.xsl
     Copyright (c) 2002 by Dr. Herong Yang
-->
<xsl:stylesheet version="1.0"
   xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
   xmlns:fo="http://www.w3.org/1999/XSL/Format">
 <xsl:output method="xml"/>
 <xsl:preserve-space elements="html"/>
 <xsl:template match="html">
  <xsl:variable name="title"><xsl:value-of
   select="head/meta/@title"/></xsl:variable>
  <xsl:variable name="version"><xsl:value-of
   select="head/meta/@version"/></xsl:variable>
  <xsl:variable name="author"><xsl:value-of
   select="head/meta/@author"/></xsl:variable>
  <xsl:variable name="copyright"><xsl:value-of
   select="head/meta/@copyright"/></xsl:variable>
  <xsl:variable name="pagetitle"><xsl:value-of
   select="head/meta/@pagetitle"/></xsl:variable>
  <xsl:variable name="date"><xsl:value-of
   select="head/meta/@date"/></xsl:variable>
<fo:root>
 <fo:layout-master-set>
  <fo:simple-page-master master-name="page"
   margin-left="1.2in" margin-right="0.8in"
   margin-top="0.8in" margin-bottom="0.8in">
   <fo:region-before region-name="header" extent="0.25in"
    background-color="#ffffff"/>
   <fo:region-after region-name="footer" extent="0.4in"
    background-color="#ffffff"/>
   <fo:region-body region-name="body"
    margin-top="0.35in" margin-bottom="0.5in"/>
  </fo:simple-page-master>
 </fo:layout-master-set>
 <fo:page-sequence master-reference="page">
  <fo:static-content flow-name="header"
   font-family="sans-serif" font-size="12pt" font-style="normal">
   <fo:block border-bottom-width="1px" border-bottom-style="solid">
    <fo:list-block>
     <fo:list-item>
      <fo:list-item-label>
       <fo:block font-weight="normal" text-align="start">
        <xsl:value-of select="$pagetitle"/>
       </fo:block>
      </fo:list-item-label>
      <fo:list-item-body>
       <fo:block font-weight="normal" text-align="end">
        <fo:page-number/>
       </fo:block>
      </fo:list-item-body>
     </fo:list-item>
    </fo:list-block>
   </fo:block>
  </fo:static-content>
  <fo:static-content flow-name="footer"
   font-family="sans-serif" font-size="10pt" font-style="normal">
   <fo:block border-top-width="1px" border-top-style="solid"
    text-align="start">
    <fo:list-block>
     <fo:list-item>
      <fo:list-item-label>
       <fo:block font-weight="bold" text-align="start">
        <xsl:value-of select="$title"/>
       </fo:block>
      </fo:list-item-label>
      <fo:list-item-body>
       <fo:block font-weight="normal" text-align="end">
        by <xsl:value-of select="$author"/>
       </fo:block>
      </fo:list-item-body>
     </fo:list-item>
     <fo:list-item>
      <fo:list-item-label>
       <fo:block font-weight="normal" text-align="start">
        <xsl:value-of select="$version"/>,
        updated on <xsl:value-of select="$date"/>
       </fo:block>
      </fo:list-item-label>
      <fo:list-item-body>
       <fo:block font-weight="normal" text-align="end">
       </fo:block>
      </fo:list-item-body>
     </fo:list-item>
    </fo:list-block>
   </fo:block>
  </fo:static-content>
  <fo:flow flow-name="body"
   font-family="serif" font-size="12pt" font-style="normal">
  <xsl:apply-templates select="body"/>
  </fo:flow>
 </fo:page-sequence>
</fo:root>
 </xsl:template>
 <xsl:template match="body">
  <xsl:apply-templates select="//p/ul|//p|//ul|//pre"/>
 </xsl:template>
 <xsl:template match="p[@class='chapter_title']">
   <fo:block id="{@id}" font-size="150%" font-weight="bold"
    space-before="12pt" space-after="4pt">
  <xsl:apply-templates select="text()"/>
   </fo:block>
 </xsl:template>
 <xsl:template match="p[@class='section_title']">
   <fo:block font-size="125%" font-weight="bold"
    space-before="9pt" space-after="4pt">
  <xsl:apply-templates select="text()"/>
   </fo:block>
 </xsl:template>
 <xsl:template match="a">
   <fo:basic-link external-destination="url('{@href}')" color="#0000ff">
  <xsl:apply-templates select="text()"/>
   </fo:basic-link>
 </xsl:template>
 <xsl:template match="b">
   <fo:wrapper font-weight="bold">
  <xsl:apply-templates select="text()"/>
   </fo:wrapper>
 </xsl:template>
 <xsl:template match="i">
   <fo:wrapper font-style="italic">
  <xsl:apply-templates select="text()"/>
   </fo:wrapper>
 </xsl:template>
 <xsl:template match="p[@class='toc_item']">
   <fo:block space-before="8pt" space-after="4pt"
    text-align-last="justify">
  <xsl:apply-templates select="a|text()"/>
   </fo:block>
 </xsl:template>
 <xsl:template match="p">
   <fo:block space-before="8pt" space-after="4pt">
  <xsl:apply-templates select="a|b|i|text()"/>
   </fo:block>
 </xsl:template>
 <xsl:template match="text()">
  <xsl:value-of select="."/>
 </xsl:template>
 <xsl:template match="pre[@class='block_source']">
    <fo:block font-family="monospace" font-size="10pt"
     background-color="#cfcfcf"
     white-space-collapse="false" wrap-option="no-wrap">
  <xsl:value-of select="./text()"/>
    </fo:block>
 </xsl:template>
 <xsl:template match="pre[@class='block_bold']">
    <fo:block font-family="monospace" font-size="10pt"
     background-color="#cfcfcf" font-weight="bold"
     white-space-collapse="false" wrap-option="no-wrap">
  <xsl:value-of select="./text()"/>
    </fo:block>
 </xsl:template>
 <xsl:template match="pre[@class='block_syntax']">
    <fo:block font-family="monospace" font-size="10pt"
     background-color="#cfcfcf" font-style="italic"
     white-space-collapse="false" wrap-option="no-wrap">
  <xsl:value-of select="./text()"/>
    </fo:block>
 </xsl:template>
 <xsl:template match="//p/ul">
    <fo:list-block space-before="8pt" space-after="4pt">
  <xsl:apply-templates select="li"/>
    </fo:list-block>
 </xsl:template>
 <xsl:template match="ul">
    <fo:list-block space-before="1pt" space-after="1pt">
  <xsl:apply-templates select="li"/>
    </fo:list-block>
 </xsl:template>
 <xsl:template match="li">
    <fo:list-item>
     <fo:list-item-label start-indent="0.1in">
     <fo:block>•</fo:block>
     </fo:list-item-label>
     <fo:list-item-body start-indent="0.25in">
     <fo:block space-after="3pt">
  <xsl:apply-templates select="a|b|i|ul|text()"/>
     </fo:block>
     </fo:list-item-body>
    </fo:list-item>
 </xsl:template>
</xsl:stylesheet>