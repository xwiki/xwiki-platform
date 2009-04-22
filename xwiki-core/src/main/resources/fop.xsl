<?xml version="1.0" encoding="iso-8859-1"?>
<xsl:stylesheet version="1.0"
    xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
    xmlns:fo="http://www.w3.org/1999/XSL/Format"
    xmlns:html="http://www.w3.org/1999/xhtml">

    <!-- try to achieve as much output as possible with broken Fop-0.20.3rc -->

    <xsl:output method="xml"
        />

    <xsl:template match="*">
        <xsl:element name="{name(.)}">
            <xsl:copy-of select="@*"/>
            <xsl:apply-templates/>
        </xsl:element>
    </xsl:template>

    <!-- ignore fo:table-and-caption because Fop won't render tables otherwise -->
    <xsl:template match="fo:table-and-caption">
        <xsl:apply-templates/>
    </xsl:template>

    <!-- ignore fo:table-caption because surrounding fo:table-and-caption was skipped -->
    <xsl:template match="fo:table-caption">
        <xsl:apply-templates mode="fo:table-caption"/>
    </xsl:template>

    <!-- fetch @id of skipped surrounding fo:table-and-caption for linking -->
    <xsl:template match="fo:block" mode="fo:table-caption">
        <xsl:element name="{name(.)}">
            <xsl:attribute name="id">
                <xsl:value-of select="ancestor::fo:table-and-caption/@id"/>
            </xsl:attribute>
            <xsl:copy-of select="@*"/>
            <xsl:apply-templates/>
        </xsl:element>
    </xsl:template>


    <xsl:template name="recur">
        <xsl:param name="n" select="number(1)"/>
        <!-- 1-based rather than 0-based is more XML-ian :-) -->
        <xsl:param name="max"/>
<!-- Disabling this, as it seems that recent versions of FOP work very well without it, and it is causing problems when
     colgroup attributes are involved.
        <xsl:element name="fo:table-column">
            <xsl:attribute name="column-width">
                <xsl:text>proportional-column-width(1)</xsl:text>
            </xsl:attribute>
        </xsl:element>
-->
        <!--
Note that changing this template to iterate downwards is a little more
efficient and correct, IMO. Then you have no $max, you specify $n as
your highest value, and the test is $n &gt; 0 and encases this whole
section rather than just the recursive call-template.
        -->

        <xsl:if test="$n &lt; $max">
            <xsl:call-template name="recur">
                <xsl:with-param name="max" select="$max"/>
                <xsl:with-param name="n" select="1 + $n"/>
            </xsl:call-template>
        </xsl:if>
    </xsl:template>

    <!-- Let's try to add table-column to fo:table -->
    <xsl:template match="fo:table">
        <xsl:variable name="maxcols">
            <xsl:for-each select="fo:table-body/fo:table-row">
                <xsl:sort select="count(fo:table-cell)"
                    data-type="number"
                    order="descending" />
                <xsl:if test="position() = 1">
                    <xsl:value-of select="number(count(fo:table-cell))" />
                </xsl:if>
            </xsl:for-each>
        </xsl:variable>
        <xsl:element name="{name(.)}">
            <xsl:attribute name="border">
                <xsl:value-of select="@border"/>
            </xsl:attribute>
            <xsl:attribute name="border-style">
                <xsl:value-of select="@border-style"/>
            </xsl:attribute>
            <xsl:attribute name="border-color">
                <xsl:value-of select="@border-color"/>
            </xsl:attribute>
            <xsl:call-template name="recur">
            <xsl:with-param name="max" select="$maxcols"/>
            </xsl:call-template>
            <xsl:copy-of select="@*"/>
            <xsl:apply-templates/>
        </xsl:element>
    </xsl:template>

    <!-- a clever idea that doesn't quite work. fop 0.20.1 doesn't understand % -->
    <!-- and fop 0.20.2 doesn't work for me at all... -->
    <!-- mod. by smaier -->
    <xsl:template match="fo:table-column">
        <xsl:element name="{name(.)}">
            <xsl:if test="not(@column-width)">
                <xsl:attribute name="column-width">
                    <xsl:text>proportional-column-width(1)</xsl:text>
                </xsl:attribute>
            </xsl:if>
            <xsl:copy-of select="@*"/>
            <xsl:apply-templates/>
        </xsl:element>
    </xsl:template>
</xsl:stylesheet>
