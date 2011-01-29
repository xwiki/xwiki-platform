<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="1.0"
    xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
    xmlns:fo="http://www.w3.org/1999/XSL/Format"
    xmlns:html="http://www.w3.org/1999/xhtml">

    <xsl:output method="xml"/>

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
