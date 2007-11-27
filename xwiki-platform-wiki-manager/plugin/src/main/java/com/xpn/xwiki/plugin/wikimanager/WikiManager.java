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

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.plugin.applicationmanager.ApplicationManagerPlugin;
import com.xpn.xwiki.plugin.applicationmanager.ApplicationManagerPluginApi;
import com.xpn.xwiki.plugin.applicationmanager.core.doc.objects.classes.SuperClass;
import com.xpn.xwiki.plugin.applicationmanager.core.doc.objects.classes.SuperDocument;
import com.xpn.xwiki.plugin.applicationmanager.core.plugin.XWikiPluginMessageTool;
import com.xpn.xwiki.plugin.applicationmanager.doc.XWikiApplication;
import com.xpn.xwiki.plugin.wikimanager.doc.Wiki;
import com.xpn.xwiki.plugin.wikimanager.doc.XWikiServer;
import com.xpn.xwiki.plugin.wikimanager.doc.XWikiServerClass;
import com.xpn.xwiki.plugin.packaging.DocumentInfo;
import com.xpn.xwiki.plugin.packaging.PackageAPI;
import com.xpn.xwiki.util.Util;
import com.xpn.xwiki.doc.XWikiAttachment;
import com.xpn.xwiki.doc.XWikiDocument;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Hidden toolkit use by the plugin API that make all the plugins actions.
 * 
 * @version $Id: $
 */
final class WikiManager
{
    /**
     * Key to use with {@link XWikiContext#get(Object)}.
     */
    public static final String MESSAGETOOL_CONTEXT_KEY = "wikimanagermessagetool";

    /**
     * Quote string.
     */
    public static final String QUOTE = "\"";

    /**
     * Open bracket.
     */
    public static final String OPEN_BRACKET = "(";

    /**
     * Close bracket.
     */
    public static final String CLOSE_BRACKET = ")";

    /**
     * The logging tool.
     */
    protected static final Log LOG = LogFactory.getLog(WikiManager.class);

    // ////////////////////////////////////////////////////////////////////////////

    /**
     * Default bundle manager where to find translated messages.
     */
    private static final XWikiPluginMessageTool DEFAULTMESSAGETOOL = new WikiManagerMessageTool();

    /**
     * Unique instance of WikiManager.
     */
    private static WikiManager instance;

    /**
     * Hidden constructor of WikiManager only access via getInstance().
     */
    private WikiManager()
    {
    }

    /**
     * @return a unique instance of WikiManager. Thread safe.
     */
    public static WikiManager getInstance()
    {
        synchronized (WikiManager.class) {
            if (instance == null) {
                instance = new WikiManager();
            }
        }

        return instance;
    }

    /**
     * Get the {@link XWikiPluginMessageTool} to use with WikiManager.
     * 
     * @param context the XWiki context.
     * @return a translated strings manager.
     */
    public XWikiPluginMessageTool getMessageTool(XWikiContext context)
    {
        XWikiPluginMessageTool messagetool =
            (XWikiPluginMessageTool) context.get(MESSAGETOOL_CONTEXT_KEY);

        return messagetool != null ? messagetool : DEFAULTMESSAGETOOL;
    }

    // ////////////////////////////////////////////////////////////////////////////
    // Utils

    /**
     * Encapsulate {@link com.xpn.xwiki.XWiki#saveDocument(XWikiDocument, XWikiContext)} adding wiki
     * switch.
     * 
     * @param wikiName the name of the wiki where to save the document.
     * @param doc the document to save.
     * @param comment the comment to use when saving document.
     * @param context the XWiki Context.
     * @throws XWikiException error when calling
     *             {@link XWiki#saveDocument(XWikiDocument, String, XWikiContext)}.
     * @see com.xpn.xwiki.XWiki#saveDocument(XWikiDocument, XWikiContext)
     */
    public void saveDocument(String wikiName, XWikiDocument doc, String comment,
        XWikiContext context) throws XWikiException
    {
        String database = context.getDatabase();

        try {
            context.setDatabase(wikiName);
            context.getWiki().saveDocument(doc, comment, context);
        } finally {
            context.setDatabase(database);
        }
    }

