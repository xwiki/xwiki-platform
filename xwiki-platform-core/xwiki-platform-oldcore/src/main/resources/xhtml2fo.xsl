<?xml version="1.0" encoding="UTF-8"?>
<!--

Copyright Antenna House, Inc. (http://www.antennahouse.com) 2001, 2002.

Since this stylesheet is originally developed by Antenna House to be used with XSL Formatter, it may not be compatible with another XSL-FO processors.

Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"), to
deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, and/or sell copies
of the Software, and to permit persons to whom the Software is furnished to do so, provided that the above copyright notice(s) and this permission
notice appear in all copies of the Software and that both the above copyright notice(s) and this permission notice appear in supporting documentation.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT OF THIRD PARTY RIGHTS. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR HOLDERS INCLUDED IN THIS NOTICE
BE LIABLE FOR ANY CLAIM, OR ANY SPECIAL INDIRECT OR CONSEQUENTIAL DAMAGES, OR ANY DAMAGES WHATSOEVER RESULTING FROM LOSS OF USE, DATA OR PROFITS,
WHETHER IN AN ACTION OF CONTRACT, NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF OR IN CONNECTION WITH THE USE OR PERFORMANCE OF THIS SOFTWARE.

-->
<xsl:stylesheet version="1.0"
                xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                xmlns:svg="http://www.w3.org/2000/svg"
                xmlns:fo="http://www.w3.org/1999/XSL/Format"
                xmlns:html="http://www.w3.org/1999/xhtml">

    <xsl:output method="xml"
                version="1.0"
                encoding="UTF-8"
                indent="no"/>

    <!--======================================================================
    Parameters
    =======================================================================-->

    <!-- page size -->
    <xsl:param name="page-width">auto</xsl:param>
    <xsl:param name="page-height">auto</xsl:param>
    <xsl:param name="page-margin-top">1in</xsl:param>
    <xsl:param name="page-margin-bottom">1in</xsl:param>
    <xsl:param name="page-margin-left">1in</xsl:param>
    <xsl:param name="page-margin-right">1in</xsl:param>

    <!-- page header and footer -->
    <xsl:param name="page-header-margin">0.5in</xsl:param>
    <xsl:param name="page-footer-margin">0.5in</xsl:param>
    <xsl:param name="title-print-in-header">true</xsl:param>
    <xsl:param name="page-number-print-in-footer">true</xsl:param>

    <!-- multi column -->
    <xsl:param name="column-count">1</xsl:param>
    <xsl:param name="column-gap">12pt</xsl:param>

    <!-- writing-mode: lr-tb | rl-tb | tb-rl -->
    <xsl:param name="writing-mode">lr-tb</xsl:param>

    <!-- text-align: justify | start -->
    <xsl:param name="text-align">start</xsl:param>

    <!-- hyphenate: true | false -->
    <xsl:param name="hyphenate">false</xsl:param>

    <!-- language -->
    <xsl:param name="language" select="//html:head/html:meta[@name='language']/@content"></xsl:param>


    <!--======================================================================
    Attribute Sets
    =======================================================================-->

    <!--=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-
    Root
    =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-->

    <xsl:attribute-set name="root">
        <xsl:attribute name="writing-mode"><xsl:value-of select="$writing-mode"/></xsl:attribute>
        <xsl:attribute name="hyphenate"><xsl:value-of select="$hyphenate"/></xsl:attribute>
        <xsl:attribute name="text-align"><xsl:value-of select="$text-align"/></xsl:attribute>
        <xsl:attribute name="font-family">
          <xsl:choose>
            <xsl:when test="starts-with($language, 'ja')">FreeSans, IPAMincho, AR PL UMing CN, sans-serif</xsl:when>
            <xsl:when test="starts-with($language, 'ko') or starts-with($language, 'kr')">FreeSans, Baekmuk Gulim, AR PL UMing CN, sans-serif</xsl:when>
            <xsl:otherwise>FreeSans, AR PL UMing CN, sans-serif</xsl:otherwise>
          </xsl:choose>
        </xsl:attribute>
        <!-- specified on fo:root to change the properties' initial values -->
    </xsl:attribute-set>

    <xsl:attribute-set name="page">
        <xsl:attribute name="page-width"><xsl:value-of select="$page-width"/></xsl:attribute>
        <xsl:attribute name="page-height"><xsl:value-of select="$page-height"/></xsl:attribute>
        <!-- specified on fo:simple-page-master -->
    </xsl:attribute-set>

    <xsl:attribute-set name="body">
        <!-- specified on fo:flow's only child fo:block -->
        <xsl:attribute name="font-size">0.75em</xsl:attribute>
    </xsl:attribute-set>

    <xsl:attribute-set name="page-header">
        <!-- specified on (page-header)fo:static-content's only child fo:block -->
        <xsl:attribute name="font-size">small</xsl:attribute>
        <xsl:attribute name="text-align">center</xsl:attribute>
    </xsl:attribute-set>

    <xsl:attribute-set name="page-footer">
        <!-- specified on (page-footer)fo:static-content's only child fo:block -->
        <xsl:attribute name="font-size">small</xsl:attribute>
        <xsl:attribute name="text-align">center</xsl:attribute>
    </xsl:attribute-set>

    <!--=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-
    Block-level
    =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-->

    <xsl:attribute-set name="h1">
        <xsl:attribute name="font-size">2em</xsl:attribute>
        <xsl:attribute name="font-weight">bold</xsl:attribute>
        <xsl:attribute name="font-style">normal</xsl:attribute>
        <xsl:attribute name="space-before">0.8em</xsl:attribute>
        <xsl:attribute name="space-after">0.2em</xsl:attribute>
        <xsl:attribute name="keep-with-next.within-column">1000</xsl:attribute>
        <xsl:attribute name="keep-together.within-column">1000</xsl:attribute>
        <xsl:attribute name="start-indent">0mm</xsl:attribute>
    </xsl:attribute-set>

    <xsl:attribute-set name="h2">
        <xsl:attribute name="font-size">1.5em</xsl:attribute>
        <xsl:attribute name="font-weight">bold</xsl:attribute>
        <xsl:attribute name="font-style">normal</xsl:attribute>
        <xsl:attribute name="space-before">0.8em</xsl:attribute>
        <xsl:attribute name="space-after">0.2em</xsl:attribute>
        <xsl:attribute name="keep-with-next.within-column">900</xsl:attribute>
        <xsl:attribute name="keep-together.within-column">1000</xsl:attribute>
        <xsl:attribute name="start-indent">0mm</xsl:attribute>
    </xsl:attribute-set>

    <xsl:attribute-set name="h3">
        <xsl:attribute name="font-size">1.17em</xsl:attribute>
        <xsl:attribute name="font-weight">bold</xsl:attribute>
        <xsl:attribute name="font-style">normal</xsl:attribute>
        <xsl:attribute name="space-before">0.8em</xsl:attribute>
        <xsl:attribute name="space-after">0.2em</xsl:attribute>
        <xsl:attribute name="keep-with-next.within-column">800</xsl:attribute>
        <xsl:attribute name="keep-together.within-column">1000</xsl:attribute>
        <xsl:attribute name="start-indent">0mm</xsl:attribute>
    </xsl:attribute-set>

    <xsl:attribute-set name="h4">
        <xsl:attribute name="font-size">1em</xsl:attribute>
        <xsl:attribute name="font-weight">bold</xsl:attribute>
        <xsl:attribute name="font-style">normal</xsl:attribute>
        <xsl:attribute name="space-before">0.8em</xsl:attribute>
        <xsl:attribute name="space-after">0.2em</xsl:attribute>
        <xsl:attribute name="keep-with-next.within-column">800</xsl:attribute>
        <xsl:attribute name="keep-together.within-column">1000</xsl:attribute>
        <xsl:attribute name="start-indent">0mm</xsl:attribute>
    </xsl:attribute-set>

    <xsl:attribute-set name="h5">
        <xsl:attribute name="font-size">0.85em</xsl:attribute>
        <xsl:attribute name="font-weight">bold</xsl:attribute>
        <xsl:attribute name="font-style">normal</xsl:attribute>
        <xsl:attribute name="space-before">0.8em</xsl:attribute>
        <xsl:attribute name="space-after">0.2em</xsl:attribute>
        <xsl:attribute name="keep-with-next.within-column">800</xsl:attribute>
        <xsl:attribute name="keep-together.within-column">1000</xsl:attribute>
        <xsl:attribute name="start-indent">0mm</xsl:attribute>
    </xsl:attribute-set>

    <xsl:attribute-set name="h6">
        <xsl:attribute name="font-size">0.75em</xsl:attribute>
        <xsl:attribute name="font-weight">bold</xsl:attribute>
        <xsl:attribute name="font-style">normal</xsl:attribute>
        <xsl:attribute name="space-before">0.8em</xsl:attribute>
        <xsl:attribute name="space-after">0.2em</xsl:attribute>
        <xsl:attribute name="keep-with-next.within-column">800</xsl:attribute>
        <xsl:attribute name="keep-together.within-column">1000</xsl:attribute>
        <xsl:attribute name="start-indent">0mm</xsl:attribute>
    </xsl:attribute-set>

    <xsl:attribute-set name="p">
        <xsl:attribute name="space-before">0.8em</xsl:attribute>
        <xsl:attribute name="space-after">0.8em</xsl:attribute>
        <xsl:attribute name="text-indent">1em</xsl:attribute>
    </xsl:attribute-set>

    <xsl:attribute-set name="p-initial" use-attribute-sets="p">
        <!-- initial paragraph, preceded by h1..6 or div -->
        <xsl:attribute name="text-indent">0em</xsl:attribute>
    </xsl:attribute-set>

    <xsl:attribute-set name="p-initial-first" use-attribute-sets="p-initial">
        <!-- initial paragraph, first child of div, body or td -->
    </xsl:attribute-set>

    <xsl:attribute-set name="blockquote">
        <xsl:attribute name="start-indent">inherited-property-value(start-indent) + 24pt</xsl:attribute>
        <xsl:attribute name="end-indent">inherited-property-value(end-indent) + 24pt</xsl:attribute>
        <xsl:attribute name="space-before">1em</xsl:attribute>
        <xsl:attribute name="space-after">1em</xsl:attribute>
    </xsl:attribute-set>

    <xsl:attribute-set name="pre">
        <xsl:attribute name="font-size">0.83em</xsl:attribute>
        <xsl:attribute name="font-family">
          <xsl:choose>
            <xsl:when test="starts-with($language, 'ja')">FreeMono, IPAMincho, monospace</xsl:when>
            <xsl:when test="starts-with($language, 'ko') or starts-with($language, 'kr')">FreeMono, Baekmuk Gulim, monospace</xsl:when>
            <xsl:otherwise>FreeMono, AR PL UMing CN, monospace</xsl:otherwise>
          </xsl:choose>
        </xsl:attribute>
        <xsl:attribute name="white-space">pre</xsl:attribute>
        <xsl:attribute name="space-before">1em</xsl:attribute>
        <xsl:attribute name="space-after">1em</xsl:attribute>
    </xsl:attribute-set>

    <xsl:attribute-set name="address">
      <xsl:attribute name="font-style">
        <xsl:if test="not(starts-with($language, 'ja') or starts-with($language, 'ko') or starts-with($language, 'kr') or starts-with($language, 'zh'))">italic</xsl:if>
      </xsl:attribute>
    </xsl:attribute-set>

    <xsl:attribute-set name="hr">
        <xsl:attribute name="border">1px inset</xsl:attribute>
        <xsl:attribute name="space-before">0.67em</xsl:attribute>
        <xsl:attribute name="space-after">0.67em</xsl:attribute>
    </xsl:attribute-set>

    <!--=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-
    List
    =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-->

    <xsl:attribute-set name="ul">
        <xsl:attribute name="space-before">0.5em</xsl:attribute>
        <xsl:attribute name="space-after">0.5em</xsl:attribute>
    </xsl:attribute-set>

    <xsl:attribute-set name="ul-nested">
        <xsl:attribute name="space-before">0pt</xsl:attribute>
        <xsl:attribute name="space-after">0pt</xsl:attribute>
    </xsl:attribute-set>

    <xsl:attribute-set name="ol">
        <xsl:attribute name="space-before">0.5em</xsl:attribute>
        <xsl:attribute name="space-after">0.5em</xsl:attribute>
    </xsl:attribute-set>

    <xsl:attribute-set name="ol-nested">
        <xsl:attribute name="space-before">0pt</xsl:attribute>
        <xsl:attribute name="space-after">0pt</xsl:attribute>
    </xsl:attribute-set>

    <xsl:attribute-set name="ul-li">
        <!-- for (unordered)fo:list-item -->
        <xsl:attribute name="relative-align">baseline</xsl:attribute>
    </xsl:attribute-set>

    <xsl:attribute-set name="ol-li">
        <!-- for (ordered)fo:list-item -->
        <xsl:attribute name="relative-align">baseline</xsl:attribute>
    </xsl:attribute-set>

    <xsl:attribute-set name="dl">
        <xsl:attribute name="space-before">1em</xsl:attribute>
        <xsl:attribute name="space-after">1em</xsl:attribute>
    </xsl:attribute-set>

    <xsl:attribute-set name="dt">
        <xsl:attribute name="keep-with-next.within-column">1000</xsl:attribute>
        <xsl:attribute name="keep-together.within-column">1000</xsl:attribute>
    </xsl:attribute-set>

    <xsl:attribute-set name="dd">
        <xsl:attribute name="start-indent">inherited-property-value(start-indent) + 24pt</xsl:attribute>
    </xsl:attribute-set>

    <!--=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-
    Table
    =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-->

    <xsl:attribute-set name="inside-table">
        <!-- prevent unwanted inheritance -->
        <xsl:attribute name="start-indent">0pt</xsl:attribute>
        <xsl:attribute name="end-indent">0pt</xsl:attribute>
        <xsl:attribute name="text-indent">0pt</xsl:attribute>
        <xsl:attribute name="last-line-end-indent">0pt</xsl:attribute>
        <xsl:attribute name="text-align">start</xsl:attribute>
        <xsl:attribute name="text-align-last">relative</xsl:attribute>
    </xsl:attribute-set>

    <xsl:attribute-set name="table-and-caption" >
        <!-- horizontal alignment of table itself
        <xsl:attribute name="text-align">center</xsl:attribute>
    -->
        <!-- vertical alignment in table-cell -->
        <xsl:attribute name="display-align">center</xsl:attribute>
    </xsl:attribute-set>

    <xsl:attribute-set name="table">
        <xsl:attribute name="border-collapse">separate</xsl:attribute>
        <xsl:attribute name="border-spacing">2px</xsl:attribute>
        <xsl:attribute name="border">0px</xsl:attribute>
        <!--
        <xsl:attribute name="border-style">outset</xsl:attribute>
    -->
    </xsl:attribute-set>

    <xsl:attribute-set name="table-caption" use-attribute-sets="inside-table">
        <xsl:attribute name="text-align">center</xsl:attribute>
    </xsl:attribute-set>

    <xsl:attribute-set name="table-column">
    </xsl:attribute-set>

    <xsl:attribute-set name="thead" use-attribute-sets="inside-table">
    </xsl:attribute-set>

    <xsl:attribute-set name="tfoot" use-attribute-sets="inside-table">
    </xsl:attribute-set>

    <xsl:attribute-set name="tbody" use-attribute-sets="inside-table">
    </xsl:attribute-set>

    <xsl:attribute-set name="tr">
    </xsl:attribute-set>

    <xsl:attribute-set name="th">
        <xsl:attribute name="font-size">90%</xsl:attribute>
        <xsl:attribute name="font-weight">bold</xsl:attribute>
        <xsl:attribute name="text-align">left</xsl:attribute>
        <xsl:attribute name="border">0px solid black</xsl:attribute>
        <xsl:attribute name="background-color">lightgrey</xsl:attribute>
        <!--
        <xsl:attribute name="border-style">inset</xsl:attribute>
    -->
        <xsl:attribute name="padding">1px</xsl:attribute>
    </xsl:attribute-set>

    <xsl:attribute-set name="td">
        <xsl:attribute name="font-size">90%</xsl:attribute>
        <xsl:attribute name="border">0px solid b</xsl:attribute>
        <!--
        <xsl:attribute name="border-style">inset</xsl:attribute>
    -->
        <xsl:attribute name="padding">1px</xsl:attribute>
    </xsl:attribute-set>

    <!--=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-
    Inline-level
    =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-->

    <xsl:attribute-set name="b">
        <xsl:attribute name="font-weight">bold</xsl:attribute>
    </xsl:attribute-set>
    <xsl:attribute-set name="strong">
        <xsl:attribute name="font-weight">bold</xsl:attribute>
    </xsl:attribute-set>

    <xsl:attribute-set name="strong-em">
      <xsl:attribute name="font-weight">bold</xsl:attribute>
      <xsl:attribute name="font-style">
        <xsl:if test="not(starts-with($language, 'ja') or starts-with($language, 'ko') or starts-with($language, 'kr') or starts-with($language, 'zh'))">italic</xsl:if>
      </xsl:attribute>
    </xsl:attribute-set>

    <xsl:attribute-set name="i">
      <xsl:attribute name="font-style">
        <xsl:if test="not(starts-with($language, 'ja') or starts-with($language, 'ko') or starts-with($language, 'kr') or starts-with($language, 'zh'))">italic</xsl:if>
      </xsl:attribute>
    </xsl:attribute-set>
    <xsl:attribute-set name="cite">
      <xsl:attribute name="font-style">
        <xsl:if test="not(starts-with($language, 'ja') or starts-with($language, 'ko') or starts-with($language, 'kr') or starts-with($language, 'zh'))">italic</xsl:if>
      </xsl:attribute>
    </xsl:attribute-set>
    <xsl:attribute-set name="em">
      <xsl:attribute name="font-style">
        <xsl:if test="not(starts-with($language, 'ja') or starts-with($language, 'ko') or starts-with($language, 'kr') or starts-with($language, 'zh'))">italic</xsl:if>
      </xsl:attribute>
    </xsl:attribute-set>
    <xsl:attribute-set name="var">
      <xsl:attribute name="font-style">
        <xsl:if test="not(starts-with($language, 'ja') or starts-with($language, 'ko') or starts-with($language, 'kr') or starts-with($language, 'zh'))">italic</xsl:if>
      </xsl:attribute>
    </xsl:attribute-set>
    <xsl:attribute-set name="dfn">
      <xsl:attribute name="font-style">
        <xsl:if test="not(starts-with($language, 'ja') or starts-with($language, 'ko') or starts-with($language, 'kr') or starts-with($language, 'zh'))">italic</xsl:if>
      </xsl:attribute>
    </xsl:attribute-set>

    <xsl:attribute-set name="tt">
      <xsl:attribute name="font-family">
        <xsl:choose>
          <xsl:when test="starts-with($language, 'ja')">FreeMono, IPAMincho, monospace</xsl:when>
          <xsl:when test="starts-with($language, 'ko') or starts-with($language, 'kr')">FreeMono, Baekmuk Gulim, monospace</xsl:when>
          <xsl:otherwise>FreeMono, AR PL UMing CN, monospace</xsl:otherwise>
        </xsl:choose>
      </xsl:attribute>
    </xsl:attribute-set>
    <xsl:attribute-set name="code">
      <xsl:attribute name="font-family">
        <xsl:choose>
          <xsl:when test="starts-with($language, 'ja')">FreeMono, IPAMincho, monospace</xsl:when>
          <xsl:when test="starts-with($language, 'ko') or starts-with($language, 'kr')">FreeMono, Baekmuk Gulim, monospace</xsl:when>
          <xsl:otherwise>FreeMono, AR PL UMing CN, monospace</xsl:otherwise>
        </xsl:choose>
      </xsl:attribute>
    </xsl:attribute-set>
    <xsl:attribute-set name="kbd">
      <xsl:attribute name="font-family">
        <xsl:choose>
          <xsl:when test="starts-with($language, 'ja')">FreeMono, IPAMincho, monospace</xsl:when>
          <xsl:when test="starts-with($language, 'ko') or starts-with($language, 'kr')">FreeMono, Baekmuk Gulim, monospace</xsl:when>
          <xsl:otherwise>FreeMono, AR PL UMing CN, monospace</xsl:otherwise>
        </xsl:choose>
      </xsl:attribute>
    </xsl:attribute-set>
    <xsl:attribute-set name="samp">
      <xsl:attribute name="font-family">
        <xsl:choose>
          <xsl:when test="starts-with($language, 'ja')">FreeMono, IPAMincho, monospace</xsl:when>
          <xsl:when test="starts-with($language, 'ko') or starts-with($language, 'kr')">FreeMono, Baekmuk Gulim, monospace</xsl:when>
          <xsl:otherwise>FreeMono, AR PL UMing CN, monospace</xsl:otherwise>
        </xsl:choose>
      </xsl:attribute>
    </xsl:attribute-set>

    <xsl:attribute-set name="big">
        <xsl:attribute name="font-size">larger</xsl:attribute>
    </xsl:attribute-set>
    <xsl:attribute-set name="small">
        <xsl:attribute name="font-size">smaller</xsl:attribute>
    </xsl:attribute-set>

    <xsl:attribute-set name="sub">
        <xsl:attribute name="baseline-shift">sub</xsl:attribute>
        <xsl:attribute name="font-size">smaller</xsl:attribute>
    </xsl:attribute-set>
    <xsl:attribute-set name="sup">
        <xsl:attribute name="baseline-shift">super</xsl:attribute>
        <xsl:attribute name="font-size">smaller</xsl:attribute>
    </xsl:attribute-set>

    <xsl:attribute-set name="s">
        <xsl:attribute name="text-decoration">line-through</xsl:attribute>
    </xsl:attribute-set>
    <xsl:attribute-set name="strike">
        <xsl:attribute name="text-decoration">line-through</xsl:attribute>
    </xsl:attribute-set>
    <xsl:attribute-set name="del">
        <xsl:attribute name="text-decoration">line-through</xsl:attribute>
    </xsl:attribute-set>

    <xsl:attribute-set name="u">
        <xsl:attribute name="text-decoration">underline</xsl:attribute>
    </xsl:attribute-set>
    <xsl:attribute-set name="ins">
        <xsl:attribute name="text-decoration">underline</xsl:attribute>
    </xsl:attribute-set>

    <xsl:attribute-set name="abbr">
        <!-- e.g.,
        <xsl:attribute name="font-variant">small-caps</xsl:attribute>
        <xsl:attribute name="letter-spacing">0.1em</xsl:attribute>
    -->
    </xsl:attribute-set>

    <xsl:attribute-set name="acronym">
        <!-- e.g.,
        <xsl:attribute name="font-variant">small-caps</xsl:attribute>
        <xsl:attribute name="letter-spacing">0.1em</xsl:attribute>
    -->
    </xsl:attribute-set>

    <xsl:attribute-set name="q"/>
    <xsl:attribute-set name="q-nested"/>
    <xsl:attribute-set name="label"/>

    <!--=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-
    Image
    =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-->

    <xsl:attribute-set name="img">
        <xsl:attribute name="content-height">75%</xsl:attribute>
        <xsl:attribute name="content-width">75%</xsl:attribute>
    </xsl:attribute-set>

    <xsl:attribute-set name="img-link" use-attribute-sets="img">
        <xsl:attribute name="border">2px solid</xsl:attribute>
    </xsl:attribute-set>

    <!--=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-
    Link
    =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-->

    <xsl:attribute-set name="a-link">
        <xsl:attribute name="text-decoration">underline</xsl:attribute>
        <xsl:attribute name="color">blue</xsl:attribute>
    </xsl:attribute-set>

    <!--======================================================================
    Templates
    =======================================================================-->
    <!--

      Since XHTML and XSL-FO have somewhat different elements, the transformation is split into two steps:
      - a preprocessing step that handles special attributes that can't be mapped directly into FO
      - a transformation step that converts elements into their equivalent
      The two steps are implemented using two template modes: 'preprocess' and 'transform'. After the root
      node is matched and the initial FO content is generated, the normal XSLT template matching continues
      alternating the two modes, starting with 'preprocess'. Upon a successful match in the preprocess mode,
      the current node is preprocessed, and, if it is not ignored (for example nodes that don't have a
      meaning in FO), it will be matched again in the 'transform' mode. If another template matches the
      current node in 'transform' mode, it may continue the template matching process for its child nodes,
      again in the 'preprocess' mode.

    -->

    <!--=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-
    Generic templates
    =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-->

    <!-- The generic preprocess template that simply switches the mode to 'transform' and matches the same node again -->
    <xsl:template match="*" mode="preprocess">
        <xsl:apply-templates select="." mode="transform"/>
    </xsl:template>

    <!-- The 'style' preprocessor, which takes care of a few CSS properties that need special handling in FO.

         Note: Starting with css4j 0.16, css4j applies styles to all elements, including the body tag. For example:
           <body class="exportbody" id="body" pdfcover="0" pdftoc="0" style="display: block; margin: 8pt; ">
         As a result we need to exclude the body tag from being processed as otherwise it would lead to a fo:block
         being issued at the wrong place.
    -->
    <xsl:template match="*[@style and not(self::html:body)]" mode="preprocess">
        <!-- Remove all white space and prepend ; for easier processing of the style -->
        <xsl:variable name="style" select="concat(';', translate(normalize-space(@style), ' ', ''))"/>
        <!-- Chain the 'style' processing into several named templates -->
        <xsl:call-template name="process-style-display">
            <xsl:with-param name="style" select="$style"/>
        </xsl:call-template>
    </xsl:template>

    <!-- Transform the eventual 'display' property in equivalent FO elements -->
    <xsl:template name="process-style-display">
        <xsl:param name="style"/>
        <xsl:choose>
            <!-- display: none => stop processing this branch -->
            <xsl:when test="contains($style, ';display:none')"/>
            <!-- display: inline-block => wrap inside fo:inline-container -->
            <xsl:when test="contains($style, ';display:inline-block')">
                <fo:inline-container>
                    <fo:block>
                        <xsl:call-template name="process-style-float">
                            <xsl:with-param name="style" select="$style"/>
                        </xsl:call-template>
                    </fo:block>
                </fo:inline-container>
            </xsl:when>
            <!-- display: block => wrap inside fo:block -->
            <xsl:when test="contains($style, ';display:block')">
                <fo:block>
                    <xsl:call-template name="process-style-float">
                        <xsl:with-param name="style" select="$style"/>
                    </xsl:call-template>
                </fo:block>
            </xsl:when>
            <!-- display: inline => wrap inside fo:inline -->
            <xsl:when test="contains($style, ';display:inline')">
                <fo:inline>
                    <xsl:call-template name="process-style-float">
                        <xsl:with-param name="style" select="$style"/>
                    </xsl:call-template>
                </fo:inline>
            </xsl:when>
            <!-- TODO: other display types, like list-item, table, etc. -->
            <!-- No display or other display types, simply continue processing -->
            <xsl:otherwise>
                <xsl:call-template name="process-style-float">
                    <xsl:with-param name="style" select="$style"/>
                </xsl:call-template>
            </xsl:otherwise>
        </xsl:choose>
    </xsl:template>

    <!-- Transform the eventual 'float' property into fo:float -->
    <xsl:template name="process-style-float">
        <xsl:param name="style"/>
        <xsl:choose>
            <!-- Disabled since fop-0.95 does not support floats and completely drops floated elements -->
            <xsl:when test="0 and contains($style, ';float:')">
                <fo:float>
                    <xsl:attribute name="float">
                        <xsl:call-template name="get-style-value">
                            <xsl:with-param name="style" select="$style"/>
                            <xsl:with-param name="property" select="'float'"/>
                        </xsl:call-template>
                    </xsl:attribute>
                    <fo:block>
                        <xsl:call-template name="process-style-clear">
                            <xsl:with-param name="style" select="$style"/>
                        </xsl:call-template>
                    </fo:block>
                </fo:float>
            </xsl:when>
            <!-- No float, simply continue processing -->
            <xsl:otherwise>
                <xsl:call-template name="process-style-clear">
                    <xsl:with-param name="style" select="$style"/>
                </xsl:call-template>
            </xsl:otherwise>
        </xsl:choose>
    </xsl:template>

    <!-- Transform the eventual 'clear' property into a fo:block with the equivalen clear attribute -->
    <xsl:template name="process-style-clear">
        <xsl:param name="style"/>
        <xsl:choose>
            <xsl:when test="contains($style, ';clear:')">
                <fo:block>
                    <xsl:attribute name="clear">
                        <xsl:call-template name="get-style-value">
                            <xsl:with-param name="style" select="$style"/>
                            <xsl:with-param name="property" select="'clear'"/>
                        </xsl:call-template>
                    </xsl:attribute>
                    <xsl:apply-templates select="." mode="transform"/>
                </fo:block>
            </xsl:when>
            <!-- No furher special attributes, continue processing in 'transform' mode -->
            <xsl:otherwise>
                <xsl:apply-templates select="." mode="transform"/>
            </xsl:otherwise>
        </xsl:choose>
    </xsl:template>

    <!-- Compute the list number format based on the list-style-type style value -->
    <xsl:template name="process-list-marker">
        <xsl:param name="list-type"/>
        <xsl:variable name="style" select="concat(';', translate(normalize-space(@style), ' ', ''), ';', translate(normalize-space(../@style), ' ', ''))"/>
        <xsl:variable name="list-style-type">
            <xsl:call-template name="get-style-value">
                <xsl:with-param name="style" select="$style"/>
                <xsl:with-param name="property" select="'list-style-type'"/>
            </xsl:call-template>
        </xsl:variable>
        <xsl:choose>
            <xsl:when test="$list-style-type = 'none'" />
            <xsl:when test="$list-style-type = 'disc'">&#x2022;</xsl:when>
            <xsl:when test="$list-style-type = 'circle'">&#x25E6;</xsl:when>
            <xsl:when test="$list-style-type = 'square'">&#x25FE;</xsl:when>
            <xsl:when test="$list-style-type = 'decimal'">
                <xsl:number format="1."/>
            </xsl:when>
            <xsl:when test="$list-style-type = 'decimal-leading-zero'">
                <xsl:number format="01."/>
            </xsl:when>
            <xsl:when test="($list-style-type = 'lower-alpha') or ($list-style-type = 'lower-latin')">
                <xsl:number format="a."/>
            </xsl:when>
            <xsl:when test="($list-style-type = 'upper-alpha') or ($list-style-type = 'upper-latin')">
                <xsl:number format="A."/>
            </xsl:when>
            <xsl:when test="$list-style-type = 'lower-roman'">
                <xsl:number format="i."/>
            </xsl:when>
            <xsl:when test="$list-style-type = 'upper-roman'">
                <xsl:number format="I."/>
            </xsl:when>
            <!-- There's a bug with greek numbering, XSLT also uses lowercase final sigma before the normal sigma. -->
            <xsl:when test="$list-style-type = 'lower-greek'">
                <xsl:number format="&#x03B1;."/>
            </xsl:when>
            <!-- Disabled, XSLT doesn't support armenian numbering yet.
            <xsl:when test="$list-style-type = 'armenian'">
                <xsl:number format="&#x0531;."/>
            </xsl:when>
            -->
            <xsl:when test="$list-style-type = 'georgian'">
                <xsl:number format="&#x10D0;." letter-value="traditional"/>
            </xsl:when>
            <xsl:otherwise>
                <xsl:choose>
                    <xsl:when test="$list-type = 'ol'">
                        <xsl:number format="1."/>
                    </xsl:when>
                    <xsl:when test="$list-type = 'ul'">&#x2022;</xsl:when>
                </xsl:choose>
            </xsl:otherwise>
        </xsl:choose>
    </xsl:template>

    <!-- Get the value for a given CSS property from a style attribute -->
    <xsl:template name="get-style-value">
        <xsl:param name="style"/>
        <xsl:param name="property"/>
        <xsl:variable name="value-and-rest" select="normalize-space(substring-after($style, concat($property, ':')))"/>
        <xsl:choose>
            <xsl:when test="contains($value-and-rest, ';')">
                <xsl:value-of select="normalize-space(substring-before($value-and-rest, ';'))"/>
            </xsl:when>
            <xsl:otherwise>
                <xsl:value-of select="$value-and-rest"/>
            </xsl:otherwise>
        </xsl:choose>
    </xsl:template>

    <!--=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-
    Root
    =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-->

    <xsl:template match="html:html">
        <fo:root xsl:use-attribute-sets="root">
            <xsl:call-template name="process-common-attributes"/>
            <xsl:call-template name="make-layout-master-set"/>
            <xsl:apply-templates mode="preprocess"/>
        </fo:root>
    </xsl:template>

    <xsl:template name="make-layout-master-set">
        <fo:layout-master-set>
            <fo:simple-page-master master-name="first"
                                   xsl:use-attribute-sets="page">
                <fo:region-body margin-top="{$page-margin-top}"
                                margin-right="{$page-margin-right}"
                                margin-bottom="{$page-margin-bottom}"
                                margin-left="{$page-margin-left}"
                                column-count="{$column-count}"
                                column-gap="{$column-gap}"/>
                <xsl:choose>
                    <xsl:when test="$writing-mode = 'tb-rl'">
                        <fo:region-before extent="{$page-margin-right}" precedence="true"/>
                        <fo:region-after  extent="{$page-margin-left}" precedence="true"/>
                        <fo:region-start  region-name="page-header-first"
                                          extent="{$page-margin-top}"
                                          writing-mode="lr-tb"
                                          display-align="before"/>
                        <fo:region-end    region-name="page-footer-first"
                                          extent="{$page-margin-bottom}"
                                          writing-mode="lr-tb"
                                          display-align="after"/>
                    </xsl:when>
                    <xsl:when test="$writing-mode = 'rl-tb'">
                        <fo:region-before region-name="page-header-first"
                                          extent="{$page-margin-top}"
                                          display-align="before"/>
                        <fo:region-after  region-name="page-footer-first"
                                          extent="{$page-margin-bottom}"
                                          display-align="after"/>
                        <fo:region-start  extent="{$page-margin-right}"/>
                        <fo:region-end    extent="{$page-margin-left}"/>
                    </xsl:when>
                    <xsl:otherwise><!-- $writing-mode = 'lr-tb' -->
                        <fo:region-before region-name="page-header-first"
                                          extent="{$page-margin-top}"
                                          display-align="before"/>
                        <fo:region-after  region-name="page-footer-first"
                                          extent="{$page-margin-bottom}"
                                          display-align="after"/>
                        <fo:region-start  extent="{$page-margin-left}"/>
                        <fo:region-end    extent="{$page-margin-right}"/>
                    </xsl:otherwise>
                </xsl:choose>
            </fo:simple-page-master>
            <fo:simple-page-master master-name="rest"
                                   xsl:use-attribute-sets="page">
                <fo:region-body margin-top="{$page-margin-top}"
                                margin-right="{$page-margin-right}"
                                margin-bottom="{$page-margin-bottom}"
                                margin-left="{$page-margin-left}"
                                column-count="{$column-count}"
                                column-gap="{$column-gap}"/>
                <xsl:choose>
                    <xsl:when test="$writing-mode = 'tb-rl'">
                        <fo:region-before extent="{$page-margin-right}" precedence="true"/>
                        <fo:region-after  extent="{$page-margin-left}" precedence="true"/>
                        <fo:region-start  region-name="page-header"
                                          extent="{$page-margin-top}"
                                          writing-mode="lr-tb"
                                          display-align="before"/>
                        <fo:region-end    region-name="page-footer"
                                          extent="{$page-margin-bottom}"
                                          writing-mode="lr-tb"
                                          display-align="after"/>
                    </xsl:when>
                    <xsl:when test="$writing-mode = 'rl-tb'">
                        <fo:region-before region-name="page-header"
                                          extent="{$page-margin-top}"
                                          display-align="before"/>
                        <fo:region-after  region-name="page-footer"
                                          extent="{$page-margin-bottom}"
                                          display-align="after"/>
                        <fo:region-start  extent="{$page-margin-right}"/>
                        <fo:region-end    extent="{$page-margin-left}"/>
                    </xsl:when>
                    <xsl:otherwise><!-- $writing-mode = 'lr-tb' -->
                        <fo:region-before region-name="page-header"
                                          extent="{$page-margin-top}"
                                          display-align="before"/>
                        <fo:region-after  region-name="page-footer"
                                          extent="{$page-margin-bottom}"
                                          display-align="after"/>
                        <fo:region-start  extent="{$page-margin-left}"/>
                        <fo:region-end    extent="{$page-margin-right}"/>
                    </xsl:otherwise>
                </xsl:choose>
            </fo:simple-page-master>
            <fo:page-sequence-master master-name="all-pages">
                <fo:repeatable-page-master-alternatives>
                    <xsl:choose>
                        <xsl:when test="/html:html/html:body[@pdfcover = '1']">
                            <fo:conditional-page-master-reference page-position="first" master-reference="first"/>
                            <fo:conditional-page-master-reference page-position="rest" master-reference="rest"/>
                        </xsl:when>
                        <xsl:otherwise>
                            <fo:conditional-page-master-reference page-position="any" master-reference="rest"/>
                        </xsl:otherwise>
                    </xsl:choose>
                </fo:repeatable-page-master-alternatives>
            </fo:page-sequence-master>
        </fo:layout-master-set>
    </xsl:template>

    <!-- Ignore the head and all scripts -->
    <xsl:template match="html:head | html:script" mode="preprocess"/>

    <xsl:template match="html:body" mode="preprocess">
        <xsl:variable name="need-cover">
           <xsl:value-of select="@pdfcover"/>
        </xsl:variable>
        <xsl:variable name="need-toc">
           <xsl:value-of select="@pdftoc"/>
        </xsl:variable>
        <fo:page-sequence master-reference="all-pages" id="x-page-sequence">
            <fo:title>
                <xsl:value-of select="/html:html/html:head/html:title[@class='pdftitle']"/>
            </fo:title>
            <fo:static-content flow-name="page-header">
                <fo:block space-before.conditionality="retain"
                          space-before="{$page-header-margin}"
                          xsl:use-attribute-sets="page-header">
                    <xsl:if test="$title-print-in-header = 'true'">
                        <xsl:apply-templates select="/html:html/html:body/html:div[@class='pdfheader']" mode="pdfheader" />
                    </xsl:if>
                </fo:block>
            </fo:static-content>
            <fo:static-content flow-name="page-footer">
                <fo:block space-after.conditionality="retain"
                          space-after="{$page-footer-margin}"
                          xsl:use-attribute-sets="page-footer">
                    <xsl:if test="$page-number-print-in-footer = 'true'">
                        <xsl:apply-templates  select="/html:html/html:body/html:div[@class='pdffooter']" mode="pdffooter" />
                    </xsl:if>
                </fo:block>
            </fo:static-content>
            <fo:flow flow-name="xsl-region-body">
                <!-- cover page -->
                <xsl:if test="$need-cover = '1'">
                    <fo:block xsl:use-attribute-sets="body" width="100%">
                        <fo:block text-align="center" padding-top="10pt" padding-bottom="10pt">
                            <fo:table table-layout="fixed" width="100%">
                                <fo:table-body>
                                    <fo:table-row height="200mm">
                                        <fo:table-cell display-align="center">
                                            <xsl:apply-templates select="/html:html/html:body/html:div[@class='pdfcover']" mode="pdfcover" />
                                        </fo:table-cell>
                                    </fo:table-row>
                                </fo:table-body>
                            </fo:table>
                        </fo:block>
                     </fo:block>
                </xsl:if>
                <!-- Table of content -->
                <xsl:if test="$need-toc = '1'">
                    <fo:block xsl:use-attribute-sets="body" break-before="page">
                        <fo:block text-align="left" font-weight="bold" font-size="12pt"  padding-top="10pt" padding-bottom="10pt">
                            <xsl:apply-templates  select="/html:html/html:body/html:div[@class='pdftoc']" mode="pdftoc" />
                        </fo:block>
                        <xsl:for-each select="//html:div[@id='xwikicontent']/html:*[local-name() = 'h1'
                              or local-name() = 'h2' or local-name() = 'h3']">
                            <fo:block font-size="9pt" start-indent="10pt" width="100%" text-align-last="justify" >
                                <xsl:choose>
                                    <xsl:when test="self::html:h1">
                                        <xsl:attribute name="start-indent">0pt</xsl:attribute>
                                    </xsl:when>
                                    <xsl:when test="self::html:h2">
                                        <xsl:attribute name="start-indent">6pt</xsl:attribute>
                                    </xsl:when>
                                    <xsl:when test="self::html:h3">
                                        <xsl:attribute name="start-indent">12pt</xsl:attribute>
                                    </xsl:when>
                                </xsl:choose>
                                <fo:basic-link internal-destination="{generate-id(.)}">
                                    <!-- TODO: Make numbering work.
                                    <xsl:choose>
                                        <xsl:when test="self::html:h1">
                                           <xsl:number level="multiple" count="html:h1" format="1. "/>
                                        </xsl:when>
                                        <xsl:when test="self::html:h2">
                                           <xsl:number level="multiple" count="html:h1|html:h2" format="1.1. "/>
                                        </xsl:when>
                                        <xsl:when test="self::html:h3">
                                           <xsl:number level="multiple" count="html:h1|html:h2|html:h3" format="1.1.1. "/>
                                        </xsl:when>
                                    </xsl:choose>
                                    -->
                                    <xsl:value-of select="."/>
                                </fo:basic-link>
                                <xsl:text> </xsl:text>
                                <fo:leader leader-length.minimum="12pt" leader-length.optimum="40pt"
                                    leader-length.maximum="100%" leader-pattern="dots" />
                                <xsl:text> </xsl:text>
                                <fo:basic-link internal-destination="{generate-id(.)}">
                                    <fo:page-number-citation ref-id="{generate-id(.)}" />
                                </fo:basic-link>
                            </fo:block>
                       </xsl:for-each>
                    </fo:block>
                </xsl:if>
                <!-- the actual document content -->
                <fo:block xsl:use-attribute-sets="body"  break-before="page">
                    <xsl:call-template name="process-common-attributes"/>
                    <xsl:apply-templates mode="preprocess"/>
                </fo:block>
            </fo:flow>
        </fo:page-sequence>
    </xsl:template>

    <xsl:template match="/html:html/html:body/html:div[@class='pdfheader']" mode="pdfheader" priority="0">
        <fo:block>
            <xsl:call-template name="process-common-attributes"/>
            <xsl:apply-templates mode="preprocess"/>
        </fo:block>
    </xsl:template>

    <xsl:template match="/html:html/html:body/html:div[@class='pdffooter']" mode="pdffooter" priority="0">
        <fo:block>
            <xsl:call-template name="process-common-attributes"/>
            <xsl:apply-templates mode="preprocess"/>
        </fo:block>
    </xsl:template>

    <xsl:template match="/html:html/html:body/html:div[@class='pdfcover']" mode="pdfcover" priority="0">
        <fo:block>
            <xsl:call-template name="process-common-attributes"/>
            <xsl:apply-templates mode="preprocess"/>
        </fo:block>
    </xsl:template>

    <xsl:template match="/html:html/html:body/html:div[@class='pdftoc']" mode="pdftoc" priority="0">
        <fo:block>
            <xsl:call-template name="process-common-attributes"/>
            <xsl:apply-templates mode="preprocess"/>
        </fo:block>
    </xsl:template>

    <xsl:template match="/html:html/html:body/html:div[@class='pdfheader']" priority="1" mode="preprocess"/>

    <xsl:template match="/html:html/html:body/html:div[@class='pdffooter']" priority="1" mode="preprocess"/>

    <xsl:template match="/html:html/html:body/html:div[@class='pdfcover']" priority="1" mode="preprocess"/>

    <xsl:template match="/html:html/html:body/html:div[@class='pdftoc']" priority="1" mode="preprocess"/>

    <xsl:template match="html:span[@class='page-number']" mode="preprocess">
        <fo:page-number/>
    </xsl:template>

    <xsl:template match="html:span[@class='page-total']" mode="preprocess">
        <fo:page-number-citation-last ref-id="x-page-sequence"/>
    </xsl:template>

    <!--=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-
    process common attributes and children
    =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-->

    <xsl:template name="process-common-attributes-and-children">
        <xsl:call-template name="process-common-attributes"/>
        <xsl:apply-templates mode="preprocess"/>
    </xsl:template>

    <xsl:template name="process-common-attributes">
        <xsl:attribute name="role">
            <xsl:value-of select="concat('html:', local-name())"/>
        </xsl:attribute>

        <xsl:choose>
            <xsl:when test="@xml:lang">
                <xsl:attribute name="xml:lang">
                    <xsl:value-of select="@xml:lang"/>
                </xsl:attribute>
            </xsl:when>
            <xsl:when test="@lang">
                <xsl:attribute name="xml:lang">
                    <xsl:value-of select="@lang"/>
                </xsl:attribute>
            </xsl:when>
        </xsl:choose>

        <xsl:choose>
            <xsl:when test="@id">
                <xsl:attribute name="id">
                    <xsl:value-of select="@id"/>
                </xsl:attribute>
            </xsl:when>
            <xsl:when test="self::html:a/@name">
                <xsl:attribute name="id">
                    <xsl:value-of select="@name"/>
                </xsl:attribute>
            </xsl:when>
        </xsl:choose>

        <xsl:if test="@align">
            <xsl:choose>
                <xsl:when test="self::html:caption">
                </xsl:when>
                <xsl:when test="self::html:img or self::html:object">
                    <xsl:if test="@align = 'bottom' or @align = 'middle' or @align = 'top'">
                        <xsl:attribute name="vertical-align">
                            <xsl:value-of select="@align"/>
                        </xsl:attribute>
                    </xsl:if>
                </xsl:when>
                <xsl:otherwise>
                    <xsl:call-template name="process-cell-align">
                        <xsl:with-param name="align" select="@align"/>
                    </xsl:call-template>
                </xsl:otherwise>
            </xsl:choose>
        </xsl:if>
        <xsl:if test="@valign">
            <xsl:call-template name="process-cell-valign">
                <xsl:with-param name="valign" select="@valign"/>
            </xsl:call-template>
        </xsl:if>

        <xsl:if test="@style">
            <xsl:call-template name="process-style">
                <xsl:with-param name="style" select="@style"/>
            </xsl:call-template>
        </xsl:if>
    </xsl:template>

    <xsl:template name="process-style">
        <xsl:param name="style"/>
        <!-- e.g., style="text-align: center; color: red" converted to text-align="center" color="red" -->
        <!-- Lowercase property names. Since we're not using XPath 2.0, we must use translate() instead of lower-case(). -->
        <xsl:variable name="name"
                      select="translate(normalize-space(substring-before($style, ':')), 'ABCDEFGHIJKLMNOPQRSTUVWXYZ', 'abcdefghijklmnopqrstuvwxyz')"/>
        <xsl:if test="$name">
            <xsl:variable name="value-and-rest"
                          select="normalize-space(substring-after($style, ':'))"/>
            <xsl:variable name="value">
                <xsl:choose>
                    <xsl:when test="contains($value-and-rest, ';')">
                        <xsl:value-of select="normalize-space(substring-before(
                        $value-and-rest, ';'))"/>
                    </xsl:when>
                    <xsl:otherwise>
                        <xsl:value-of select="$value-and-rest"/>
                    </xsl:otherwise>
                </xsl:choose>
            </xsl:variable>
            <xsl:choose>
                <xsl:when test="$name = 'width' and (self::html:col or self::html:colgroup)">
                    <xsl:attribute name="column-width">
                        <xsl:value-of select="$value"/>
                    </xsl:attribute>
                </xsl:when>
                <xsl:when test="$name = 'vertical-align' and (
                self::html:table or self::html:caption or
                self::html:thead or self::html:tfoot or
                self::html:tbody or self::html:colgroup or
                self::html:col or self::html:tr or
                self::html:th or self::html:td)">
                    <xsl:choose>
                        <xsl:when test="$value = 'top'">
                            <xsl:attribute name="display-align">before</xsl:attribute>
                        </xsl:when>
                        <xsl:when test="$value = 'bottom'">
                            <xsl:attribute name="display-align">after</xsl:attribute>
                        </xsl:when>
                        <xsl:when test="$value = 'middle'">
                            <xsl:attribute name="display-align">center</xsl:attribute>
                        </xsl:when>
                        <xsl:otherwise>
                            <xsl:attribute name="display-align">auto</xsl:attribute>
                            <xsl:attribute name="relative-align">baseline</xsl:attribute>
                        </xsl:otherwise>
                    </xsl:choose>
                </xsl:when>
                <xsl:when test="$name = 'list-style'">
                </xsl:when>
                <!-- Not supported in FOP and needs to be ignored as otherwise they cause the export to fail with
                     an exception raised -->
                <xsl:when test="$name = 'border-image'"/>
                <xsl:when test="$name = 'tab-stops'"/>
                <!-- These are not valid in FO, so we ignore them -->
                <xsl:when test="$name = 'cursor'"/>
                <xsl:when test="$name = 'quotes'"/>
                <xsl:when test="$name = 'width' and self::html:img"/>
                <xsl:when test="$name = 'height' and self::html:img"/>
                <xsl:when test="$name = 'box-sizing'"/>
                <xsl:when test="$name = 'text-justify'"/>
                <xsl:when test="$name = 'text-autospace'"/>
                <xsl:when test="$name = 'font-variant-ligatures'"/>
                <xsl:when test="$name = 'font-variant-caps'"/>
                <xsl:when test="$name = 'text-decoration-style'"/>
                <xsl:when test="$name = 'text-decoration-color'"/>
                <xsl:when test="starts-with($name, 'list-')"/>
                <xsl:when test="starts-with($name, 'outline')"/>
                <!-- These are treated separately in the 'generic' template mode, since they can't be applied directly on the current element -->
                <xsl:when test="$name = 'display'"/>
                <xsl:when test="$name = 'float'"/>
                <xsl:when test="$name = 'clear'"/>
                <!-- Skip properties added when copy/pasting from MS Office -->
                <xsl:when test="starts-with($name, 'mso')"/>
                <!-- Skip browser specific properties -->
                <xsl:when test="starts-with($name, '-')"/>
                <xsl:otherwise>
                    <xsl:attribute name="{$name}">
                        <xsl:value-of select="$value"/>
                    </xsl:attribute>
                </xsl:otherwise>
            </xsl:choose>
        </xsl:if>
        <xsl:variable name="rest"
                      select="normalize-space(substring-after($style, ';'))"/>
        <xsl:if test="$rest">
            <xsl:call-template name="process-style">
                <xsl:with-param name="style" select="$rest"/>
            </xsl:call-template>
        </xsl:if>
    </xsl:template>


    <!--=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-
    Block-level
    =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-->

    <xsl:template match="html:h1" mode="transform">
        <fo:block id="{generate-id(.)}">
            <fo:block xsl:use-attribute-sets="h1">
                <xsl:call-template name="process-common-attributes-and-children"/>
            </fo:block>
        </fo:block>
    </xsl:template>

    <xsl:template match="html:h2" mode="transform">
        <fo:block id="{generate-id(.)}">
            <fo:block xsl:use-attribute-sets="h2">
                <xsl:call-template name="process-common-attributes-and-children"/>
            </fo:block>
        </fo:block>
    </xsl:template>

    <xsl:template match="html:h3" mode="transform">
        <fo:block id="{generate-id(.)}">
            <fo:block xsl:use-attribute-sets="h3">
                <xsl:call-template name="process-common-attributes-and-children"/>
            </fo:block>
        </fo:block>
    </xsl:template>

    <xsl:template match="html:h4" mode="transform">
        <fo:block xsl:use-attribute-sets="h4">
            <xsl:call-template name="process-common-attributes-and-children"/>
        </fo:block>
    </xsl:template>

    <xsl:template match="html:h5" mode="transform">
        <fo:block xsl:use-attribute-sets="h5">
            <xsl:call-template name="process-common-attributes-and-children"/>
        </fo:block>
    </xsl:template>

    <xsl:template match="html:h6" mode="transform">
        <fo:block xsl:use-attribute-sets="h6">
            <xsl:call-template name="process-common-attributes-and-children"/>
        </fo:block>
    </xsl:template>

    <xsl:template match="html:p" mode="transform">
        <fo:block xsl:use-attribute-sets="p">
            <xsl:call-template name="process-common-attributes-and-children"/>
        </fo:block>
    </xsl:template>

    <!-- initial paragraph, preceded by h1..6 or div -->
    <xsl:template match="html:p[preceding-sibling::*[1][
    self::html:h1 or self::html:h2 or self::html:h3 or
    self::html:h4 or self::html:h5 or self::html:h6]]" mode="transform">
        <fo:block xsl:use-attribute-sets="p-initial">
            <xsl:call-template name="process-common-attributes-and-children"/>
        </fo:block>
    </xsl:template>

    <!-- initial paragraph, first child of div, body or td -->
    <xsl:template match="html:p[not(preceding-sibling::*) and (
    parent::html:div or parent::html:body or
    parent::html:td)]" mode="transform">
        <fo:block xsl:use-attribute-sets="p-initial-first">
            <xsl:call-template name="process-common-attributes-and-children"/>
        </fo:block>
    </xsl:template>

    <xsl:template match="html:blockquote" mode="transform">
        <fo:block xsl:use-attribute-sets="blockquote">
            <xsl:call-template name="process-common-attributes-and-children"/>
        </fo:block>
    </xsl:template>

    <xsl:template match="html:pre" mode="transform">
        <fo:block xsl:use-attribute-sets="pre">
            <xsl:call-template name="process-pre"/>
        </fo:block>
    </xsl:template>

    <xsl:template name="process-pre" mode="transform">
        <xsl:call-template name="process-common-attributes"/>
        <!-- remove leading CR/LF/CRLF char -->
        <xsl:variable name="crlf"><xsl:text>&#xD;&#xA;</xsl:text></xsl:variable>
        <xsl:variable name="lf"><xsl:text>&#xA;</xsl:text></xsl:variable>
        <xsl:variable name="cr"><xsl:text>&#xD;</xsl:text></xsl:variable>
        <xsl:for-each select="node()">
            <xsl:choose>
                <xsl:when test="position() = 1 and self::text()">
                    <xsl:choose>
                        <xsl:when test="starts-with(., $lf)">
                            <xsl:value-of select="substring(., 2)"/>
                        </xsl:when>
                        <xsl:when test="starts-with(., $crlf)">
                            <xsl:value-of select="substring(., 3)"/>
                        </xsl:when>
                        <xsl:when test="starts-with(., $cr)">
                            <xsl:value-of select="substring(., 2)"/>
                        </xsl:when>
                        <xsl:otherwise>
                            <xsl:apply-templates select="." mode="preprocess"/>
                        </xsl:otherwise>
                    </xsl:choose>
                </xsl:when>
                <xsl:otherwise>
                    <xsl:apply-templates select="." mode="preprocess"/>
                </xsl:otherwise>
            </xsl:choose>
        </xsl:for-each>
    </xsl:template>

    <xsl:template match="html:address" mode="transform">
        <fo:block xsl:use-attribute-sets="address">
            <xsl:call-template name="process-common-attributes-and-children"/>
        </fo:block>
    </xsl:template>

    <xsl:template match="html:hr" mode="transform">
        <fo:block xsl:use-attribute-sets="hr">
            <xsl:call-template name="process-common-attributes"/>
        </fo:block>
    </xsl:template>

    <xsl:template match="html:div" mode="transform">
        <!-- need fo:block-container? or normal fo:block -->
        <xsl:variable name="need-block-container">
            <xsl:call-template name="need-block-container"/>
        </xsl:variable>
        <xsl:choose>
            <xsl:when test="$need-block-container = 'true'">
                <fo:block-container>
                    <xsl:if test="@dir">
                        <xsl:attribute name="writing-mode">
                            <xsl:choose>
                                <xsl:when test="@dir = 'rtl'">rl-tb</xsl:when>
                                <xsl:otherwise>lr-tb</xsl:otherwise>
                            </xsl:choose>
                        </xsl:attribute>
                    </xsl:if>
                    <xsl:call-template name="process-common-attributes"/>
                    <fo:block start-indent="0pt" end-indent="0pt">
                        <xsl:apply-templates mode="preprocess"/>
                    </fo:block>
                </fo:block-container>
            </xsl:when>
            <xsl:otherwise>
                <!-- normal block -->
                <fo:block>
                    <xsl:call-template name="process-common-attributes-and-children"/>
                </fo:block>
            </xsl:otherwise>
        </xsl:choose>
    </xsl:template>

    <xsl:template name="need-block-container">
        <xsl:choose>
            <xsl:when test="@dir">true</xsl:when>
            <xsl:when test="@style">
                <xsl:variable name="s"
                              select="concat(';', translate(normalize-space(@style),
                    ' ', ''))"/>
                <xsl:choose>
                    <xsl:when test="contains($s, ';width:') or
                    contains($s, ';height:') or
                    contains($s, ';position:absolute') or
                    contains($s, ';position:fixed') or
                    contains($s, ';writing-mode:')">true</xsl:when>
                    <xsl:otherwise>false</xsl:otherwise>
                </xsl:choose>
            </xsl:when>
            <xsl:otherwise>false</xsl:otherwise>
        </xsl:choose>
    </xsl:template>

    <xsl:template match="html:center" mode="transform">
        <fo:block text-align="center">
            <xsl:call-template name="process-common-attributes-and-children"/>
        </fo:block>
    </xsl:template>

    <xsl:template match="html:fieldset | html:form | html:dir | html:menu" mode="transform">
        <fo:block space-before="1em" space-after="1em">
            <xsl:call-template name="process-common-attributes-and-children"/>
        </fo:block>
    </xsl:template>

    <!--=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-
    List
    =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-->

    <xsl:template match="html:ul" mode="transform">
        <fo:list-block xsl:use-attribute-sets="ul">
            <xsl:call-template name="process-common-attributes-and-children"/>
        </fo:list-block>
    </xsl:template>

    <xsl:template match="html:li//html:ul" mode="transform">
        <fo:list-block xsl:use-attribute-sets="ul-nested">
            <xsl:call-template name="process-common-attributes-and-children"/>
        </fo:list-block>
    </xsl:template>

    <xsl:template match="html:ol" mode="transform">
        <fo:list-block xsl:use-attribute-sets="ol">
            <xsl:call-template name="process-common-attributes-and-children"/>
        </fo:list-block>
    </xsl:template>

    <xsl:template match="html:li//html:ol" mode="transform">
        <fo:list-block xsl:use-attribute-sets="ol-nested">
            <xsl:call-template name="process-common-attributes-and-children"/>
        </fo:list-block>
    </xsl:template>

    <xsl:template match="html:ul/html:li" mode="transform">
        <fo:list-item xsl:use-attribute-sets="ul-li">
            <xsl:call-template name="process-li">
                <xsl:with-param name="list-type" select="'ul'"/>
            </xsl:call-template>
        </fo:list-item>
    </xsl:template>

    <xsl:template match="html:ol/html:li" mode="transform">
        <fo:list-item xsl:use-attribute-sets="ol-li">
            <xsl:call-template name="process-li">
                <xsl:with-param name="list-type" select="'ol'"/>
            </xsl:call-template>
        </fo:list-item>
    </xsl:template>

    <xsl:template name="process-li">
        <xsl:param name="list-type"/>
        <xsl:call-template name="process-common-attributes"/>
        <fo:list-item-label end-indent="label-end()"
                            text-align="end" wrap-option="no-wrap">
            <fo:block>
                <xsl:call-template name="process-list-marker" >
                    <xsl:with-param name="list-type" select="$list-type"/>
                </xsl:call-template>
            </fo:block>
        </fo:list-item-label>
        <fo:list-item-body start-indent="body-start()">
            <fo:block>
                <xsl:apply-templates mode="preprocess"/>
            </fo:block>
        </fo:list-item-body>
    </xsl:template>

    <xsl:template match="html:dl" mode="transform">
        <fo:block xsl:use-attribute-sets="dl">
            <xsl:call-template name="process-common-attributes-and-children"/>
        </fo:block>
    </xsl:template>

    <xsl:template match="html:dt" mode="transform">
        <fo:block xsl:use-attribute-sets="dt">
            <xsl:call-template name="process-common-attributes-and-children"/>
        </fo:block>
    </xsl:template>

    <xsl:template match="html:dd" mode="transform">
        <fo:block xsl:use-attribute-sets="dd">
            <xsl:call-template name="process-common-attributes-and-children"/>
        </fo:block>
    </xsl:template>

    <!--=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-
    Table
    =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-->

    <xsl:template match="html:table" mode="transform">
        <fo:table-and-caption xsl:use-attribute-sets="table-and-caption">
            <xsl:call-template name="make-table-caption"/>
            <fo:table xsl:use-attribute-sets="table">
                <xsl:call-template name="process-table"/>
            </fo:table>
        </fo:table-and-caption>
    </xsl:template>

    <xsl:template name="make-table-caption">
        <xsl:if test="html:caption/@align">
            <xsl:attribute name="caption-side">
                <xsl:value-of select="html:caption/@align"/>
            </xsl:attribute>
        </xsl:if>
        <xsl:apply-templates select="html:caption" mode="preprocess"/>
    </xsl:template>

    <xsl:template name="process-table">
        <xsl:if test="@style">
            <xsl:call-template name="process-table-style">
                <xsl:with-param name="style" select="@style"/>
            </xsl:call-template>
        </xsl:if>
        <xsl:if test="@width">
            <xsl:attribute name="inline-progression-dimension">
                <xsl:choose>
                    <xsl:when test="contains(@width, '%')">
                        <xsl:value-of select="@width"/>
                    </xsl:when>
                    <xsl:otherwise>
                        <xsl:value-of select="@width"/>px</xsl:otherwise>
                </xsl:choose>
            </xsl:attribute>
        </xsl:if>
        <xsl:if test="@border or @frame">
            <xsl:choose>
                <xsl:when test="@border &gt; 0">
                    <xsl:attribute name="border">
                        <xsl:value-of select="@border"/>px</xsl:attribute>
                </xsl:when>
            </xsl:choose>
            <xsl:choose>
                <xsl:when test="@border = '0' or @frame = 'void'">
                    <xsl:attribute name="border-style">hidden</xsl:attribute>
                </xsl:when>
                <xsl:when test="@frame = 'above'">
                    <xsl:attribute name="border-style">outset hidden hidden hidden</xsl:attribute>
                </xsl:when>
                <xsl:when test="@frame = 'below'">
                    <xsl:attribute name="border-style">hidden hidden outset hidden</xsl:attribute>
                </xsl:when>
                <xsl:when test="@frame = 'hsides'">
                    <xsl:attribute name="border-style">outset hidden</xsl:attribute>
                </xsl:when>
                <xsl:when test="@frame = 'vsides'">
                    <xsl:attribute name="border-style">hidden outset</xsl:attribute>
                </xsl:when>
                <xsl:when test="@frame = 'lhs'">
                    <xsl:attribute name="border-style">hidden hidden hidden outset</xsl:attribute>
                </xsl:when>
                <xsl:when test="@frame = 'rhs'">
                    <xsl:attribute name="border-style">hidden outset hidden hidden</xsl:attribute>
                </xsl:when>
                <xsl:otherwise>
                    <xsl:attribute name="border-style">outset</xsl:attribute>
                </xsl:otherwise>
            </xsl:choose>
        </xsl:if>
        <xsl:if test="@cellspacing">
            <xsl:attribute name="border-spacing">
                <xsl:value-of select="@cellspacing"/>px</xsl:attribute>
            <xsl:attribute name="border-collapse">separate</xsl:attribute>
        </xsl:if>
        <xsl:if test="@rules and (@rules = 'groups' or
        @rules = 'rows' or
        @rules = 'cols' or
        @rules = 'all' and (not(@border or @frame) or
        @border = '0' or @frame and
        not(@frame = 'box' or @frame = 'border')))">
            <xsl:attribute name="border-collapse">collapse</xsl:attribute>
            <xsl:if test="not(@border or @frame)">
                <xsl:attribute name="border-style">hidden</xsl:attribute>
            </xsl:if>
        </xsl:if>
        <xsl:call-template name="process-common-attributes"/>
        <xsl:apply-templates select="html:col | html:colgroup" mode="preprocess"/>
        <xsl:apply-templates select="html:thead" mode="preprocess"/>
        <xsl:apply-templates select="html:tfoot" mode="preprocess"/>
        <xsl:choose>
            <xsl:when test="html:tbody">
                <xsl:apply-templates select="html:tbody" mode="preprocess"/>
            </xsl:when>
            <xsl:otherwise>
                <fo:table-body xsl:use-attribute-sets="tbody">
                    <xsl:apply-templates select="html:tr" mode="preprocess"/>
                </fo:table-body>
            </xsl:otherwise>
        </xsl:choose>
    </xsl:template>

    <xsl:template name="process-table-style">
        <xsl:param name="style"/>
        <!-- e.g., style="text-align: center; color: red"
    converted to text-align="center" color="red" -->
        <xsl:variable name="name"
                      select="normalize-space(substring-before($style, ':'))"/>
        <xsl:if test="$name">
            <xsl:variable name="value-and-rest"
                          select="normalize-space(substring-after($style, ':'))"/>
            <xsl:variable name="value">
                <xsl:choose>
                    <xsl:when test="contains($value-and-rest, ';')">
                        <xsl:value-of select="normalize-space(substring-before(
                    $value-and-rest, ';'))"/>
                    </xsl:when>
                    <xsl:otherwise>
                        <xsl:value-of select="$value-and-rest"/>
                    </xsl:otherwise>
                </xsl:choose>
            </xsl:variable>
            <xsl:choose>
                <xsl:when test="$name = 'border'">
                    <xsl:attribute name="border">
                        <xsl:value-of select="$value"/>
                    </xsl:attribute>
                </xsl:when>
                <xsl:when test="$name = 'border-style'">
                    <xsl:attribute name="border-style">
                        <xsl:value-of select="$value"/>
                    </xsl:attribute>
                </xsl:when>
                <xsl:when test="$name = 'border-color'">
                    <xsl:attribute name="border-color">
                        <xsl:value-of select="$value"/>
                    </xsl:attribute>
                </xsl:when>
            </xsl:choose>
        </xsl:if>
        <xsl:variable name="rest"
                      select="normalize-space(substring-after($style, ';'))"/>
        <xsl:if test="$rest">
            <xsl:call-template name="process-table-style">
                <xsl:with-param name="style" select="$rest"/>
            </xsl:call-template>
        </xsl:if>
    </xsl:template>

    <xsl:template match="html:caption" mode="transform">
        <fo:table-caption xsl:use-attribute-sets="table-caption">
            <xsl:call-template name="process-common-attributes"/>
            <fo:block>
                <xsl:apply-templates mode="preprocess"/>
            </fo:block>
        </fo:table-caption>
    </xsl:template>

    <xsl:template match="html:thead" mode="transform">
        <fo:table-header xsl:use-attribute-sets="thead">
            <xsl:call-template name="process-table-rowgroup"/>
        </fo:table-header>
    </xsl:template>

    <xsl:template match="html:tfoot" mode="transform">
        <fo:table-footer xsl:use-attribute-sets="tfoot">
            <xsl:call-template name="process-table-rowgroup"/>
        </fo:table-footer>
    </xsl:template>

    <xsl:template match="html:tbody" mode="transform">
        <fo:table-body xsl:use-attribute-sets="tbody">
            <xsl:call-template name="process-table-rowgroup"/>
        </fo:table-body>
    </xsl:template>

    <xsl:template name="process-table-rowgroup">
        <xsl:if test="ancestor::html:table[1]/@rules = 'groups'">
            <xsl:attribute name="border">0px solid</xsl:attribute>
        </xsl:if>
        <xsl:call-template name="process-common-attributes-and-children"/>
    </xsl:template>

    <xsl:template match="html:colgroup" mode="transform">
        <fo:table-column xsl:use-attribute-sets="table-column">
            <xsl:call-template name="process-table-column"/>
        </fo:table-column>
    </xsl:template>

    <xsl:template match="html:colgroup[html:col]" mode="transform">
        <xsl:apply-templates mode="preprocess"/>
    </xsl:template>

    <xsl:template match="html:col" mode="transform">
        <fo:table-column xsl:use-attribute-sets="table-column">
            <xsl:call-template name="process-table-column"/>
        </fo:table-column>
    </xsl:template>

    <xsl:template name="process-table-column">
        <xsl:if test="parent::html:colgroup">
            <xsl:call-template name="process-col-width">
                <xsl:with-param name="width" select="../@width"/>
            </xsl:call-template>
            <xsl:call-template name="process-cell-align">
                <xsl:with-param name="align" select="../@align"/>
            </xsl:call-template>
            <xsl:call-template name="process-cell-valign">
                <xsl:with-param name="valign" select="../@valign"/>
            </xsl:call-template>
        </xsl:if>
        <xsl:if test="@span">
            <xsl:attribute name="number-columns-repeated">
                <xsl:value-of select="@span"/>
            </xsl:attribute>
        </xsl:if>
        <xsl:call-template name="process-col-width">
            <xsl:with-param name="width" select="@width"/>
            <!-- it may override parent colgroup's width -->
        </xsl:call-template>
        <xsl:if test="ancestor::html:table[1]/@rules = 'cols'">
            <xsl:attribute name="border">0px solid</xsl:attribute>
        </xsl:if>
        <xsl:call-template name="process-common-attributes"/>
        <!-- this processes also align and valign -->
    </xsl:template>

    <xsl:template match="html:tr" mode="transform">
        <fo:table-row xsl:use-attribute-sets="tr">
            <xsl:call-template name="process-table-row"/>
        </fo:table-row>
    </xsl:template>

    <xsl:template match="html:tr[parent::html:table and html:th and not(html:td)]" mode="transform">
        <fo:table-row xsl:use-attribute-sets="tr" keep-with-next="1000">
            <xsl:call-template name="process-table-row"/>
        </fo:table-row>
    </xsl:template>

    <xsl:template name="process-table-row">
        <xsl:if test="ancestor::html:table[1]/@rules = 'rows'">
            <xsl:attribute name="border">0px solid</xsl:attribute>
        </xsl:if>
        <xsl:call-template name="process-common-attributes-and-children"/>
    </xsl:template>

    <xsl:template match="html:th" mode="transform">
        <fo:table-cell xsl:use-attribute-sets="th">
            <xsl:call-template name="process-table-cell"/>
        </fo:table-cell>
    </xsl:template>

    <xsl:template match="html:td" mode="transform">
        <fo:table-cell xsl:use-attribute-sets="td">
            <xsl:call-template name="process-table-cell"/>
        </fo:table-cell>
    </xsl:template>

    <xsl:template name="process-table-cell">
        <xsl:if test="@colspan">
            <xsl:attribute name="number-columns-spanned">
                <xsl:value-of select="@colspan"/>
            </xsl:attribute>
        </xsl:if>
        <xsl:if test="@rowspan">
            <xsl:attribute name="number-rows-spanned">
                <xsl:value-of select="@rowspan"/>
            </xsl:attribute>
        </xsl:if>
        <xsl:for-each select="ancestor::html:table[1]">
            <xsl:if test="(@border or @rules) and (@rules = 'all' or
            not(@rules) and not(@border = '0'))">
                <xsl:attribute name="border-style">inset</xsl:attribute>
            </xsl:if>
            <xsl:if test="@cellpadding">
                <xsl:attribute name="padding">
                    <xsl:choose>
                        <xsl:when test="contains(@cellpadding, '%')">
                            <xsl:value-of select="@cellpadding"/>
                        </xsl:when>
                        <xsl:otherwise>
                            <xsl:value-of select="@cellpadding"/>px</xsl:otherwise>
                    </xsl:choose>
                </xsl:attribute>
            </xsl:if>
        </xsl:for-each>
        <xsl:if test="not(@align or ../@align or
        ../parent::*[self::html:thead or self::html:tfoot or
        self::html:tbody]/@align) and
        ancestor::html:table[1]/*[self::html:col or
        self::html:colgroup]/descendant-or-self::*/@align">
            <xsl:attribute name="text-align">from-table-column()</xsl:attribute>
        </xsl:if>
        <xsl:if test="not(@valign or ../@valign or
        ../parent::*[self::html:thead or self::html:tfoot or
        self::html:tbody]/@valign) and
        ancestor::html:table[1]/*[self::html:col or
        self::html:colgroup]/descendant-or-self::*/@valign">
            <xsl:attribute name="display-align">from-table-column()</xsl:attribute>
            <xsl:attribute name="relative-align">from-table-column()</xsl:attribute>
        </xsl:if>
        <xsl:call-template name="process-common-attributes"/>
        <fo:block>
            <xsl:apply-templates mode="preprocess"/>
        </fo:block>
    </xsl:template>

    <xsl:template name="process-col-width">
        <xsl:param name="width"/>
        <xsl:if test="$width and $width != '0*'">
            <xsl:attribute name="column-width">
                <xsl:choose>
                    <xsl:when test="contains($width, '*')">
                        <xsl:text>proportional-column-width(</xsl:text>
                        <xsl:value-of select="substring-before($width, '*')"/>
                        <xsl:text>)</xsl:text>
                    </xsl:when>
                    <xsl:when test="contains($width, '%')">
                        <xsl:value-of select="$width"/>
                    </xsl:when>
                    <xsl:otherwise>
                        <xsl:value-of select="$width"/>px</xsl:otherwise>
                </xsl:choose>
            </xsl:attribute>
        </xsl:if>
    </xsl:template>

    <xsl:template name="process-cell-align">
        <xsl:param name="align"/>
        <xsl:if test="$align">
            <xsl:attribute name="text-align">
                <xsl:choose>
                    <xsl:when test="$align = 'char'">
                        <xsl:choose>
                            <xsl:when test="$align/../@char">
                                <xsl:value-of select="$align/../@char"/>
                            </xsl:when>
                            <xsl:otherwise>
                                <xsl:value-of select="'.'"/>
                                <!-- todo: it should depend on xml:lang ... -->
                            </xsl:otherwise>
                        </xsl:choose>
                    </xsl:when>
                    <xsl:otherwise>
                        <xsl:value-of select="$align"/>
                    </xsl:otherwise>
                </xsl:choose>
            </xsl:attribute>
        </xsl:if>
    </xsl:template>

    <xsl:template name="process-cell-valign">
        <xsl:param name="valign"/>
        <xsl:if test="$valign">
            <xsl:attribute name="display-align">
                <xsl:choose>
                    <xsl:when test="$valign = 'middle'">center</xsl:when>
                    <xsl:when test="$valign = 'bottom'">after</xsl:when>
                    <xsl:when test="$valign = 'baseline'">auto</xsl:when>
                    <xsl:otherwise>before</xsl:otherwise>
                </xsl:choose>
            </xsl:attribute>
            <xsl:if test="$valign = 'baseline'">
                <xsl:attribute name="relative-align">baseline</xsl:attribute>
            </xsl:if>
        </xsl:if>
    </xsl:template>

    <!--=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-
    Inline-level
    =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-->

    <xsl:template match="html:b" mode="transform">
        <fo:inline xsl:use-attribute-sets="b">
            <xsl:call-template name="process-common-attributes-and-children"/>
        </fo:inline>
    </xsl:template>

    <xsl:template match="html:strong" mode="transform">
        <fo:inline xsl:use-attribute-sets="strong">
            <xsl:call-template name="process-common-attributes-and-children"/>
        </fo:inline>
    </xsl:template>

    <xsl:template match="html:strong//html:em | html:em//html:strong" mode="transform">
        <fo:inline xsl:use-attribute-sets="strong-em">
            <xsl:call-template name="process-common-attributes-and-children"/>
        </fo:inline>
    </xsl:template>

    <xsl:template match="html:i" mode="transform">
        <fo:inline xsl:use-attribute-sets="i">
            <xsl:call-template name="process-common-attributes-and-children"/>
        </fo:inline>
    </xsl:template>

    <xsl:template match="html:cite" mode="transform">
        <fo:inline xsl:use-attribute-sets="cite">
            <xsl:call-template name="process-common-attributes-and-children"/>
        </fo:inline>
    </xsl:template>

    <xsl:template match="html:em" mode="transform">
        <fo:inline xsl:use-attribute-sets="em">
            <xsl:call-template name="process-common-attributes-and-children"/>
        </fo:inline>
    </xsl:template>

    <xsl:template match="html:var" mode="transform">
        <fo:inline xsl:use-attribute-sets="var">
            <xsl:call-template name="process-common-attributes-and-children"/>
        </fo:inline>
    </xsl:template>

    <xsl:template match="html:dfn" mode="transform">
        <fo:inline xsl:use-attribute-sets="dfn">
            <xsl:call-template name="process-common-attributes-and-children"/>
        </fo:inline>
    </xsl:template>

    <xsl:template match="html:tt" mode="transform">
        <fo:inline xsl:use-attribute-sets="tt">
            <xsl:call-template name="process-common-attributes-and-children"/>
        </fo:inline>
    </xsl:template>

    <xsl:template match="html:code" mode="transform">
        <fo:inline xsl:use-attribute-sets="code">
            <xsl:call-template name="process-common-attributes-and-children"/>
        </fo:inline>
    </xsl:template>

    <xsl:template match="html:kbd" mode="transform">
        <fo:inline xsl:use-attribute-sets="kbd">
            <xsl:call-template name="process-common-attributes-and-children"/>
        </fo:inline>
    </xsl:template>

    <xsl:template match="html:samp" mode="transform">
        <fo:inline xsl:use-attribute-sets="samp">
            <xsl:call-template name="process-common-attributes-and-children"/>
        </fo:inline>
    </xsl:template>

    <xsl:template match="html:big" mode="transform">
        <fo:inline xsl:use-attribute-sets="big">
            <xsl:call-template name="process-common-attributes-and-children"/>
        </fo:inline>
    </xsl:template>

    <xsl:template match="html:small" mode="transform">
        <fo:inline xsl:use-attribute-sets="small">
            <xsl:call-template name="process-common-attributes-and-children"/>
        </fo:inline>
    </xsl:template>

    <xsl:template match="html:sub" mode="transform">
        <fo:inline xsl:use-attribute-sets="sub">
            <xsl:call-template name="process-common-attributes-and-children"/>
        </fo:inline>
    </xsl:template>

    <xsl:template match="html:sup" mode="transform">
        <fo:inline xsl:use-attribute-sets="sup">
            <xsl:call-template name="process-common-attributes-and-children"/>
        </fo:inline>
    </xsl:template>

    <xsl:template match="html:s" mode="transform">
        <fo:inline xsl:use-attribute-sets="s">
            <xsl:call-template name="process-common-attributes-and-children"/>
        </fo:inline>
    </xsl:template>

    <xsl:template match="html:strike" mode="transform">
        <fo:inline xsl:use-attribute-sets="strike">
            <xsl:call-template name="process-common-attributes-and-children"/>
        </fo:inline>
    </xsl:template>

    <xsl:template match="html:del" mode="transform">
        <fo:inline xsl:use-attribute-sets="del">
            <xsl:call-template name="process-common-attributes-and-children"/>
        </fo:inline>
    </xsl:template>

    <xsl:template match="html:u" mode="transform">
        <fo:inline xsl:use-attribute-sets="u">
            <xsl:call-template name="process-common-attributes-and-children"/>
        </fo:inline>
    </xsl:template>

    <xsl:template match="html:ins" mode="transform">
        <fo:inline xsl:use-attribute-sets="ins">
            <xsl:call-template name="process-common-attributes-and-children"/>
        </fo:inline>
    </xsl:template>

    <xsl:template match="html:abbr" mode="transform">
        <fo:inline xsl:use-attribute-sets="abbr">
            <xsl:call-template name="process-common-attributes-and-children"/>
        </fo:inline>
    </xsl:template>

    <xsl:template match="html:acronym" mode="transform">
        <fo:inline xsl:use-attribute-sets="acronym">
            <xsl:call-template name="process-common-attributes-and-children"/>
        </fo:inline>
    </xsl:template>

    <xsl:template match="html:span" mode="transform">
        <fo:inline>
            <xsl:call-template name="process-common-attributes-and-children"/>
        </fo:inline>
    </xsl:template>

    <xsl:template match="html:label" mode="transform">
        <fo:inline xsl:use-attribute-sets="label">
            <xsl:call-template name="process-common-attributes-and-children"/>
        </fo:inline>
    </xsl:template>

    <xsl:template match="html:span[@dir]" mode="transform">
        <fo:bidi-override direction="{@dir}" unicode-bidi="embed">
            <xsl:call-template name="process-common-attributes-and-children"/>
        </fo:bidi-override>
    </xsl:template>

    <xsl:template match="html:span[@style and contains(@style, 'writing-mode')]" mode="transform">
        <fo:inline-container alignment-baseline="central"
                             text-indent="0pt"
                             last-line-end-indent="0pt"
                             start-indent="0pt"
                             end-indent="0pt"
                             text-align="center"
                             text-align-last="center">
            <xsl:call-template name="process-common-attributes"/>
            <fo:block wrap-option="no-wrap" line-height="1">
                <xsl:apply-templates mode="preprocess"/>
            </fo:block>
        </fo:inline-container>
    </xsl:template>

    <xsl:template match="html:bdo" mode="transform">
        <fo:bidi-override direction="{@dir}" unicode-bidi="bidi-override">
            <xsl:call-template name="process-common-attributes-and-children"/>
        </fo:bidi-override>
    </xsl:template>

    <xsl:template match="html:br" mode="transform">
        <fo:block>
            <xsl:call-template name="process-common-attributes"/>
        </fo:block>
    </xsl:template>

    <xsl:template match="html:q" mode="transform">
        <fo:inline xsl:use-attribute-sets="q">
            <xsl:call-template name="process-common-attributes"/>
            <xsl:choose>
                <xsl:when test="lang('ja')">
                    <xsl:text>&#x300C;</xsl:text>
                    <xsl:apply-templates mode="preprocess"/>
                    <xsl:text>&#x300D;</xsl:text>
                </xsl:when>
                <xsl:otherwise>
                    <!-- lang('en') -->
                    <xsl:text>&#x201C;</xsl:text>
                    <xsl:apply-templates mode="preprocess"/>
                    <xsl:text>&#x201D;</xsl:text>
                    <!-- todo: other languages ...-->
                </xsl:otherwise>
            </xsl:choose>
        </fo:inline>
    </xsl:template>

    <xsl:template match="html:q//html:q" mode="transform">
        <fo:inline xsl:use-attribute-sets="q-nested">
            <xsl:call-template name="process-common-attributes"/>
            <xsl:choose>
                <xsl:when test="lang('ja')">
                    <xsl:text>&#x300E;</xsl:text>
                    <xsl:apply-templates mode="preprocess"/>
                    <xsl:text>&#x300F;</xsl:text>
                </xsl:when>
                <xsl:otherwise>
                    <!-- lang('en') -->
                    <xsl:text>&#x2018;</xsl:text>
                    <xsl:apply-templates mode="preprocess"/>
                    <xsl:text>&#x2019;</xsl:text>
                </xsl:otherwise>
            </xsl:choose>
        </fo:inline>
    </xsl:template>

    <!--=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-
    Image
    =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-->

    <xsl:template match="html:img" mode="transform">
        <fo:external-graphic xsl:use-attribute-sets="img">
            <xsl:call-template name="process-img"/>
        </fo:external-graphic>
    </xsl:template>

    <xsl:template match="html:img[ancestor::html:a/@href]" mode="transform">
        <fo:external-graphic xsl:use-attribute-sets="img-link">
            <xsl:call-template name="process-img"/>
        </fo:external-graphic>
    </xsl:template>

    <xsl:template name="process-img">
        <xsl:param name="dpi-resampling">0.75</xsl:param>
        <xsl:variable name="style" select="concat(';', translate(normalize-space(@style), ' ', ''))"/>
        <xsl:variable name="has-width" select="@width or contains($style, ';width:')"/>
        <xsl:variable name="has-height" select="@height or contains($style, ';height:')"/>
        <xsl:variable name="width">
            <xsl:choose>
                <xsl:when test="contains($style, ';width:')">
                    <xsl:call-template name="get-style-value">
                        <xsl:with-param name="style" select="$style"/>
                        <xsl:with-param name="property" select="'width'"/>
                    </xsl:call-template>
                </xsl:when>
                <xsl:when test="@width">
                    <xsl:value-of select="@width"/>
                </xsl:when>
            </xsl:choose>
        </xsl:variable>
        <xsl:variable name="height">
            <xsl:choose>
                <xsl:when test="contains($style, ';height:')">
                    <xsl:call-template name="get-style-value">
                        <xsl:with-param name="style" select="$style"/>
                        <xsl:with-param name="property" select="'height'"/>
                    </xsl:call-template>
                </xsl:when>
                <xsl:when test="@height">
                    <xsl:value-of select="@height"/>
                </xsl:when>
            </xsl:choose>
        </xsl:variable>
        <!-- If both height and width are specified, allow the image to be transformed to a different aspect ration -->
        <!-- Disabled, since images overlowing only one of the dimensions (e.g. larger width, but good height) will be stretched.
        <xsl:if test="$has-height and $has-width"><xsl:attribute name="scaling">non-uniform</xsl:attribute></xsl:if>-->
        <!-- Allow images to be resized -->
        <!-- DON'T try to correct DPI on images without specified dimensions! -->
        <xsl:attribute name="content-height">scale-to-fit</xsl:attribute>
        <xsl:attribute name="content-width">scale-to-fit</xsl:attribute>
        <!-- Min and max width -->
        <xsl:attribute name="inline-progression-dimension.minimum">auto</xsl:attribute>
        <xsl:attribute name="inline-progression-dimension.maximum">100%</xsl:attribute>
        <!-- Min and max height -->
        <xsl:attribute name="block-progression-dimension.minimum">auto</xsl:attribute>
        <xsl:attribute name="block-progression-dimension.maximum">700px</xsl:attribute>
        <!-- Scale px lengths in order to get from 72 to 96 DPI -->
        <xsl:if test="$has-width">
            <!-- Preserve the original unit -->
            <xsl:variable name="unit"><xsl:value-of select="translate($width, '01234567890.', '')"/></xsl:variable>
            <!-- Extract the original dimension without the unit -->
            <xsl:variable name="width-no-unit">
                <xsl:choose>
                    <xsl:when test="$unit = 'px' or $unit = ''">
                        <xsl:value-of select="number(translate($width, 'pxtcminem%', '')) * $dpi-resampling"/>
                    </xsl:when>
                    <xsl:otherwise>
                        <xsl:value-of select="number(translate($width, 'pxtcminem%', ''))"/>
                    </xsl:otherwise>
                </xsl:choose>
            </xsl:variable>
            <xsl:attribute name="inline-progression-dimension.optimum"><xsl:value-of select="$width-no-unit"/><xsl:value-of select="$unit"/></xsl:attribute>
        </xsl:if>
        <!-- There's a bug in FOP 1.0, specifying a height as percentage makes the image invisible. -->
        <xsl:if test="$has-height and translate($height, '01234567890. ', '') != '%'">
            <xsl:variable name="unit"><xsl:value-of select="translate($height, '01234567890. ', '')"/></xsl:variable>
            <xsl:variable name="height-no-unit">
                <xsl:choose>
                    <xsl:when test="$unit = 'px' or $unit = ''">
                        <xsl:value-of select="number(translate($height, 'pxtcminem% ', '')) * $dpi-resampling"/>
                    </xsl:when>
                    <xsl:otherwise>
                        <xsl:value-of select="number(translate($height, 'pxtcminem% ', ''))"/>
                    </xsl:otherwise>
                </xsl:choose>
            </xsl:variable>
            <xsl:attribute name="block-progression-dimension.optimum"><xsl:value-of select="$height-no-unit"/><xsl:value-of select="$unit"/></xsl:attribute>
        </xsl:if>
        <!-- The actual image reference -->
        <xsl:attribute name="src">
            <xsl:text>url('</xsl:text>
            <xsl:choose>
                <xsl:when test="contains(@src, '?') and starts-with(@src, 'file:')">
                    <xsl:value-of select="substring-before(@src, '?')"/>
                </xsl:when>
                <xsl:otherwise>
                    <xsl:value-of select="@src"/>
                </xsl:otherwise>
            </xsl:choose>
            <xsl:text>')</xsl:text>
        </xsl:attribute>
        <xsl:if test="@alt">
            <xsl:attribute name="role">
                <xsl:value-of select="@alt"/>
            </xsl:attribute>
        </xsl:if>
        <xsl:if test="@border">
            <xsl:attribute name="border">
                <xsl:value-of select="@border"/>px solid</xsl:attribute>
        </xsl:if>
        <xsl:call-template name="process-common-attributes"/>
    </xsl:template>

    <xsl:template match="html:object" mode="transform">
        <xsl:apply-templates mode="preprocess"/>
    </xsl:template>

    <!-- These elements don't have an equivalent in XSL-FO, just ignore them -->
    <xsl:template match="html:param" mode="preprocess"/>
    <xsl:template match="html:map" mode="preprocess"/>
    <xsl:template match="html:area" mode="preprocess"/>
    <xsl:template match="html:input" mode="preprocess"/>
    <xsl:template match="html:select" mode="preprocess"/>
    <xsl:template match="html:optgroup" mode="preprocess"/>
    <xsl:template match="html:option" mode="preprocess"/>
    <xsl:template match="html:textarea" mode="preprocess"/>
    <xsl:template match="html:legend" mode="preprocess"/>
    <xsl:template match="html:button" mode="preprocess"/>

    <!--=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-
    Link
    =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-->

    <xsl:template match="html:a" mode="transform">
        <fo:inline>
            <xsl:call-template name="process-common-attributes-and-children"/>
        </fo:inline>
    </xsl:template>

    <xsl:template match="html:a[@href]" mode="transform">
        <fo:basic-link xsl:use-attribute-sets="a-link">
            <xsl:call-template name="process-a-link"/>
        </fo:basic-link>
    </xsl:template>

    <xsl:template name="process-a-link">
        <xsl:call-template name="process-common-attributes"/>
        <xsl:choose>
            <xsl:when test="@href = '#' or @href = ''">
                <xsl:attribute name="internal-destination">xwikimaincontainer</xsl:attribute>
            </xsl:when>
            <xsl:when test="starts-with(@href,'#')">
                <xsl:attribute name="internal-destination">
                    <xsl:value-of select="substring-after(@href,'#')"/>
                </xsl:attribute>
            </xsl:when>
            <xsl:otherwise>
                <xsl:attribute name="external-destination">
                    <xsl:text>url('</xsl:text>
                    <xsl:value-of select="@href"/>
                    <xsl:text>')</xsl:text>
                </xsl:attribute>
            </xsl:otherwise>
        </xsl:choose>
        <xsl:if test="@title">
            <xsl:attribute name="role">
                <xsl:value-of select="@title"/>
            </xsl:attribute>
        </xsl:if>
        <xsl:apply-templates mode="preprocess"/>
    </xsl:template>

    <!--=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-
    Ruby
    =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-->

    <xsl:template match="html:ruby" mode="transform">
        <fo:inline-container alignment-baseline="central"
                             block-progression-dimension="1em"
                             text-indent="0pt"
                             last-line-end-indent="0pt"
                             start-indent="0pt"
                             end-indent="0pt"
                             text-align="center"
                             text-align-last="center">
            <xsl:call-template name="process-common-attributes"/>
            <fo:block font-size="50%"
                      wrap-option="no-wrap"
                      line-height="1"
                      space-before.conditionality="retain"
                      space-before="-1.1em"
                      space-after="0.1em"
                      role="html:rt">
                <xsl:for-each select="html:rt | html:rtc[1]/html:rt">
                    <xsl:call-template name="process-common-attributes"/>
                    <xsl:apply-templates mode="preprocess"/>
                </xsl:for-each>
            </fo:block>
            <fo:block wrap-option="no-wrap" line-height="1" role="html:rb">
                <xsl:for-each select="html:rb | html:rbc[1]/html:rb">
                    <xsl:call-template name="process-common-attributes"/>
                    <xsl:apply-templates mode="preprocess"/>
                </xsl:for-each>
            </fo:block>
            <xsl:if test="html:rtc[2]/html:rt">
                <fo:block font-size="50%"
                          wrap-option="no-wrap"
                          line-height="1"
                          space-before="0.1em"
                          space-after.conditionality="retain"
                          space-after="-1.1em"
                          role="html:rt">
                    <xsl:for-each select="html:rt | html:rtc[2]/html:rt">
                        <xsl:call-template name="process-common-attributes"/>
                        <xsl:apply-templates mode="preprocess"/>
                    </xsl:for-each>
                </fo:block>
            </xsl:if>
        </fo:inline-container>
    </xsl:template>

    <!-- Preserve inline SVG images as <fo:instream-foreign-object>s -->
    <xsl:template match="html:*/svg:svg" mode="transform">
        <fo:instream-foreign-object
          content-width="scale-to-fit" content-height="scale-to-fit"
          inline-progression-dimension.minimum="auto" inline-progression-dimension.maximum="100%"
          block-progression-dimension.minimum="auto" block-progression-dimension.maximum="700px">
            <xsl:copy>
                <xsl:apply-templates select="@*|node()" mode="svg"/>
            </xsl:copy>
        </fo:instream-foreign-object>
    </xsl:template>
    <xsl:template match="@*|node()" mode="svg">
        <xsl:copy>
            <xsl:apply-templates select="@*|node()" mode="svg"/>
        </xsl:copy>
    </xsl:template>
</xsl:stylesheet>
