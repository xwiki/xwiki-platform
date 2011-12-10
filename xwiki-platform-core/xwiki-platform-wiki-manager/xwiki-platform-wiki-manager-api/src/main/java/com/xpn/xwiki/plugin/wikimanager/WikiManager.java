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

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xwiki.bridge.event.WikiCreateFailedEvent;
import org.xwiki.bridge.event.WikiCreatedEvent;
import org.xwiki.bridge.event.WikiCreatingEvent;
import org.xwiki.observation.ObservationManager;
import org.xwiki.script.service.ScriptService;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.plugin.applicationmanager.ApplicationManagerPlugin;
import com.xpn.xwiki.plugin.applicationmanager.ApplicationManagerPluginApi;
import com.xpn.xwiki.plugin.applicationmanager.core.plugin.XWikiPluginMessageTool;
import com.xpn.xwiki.plugin.applicationmanager.doc.XWikiApplication;
import com.xpn.xwiki.plugin.wikimanager.doc.Wiki;
import com.xpn.xwiki.plugin.wikimanager.doc.XWikiServer;
import com.xpn.xwiki.plugin.wikimanager.doc.XWikiServerClass;
import com.xpn.xwiki.user.api.XWikiRightService;
import com.xpn.xwiki.util.Util;
import com.xpn.xwiki.web.Utils;

/**
 * Hidden toolkit use by the plugin API that make all the plugins actions.
 * 
 * @version $Id$
 */
public final class WikiManager
{
    /**
     * The logging tool.
     */
    protected static final Logger LOGGER = LoggerFactory.getLogger(WikiManager.class);

    /**
     * The message tool to use to generate error or comments.
     */
    private XWikiPluginMessageTool messageTool;

    /**
     * Used to fill newly created wiki.
     */
    private WikiCopy wikiCopy;

    // ////////////////////////////////////////////////////////////////////////////