    /**
     * Encapsulate {@link com.xpn.xwiki.XWiki#getDocument(String, XWikiContext)} adding wiki switch.
     * 
     * @param wikiName the name of the wiki where to get the document.
     * @param fullname the full name of the document to get.
     * @param context the XWiki context.
     * @return the document with full name equals to <code>fullname</code> and wiki
     *         <code>wikiName</code>. If it dos not exist return new XWikiDocument.
     * @throws XWikiException error when calling {@link XWiki#getDocument(String, XWikiContext)}}.
     * @see com.xpn.xwiki.XWiki#getDocument(String, XWikiContext)
     */
    public XWikiDocument getDocument(String wikiName, String fullname, XWikiContext context)
        throws XWikiException
    {
        String database = context.getDatabase();

        try {
            context.setDatabase(wikiName);
            return context.getWiki().getDocument(fullname, context);
        } finally {
            context.setDatabase(database);
        }
    }

    /**
     * Encapsulate
     * {@link com.xpn.xwiki.store.XWikiStoreInterface#searchDocuments(String, XWikiContext)} adding
     * wiki switch.
     * 
     * @param wikiName the name of the wiki where to search for documents.
     * @param wheresql the conditions to add to HQL request.
     * @param context the XWiki context.
     * @return the list of documents that match the <code>wheresql</code> conditions. If nothing
     *         found return empty List.
     * @throws XWikiException error when seraching for documents.
     * @see com.xpn.xwiki.store.XWikiStoreInterface#searchDocuments(String, XWikiContext)
     */
    public List searchDocuments(String wikiName, String wheresql, XWikiContext context)
        throws XWikiException
    {
        String database = context.getDatabase();

        try {
            context.setDatabase(wikiName);
            return context.getWiki().getStore().searchDocuments(wheresql, context);
        } finally {
            context.setDatabase(database);
        }
    }

    // ////////////////////////////////////////////////////////////////////////////
    // Wikis management

    /**
     * Get {@link Wiki} described by provided document.
     * 
     * @param document the wiki document descriptor.
     * @param context the XWiki context.
     * @return the {@link Wiki} object.
     * @throws XWikiException error when creating {@link Wiki} object.
     */
    public Wiki getWikiFromDocument(XWikiDocument document, XWikiContext context)
        throws XWikiException
    {
        return new Wiki(document, context);
    }

    /**
     * Get {@link Wiki} with provided name.
     * 
     * @param wikiName the name of the wiki.
     * @param context the XWiki context.
     * @return the {@link Wiki} object.
     * @throws XWikiException error when getting document from wiki name.
     */
    public Wiki getWikiFromName(String wikiName, XWikiContext context) throws XWikiException
    {
        return getWikiFromDocumentName(XWikiServerClass.getInstance(context)
            .getItemDocumentDefaultFullName(wikiName, context), context);
    }

    /**
     * Get all {@link Wiki}.
     * 
     * @param context the XWiki context.
     * @return the list of all {@link Wiki}.
     * @throws XWikiException error when getting wikis documents descriptors.
     */
    public List getAllWikis(XWikiContext context) throws XWikiException
    {
        List wikiList = new ArrayList();

        List parameterValues = new ArrayList();

        String wheresql =
            XWikiServerClass.getInstance(context).createWhereClause(null, parameterValues);
        List documents =
            context.getWiki().getStore().searchDocuments(wheresql, parameterValues, context);

        for (Iterator it = documents.iterator(); it.hasNext();) {
            XWikiDocument document = (XWikiDocument) it.next();

            wikiList.add(new Wiki(document, context));
        }

        return wikiList;
    }

    /**
     * Get {@link Wiki} described by document with provided full name.
     * 
     * @param documentFullName the full name of the wiki document descriptor.
     * @param context the XWiki context.
     * @return the {@link Wiki} object.
     * @throws XWikiException error when getting document.
     */
    public Wiki getWikiFromDocumentName(String documentFullName, XWikiContext context)
        throws XWikiException
    {
        return getWikiFromDocument(context.getWiki().getDocument(documentFullName, context),
            context);
    }

    /**
     * Get the documents for which copied document content will be replace by an
     * #includeInContext(SourceDocument) macro call.
     * 
     * @param wiki the name of the wiki where to find the list of documents.
     * @param context the XWiki context.
     * @return the list of documents to include.
     * @throws XWikiException error when getting Applications descriptors where searched documents
     *             are listed.
     */
    private Collection getDocsNameToInclude(String wiki, XWikiContext context)
        throws XWikiException
    {
        // Get applications manger
        ApplicationManagerPluginApi appmanager =
            (ApplicationManagerPluginApi) context.getWiki().getPluginApi(
                ApplicationManagerPlugin.PLUGIN_NAME, context);

        if (appmanager == null) {
            return null;
        }

        // //////////////////////////////////
        // Get documents to include

        String database = context.getDatabase();

        Collection docsToInclude = null;

        try {
            context.setDatabase(wiki);

            XWikiApplication rootApp = appmanager.getRootApplication();

            if (rootApp != null) {
                docsToInclude = rootApp.getDocsNameToInclude(true);
            } else {
                docsToInclude =
                    XWikiApplication
                        .getDocsNameToInclude(appmanager.getApplicationDocumentList());
            }
        } finally {
            context.setDatabase(database);
        }

        return docsToInclude;
    }

