Follow this steps to integrate the Alfresco plugin on XE 2.7.2:

1. Copy the following jars to WEB-INF/lib

http://search.maven.org/remotecontent?filepath=org/apache/httpcomponents/httpclient/4.0.2/httpclient-4.0.2.jar (new)

http://search.maven.org/remotecontent?filepath=org/apache/httpcomponents/httpcore/4.0.1/httpcore-4.0.1.jar (new)

http://maven.xwiki.org/externals/com/google/gwt/gwt-servlet/2.3.0-xwiki-20110506/gwt-servlet-2.3.0-xwiki-20110506.jar (remove existing jar gwt-servlet-2.0.4.jar)

xwiki-platform-wysiwyg-client-3.2-SNAPSHOT-shared.jar (remove existing jar xwiki-web-gwt-wysiwyg-client-2.7.2-shared.jar)

xwiki-web-gwt-wysiwyg-server-2.7.2.jar (overwrite existing jar)

xwiki-platform-wysiwyg-plugin-alfresco-server-3.2-SNAPSHOT.jar (new)

2. Update resources/js/xwiki/wysiwyg/xwe directory (delete the existing folder and copy the new one from the zip; don't overwrite!)

3. Enable Alfresco WYSIWYG editor plugin. Edit templates/macros.vm and replace this line:

#set($ok = $parameters.put('plugins', $xwiki.getXWikiPreference('wysiwyg.plugins', "submit line separator embed text valign list indent history format symbol link image table macro import#if($full && $request.sync) sync#end")
))

with:

#set($ok = $parameters.put('plugins', $xwiki.getXWikiPreference('wysiwyg.plugins', "submit line separator embed text valign list indent history format symbol link image table macro import alfresco")))

4. Add Alfresco menu entries. Edit templates/macros.vm and replace this line:

#set($ok = $parameters.put('menu', $xwiki.getXWikiPreference('wysiwyg.menu', 'link image table macro import')))

with:

#set($ok = $parameters.put('menu', $xwiki.getXWikiPreference('wysiwyg.menu', '[{"feature": "link", "subMenu":["linkEdit", "linkRemove", "linkWikiPage", "linkAttachment", "|", "linkWebPage", "linkEmail", "alfrescoLink"]}, {"fe
ature":"image", "subMenu":["imageInsertAttached", "imageInsertURL", "imageEdit", "alfrescoImage", "imageRemove"]}, {"feature":"table", "subMenu":["inserttable", "insertcolbefore", "insertcolafter", "deletecol", "|", "insertro
wbefore", "insertrowafter", "deleterow", "|", "deletetable"]}, {"feature":"macro", "subMenu":["macroInsert", "macroEdit", "|", "macroRefresh", "|", "macroCollapse", "macroExpand"]}, {"feature":"import", "subMenu":["importOffi
ce"]}]')))

5. Edit xwiki.properties and add the following configuration properties:

## Required.
alfresco.serverURL=http://localhost:8080

## Required; use the Alfresco Node Browser or the Details View to find the node reference of the folder that you want to be displayed by default.
alfresco.defaultNodeRef=workspace://SpacesStore/e47e3e7d-c345-4558-97f7-1e846453dd4b

Optionally you can configure:

## Optional, defaults to "siteMinder"; if this doesn't work, try "ticket" or "basic", for which you need to configure the user name and password.
alfresco.authenticatorHint=siteMinder

## Required only for basic and ticket authentication.
alfresco.username=Admin

## Required only for basic and ticket authentication
alfresco.password=admin

6. Clear browser cache.