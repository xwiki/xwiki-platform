<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">

<xsl:output method="xml" encoding="ISO-8859-1"/>

<xsl:template match="@* | node()">

   <xsl:copy>
   <xsl:apply-templates select="@* | node()" />
   </xsl:copy>
</xsl:template>

<xsl:template match="xwikidoc">
<xsl:copy>
   <xsl:apply-templates select="@* | node()" />
<object>
<name>XWiki.SearchSuggestConfig</name>
<number>6</number>
<className>XWiki.SearchSuggestSourceClass</className>
<guid>09634c83-a5f3-4574-9bd9-1670980d30a7</guid>
<property>
<activated>1</activated>
</property>
<property>
<highlight>0</highlight>
</property>
<property>
<icon>$xwiki.getSkinFile('icons/silk/chart_organisation_add.gif')</icon>
</property>
<property>
<name>Workspaces</name>
</property>
<property>
<query>XWiki.XWikiServerClass.wikiprettyname:__INPUT__* AND object:WorkspaceManager.WorkspaceClass</query>
</property>
<property>
<resultsNumber>3</resultsNumber>
</property>
<property>
<url>$xwiki.getURL("${xcontext.mainWikiName}:WorkspaceManager.WorkspacesSuggestLuceneService", 'get', 'outputSyntax=plain')</url>
</property>
</object>
</xsl:copy>
</xsl:template>

</xsl:stylesheet>