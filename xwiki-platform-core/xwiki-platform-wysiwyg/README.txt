Follow this steps to integrate the Alfresco plugin on XE 2.7.2:

1. Copy the following jars to WEB-INF/lib
http://search.maven.org/remotecontent?filepath=org/apache/httpcomponents/httpclient/4.0.2/httpclient-4.0.2.jar (new)
http://search.maven.org/remotecontent?filepath=org/apache/httpcomponents/httpcore/4.0.1/httpcore-4.0.1.jar (new)
http://maven.xwiki.org/externals/com/google/gwt/gwt-servlet/2.3.0-xwiki-20110506/gwt-servlet-2.3.0-xwiki-20110506.jar (remove existing jar)
xwiki-platform-wysiwyg-client-3.2-SNAPSHOT-shared.jar (remove existing jar xwiki-web-gwt-wysiwyg-client-2.7.2-shared.jar)
xwiki-platform-wysiwyg-plugin-alfresco-server-3.2-SNAPSHOT.jar (new)

2. Update resources/js/xwiki/wysiwyg/xwe directory (delete + copy)

3. Enable Alfresco WYSIWYG editor plugin. Edit templates/macros.vm and replace this line:

#set($ok = $parameters.put('plugins', $xwiki.getXWikiPreference('wysiwyg.plugins', "submit line separator embed text valign list indent history format symbol link image table macro import#if($full && $request.sync) sync#end")
))

with:

#set($ok = $parameters.put('plugins', $xwiki.getXWikiPreference('wysiwyg.plugins', "submit line separator embed text valign list indent history format symbol link image table macro import alfresco#if($full && $request.sync) s
ync#end")))

4. Add Alfresco menu entries. Edit templates/macros.vm and replace this line:

#set($ok = $parameters.put('menu', $xwiki.getXWikiPreference('wysiwyg.menu', 'link image table macro import')))

with:

#set($ok = $parameters.put('menu', $xwiki.getXWikiPreference('wysiwyg.menu', '[{"feature": "link", "subMenu":["linkEdit", "linkRemove", "linkWikiPage", "linkAttachment", "|", "linkWebPage", "linkEmail", "alfrescoLink"]}, {"fe
ature":"image", "subMenu":["imageInsertAttached", "imageInsertURL", "imageEdit", "alfrescoImage", "imageRemove"]}, {"feature":"table", "subMenu":["inserttable", "insertcolbefore", "insertcolafter", "deletecol", "|", "insertro
wbefore", "insertrowafter", "deleterow", "|", "deletetable"]}, {"feature":"macro", "subMenu":["macroInsert", "macroEdit", "|", "macroRefresh", "|", "macroCollapse", "macroExpand"]}, {"feature":"import", "subMenu":["importOffi
ce"]}]')))

5. Edit xwiki.properties and add the following configuration properties:

alfresco.serverURL=http://localhost:8080 (required)
alfresco.username=Admin (required only for basic and ticket authentication)
alfresco.password=admin (required only for basic and ticket authentication)
alfresco.defaultNodeRef=workspace://SpacesStore/e47e3e7d-c345-4558-97f7-1e846453dd4b (required; use the Alfresco Node Browser or the Details View to find the node reference of the folder that you want to be displayed by default)
alfresco.authenticatorHint=siteMinder (optional, defaults to "siteMinder"; if this doesn't work, try "ticket" or "basic", for which you need to configure the user name and password)
