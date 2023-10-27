<?xml version="1.1" encoding="UTF-8"?>

<!--
 * See the NOTICE file distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
-->

<xwikidoc version="1.5" reference="RTFrontend.ConvertHTML" locale="">
  <web>RTFrontend</web>
  <name>ConvertHTML</name>
  <language/>
  <defaultLanguage/>
  <translation>0</translation>
  <creator>xwiki:XWiki.Admin</creator>
  <parent>xwiki:XWiki.WebHome</parent>
  <author>xwiki:XWiki.Admin</author>
  <contentAuthor>xwiki:XWiki.Admin</contentAuthor>
  <version>1.1</version>
  <title>Realtime HTML Converter</title>
  <comment/>
  <minorEdit>false</minorEdit>
  <syntaxId>xwiki/2.1</syntaxId>
  <hidden>true</hidden>
  <content>{{velocity wiki=false}}
## TODO: We should pass directly the document reference.
#set ($wiki = "$!request.getParameter('wiki')")
#set ($space = "$!request.getParameter('space')")
#set ($page = "$!request.getParameter('page')")
#set ($documentReference = $services.model.createDocumentReference($wiki, $space, $page))
#set ($document = $xwiki.getDocument($documentReference))
## FIXME: This requires programming rights!
#set ($discard = $xcontext.setDoc($document.document))
## The $source variable is used inside the content sheet.
#set ($source = {
  'content': $request.text,
  'syntax': $document.syntax,
  'documentReference': $documentReference
})
#if ($xcontext.action == 'get')
  ## Check that the CSRF token matches the user.
  #if (!$services.csrf.isTokenValid($request.form_token))
    $response.sendError(403, $services.localization.render('rtfFrontend.convertHtml.invalidCsrfToken'))
  #else
    ## FIXME: We shouldn't depend on CKEditor. This code should work independent of the configured WYSIWYG editor.
    $xwiki.getDocument('CKEditor.ContentSheet').getRenderedContent()
  #end
#end
{{/velocity}}</content>
</xwikidoc>