    /**
     * @param messageTool the message tool
     */
    public WikiManager(XWikiPluginMessageTool messageTool)
    {
        this.messageTool = messageTool;
        this.wikiCopy = new WikiCopy(messageTool);
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
     * Encapsulate {@link com.xpn.xwiki.XWiki#getDocument(String, XWikiContext)} adding wiki switch.
     * 
     * @param wikiName the name of the wiki where to get the document.
     * @param fullname the full name of the document to get.
     * @param context the XWiki context.
     * @return the document with full name equals to <code>fullname</code> and wiki <code>wikiName</code>. If it does
     *         not exist return new XWikiDocument.
     * @throws XWikiException error when calling {@link XWiki#getDocument(String, XWikiContext)} .
     * @see com.xpn.xwiki.XWiki#getDocument(String, XWikiContext)
     */
    public XWikiDocument getDocument(String wikiName, String fullname, XWikiContext context) throws XWikiException
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
     * Encapsulate {@link com.xpn.xwiki.store.XWikiStoreInterface#searchDocuments(String, XWikiContext)} adding wiki
     * switch.
     * 
     * @param wikiName the name of the wiki where to search for documents.
     * @param wheresql the conditions to add to HQL request.
     * @param context the XWiki context.
     * @return the list of documents that match the <code>wheresql</code> conditions. If nothing found return empty
     *         List.
     * @throws XWikiException error when searching for documents.
     * @see com.xpn.xwiki.store.XWikiStoreInterface#searchDocuments(String, XWikiContext)
     */
    public List<XWikiDocument> searchDocuments(String wikiName, String wheresql, XWikiContext context)
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
     * @param context the XWiki context
     * @return true if the it's possible to create a wiki in this context
     */
    public boolean canCreateWiki(XWikiContext context)
    {
        XWikiRightService rightService = context.getWiki().getRightService();

        return context.getWiki().isVirtualMode() && rightService.hasAdminRights(context)
            && rightService.hasProgrammingRights(context);
    }

    /**
     * @param context the XWiki context
     * @return true if the it's possible to edit a wiki descriptor in this context
     */
    public boolean canEditWiki(XWikiContext context)
    {
        XWikiRightService rightService = context.getWiki().getRightService();

        return rightService.hasAdminRights(context) && rightService.hasProgrammingRights(context);
    }

    /**
     * @param context the XWiki context
     * @return true if the it's possible to delete a wiki in this context
     */
    public boolean canDeleteWiki(XWikiContext context)
    {
        XWikiRightService rightService = context.getWiki().getRightService();

        return context.getWiki().isVirtualMode() && rightService.hasAdminRights(context)
            && rightService.hasProgrammingRights(context);
    }

    /**
     * Get {@link Wiki} described by provided document.
     * 
     * @param document the wiki document descriptor.
     * @param context the XWiki context.
     * @return the {@link Wiki} object.
     * @throws XWikiException error when creating {@link Wiki} object.
     */
    public Wiki getWikiFromDocument(XWikiDocument document, XWikiContext context) throws XWikiException
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
        return getWikiFromDocumentName(context.getMainXWiki() + ":"
            + XWikiServerClass.getInstance(context).getItemDocumentDefaultFullName(wikiName, context), context);
    }

    /**
     * Get {@link Wiki} described by document with provided full name.
     * 
     * @param documentFullName the full name of the wiki document descriptor.
     * @param context the XWiki context.
     * @return the {@link Wiki} object.
     * @throws XWikiException error when getting document.
     */
    public Wiki getWikiFromDocumentName(String documentFullName, XWikiContext context) throws XWikiException
    {
        return getWikiFromDocument(context.getWiki().getDocument(documentFullName, context), context);
    }

    /**
     * Get all {@link Wiki}.
     * 
     * @param context the XWiki context.
     * @return the list of all {@link Wiki}.
     * @throws XWikiException error when getting wikis documents descriptors.
     */
    public List<Wiki> getAllWikis(XWikiContext context) throws XWikiException
    {
        List<Wiki> wikiList = new ArrayList<Wiki>();

        List<Object> parameterValues = new ArrayList<Object>();

        String wheresql = XWikiServerClass.getInstance(context).createWhereClause(null, parameterValues);

        String database = context.getDatabase();
        try {
            context.setDatabase(context.getMainXWiki());

            List<XWikiDocument> documents =
                context.getWiki().getStore().searchDocuments(wheresql, parameterValues, context);

            for (XWikiDocument document : documents) {
                wikiList.add(new Wiki(document, context));
            }

            return wikiList;
        } finally {
            context.setDatabase(database);
        }
    }

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
            (ApplicationManagerPluginApi) Utils.getComponent(ScriptService.class, ApplicationManagerPlugin.PLUGIN_NAME);

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
     * Create a new virtual wiki. The new wiki is initialized with provided xar package.
     * 
     * @param userWikiSuperDoc a wiki descriptor document from which the new wiki descriptor document will be created.
     * @param packageName the name of the attached XAR file to import in the new wiki.
     * @param failOnExist if true throw exception when wiki already exist. If false overwrite existing wiki.
     * @param comment the comment to use when saving descriptor document.
     * @param context the XWiki context.
     * @return the new wiki descriptor document.
     * @throws XWikiException error when:
     *             <ul>
     *             <li>getting unique instance of {@link XWikiServerClass}.</li>
     *             <li>or getting user documents.</li>
     *             <li>{@link WikiManagerException#ERROR_WM_XWIKINOTVIRTUAL} : xwiki is not in virtual mode.</li>
     *             <li>{@link WikiManagerException#ERROR_XWIKI_USERDOESNOTEXIST}: provided user does not exists.</li>
     *             <li>{@link WikiManagerException#ERROR_XWIKI_USER_INACTIVE}: provided user is not active.</li>
     *             <li>{@link WikiManagerException#ERROR_WM_WIKINAMEFORBIDDEN}: provided wiki name can't be used to
     *             create new wiki.</li>
     *             <li>{@link WikiManagerException#ERROR_WM_WIKIALREADYEXISTS}: wiki descriptor already exists.</li>
     *             <li>{@link WikiManagerException#ERROR_WM_UPDATEDATABASE}: error occurred when updating database.</li>
     *             </ul>
     * @see #createNewWiki(XWikiServer, boolean, String, XWikiContext)
     * @see #createNewWikiFromTemplate(XWikiServer, String, boolean, String, XWikiContext)
     */
    public XWikiServer createNewWikiFromPackage(XWikiServer userWikiSuperDoc, String packageName, boolean failOnExist,
        String comment, XWikiContext context) throws XWikiException
    {
        return createNewWiki(userWikiSuperDoc, failOnExist, null, packageName, comment, context);
    }

    /**
     * Create a new virtual wiki. The new wiki is a copy of provided existing wiki.
     * 
     * @param userWikiSuperDoc a wiki descriptor document from which the new wiki descriptor document will be created.
     * @param templateWikiName the of the wiki from where to copy document to the new wiki.
     * @param failOnExist if true throw exception when wiki already exist. If false overwrite existing wiki.
     * @param comment the comment to use when saving descriptor document.
     * @param context the XWiki context.
     * @return the new wiki descriptor document.
     * @throws XWikiException error when:
     *             <ul>
     *             <li>getting unique instance of {@link XWikiServerClass}.</li>
     *             <li>or getting user documents.</li>
     *             <li>{@link WikiManagerException#ERROR_WM_XWIKINOTVIRTUAL}: xwiki is not in virtual mode.</li>
     *             <li>{@link WikiManagerException#ERROR_XWIKI_USERDOESNOTEXIST}: provided user does not exists.</li>
     *             <li>{@link WikiManagerException#ERROR_XWIKI_USER_INACTIVE}: provided user is not active.</li>
     *             <li>{@link WikiManagerException#ERROR_WM_WIKINAMEFORBIDDEN}: provided wiki name can't be used to
     *             create new wiki.</li>
     *             <li>{@link WikiManagerException#ERROR_WM_WIKIALREADYEXISTS}: wiki descriptor already exists.</li>
     *             <li>{@link WikiManagerException#ERROR_WM_UPDATEDATABASE}: error occurred when updating database.</li>
     *             </ul>
     * @see #createNewWiki(XWikiServer, boolean, String, XWikiContext)
     * @see #createNewWikiFromPackage(XWikiServer, String, boolean, String, XWikiContext)
     */
    public XWikiServer createNewWikiFromTemplate(XWikiServer userWikiSuperDoc, String templateWikiName,
        boolean failOnExist, String comment, XWikiContext context) throws XWikiException
    {
        return createNewWiki(userWikiSuperDoc, failOnExist, templateWikiName, null, comment, context);
    }

    /**
     * Create a new empty virtual wiki.
     * 
     * @param userWikiSuperDoc a wiki descriptor document from which the new wiki descriptor document will be created.
     * @param failOnExist if true throw exception when wiki already exist. If false overwrite existing wiki.
     * @param comment the comment to use when saving descriptor document.
     * @param context the XWiki context.
     * @return the new wiki descriptor document.
     * @throws XWikiException error when:
     *             <ul>
     *             <li>getting unique instance of {@link XWikiServerClass}.</li>
     *             <li>or getting user documents.</li>
     *             <li>{@link WikiManagerException#ERROR_WM_XWIKINOTVIRTUAL}: xwiki is not in virtual mode.</li>
     *             <li>{@link WikiManagerException#ERROR_XWIKI_USERDOESNOTEXIST}: provided user does not exists.</li>
     *             <li>{@link WikiManagerException#ERROR_XWIKI_USER_INACTIVE}: provided user is not active.</li>
     *             <li>{@link WikiManagerException#ERROR_WM_WIKINAMEFORBIDDEN}: provided wiki name can't be used to
     *             create new wiki.</li>
     *             <li>{@link WikiManagerException#ERROR_WM_WIKIALREADYEXISTS}: wiki descriptor already exists.</li>
     *             <li>{@link WikiManagerException#ERROR_WM_UPDATEDATABASE}: error occurred when updating database.</li>
     *             </ul>
     */
    public XWikiServer createNewWiki(XWikiServer userWikiSuperDoc, boolean failOnExist, String comment,
        XWikiContext context) throws XWikiException
    {
        return createNewWiki(userWikiSuperDoc, failOnExist, null, null, comment, context);
    }

    /**
     * Create new wiki.
     * 
     * @param userWikiSuperDoc a wiki descriptor document from which the new wiki descriptor document will be created.
     * @param failOnExist if true throw exception when wiki already exist. If false overwrite existing wiki.
     * @param templateWikiName the name of the wiki from where to copy document to the new wiki.
     * @param packageName the name of the attached XAR file to import in the new wiki.
     * @param comment the comment to use when saving descriptor document.
     * @param context the XWiki context.
     * @return the new wiki descriptor document.
     * @throws XWikiException error when:
     *             <ul>
     *             <li>getting unique instance of {@link XWikiServerClass}.</li>
     *             <li>or getting user descriptor documents.</li>
     *             <li>{@link WikiManagerException#ERROR_WM_XWIKINOTVIRTUAL}: xwiki is not in virtual mode.</li>
     *             <li>{@link WikiManagerException#ERROR_XWIKI_USERDOESNOTEXIST}: provided user does not exists.</li>
     *             <li>{@link WikiManagerException#ERROR_WM_WIKINAMEFORBIDDEN}: provided wiki name can't be used to
     *             create new wiki.</li>
     *             <li>{@link WikiManagerException#ERROR_WM_WIKIALREADYEXISTS}: wiki descriptor already exists.</li>
     *             <li>{@link WikiManagerException#ERROR_WM_UPDATEDATABASE}: error occurred when updating database.</li>
     *             <li>{@link WikiManagerException#ERROR_WM_PACKAGEDOESNOTEXISTS}: attached package does not exists.</li>
     *             <li>{@link WikiManagerException#ERROR_WM_PACKAGEIMPORT}: package loading failed.</li>
     *             <li>{@link WikiManagerException#ERROR_WM_PACKAGEINSTALL}: loaded package insertion into database
     *             failed.</li>
     *             </ul>
     */
    public XWikiServer createNewWiki(XWikiServer userWikiSuperDoc, boolean failOnExist, String templateWikiName,
        String packageName, String comment, XWikiContext context) throws XWikiException
    {
        XWikiPluginMessageTool msg = getMessageTool(context);

        XWiki xwiki = context.getWiki();

        if (!xwiki.isVirtualMode()) {
            throw new WikiManagerException(WikiManagerException.ERROR_WM_XWIKINOTVIRTUAL,
                msg.get(WikiManagerMessageTool.ERROR_XWIKINOTVIRTUAL));
        }

        String newWikiName = userWikiSuperDoc.getWikiName();

        String database = context.getDatabase();

        try {
            // Return to root database
            context.setDatabase(context.getMainXWiki());

            // Wiki name forbidden
            String wikiForbiddenList = xwiki.Param("xwiki.virtual.reserved_wikis");
            if (Util.contains(newWikiName, wikiForbiddenList, ", ")) {
                throw new WikiManagerException(WikiManagerException.ERROR_WM_WIKINAMEFORBIDDEN, msg.get(
                    WikiManagerMessageTool.ERROR_WIKINAMEFORBIDDEN, newWikiName));
            }

            // Update or create wiki descriptor document that will be saved.
            XWikiServer wikiSuperDocToSave = getWikiDescriptorToSave(userWikiSuperDoc, failOnExist, context);

            // Check owner
            if (getDocument(xwiki.getDatabase(), wikiSuperDocToSave.getOwner(), context).isNew()) {
                LOGGER.warn(msg.get(WikiManagerMessageTool.ERROR_USERDOESNOTEXIST, wikiSuperDocToSave.getOwner()));
                wikiSuperDocToSave.setOwner(XWikiRightService.SUPERADMIN_USER);
            }

            Utils.getComponent(ObservationManager.class).notify(new WikiCreatingEvent(newWikiName), newWikiName,
                context);

            boolean sucess = false;
            try {
                // Create wiki database/schema
                createWikiDatabase(newWikiName, context, templateWikiName == null);

                // Save new wiki descriptor document.
                XWikiDocument wikiSuperXDocToSave = wikiSuperDocToSave.getDocument();
                context.getWiki().saveDocument(wikiSuperXDocToSave, comment, context);

                sucess = true;

                // Copy template wiki into new wiki
                if (templateWikiName != null) {
                    this.wikiCopy.copyWiki(templateWikiName, newWikiName, comment, context);
                }

                // Import XAR package into new wiki
                if (packageName != null) {
                    this.wikiCopy.importPackage(packageName, newWikiName, context);
                }

                // Return to root database
                context.setDatabase(context.getMainXWiki());
            } finally {
                if (sucess) {
                    Utils.getComponent(ObservationManager.class).notify(new WikiCreatedEvent(newWikiName), newWikiName,
                        context);
                } else {
                    // Sending this event because we send WikiCreatingEvent
                    Utils.getComponent(ObservationManager.class).notify(new WikiCreateFailedEvent(newWikiName),
                        newWikiName, context);
                }
            }

            return wikiSuperDocToSave;
        } finally {
            context.setDatabase(database);
        }
    }

    /**
     * Update or create new wiki description document without saving it.
     * 
     * @param userWikiSuperDoc a wiki descriptor document from which the new wiki descriptor document will be created.
     * @param failOnExist if true throw exception when wiki already exist. If false overwrite existing wiki.
     * @param context the XWiki context.
     * @return the new wiki descriptor document to save.
     * @throws XWikiException <ul>
     *             <li>{@link WikiManagerException#ERROR_WM_WIKIALREADYEXISTS}: wiki descriptor already exists.</li>
     *             </ul>
     */
    private XWikiServer getWikiDescriptorToSave(XWikiServer userWikiSuperDoc, boolean failOnExist, XWikiContext context)
        throws XWikiException
    {
        XWikiPluginMessageTool msg = getMessageTool(context);

        XWiki xwiki = context.getWiki();

        XWikiServerClass wikiClass = XWikiServerClass.getInstance(context);

        XWikiServer wikiSuperDocToSave;

        // If modify existing document
        if (!userWikiSuperDoc.isFromCache()) {
            // Verify if server page already exist
            XWikiDocument docToSave = getDocument(context.getMainXWiki(), userWikiSuperDoc.getFullName(), context);

            if (!docToSave.isNew() && wikiClass.isInstance(docToSave)) {
                // If we are not allowed to continue in case wiki descriptor page already
                // exists.
                if (failOnExist) {
                    throw new WikiManagerException(WikiManagerException.ERROR_WM_WIKIALREADYEXISTS, msg.get(
                        WikiManagerMessageTool.ERROR_DESCRIPTORALREADYEXISTS, userWikiSuperDoc.getFullName()));
                } else if (LOGGER.isWarnEnabled()) {
                    LOGGER
                        .warn(msg.get(WikiManagerMessageTool.LOG_DESCRIPTORALREADYEXISTS, userWikiSuperDoc.toString()));
                }
            }

            wikiSuperDocToSave = wikiClass.newXObjectDocument(docToSave, 0, context);

            // clear entry in virtual wiki cache
            if (!wikiSuperDocToSave.getServer().equals(userWikiSuperDoc.getServer())) {
                xwiki.getVirtualWikiCache().remove(userWikiSuperDoc.getServer());
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
     * @param initClasses indicate if standard classes should be initalized
     * @throws WikiManagerException <ul>
     *             <li>{@link WikiManagerException#ERROR_WM_XWIKINOTVIRTUAL}: xwiki is not in virtual mode.</li>
     *             <li>{@link WikiManagerException#ERROR_WM_UPDATEDATABASE}: error occurred when updating database.</li>
     *             </ul>
     */
    private void createWikiDatabase(String targetWiki, XWikiContext context, boolean initClasses)
        throws WikiManagerException
    {
        XWikiPluginMessageTool msg = getMessageTool(context);

        XWiki xwiki = context.getWiki();

        if (!xwiki.isVirtualMode()) {
            throw new WikiManagerException(WikiManagerException.ERROR_WM_XWIKINOTVIRTUAL,
                msg.get(WikiManagerMessageTool.ERROR_XWIKINOTVIRTUAL));
        }

        // Create database/schema
        try {
            xwiki.getStore().createWiki(targetWiki, context);
        } catch (Exception e) {
            LOGGER.warn(msg.get(WikiManagerMessageTool.LOG_DATABASECREATIONEXCEPTION, targetWiki), e);
        }

        // Init database/schema
        try {
            xwiki.updateDatabase(targetWiki, true, initClasses, context);
        } catch (Exception e) {
            throw new WikiManagerException(WikiManagerException.ERROR_WM_UPDATEDATABASE, msg.get(
                WikiManagerMessageTool.ERROR_UPDATEDATABASE, targetWiki), e);
        }
    }

    /**
     * Delete an existing wiki.
     * 
     * @param wikiNameToDelete the name of the wiki to delete.
     * @param deleteDatabase if true wiki's database is also removed.
     * @param context the XWiki context.
     * @throws XWikiException error when:
     *             <ul>
     *             <li>getting wiki descriptor document.</li>
     *             <li>or deleteing wiki.</li>
     *             </ul>
     * @since 1.1
     */
    public void deleteWiki(String wikiNameToDelete, boolean deleteDatabase, XWikiContext context) throws XWikiException
    {
        Wiki wiki = getWikiFromName(wikiNameToDelete, context);

        if (!XWikiServerClass.getInstance(context).isInstance(wiki)) {
            throw new WikiManagerException(WikiManagerException.ERROR_WM_WIKIDOESNOTEXISTS, getMessageTool(context)
                .get(WikiManagerMessageTool.ERROR_WIKIDOESNOTEXISTS, wikiNameToDelete));
        }

        wiki.delete(deleteDatabase);
    }

    /**
     * Delete an existing wiki alias. If it's the last alias it delete the wiki.
     * 
     * @param wikiNameToDelete the name of the wiki to delete.
     * @param objectId the id of the XWiki object included in the document to manage.
     * @param context the XWiki context.
     * @throws XWikiException error when:
     *             <ul>
     *             <li>getting wiki descriptor document.</li>
     *             <li>or deleteing wiki.</li>
     *             </ul>
     * @since 1.1
     */
    public void deleteWikiAlias(String wikiNameToDelete, int objectId, XWikiContext context) throws XWikiException
    {
        Wiki wiki = getWikiFromName(wikiNameToDelete, context);
        XWikiServer alias = wiki.getWikiAlias(objectId);

        if (wiki.countWikiAliases() == 1) {
            wiki.delete(true);
        } else {
            alias.delete();
        }
    }

    /**
     * Get the wiki descriptor document.
     * 
     * @param wikiName the name of the wiki.
     * @param objectId the id of the XWiki object included in the document to manage.
     * @param validate when wiki descriptor document does not exist :
     *            <ul>
     *            <li>if true, throw an exception with code {@link WikiManagerException#ERROR_WM_WIKIDOESNOTEXISTS}
     *            <li>if false, return new document unsaved
     *            </ul>
     * @param context the XWiki context.
     * @return a wiki descriptor document.
     * @throws XWikiException error when getting wiki descriptor document.
     */
    public XWikiServer getWikiAlias(String wikiName, int objectId, boolean validate, XWikiContext context)
        throws XWikiException
    {
        return XWikiServerClass.getInstance(context).getWikiAlias(wikiName, objectId, validate, context);
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
    public List<XWikiServer> getWikiAliasList(XWikiContext context) throws XWikiException
    {
        return XWikiServerClass.getInstance(context).searchXObjectDocuments(context);
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

    /**
     * Create a template wiki. The new template wiki is initialized with provided xar package.
     * <p>
     * A template wiki is a wiki which the XWiki.XWikiServerClass "visibility" field is set to "template".
     * 
     * @param wikiXObjectDocument a wiki descriptor document from which the new template wiki descriptor document will
     *            be created.
     * @param packageName the name of the attached XAR file to import in the new template wiki.
     * @param comment the comment to use when saving descriptor document.
     * @param context the XWiki context.
     * @throws XWikiException error when creating new wiki from XAR package.
     */
    public void createWikiTemplate(XWikiServer wikiXObjectDocument, String packageName, String comment,
        XWikiContext context) throws XWikiException
    {
        wikiXObjectDocument.setIsWikiTemplate(true);

        // Create empty wiki
        createNewWikiFromPackage(wikiXObjectDocument, packageName, false, comment, context);
    }
}
