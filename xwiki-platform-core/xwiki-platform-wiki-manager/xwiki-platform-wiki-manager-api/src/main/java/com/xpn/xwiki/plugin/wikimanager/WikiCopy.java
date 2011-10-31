/*
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
 */

package com.xpn.xwiki.plugin.wikimanager;

import java.io.IOException;
import java.text.MessageFormat;
import java.util.Collection;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xwiki.rendering.syntax.Syntax;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiAttachment;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.plugin.applicationmanager.ApplicationManagerPlugin;
import com.xpn.xwiki.plugin.applicationmanager.ApplicationManagerPluginApi;
import com.xpn.xwiki.plugin.applicationmanager.core.doc.objects.classes.XObjectDocument;
import com.xpn.xwiki.plugin.applicationmanager.core.plugin.XWikiPluginMessageTool;
import com.xpn.xwiki.plugin.applicationmanager.doc.XWikiApplication;
import com.xpn.xwiki.plugin.packaging.DocumentInfo;
import com.xpn.xwiki.plugin.packaging.PackageAPI;
import com.xpn.xwiki.plugin.wikimanager.doc.XWikiServer;
import com.xpn.xwiki.plugin.wikimanager.doc.XWikiServerClass;

/**
 * Utility classe used to fill a newly created wiki.
 * 
 * @version $Id$
 */
public final class WikiCopy
{
    /**
     * The logging tool.
     */
    protected static final Logger LOGGER = LoggerFactory.getLogger(WikiManager.class);

    /**
     * The message tool to use to generate error or comments.
     */
    private XWikiPluginMessageTool messageTool;

    // ////////////////////////////////////////////////////////////////////////////

    /**
     * @param messageTool the message tool
     */
    public WikiCopy(XWikiPluginMessageTool messageTool)
    {
        this.messageTool = messageTool;
    }

    /**
     * Get the {@link XWikiPluginMessageTool} to use with WikiManager.
     * 
     * @param context the XWiki context.
     * @return a translated strings manager.
     */
    public XWikiPluginMessageTool getMessageTool(XWikiContext context)
    {
        return this.messageTool != null ? this.messageTool : WikiManagerMessageTool.getDefault(context);
    }

    // ////////////////////////////////////////////////////////////////////////////
    // Utils

    /**
     * Get the documents for which copied document content will be replace by an #includeInContext(SourceDocument) or
     * #includeTopic(SourceDocument) macro call.
     * 
     * @param wiki the name of the wiki where to find the list of documents.
     * @param context the XWiki context.
     * @return a pair of list of documents names to include and list of documents names to link.
     * @throws XWikiException error when getting Applications descriptors where searched documents are listed.
     */
    private Collection<String>[] getDocsNames(String wiki, XWikiContext context) throws XWikiException
    {
        Collection<String>[] docsNames = new Collection[2];

        // Get applications manger
        ApplicationManagerPluginApi appmanager =
            (ApplicationManagerPluginApi) context.getWiki().getPluginApi(ApplicationManagerPlugin.PLUGIN_NAME, context);

        if (appmanager == null) {
            return null;
        }

        // //////////////////////////////////
        // Get documents to include or link

        String database = context.getDatabase();

        try {
            context.setDatabase(wiki);

            XWikiApplication rootApp = appmanager.getRootApplication();

            if (rootApp != null) {
                docsNames[0] = rootApp.getDocsNameToInclude(true);
                docsNames[1] = rootApp.getDocsNameToLink(true);
            } else {
                Collection<XWikiApplication> applications = appmanager.getApplicationDocumentList();
                docsNames[0] = XWikiApplication.getDocsNameToInclude(applications);
                docsNames[1] = XWikiApplication.getDocsNameToLink(applications);
            }
        } finally {
            context.setDatabase(database);
        }

        return docsNames;
    }

    /**
     * Copy all documents from <code>sourceWiki</code> wiki to <code>targetWiki</code> wiki.
     * <p>
     * It also take care of ApplicationManager descriptors "documents to include" and "documents to link".
     * </p>
     * 
     * @param sourceWiki the wiki from where to copy documents and get lists of "document to link" and "documents to
     *            copy".
     * @param targetWiki the wiki where to copy documents.
     * @param comment the comment to use when saving documents.
     * @param context the XWiki context.
     * @throws XWikiException error when:
     *             <ul>
     *             <li>copying on of the source wiki to target wiki.</li>
     *             <li>or getting documents to include.</li>
     *             <li>or getting documents to link.</li>
     *             </ul>
     */
    public void copyWiki(String sourceWiki, String targetWiki, String comment, XWikiContext context)
        throws XWikiException
    {
        XWiki xwiki = context.getWiki();

        // Copy all the wiki
        xwiki.copyWiki(sourceWiki, targetWiki, null, context);

        String database = context.getDatabase();
        try {
            context.setDatabase(targetWiki);

            Collection<String>[] docsNames = getDocsNames(sourceWiki, context);

            if (docsNames != null) {
                Object[] includeFormatParams = new Object[] {sourceWiki, XObjectDocument.WIKI_SPACE_SEPARATOR, null};

                // Replace documents contents to include
                for (Object item : docsNames[0]) {
                    String docFullName = (String) item;
                    XWikiDocument targetDoc = xwiki.getDocument(docFullName, context);

                    includeFormatParams[2] = docFullName;
                    targetDoc.setContent(MessageFormat.format("#includeInContext(\"{0}{1}{2}\")", includeFormatParams));
                    targetDoc.setSyntax(Syntax.XWIKI_1_0);
                    xwiki.saveDocument(targetDoc, context);
                }

                // Replace documents contents to link
                for (Object item : docsNames[1]) {
                    String docFullName = (String) item;
                    XWikiDocument targetDoc = xwiki.getDocument(docFullName, context);

                    includeFormatParams[2] = docFullName;
                    targetDoc.setContent(MessageFormat.format("#includeTopic(\"{0}{1}{2}\")", includeFormatParams));
                    targetDoc.setSyntax(Syntax.XWIKI_1_0);
                    xwiki.saveDocument(targetDoc, context);
                }
            }
        } finally {
            context.setDatabase(database);
        }
    }