    /**
     * Get the documents for which copied document content will be replace by an
     * #includeTopic(SourceDocument) macro call.
     * 
     * @param wiki the name of the wiki where to find the list of documents.
     * @param context the XWiki context.
     * @return the list of documents to include.
     * @throws XWikiException error when getting Applications descriptors where searched documents
     *             are listed.
     */
    private Collection getDocsNameToLink(String wiki, XWikiContext context) throws XWikiException
    {
        // Get applications manger
        ApplicationManagerPluginApi appmanager =
            (ApplicationManagerPluginApi) context.getWiki().getPluginApi(
                ApplicationManagerPlugin.PLUGIN_NAME, context);

        if (appmanager == null) {
            return null;
        }

        // //////////////////////////////////
        // Get documents to link

        String database = context.getDatabase();

        Collection docsToLink = null;

        try {
            context.setDatabase(wiki);

            XWikiApplication rootApp = appmanager.getRootApplication();

            if (rootApp != null) {
                docsToLink = rootApp.getDocsNameToLink(true);
            } else {
                docsToLink =
                    XWikiApplication.getDocsNameToLink(appmanager.getApplicationDocumentList());
            }
        } finally {
            context.setDatabase(database);
        }

        return docsToLink;
    }

    /**
     * Copy all documents from <code>sourceWiki</code> wiki to <code>targetWiki</code> wiki.
     * <p>
     * It also take care of ApplicationManager descriptors "documents to include" and "documents to
     * link".
     * </p>
     * 
     * @param sourceWiki the wiki from where to copy documents and get lists of "document to link"
     *            and "documents to copy".
     * @param targetWiki the wiki where to copy documents.
     * @param language the documents language to copy.
     * @param comment the comment to use when saving documents.
     * @param context the XWiki context.
     * @throws XWikiException error when:
     *             <ul>
     *             <li>copying on of the source wiki to target wiki.</li>
     *             <li>or getting documents to include.</li>
     *             <li>or getting documents to link.</li>
     *             </ul>
     */
    private void copyWiki(String sourceWiki, String targetWiki, String language, String comment,
        XWikiContext context) throws XWikiException
    {
        XWiki xwiki = context.getWiki();

        // Copy all the wiki
        xwiki.copyWikiWeb(null, sourceWiki, targetWiki, language, true, context);

        String database = context.getDatabase();
        try {
            context.setDatabase(targetWiki);

            // Replace documents contents to include
            Collection docsNameToInclude = getDocsNameToInclude(sourceWiki, context);
            for (Iterator it = docsNameToInclude.iterator(); it.hasNext();) {
                String docFullName = (String) it.next();
                XWikiDocument targetDoc = xwiki.getDocument(docFullName, context);

                targetDoc.setContent("#includeInContext" + OPEN_BRACKET + QUOTE + sourceWiki
                    + SuperDocument.WIKI_SPACE_SEPARATOR + docFullName + QUOTE + CLOSE_BRACKET);
            }

            // Replace documents contents to link
            Collection docsNameToLink = getDocsNameToLink(sourceWiki, context);
            for (Iterator it = docsNameToLink.iterator(); it.hasNext();) {
                String docFullName = (String) it.next();
                XWikiDocument targetDoc = xwiki.getDocument(docFullName, context);

                targetDoc.setContent("#includeTopic" + OPEN_BRACKET + QUOTE + sourceWiki
                    + SuperDocument.WIKI_SPACE_SEPARATOR + docFullName + QUOTE + CLOSE_BRACKET);
            }
        } finally {
            context.setDatabase(database);
        }
    }

