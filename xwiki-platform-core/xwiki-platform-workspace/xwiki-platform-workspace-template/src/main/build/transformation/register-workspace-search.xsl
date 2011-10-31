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
<class>
<name>XWiki.SearchSuggestSourceClass</name>
<customClass></customClass>
<customMapping></customMapping>
<defaultViewSheet></defaultViewSheet>
<defaultEditSheet></defaultEditSheet>
<defaultWeb></defaultWeb>
<nameField></nameField>
<validationScript></validationScript>
<activated>
<defaultValue></defaultValue>
<disabled>0</disabled>
<displayFormType>checkbox</displayFormType>
<displayType></displayType>
<name>activated</name>
<number>7</number>
<prettyName>activated</prettyName>
<unmodifiable>0</unmodifiable>
<validationMessage></validationMessage>
<validationRegExp></validationRegExp>
<classType>com.xpn.xwiki.objects.classes.BooleanClass</classType>
</activated>
<highlight>
<defaultValue></defaultValue>
<disabled>0</disabled>
<displayFormType>checkbox</displayFormType>
<displayType></displayType>
<name>highlight</name>
<number>6</number>
<prettyName>highlight</prettyName>
<unmodifiable>0</unmodifiable>
<validationMessage></validationMessage>
<validationRegExp></validationRegExp>
<classType>com.xpn.xwiki.objects.classes.BooleanClass</classType>
</highlight>
<icon>
<disabled>0</disabled>
<name>icon</name>
<number>5</number>
<picker>0</picker>
<prettyName>icon</prettyName>
<size>30</size>
<unmodifiable>0</unmodifiable>
<validationMessage></validationMessage>
<validationRegExp></validationRegExp>
<classType>com.xpn.xwiki.objects.classes.StringClass</classType>
</icon>
<name>
<disabled>0</disabled>
<name>name</name>
<number>1</number>
<picker>0</picker>
<prettyName>name</prettyName>
<size>30</size>
<unmodifiable>0</unmodifiable>
<validationMessage></validationMessage>
<validationRegExp></validationRegExp>
<classType>com.xpn.xwiki.objects.classes.StringClass</classType>
</name>
<query>
<disabled>0</disabled>
<name>query</name>
<number>3</number>
<picker>0</picker>
<prettyName>query</prettyName>
<size>30</size>
<unmodifiable>0</unmodifiable>
<validationMessage></validationMessage>
<validationRegExp></validationRegExp>
<classType>com.xpn.xwiki.objects.classes.StringClass</classType>
</query>
<resultsNumber>
<disabled>0</disabled>
<name>resultsNumber</name>
<number>4</number>
<picker>0</picker>
<prettyName>resultsNumber</prettyName>
<size>30</size>
<unmodifiable>0</unmodifiable>
<validationMessage></validationMessage>
<validationRegExp></validationRegExp>
<classType>com.xpn.xwiki.objects.classes.StringClass</classType>
</resultsNumber>
<url>
<disabled>0</disabled>
<name>url</name>
<number>2</number>
<picker>0</picker>
<prettyName>url</prettyName>
<size>30</size>
<unmodifiable>0</unmodifiable>
<validationMessage></validationMessage>
<validationRegExp></validationRegExp>
<classType>com.xpn.xwiki.objects.classes.StringClass</classType>
</url>
</class>
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
<icon>$xwiki.getSkinFile('icons/silk/chart_organisation.gif')</icon>
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