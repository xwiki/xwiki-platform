Follow this steps to integrate the Alfresco plugin on XE:

1. Copy the following jars to WEB-INF/lib

xwiki-platform-wysiwyg-client-x.y-SNAPSHOT-shared.jar (overwrite existing jar)

xwiki-platform-wysiwyg-server-x.y-SNAPSHOT.jar (overwrite existing jar)

xwiki-platform-wysiwyg-plugin-alfresco-server-x.y-SNAPSHOT.jar (new)

2. Update resources/js/xwiki/wysiwyg/xwe directory (delete the existing folder and copy the new one from the zip; don't overwrite!)

3. Enable "alfresco" WYSIWYG editor plugin from the administration section (don't forget to save the administration section!).

4. Add Alfresco menu entries. For this you have to edit XWiki.WysiwygEditorConfig page in object mode and set the value of the menu property to:

[{"feature": "link", "subMenu":["linkEdit", "linkRemove", "linkWikiPage", "linkAttachment", "|", "linkWebPage", "linkEmail", "alfrescoLink"]}, {"feature":"image", "subMenu":["imageInsertAttached", "imageInsertURL", "imageEdit", "alfrescoImage", "imageRemove"]}, {"feature":"table", "subMenu":["inserttable", "insertcolbefore", "insertcolafter", "deletecol", "|", "insertrowbefore", "insertrowafter", "deleterow", "|", "deletetable"]}, {"feature":"macro", "subMenu":["macroInsert", "macroEdit", "|", "macroRefresh", "|", "macroCollapse", "macroExpand"]}, {"feature":"import", "subMenu":["importOffice"]}]

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