    /**
     * Create a new virtual wiki. The new wiki is initialized with provided xar package.
     * 
     * @param userWikiSuperDoc a wiki descriptor document from which the new wiki descriptor
     *            document will be created.
     * @param packageName the name of the attached XAR file to import in the new wiki.
     * @param failOnExist if true throw exception when wiki already exist. If false overwrite
     *            existing wiki.
     * @param comment the comment to use when saving descriptor document.
     * @param context the XWiki context.
     * @return the new wiki descriptor document.
     * @throws XWikiException error when:
     *             <ul>
     *             <li>getting unique instance of {@link XWikiServerClass}.</li>
     *             <li>or getting user documents.</li>
     *             <li>{@link WikiManagerException#ERROR_WM_XWIKINOTVIRTUAL} : xwiki is not in
     *             virtual mode.</li>
     *             <li>{@link WikiManagerException#ERROR_XWIKI_USERDOESNOTEXIST}: provided user
     *             does not exists.</li>
     *             <li>{@link WikiManagerException#ERROR_XWIKI_USER_INACTIVE}: provided user is
     *             not active.</li>
     *             <li>{@link WikiManagerException#ERROR_WM_WIKINAMEFORBIDDEN}: provided wiki name
     *             can't be used to create new wiki.</li>
     *             <li>{@link WikiManagerException#ERROR_WM_WIKIALREADYEXISTS}: wiki descriptor
     *             already exists.</li>
     *             <li>{@link WikiManagerException#ERROR_WM_UPDATEDATABASE}: error occurred when
     *             updating database.</li>
     *             </ul>
     * @see #createNewWiki(XWikiServer, boolean, String, XWikiContext)
     * @see #createNewWikiFromTemplate(XWikiServer, String, boolean, String, XWikiContext)
     */
    public XWikiServer createNewWikiFromPackage(XWikiServer userWikiSuperDoc, String packageName,
        boolean failOnExist, String comment, XWikiContext context) throws XWikiException
    {
        return createNewWiki(userWikiSuperDoc, failOnExist, null, packageName, comment, context);
    }

    /**
     * Create a new virtual wiki. The new wiki is a copy of provided existing wiki.
     * 
     * @param userWikiSuperDoc a wiki descriptor document from which the new wiki descriptor
     *            document will be created.
     * @param templateWikiName the of the wiki from where to copy document to the new wiki.
     * @param failOnExist if true throw exception when wiki already exist. If false overwrite
     *            existing wiki.
     * @param comment the comment to use when saving descriptor document.
     * @param context the XWiki context.
     * @return the new wiki descriptor document.
     * @throws XWikiException error when:
     *             <ul>
     *             <li>getting unique instance of {@link XWikiServerClass}.</li>
     *             <li>or getting user documents.</li>
     *             <li>{@link WikiManagerException#ERROR_WM_XWIKINOTVIRTUAL}: xwiki is not in
     *             virtual mode.</li>
     *             <li>{@link WikiManagerException#ERROR_XWIKI_USERDOESNOTEXIST}: provided user
     *             does not exists.</li>
     *             <li>{@link WikiManagerException#ERROR_XWIKI_USER_INACTIVE}: provided user is
     *             not active.</li>
     *             <li>{@link WikiManagerException#ERROR_WM_WIKINAMEFORBIDDEN}: provided wiki name
     *             can't be used to create new wiki.</li>
     *             <li>{@link WikiManagerException#ERROR_WM_WIKIALREADYEXISTS}: wiki descriptor
     *             already exists.</li>
     *             <li>{@link WikiManagerException#ERROR_WM_UPDATEDATABASE}: error occurred when
     *             updating database.</li>
     *             </ul>
     * @see #createNewWiki(XWikiServer, boolean, String, XWikiContext)
     * @see #createNewWikiFromPackage(XWikiServer, String, boolean, String, XWikiContext)
     */
    public XWikiServer createNewWikiFromTemplate(XWikiServer userWikiSuperDoc,
        String templateWikiName, boolean failOnExist, String comment, XWikiContext context)
        throws XWikiException
    {
        return createNewWiki(userWikiSuperDoc, failOnExist, templateWikiName, null, comment,
            context);
    }

