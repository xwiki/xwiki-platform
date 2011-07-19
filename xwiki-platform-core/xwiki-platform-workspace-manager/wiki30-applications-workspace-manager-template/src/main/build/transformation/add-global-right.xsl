<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">

<xsl:output method="xml" encoding="ISO-8859-1"/>

<!-- Name of the page where the object will be located. (* mandatory) -->
<xsl:param name="objectName"></xsl:param>
<!-- Comma separated list of groups. -->
<xsl:param name="groups"></xsl:param>
<!-- Comma separated list of users. -->
<xsl:param name="users"></xsl:param>
<!-- Comma separated list of view, edit, delete, admin or programming rights. -->
<xsl:param name="levels"></xsl:param>
<!-- 1 or 0. 1 by default. -->
<xsl:param name="allow">1</xsl:param>

<xsl:template match="@* | node()">
   
   <xsl:copy>
   <xsl:apply-templates select="@* | node()" />
   </xsl:copy>
</xsl:template>

<xsl:template match="xwikidoc">
<!-- Determine maximum value for the rights number. -->
<xsl:variable name="maxNumber">
  <xsl:for-each select="//object[className = 'XWiki.XWikiGlobalRights']/number">
    <xsl:sort data-type="number" order="descending"/>
    <xsl:if test="position() = 1">
      <xsl:copy-of select="."/>
    </xsl:if>
  </xsl:for-each>
</xsl:variable>

<xsl:copy>
   <xsl:apply-templates select="@* | node()" />
<object>
<class>
<name>XWiki.XWikiGlobalRights</name>
<customClass></customClass>
<customMapping></customMapping>
<defaultViewSheet></defaultViewSheet>
<defaultEditSheet></defaultEditSheet>
<defaultWeb></defaultWeb>
<nameField></nameField>
<validationScript></validationScript>
<allow>
<defaultValue>1</defaultValue>
<disabled>0</disabled>
<displayFormType>select</displayFormType>
<displayType>allow</displayType>
<name>allow</name>
<number>4</number>
<prettyName>Allow/Deny</prettyName>
<unmodifiable>0</unmodifiable>
<classType>com.xpn.xwiki.objects.classes.BooleanClass</classType>
</allow>
<groups>
<cache>0</cache>
<disabled>0</disabled>
<displayType>select</displayType>
<multiSelect>1</multiSelect>
<name>groups</name>
<number>1</number>
<prettyName>Groups</prettyName>
<relationalStorage>0</relationalStorage>
<separator> </separator>
<size>5</size>
<unmodifiable>0</unmodifiable>
<usesList>1</usesList>
<classType>com.xpn.xwiki.objects.classes.GroupsClass</classType>
</groups>
<levels>
<cache>0</cache>
<disabled>0</disabled>
<displayType>select</displayType>
<multiSelect>1</multiSelect>
<name>levels</name>
<number>2</number>
<prettyName>Levels</prettyName>
<relationalStorage>0</relationalStorage>
<separator> </separator>
<size>3</size>
<unmodifiable>0</unmodifiable>
<classType>com.xpn.xwiki.objects.classes.LevelsClass</classType>
</levels>
<users>
<cache>0</cache>
<disabled>0</disabled>
<displayType>select</displayType>
<multiSelect>1</multiSelect>
<name>users</name>
<number>3</number>
<prettyName>Users</prettyName>
<relationalStorage>0</relationalStorage>
<separator> </separator>
<size>5</size>
<unmodifiable>0</unmodifiable>
<usesList>1</usesList>
<classType>com.xpn.xwiki.objects.classes.UsersClass</classType>
</users>
</class>
<name><xsl:value-of select="$objectName"/></name>
<number>
  <!-- If a max value exists, use it, otherwise use 0. -->
  <xsl:choose>
    <xsl:when test="$maxNumber &gt; -1">
      <xsl:value-of select="$maxNumber + 1"/>
    </xsl:when>
    <xsl:otherwise>0</xsl:otherwise>
  </xsl:choose>
</number>
<className>XWiki.XWikiGlobalRights</className>
<property>
<users><xsl:value-of select="$users"/></users>
</property>
<property>
<groups><xsl:value-of select="$groups"/></groups>
</property>
<property>
<levels><xsl:value-of select="$levels"/></levels>
</property>
<property>
<allow><xsl:value-of select="$allow"/></allow>
</property>
</object>
</xsl:copy>
</xsl:template>

</xsl:stylesheet>