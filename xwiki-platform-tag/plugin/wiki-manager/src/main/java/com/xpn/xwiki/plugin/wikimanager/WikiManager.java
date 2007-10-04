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
import com.xpn.xwiki.plugin.applicationmanager.doc.XWikiApplication;
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

public class WikiManager
{
    protected static final Log LOG = LogFactory.getLog(WikiManager.class);

    // ////////////////////////////////////////////////////////////////////////////

    private WikiManager()
    {
    }

    /**
     * Unique instance of WikiManager.
     */
    private static WikiManager _instance = null;

    /**
     * @return a unique instance of WikiManager. Thread safe.
     */
    public static WikiManager getInstance()
    {
        synchronized (WikiManager.class) {
            if (_instance == null)
                _instance = new WikiManager();
        }

        return _instance;
    }

    // ////////////////////////////////////////////////////////////////////////////
    // Utils

    /**
     * Encapsulate {@link com.xpn.xwiki.XWiki#saveDocument(XWikiDocument, XWikiContext)} adding wiki
     * switch.
     * 
     * @param wikiName the name of the wiki where to save the document.
     * @param doc the document to save.
     * @param context the XWiki Context.
     * @throws XWikiException
     * @see com.xpn.xwiki.XWiki#saveDocument(XWikiDocument, XWikiContext)
     */
    public void saveDocument(String wikiName, XWikiDocument doc, XWikiContext context)
        throws XWikiException
    {
        String database = context.getDatabase();

        try {
            context.setDatabase(wikiName);
            context.getWiki().saveDocument(doc, context);
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
     * @throws XWikiException
     * @see om.xpn.xwiki.XWiki#getDocument(String, XWikiContext)
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
     * Encapsulate {@link com.xpn.xwiki.XWiki#searchDocuments(String, XWikiContext)} adding wiki
     * switch.
     * 
     * @param wikiName the name of the wiki where to search for documents.
     * @param wheresql the conditions to add to HQL request.
     * @param context the XWiki context.
     * @return the list of documents that match the <code>wheresql</code> conditions. If nothing
     *         found return empty List.
     * @throws XWikiException
     * @see com.xpn.xwiki.XWiki#searchDocuments(String, XWikiContext)
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

    private Collection getDocsNameToInclude(String wiki, XWikiContext context)
        throws XWikiException
    {
        // Get applications manger
        ApplicationManagerPluginApi appmanager =
            (ApplicationManagerPluginApi) context.getWiki().getPluginApi(
                ApplicationManagerPlugin.PLUGIN_NAME, context);

        if (appmanager == null)
            return null;

        // //////////////////////////////////
        // Get documents to include

        String database = context.getDatabase();

        Collection docsToInclude = null;

        try {
            context.setDatabase(wiki);

            XWikiApplication rootApp = appmanager.getRootApplication();

            if (rootApp != null)
                docsToInclude = rootApp.getDocsNameToInclude(true, context);
            else
                docsToInclude =
                    XWikiApplication.getDocsNameToInclude(
                        appmanager.getApplicationDocumentList(), true, context);
        } finally {
            context.setDatabase(database);
        }

        return docsToInclude;
    }
    
    private Collection getDocsNameToLink(String wiki, XWikiContext context) throws XWikiException
    {
        // Get applications manger
        ApplicationManagerPluginApi appmanager =
            (ApplicationManagerPluginApi) context.getWiki().getPluginApi(
                ApplicationManagerPlugin.PLUGIN_NAME, context);

        if (appmanager == null)
            return null;

        // //////////////////////////////////
        // Get documents to link

        String database = context.getDatabase();

        Collection docsToLink = null;

        try {
            context.setDatabase(wiki);

            XWikiApplication rootApp = appmanager.getRootApplication();

            if (rootApp != null)
                docsToLink = rootApp.getDocsNameToLink(true, context);
            else
                docsToLink =
                    XWikiApplication.getDocsNameToLink(
                        appmanager.getApplicationDocumentList(), true, context);
        } finally {
            context.setDatabase(database);
        }

        return docsToLink;
    }

    private void copyWiki(String sourceWiki, String targetWiki, String language,
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

                targetDoc.setContent("#includeInContext(\"" + sourceWiki + ":" + docFullName
                    + "\")");
            }

            // Replace documents contents to link
            Collection docsNameToLink = getDocsNameToLink(sourceWiki, context);
            for (Iterator it = docsNameToLink.iterator(); it.hasNext();) {
                String docFullName = (String) it.next();
                XWikiDocument targetDoc = xwiki.getDocument(docFullName, context);

                targetDoc.setContent("#includeTopic(\"" + sourceWiki + ":" + docFullName + "\")");
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
     * @param context the XWiki context.
     * @return the new wiki descriptor document.
     * @throws XWikiException
     * @see #createNewWiki(XWikiServer, boolean, XWikiContext)
     * @see #createNewWikiFromTemplate(XWikiServer, String, boolean, XWikiContext)
     */
    public XWikiServer createNewWikiFromPackage(XWikiServer userWikiSuperDoc, String packageName,
        boolean failOnExist, XWikiContext context) throws XWikiException
    {
        return createNewWiki(userWikiSuperDoc, failOnExist, null, packageName, context);
    }

    /**
     * Create a new virtual wiki. The new wiki is a copy of provided existing wiki.
     * 
     * @param userWikiSuperDoc a wiki descriptor document from which the new wiki descriptor
     *            document will be created.
     * @param templateWikiName the of the wiki from where to copy document to the new wiki.
     * @param failOnExist if true throw exception when wiki already exist. If false overwrite
     *            existing wiki.
     * @param context the XWiki context.
     * @return the new wiki descriptor document.
     * @throws XWikiException
     * @see #createNewWiki(XWikiServer, boolean, XWikiContext)
     * @see #createNewWikiFromPackage(XWikiServer, String, boolean, XWikiContext)
     */
    public XWikiServer createNewWikiFromTemplate(XWikiServer userWikiSuperDoc,
        String templateWikiName, boolean failOnExist, XWikiContext context) throws XWikiException
    {
        return createNewWiki(userWikiSuperDoc, failOnExist, templateWikiName, null, context);
    }

    /**
     * Create a new empty virtual wiki.
     * 
     * @param userWikiSuperDoc a wiki descriptor document from which the new wiki descriptor
     *            document will be created.
     * @param failOnExist if true throw exception when wiki already exist. If false overwrite
     *            existing wiki.
     * @param context the XWiki context.
     * @return the new wiki descriptor document.
     * @throws XWikiException
     */
    public XWikiServer createNewWiki(XWikiServer userWikiSuperDoc, boolean failOnExist,
        XWikiContext context) throws XWikiException
    {
        return createNewWiki(userWikiSuperDoc, failOnExist, null, null, context);
    }

    private XWikiServer createNewWiki(XWikiServer userWikiSuperDoc, boolean failOnExist,
        String templateWikiName, String packageName, XWikiContext context) throws XWikiException
    {
        if (userWikiSuperDoc.getOwner().length() == 0)
            throw new WikiManagerException(WikiManagerException.ERROR_XWIKI_USER_INACTIVE,
                "Invalid user \"" + userWikiSuperDoc.getOwner() + "\"");

        XWiki xwiki = context.getWiki();

        if (!xwiki.isVirtual())
            throw new WikiManagerException(WikiManagerException.ERROR_WIKIMANAGER_XWIKI_NOT_VIRTUAL,
                "XWiki is not in virtual mode. Make sure property \"xwiki.virtual\" is setted to \"xwiki.virtual=1\" in xwiki.cfg file");

        XWikiServerClass wikiClass = XWikiServerClass.getInstance(context);

        String newWikiName = userWikiSuperDoc.getWikiName();

        String database = context.getDatabase();

        try {
            // Return to root database
            context.setDatabase(context.getMainXWiki());

            XWikiDocument userdoc =
                getDocument(xwiki.getDatabase(), userWikiSuperDoc.getOwner(), context);

            // User does not exist
            if (userdoc.isNew()) {
                if (LOG.isErrorEnabled())
                    LOG.error("Wiki creation (" + userWikiSuperDoc + ") failed: "
                        + "user does not exist");
                throw new WikiManagerException(WikiManagerException.ERROR_XWIKI_USER_DOES_NOT_EXIST,
                    "User \"" + userWikiSuperDoc.getOwner() + "\" does not exist");
            }

            // User is not active
            if (!(userdoc.getIntValue("XWiki.XWikiUsers", "active") == 1)) {
                if (LOG.isErrorEnabled())
                    LOG.error("Wiki creation (" + userWikiSuperDoc + ") failed: "
                        + "user is not active");
                throw new WikiManagerException(WikiManagerException.ERROR_XWIKI_USER_INACTIVE,
                    "User \"" + userWikiSuperDoc.getOwner() + "\" is not active");
            }

            // Wiki name forbidden
            String wikiForbiddenList = xwiki.Param("xwiki.virtual.reserved_wikis");
            if (Util.contains(newWikiName, wikiForbiddenList, ", ")) {
                if (LOG.isErrorEnabled())
                    LOG.error("Wiki creation (" + userWikiSuperDoc + ") failed: "
                        + "wiki name is forbidden");
                throw new WikiManagerException(WikiManagerException.ERROR_WIKIMANAGER_WIKI_NAME_FORBIDDEN,
                    "Wiki name \"" + newWikiName + "\" forbidden");
            }

            XWikiServer wikiSuperDocToSave;

            // If modify existing document
            if (!userWikiSuperDoc.isFromCache()) {
                // Verify is server page already exist
                XWikiDocument docToSave =
                    getDocument(context.getMainXWiki(), userWikiSuperDoc.getFullName(), context);

                if (!docToSave.isNew() && wikiClass.isInstance(docToSave, context)) {
                    // If we are not allowed to continue if server page already exists
                    if (failOnExist) {
                        if (LOG.isErrorEnabled())
                            LOG.error("Wiki creation (" + userWikiSuperDoc + ") failed: "
                                + "wiki server page already exists");
                        throw new WikiManagerException(WikiManagerException.ERROR_WIKIMANAGER_WIKISERVER_ALREADY_EXISTS,
                            "Wiki \"" + userWikiSuperDoc.getFullName()
                                + "\" document already exist");
                    } else if (LOG.isWarnEnabled())
                        LOG.warn("Wiki creation (" + userWikiSuperDoc + ") failed: "
                            + "wiki server page already exists");

                }

                wikiSuperDocToSave =
                    (XWikiServer) XWikiServerClass.getInstance(context).newSuperDocument(
                        docToSave, context);

                // clear entry in virtual wiki cache
                if (!wikiSuperDocToSave.getServer().equals(userWikiSuperDoc.getServer()))
                    xwiki.getVirtualWikiMap().flushEntry(userWikiSuperDoc.getServer());

                wikiSuperDocToSave.mergeBaseObject(userWikiSuperDoc);
            } else
                wikiSuperDocToSave = userWikiSuperDoc;

            // Create wiki database
            try {
                xwiki.getStore().createWiki(newWikiName, context);
            } catch (XWikiException e) {
                if (LOG.isErrorEnabled()) {
                    if (e.getCode() == 10010)
                        LOG.error("Wiki creation (" + userWikiSuperDoc + ") failed: "
                            + "wiki database already exists");
                    else if (e.getCode() == 10011)
                        LOG.error("Wiki creation (" + userWikiSuperDoc + ") failed: "
                            + "wiki database creation failed");
                    else
                        LOG.error("Wiki creation (" + userWikiSuperDoc + ") failed: "
                            + "wiki database creation threw exception", e);
                }
            } catch (Exception e) {
                LOG.error("Wiki creation (" + userWikiSuperDoc + ") failed: "
                    + "wiki database creation threw exception", e);
            }

            try {
                xwiki.updateDatabase(newWikiName, true, false, context);
            } catch (Exception e) {
                throw new WikiManagerException(WikiManagerException.ERROR_WIKIMANAGER_WIKISERVER_ALREADY_EXISTS,
                    "Wiki \"" + newWikiName + "\" database update failed",
                    e);
            }

            String language = userWikiSuperDoc.getLanguage();
            if (language.length() == 0)
                language = null;

            // Copy base wiki
            if (templateWikiName != null) {
                copyWiki(templateWikiName, newWikiName, language, context);
            }

            if (packageName != null) {
                // Prepare to import
                XWikiDocument doc = context.getDoc();

                XWikiAttachment packFile = doc.getAttachment(packageName);

                if (packFile == null)
                    throw new WikiManagerException(WikiManagerException.ERROR_WIKIMANAGER_CANNOT_CREATE_WIKI,
                        "Package " + packageName + " does not exists.");

                // Import
                PackageAPI importer =
                    ((PackageAPI) context.getWiki().getPluginApi("package", context));

                try {
                    importer.Import(packFile.getContent(context));
                } catch (IOException e) {
                    throw new WikiManagerException(WikiManagerException.ERROR_WIKIMANAGER_CANNOT_CREATE_WIKI,
                        "Fail to import package " + packageName,
                        e);
                }

                context.setDatabase(newWikiName);

                if (importer.install() == DocumentInfo.INSTALL_IMPOSSIBLE)
                    throw new WikiManagerException(WikiManagerException.ERROR_WIKIMANAGER_CANNOT_CREATE_WIKI,
                        "Fail to install package " + packageName);
            }

            // Create user page in his wiki
            // Let's not create it anymore.. this makes the creator loose super admin rights on
            // his wiki
            // xwiki.copyDocument(userWikiSuperDoc.getOwner(), database, newWikiName, language,
            // context);

            // Return to root database
            context.setDatabase(context.getMainXWiki());

            wikiSuperDocToSave.save();

            return wikiSuperDocToSave;
        } finally {
            context.setDatabase(database);
        }
    }

    /**
     * Delete an existing wiki.
     * <p>
     * Only delete the wiki descriptor the corresponding database always exist after delete.
     * 
     * @param wikiNameToDelete the name of te wiki to delete.
     * @param context the XWiki context.
     * @throws XWikiException
     */
    public void deleteWiki(String wikiNameToDelete, XWikiContext context) throws XWikiException
    {
        XWikiServer doc = getWiki(wikiNameToDelete, context, true);

        doc.delete(context);
    }

    /**
     * Get the wiki descriptor document.
     * 
     * @param wikiName the name of the wiki.
     * @param context the XWiki context.
     * @param validate when wiki descriptor document does not exist :
     *            <ul>
     *            <li> if true, throw an exception with code
     *            {@link WikiManagerException#ERROR_WIKIMANAGER_SERVER_DOES_NOT_EXIST}
     *            <li> if false, return new document unsaved
     *            </ul>
     * @return a wiki descriptor document.
     * @throws XWikiException
     */
    public XWikiServer getWiki(String wikiName, XWikiContext context, boolean validate)
        throws XWikiException
    {
        return XWikiServerClass.getInstance(context).getWikiServer(wikiName, context, validate);
    }

    /**
     * Get all wiki descriptors documents.
     * 
     * @param context the XWiki context.
     * @return a list of XWikiDocuments.
     * @throws XWikiException
     */
    public List getWikiDocumentList(XWikiContext context) throws XWikiException
    {
        return XWikiServerClass.getInstance(context).searchItemDocuments(context);
    }

    /**
     * Get all the wikis descriptors documents.
     * 
     * @param context the XWiki context.
     * @return a list of XWikiServer.
     * @throws XWikiException
     */
    public List getWikiList(XWikiContext context) throws XWikiException
    {
        List documentList = getWikiDocumentList(context);

        List applicationList = new ArrayList(documentList.size());

        for (Iterator it = documentList.iterator(); it.hasNext();) {
            applicationList.add(XWikiServerClass.getInstance(context).newSuperDocument(
                (XWikiDocument) it.next(), context));
        }

        return applicationList;
    }

    /**
     * Indicate of wiki descriptor document exist.
     * 
     * @param wikiName the name of the wiki.
     * @param context the XWiki context.
     * @return true if wiki descriptor exist, false if not.
     */
    public boolean isWikiExist(String wikiName, XWikiContext context)
    {
        try {
            return getWiki(wikiName, context, true) != null;
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
     * @param context the XWiki context.
     * @param validate when wiki descriptor document does not exist :
     *            <ul>
     *            <li> if true, throw an exception with code
     *            {@link WikiManagerException#ERROR_WIKIMANAGER_SERVER_DOES_NOT_EXIST}
     *            <li> if false, return new document unsaved
     *            </ul>
     * @return a wiki descriptor document.
     * @throws XWikiException
     */
    public XWikiServer getWikiTemplate(String wikiName, XWikiContext context, boolean validate)
        throws XWikiException
    {
        return XWikiServerClass.getInstance(context).getWikiTemplateServer(wikiName, context,
            validate);
    }

    /**
     * Get all the templates wikis descriptors documents.
     * <p>
     * A template wiki is a wiki which the XWiki.XWikiServerClass "visibility" field is set to
     * "template".
     * 
     * @param context the XWiki context.
     * @return a list of XWikiDocuments.
     * @throws XWikiException
     */
    public List getWikiTemplateList(XWikiContext context) throws XWikiException
    {
        return XWikiServerClass.getInstance(context).searchItemDocumentsByField(
            XWikiServerClass.FIELD_visibility, XWikiServerClass.FIELDL_visibility_template,
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
     * @param context the XWiki context.
     * @throws XWikiException
     */
    public void createWikiTemplate(XWikiServer wikiSuperDocument, String packageName,
        XWikiContext context) throws XWikiException
    {
        wikiSuperDocument.setVisibility(XWikiServerClass.FIELDL_visibility_template);

        // Create empty wiki
        WikiManager.getInstance().createNewWikiFromPackage(wikiSuperDocument, packageName, false,
            context);
    }
}