    /**
     * Create a new empty virtual wiki.
     * 
     * @param userWikiSuperDoc a wiki descriptor document from which the new wiki descriptor
     *            document will be created.
     * @param failOnExist if true throw exception when wiki already exist. If false overwrite
     *            existing wiki.
     * @param comment the comment to use when saving descriptor document.
     * @param context the XWiki context.
     * @return the new wiki descriptor document.
     * @throws XWikiException error when:
     *             <ul>
     *             <li>getting unique instance of {@link XWikiServerClass}.</li>
     *             <li>or getting user documents.</li>
     *             <li>{@link WikiManagerException#ERROR_WM_XWIKINOTVIRTUAL}: xwiki is not in
     *             virtual mode.</li>
     *             <li>{@link WikiManagerException#ERROR_XWIKI_USERDOESNOTEXIST}: provided user
     *             does not exists.</li>
     *             <li>{@link WikiManagerException#ERROR_XWIKI_USER_INACTIVE}: provided user is
     *             not active.</li>
     *             <li>{@link WikiManagerException#ERROR_WM_WIKINAMEFORBIDDEN}: provided wiki name
     *             can't be used to create new wiki.</li>
     *             <li>{@link WikiManagerException#ERROR_WM_WIKIALREADYEXISTS}: wiki descriptor
     *             already exists.</li>
     *             <li>{@link WikiManagerException#ERROR_WM_UPDATEDATABASE}: error occurred when
     *             updating database.</li>
     *             </ul>
     */
    public XWikiServer createNewWiki(XWikiServer userWikiSuperDoc, boolean failOnExist,
        String comment, XWikiContext context) throws XWikiException
    {
        return createNewWiki(userWikiSuperDoc, failOnExist, null, null, comment, context);
    }

    /**
     * Create new wiki.
     * 
     * @param userWikiSuperDoc a wiki descriptor document from which the new wiki descriptor
     *            document will be created.
     * @param failOnExist if true throw exception when wiki already exist. If false overwrite
     *            existing wiki.
     * @param templateWikiName the name of the wiki from where to copy document to the new wiki.
     * @param packageName the name of the attached XAR file to import in the new wiki.
     * @param comment the comment to use when saving descriptor document.
     * @param context the XWiki context.
     * @return the new wiki descriptor document.
     * @throws XWikiException error when:
     *             <ul>
     *             <li>getting unique instance of {@link XWikiServerClass}.</li>
     *             <li>or getting user descriptor documents.</li>
     *             <li>{@link WikiManagerException#ERROR_WM_XWIKINOTVIRTUAL}: xwiki is not in
     *             virtual mode.</li>
     *             <li>{@link WikiManagerException#ERROR_XWIKI_USERDOESNOTEXIST}: provided user
     *             does not exists.</li>
     *             <li>{@link WikiManagerException#ERROR_WM_WIKINAMEFORBIDDEN}: provided wiki name
     *             can't be used to create new wiki.</li>
     *             <li>{@link WikiManagerException#ERROR_WM_WIKIALREADYEXISTS}: wiki descriptor
     *             already exists.</li>
     *             <li>{@link WikiManagerException#ERROR_WM_UPDATEDATABASE}: error occurred when
     *             updating database.</li>
     *             <li>{@link WikiManagerException#ERROR_WM_PACKAGEDOESNOTEXISTS}: attached
     *             package does not exists.</li>
     *             <li>{@link WikiManagerException#ERROR_WM_PACKAGEIMPORT}: package loading
     *             failed.</li>
     *             <li>{@link WikiManagerException#ERROR_WM_PACKAGEINSTALL}: loaded package
     *             insertion into database failed.</li>
     *             </ul>
     */
    public XWikiServer createNewWiki(XWikiServer userWikiSuperDoc, boolean failOnExist,
        String templateWikiName, String packageName, String comment, XWikiContext context)
        throws XWikiException
    {
        XWikiPluginMessageTool msg = getMessageTool(context);

        XWiki xwiki = context.getWiki();

        if (!xwiki.isVirtual()) {
            throw new WikiManagerException(WikiManagerException.ERROR_WM_XWIKINOTVIRTUAL, msg
                .get(WikiManagerMessageTool.ERROR_XWIKINOTVIRTUAL));
        }

        String newWikiName = userWikiSuperDoc.getWikiName();

        String database = context.getDatabase();

        try {
            // Return to root database
            context.setDatabase(context.getMainXWiki());

            XWikiDocument userdoc =
                getDocument(xwiki.getDatabase(), userWikiSuperDoc.getOwner(), context);

            // User does not exist
            if (userdoc.isNew()) {
                throw new WikiManagerException(WikiManagerException.ERROR_XWIKI_USERDOESNOTEXIST,
                    msg.get(WikiManagerMessageTool.ERROR_USERDOESNOTEXIST, userWikiSuperDoc
                        .getOwner()));
            }

            // Wiki name forbidden
            String wikiForbiddenList = xwiki.Param("xwiki.virtual.reserved_wikis");
            if (Util.contains(newWikiName, wikiForbiddenList, ", ")) {
                throw new WikiManagerException(WikiManagerException.ERROR_WM_WIKINAMEFORBIDDEN,
                    msg.get(WikiManagerMessageTool.ERROR_WIKINAMEFORBIDDEN, newWikiName));
            }

            // Update or create wiki descriptor document that will be save at and of wiki creation.
            XWikiServer wikiSuperDocToSave =
                getWikiDescriptorToSave(userWikiSuperDoc, failOnExist, context);

            // Create wiki database/schema
            createWikiDatabase(newWikiName, context);

            String language = userWikiSuperDoc.getLanguage();
            if (language.length() == 0) {
                language = null;
            }

            // Copy template wiki into new wiki
            if (templateWikiName != null) {
                copyWiki(templateWikiName, newWikiName, language, comment, context);
            }

            // Import XAR package into new wiki
            if (packageName != null) {
                importPackage(packageName, newWikiName, context);
            }

            // Return to root database
            context.setDatabase(context.getMainXWiki());

            // Save new wiki descriptor document.
            wikiSuperDocToSave.save(comment);

            return wikiSuperDocToSave;
        } finally {
            context.setDatabase(database);
        }
    }