    /**
     * Import XAR package into wiki.
     * 
     * @param packageName the name of the attached package file.
     * @param targetWiki the name of the wiki where to install loaded {@link XWikiDocument} from XAR package.
     * @param context the XWiki context.
     * @throws XWikiException error when:
     *             <ul>
     *             <li>{@link WikiManagerException#ERROR_WM_XWIKINOTVIRTUAL}: xwiki is not in virtual mode.</li>
     *             <li>{@link WikiManagerException#ERROR_WM_PACKAGEDOESNOTEXISTS}: attached package does not exists.</li>
     *             <li>{@link WikiManagerException#ERROR_WM_PACKAGEIMPORT}: package loading failed.</li>
     *             <li>{@link WikiManagerException#ERROR_WM_PACKAGEINSTALL}: loaded package insertion into database
     *             failed.</li>
     *             </ul>
     */
    public void importPackage(String packageName, String targetWiki, XWikiContext context) throws XWikiException
    {
        XWikiPluginMessageTool msg = getMessageTool(context);

        XWiki xwiki = context.getWiki();

        if (!xwiki.isVirtualMode()) {
            throw new WikiManagerException(WikiManagerException.ERROR_WM_XWIKINOTVIRTUAL,
                msg.get(WikiManagerMessageTool.ERROR_XWIKINOTVIRTUAL));
        }

        // Prepare to import
        XWikiDocument doc = context.getDoc();

        XWikiAttachment packFile = doc.getAttachment(packageName);

        if (packFile == null) {
            throw new WikiManagerException(WikiManagerException.ERROR_WM_PACKAGEDOESNOTEXISTS, msg.get(
                WikiManagerMessageTool.ERROR_PACKAGEDOESNOTEXISTS, packageName));
        }

        // Get packager plugin
        PackageAPI importer = ((PackageAPI) context.getWiki().getPluginApi("package", context));

        String database = context.getDatabase();

        try {
            context.setDatabase(targetWiki);

            // Import package
            try {
                importer.Import(packFile.getContent(context));
            } catch (IOException e) {
                throw new WikiManagerException(WikiManagerException.ERROR_WM_PACKAGEIMPORT, msg.get(
                    WikiManagerMessageTool.ERROR_PACKAGEIMPORT, packageName), e);
            }

            // Install imported documents
            if (importer.install() == DocumentInfo.INSTALL_IMPOSSIBLE) {
                throw new WikiManagerException(WikiManagerException.ERROR_WM_PACKAGEINSTALL, msg.get(
                    WikiManagerMessageTool.ERROR_PACKAGEINSTALL, packageName));
            }
        } finally {
            context.setDatabase(database);
        }
    }

    // ////////////////////////////////////////////////////////////////////////////
    // Template management

    /**
     * Get template wiki descriptor document.
     * <p>
     * A template wiki is a wiki which the XWiki.XWikiServerClass "visibility" field is set to "template".
     * 
     * @param wikiName the name of the template wiki.
     * @param objectId the id of the XWiki object included in the document to manage.
     * @param context the XWiki context.
     * @param validate when wiki descriptor document does not exist :
     *            <ul>
     *            <li> if true, throw an exception with code {@link WikiManagerException#ERROR_WM_WIKIDOESNOTEXISTS} 
     *            <li> if false, return new document unsaved
     *            </ul>
     * @return a wiki descriptor document.
     * @throws XWikiException error when:
     *             <ul>
     *             <li>getting {@link XWikiServerClass} unique instance.</li> <li>or when searching for wiki descriptor
     *             with "visibility" field equals to "template".</li>
     *             </ul>
     */
    public XWikiServer getWikiTemplateAlias(String wikiName, int objectId, XWikiContext context, boolean validate)
        throws XWikiException
    {
        return XWikiServerClass.getInstance(context).getWikiTemplateAlias(wikiName, objectId, validate, context);
    }

    /**
     * Get all the templates wikis descriptors documents.
     * <p>
     * A template wiki is a wiki which the XWiki.XWikiServerClass "visibility" field is set to "template".
     * 
     * @param context the XWiki context.
     * @return a list of {@link XWikiServer}.
     * @throws XWikiException eeor when:
     *             <ul>
     *             <li>getting {@link XWikiServerClass} unique instance.</li> <li>or when searching for all wikis
     *             descriptors with "visibility" field equals to "template".</li>
     *             </ul>
     */
    public List<XWikiServer> getWikiTemplateAliasList(XWikiContext context) throws XWikiException
    {
        return XWikiServerClass.getInstance(context).searchXObjectDocumentsByField(
            XWikiServerClass.FIELD_ISWIKITEMPLATE, 1, "IntegerProperty", context);
    }
}
