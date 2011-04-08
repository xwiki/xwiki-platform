<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="1.0"
    xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
    xmlns:fo="http://www.w3.org/1999/XSL/Format"
    xmlns:html="http://www.w3.org/1999/xhtml">

    <xsl:output method="xml"/>

    <xsl:template match="@*|node()">
        <xsl:copy>
            <xsl:apply-templates select="@*|node()"/>
        </xsl:copy>
    </xsl:template>

    <!-- Skip fo:table-and-caption because Fop won't render tables otherwise -->
    <xsl:template match="fo:table-and-caption">
        <xsl:apply-templates/>
    </xsl:template>

    <!-- Ignore fo:table-caption because it is not supported -->
    <xsl:template match="fo:table-caption" />
</xsl:stylesheet>