    /**
     * Update or create new wiki description document without saving it.
     * 
     * @param userWikiSuperDoc a wiki descriptor document from which the new wiki descriptor
     *            document will be created.
     * @param failOnExist if true throw exception when wiki already exist. If false overwrite
     *            existing wiki.
     * @param context the XWiki context.
     * @return the new wiki descriptor document to save.
     * @throws XWikiException
     *             <ul>
     *             <li>{@link WikiManagerException#ERROR_WM_WIKIALREADYEXISTS}: wiki descriptor
     *             already exists.</li>
     *             </ul>
     */
    private XWikiServer getWikiDescriptorToSave(XWikiServer userWikiSuperDoc,
        boolean failOnExist, XWikiContext context) throws XWikiException
    {
        XWikiPluginMessageTool msg = getMessageTool(context);

        XWiki xwiki = context.getWiki();

        SuperClass wikiClass = XWikiServerClass.getInstance(context);

        XWikiServer wikiSuperDocToSave;

        // If modify existing document
        if (!userWikiSuperDoc.isFromCache()) {
            // Verify if server page already exist
            XWikiDocument docToSave =
                getDocument(context.getMainXWiki(), userWikiSuperDoc.getFullName(), context);

            if (!docToSave.isNew() && wikiClass.isInstance(docToSave)) {
                // If we are not allowed to continue in case wiki descriptor page already
                // exists.
                if (failOnExist) {
                    throw new WikiManagerException(WikiManagerException.ERROR_WM_WIKIALREADYEXISTS,
                        msg.get(WikiManagerMessageTool.ERROR_DESCRIPTORALREADYEXISTS,
                            userWikiSuperDoc.getFullName()));
                } else if (LOG.isWarnEnabled()) {
                    LOG.warn(msg.get(WikiManagerMessageTool.LOG_DESCRIPTORALREADYEXISTS,
                        userWikiSuperDoc.toString()));
                }
            }

            wikiSuperDocToSave = (XWikiServer) wikiClass.newSuperDocument(docToSave, 0, context);

            // clear entry in virtual wiki cache
            if (!wikiSuperDocToSave.getServer().equals(userWikiSuperDoc.getServer())) {
                xwiki.getVirtualWikiMap().flushEntry(userWikiSuperDoc.getServer());
            }

            wikiSuperDocToSave.mergeObject(userWikiSuperDoc);
        } else {
            wikiSuperDocToSave = userWikiSuperDoc;
        }

        return wikiSuperDocToSave;
    }

    /**
     * Create and init new database/schema.
     * 
     * @param targetWiki the name of the new database/schema.
     * @param context the Xwiki context.
     * @throws WikiManagerException
     *             <ul>
     *             <li>{@link WikiManagerException#ERROR_WM_XWIKINOTVIRTUAL}: xwiki is not in
     *             virtual mode.</li>
     *             <li>{@link WikiManagerException#ERROR_WM_UPDATEDATABASE}: error occurred when
     *             updating database.</li>
     *             </ul>
     */
    private void createWikiDatabase(String targetWiki, XWikiContext context)
        throws WikiManagerException
    {
        XWikiPluginMessageTool msg = getMessageTool(context);

        XWiki xwiki = context.getWiki();

        if (!xwiki.isVirtual()) {
            throw new WikiManagerException(WikiManagerException.ERROR_WM_XWIKINOTVIRTUAL, msg
                .get(WikiManagerMessageTool.ERROR_XWIKINOTVIRTUAL));
        }

        // Create database/schema
        try {
            xwiki.getStore().createWiki(targetWiki, context);
        } catch (Exception e) {
            LOG
                .warn(msg.get(WikiManagerMessageTool.LOG_DATABASECREATIONEXCEPTION, targetWiki),
                    e);
        }

        // Init database/schema
        try {
            xwiki.updateDatabase(targetWiki, true, false, context);
        } catch (Exception e) {
            throw new WikiManagerException(WikiManagerException.ERROR_WM_UPDATEDATABASE, msg.get(
                WikiManagerMessageTool.ERROR_UPDATEDATABASE, targetWiki), e);
        }
    }

    /**
     * Import XAR package into wiki.
     * 
     * @param packageName the name of the attached package file.
     * @param targetWiki the name of the wiki where to install loaded {@link XWikiDocument} from XAR
     *            package.
     * @param context the XWiki context.
     * @throws XWikiException error when:
     *             <ul>
     *             <li>{@link WikiManagerException#ERROR_WM_XWIKINOTVIRTUAL}: xwiki is not in
     *             virtual mode.</li>
     *             <li>{@link WikiManagerException#ERROR_WM_PACKAGEDOESNOTEXISTS}: attached
     *             package does not exists.</li>
     *             <li>{@link WikiManagerException#ERROR_WM_PACKAGEIMPORT}: package loading
     *             failed.</li>
     *             <li>{@link WikiManagerException#ERROR_WM_PACKAGEINSTALL}: loaded package
     *             insertion into database failed.</li>
     *             </ul>
     */
    public void importPackage(String packageName, String targetWiki, XWikiContext context)
        throws XWikiException
    {
        XWikiPluginMessageTool msg = getMessageTool(context);

        XWiki xwiki = context.getWiki();

        if (!xwiki.isVirtual()) {
            throw new WikiManagerException(WikiManagerException.ERROR_WM_XWIKINOTVIRTUAL, msg
                .get(WikiManagerMessageTool.ERROR_XWIKINOTVIRTUAL));
        }

        // Prepare to import
        XWikiDocument doc = context.getDoc();

        XWikiAttachment packFile = doc.getAttachment(packageName);

        if (packFile == null) {
            throw new WikiManagerException(WikiManagerException.ERROR_WM_PACKAGEDOESNOTEXISTS,
                msg.get(WikiManagerMessageTool.ERROR_PACKAGEDOESNOTEXISTS, packageName));
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
                throw new WikiManagerException(WikiManagerException.ERROR_WM_PACKAGEIMPORT, msg
                    .get(WikiManagerMessageTool.ERROR_PACKAGEIMPORT, packageName), e);
            }

            // Install imported documents
            if (importer.install() == DocumentInfo.INSTALL_IMPOSSIBLE) {
                throw new WikiManagerException(WikiManagerException.ERROR_WM_PACKAGEINSTALL, msg
                    .get(WikiManagerMessageTool.ERROR_PACKAGEINSTALL, packageName));
            }
        } finally {
            context.setDatabase(database);
        }
    }

    /**
     * Delete an existing wiki.
     * <p>
     * Only delete the wiki descriptor the corresponding database always exist after delete.
     * 
     * @param wikiNameToDelete the name of the wiki to delete.
     * @param objectId the id of the XWiki object included in the document to manage.
     * @param context the XWiki context.
     * @throws XWikiException error when:
     *             <ul>
     *             <li>getting wiki descriptor document.</li>
     *             <li>or deleteing wiki.</li>
     *             </ul>
     */
    public void deleteWiki(String wikiNameToDelete, int objectId, XWikiContext context)
        throws XWikiException
    {
        XWikiServer doc = getWikiAlias(wikiNameToDelete, objectId, true, context);

        doc.delete();
    }

    /**
     * Get the wiki descriptor document.
     * 
     * @param wikiName the name of the wiki.
     * @param objectId the id of the XWiki object included in the document to manage.
     * @param validate when wiki descriptor document does not exist :
     *            <ul>
     *            <li> if true, throw an exception with code
     *            {@link WikiManagerException#ERROR_WM_WIKIDOESNOTEXISTS}
     *            <li> if false, return new document unsaved
     *            </ul>
     * @param context the XWiki context.
     * @return a wiki descriptor document.
     * @throws XWikiException error when getting wiki descriptor document.
     */
    public XWikiServer getWikiAlias(String wikiName, int objectId, boolean validate,
        XWikiContext context) throws XWikiException
    {
        return XWikiServerClass.getInstance(context).getWikiAlias(wikiName, objectId, validate,
            context);
    }

    /**
     * Get all the wikis descriptors documents.
     * 
     * @param context the XWiki context.
     * @return a list of XWikiServer.
     * @throws XWikiException error when:
     *             <ul>
     *             <li>getting the list of wiki descriptor {@link XWikiDocument}.</li>
     *             <li>or getting {@link XWikiServerClass} unique instance.</li>
     *             </ul>
     */
    public List getWikiAliasList(XWikiContext context) throws XWikiException
    {
        return XWikiServerClass.getInstance(context).searchSuperDocuments(context);
    }

    /**
     * Indicate of wiki descriptor document exist.
     * 
     * @param wikiName the name of the wiki.
     * @param objectId the id of the XWiki object included in the document to manage.
     * @param context the XWiki context.
     * @return true if wiki descriptor exist, false if not.
     */
    public boolean isWikiAliasExist(String wikiName, int objectId, XWikiContext context)
    {
        try {
            return getWikiAlias(wikiName, objectId, true, context) != null;
        } catch (XWikiException e) {
            return false;
        }
    }

    // ////////////////////////////////////////////////////////////////////////////
    // Template management

    /**
     * Get template wiki descriptor document.
     * <p>
     * A template wiki is a wiki which the XWiki.XWikiServerClass "visibility" field is set to
     * "template".
     * 
     * @param wikiName the name of the template wiki.
     * @param objectId the id of the XWiki object included in the document to manage.
     * @param context the XWiki context.
     * @param validate when wiki descriptor document does not exist :
     *            <ul>
     *            <li> if true, throw an exception with code
     *            {@link WikiManagerException#ERROR_WM_WIKIDOESNOTEXISTS}
     *            <li> if false, return new document unsaved
     *            </ul>
     * @return a wiki descriptor document.
     * @throws XWikiException error when:
     *             <ul>
     *             <li>getting {@link XWikiServerClass} unique instance.</li>
     *             <li>or when searching for wiki descriptor with "visibility" field equals to
     *             "template".</li>
     *             </ul>
     */
    public XWikiServer getWikiTemplateAlias(String wikiName, int objectId, XWikiContext context,
        boolean validate) throws XWikiException
    {
        return XWikiServerClass.getInstance(context).getWikiTemplateAlias(wikiName, objectId,
            validate, context);
    }

    /**
     * Get all the templates wikis descriptors documents.
     * <p>
     * A template wiki is a wiki which the XWiki.XWikiServerClass "visibility" field is set to
     * "template".
     * 
     * @param context the XWiki context.
     * @return a list of {@link XWikiServer}.
     * @throws XWikiException eeor when:
     *             <ul>
     *             <li>getting {@link XWikiServerClass} unique instance.</li>
     *             <li>or when searching for all wikis descriptors with "visibility" field equals
     *             to "template".</li>
     *             </ul>
     */
    public List getWikiTemplateAliasList(XWikiContext context) throws XWikiException
    {
        return XWikiServerClass.getInstance(context).searchSuperDocumentsByField(
            XWikiServerClass.FIELD_VISIBILITY, XWikiServerClass.FIELDL_VISIBILITY_TEMPLATE,
            "StringProperty", context);
    }

    /**
     * Create a template wiki. The new template wiki is initialized with provided xar package.
     * <p>
     * A template wiki is a wiki which the XWiki.XWikiServerClass "visibility" field is set to
     * "template".
     * 
     * @param wikiSuperDocument a wiki descriptor document from which the new template wiki
     *            descriptor document will be created.
     * @param packageName the name of the attached XAR file to import in the new template wiki.
     * @param comment the comment to use when saving descriptor document.
     * @param context the XWiki context.
     * @throws XWikiException error when creating new wiki from XAR package.
     */
    public void createWikiTemplate(XWikiServer wikiSuperDocument, String packageName,
        String comment, XWikiContext context) throws XWikiException
    {
        wikiSuperDocument.setVisibility(XWikiServerClass.FIELDL_VISIBILITY_TEMPLATE);

        // Create empty wiki
        createNewWikiFromPackage(wikiSuperDocument, packageName, false, comment, context);
    }
}
