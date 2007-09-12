/*
 * Copyright 2006-2007, XpertNet SARL, and individual contributors.
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

package com.xpn.xwiki.plugin.multiwiki;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.plugin.applicationmanager.ApplicationManagerPlugin;
import com.xpn.xwiki.plugin.applicationmanager.ApplicationManagerPluginApi;
import com.xpn.xwiki.plugin.applicationmanager.doc.XWikiApplication;
import com.xpn.xwiki.plugin.multiwiki.doc.XWikiServer;
import com.xpn.xwiki.plugin.multiwiki.doc.XWikiServerClass;
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

    private static WikiManager _instance = null;

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

    public void saveDocument(String wiki, XWikiDocument doc, XWikiContext context)
        throws XWikiException
    {
        String database = context.getDatabase();

        try {
            context.setDatabase(wiki);
            context.getWiki().saveDocument(doc, context);
        } finally {
            context.setDatabase(database);
        }
    }

    public XWikiDocument getDocument(String wiki, String fullname, XWikiContext context)
        throws XWikiException
    {
        String database = context.getDatabase();

        try {
            context.setDatabase(wiki);
            return context.getWiki().getDocument(fullname, context);
        } finally {
            context.setDatabase(database);
        }
    }

    public List searchDocuments(String wiki, String wheresql, XWikiContext context)
        throws XWikiException
    {
        String database = context.getDatabase();

        try {
            context.setDatabase(wiki);
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
        ApplicationManagerPluginApi appmanager = (ApplicationManagerPluginApi) context.getWiki().getPluginApi(ApplicationManagerPlugin.PLUGIN_NAME, context);
        
        if (appmanager == null)
            return null;

        ////////////////////////////////////
        // Get documents to include
        
        String database = context.getDatabase();

        Collection docsToInclude = null;

        try {
            context.setDatabase(wiki);
            
            XWikiApplication rootApp = appmanager.getRootApplication(context);

            if (rootApp != null)
                docsToInclude = rootApp.getDocsNameToInclude(true, context);
            else
                docsToInclude =
                    XWikiApplication.getDocsNameToInclude(appmanager.getApplicationDocumentList(), true, context);
        } finally {
            context.setDatabase(database);
        }

        return docsToInclude;
    }

    private void copyWiki(String sourceWiki, String targetWiki, String language,
        XWikiContext context) throws XWikiException
    {
        /* TODO : add included copy support to xwiki-core and use this code
         * context.getWiki().copyWikiWeb(null, sourceWiki,
            getDocsNameToInclude(sourceWiki, context), targetWiki, language, true, context);*/
    	
    	XWiki xwiki = context.getWiki();
    	
    	// Copy all the wiki
    	xwiki.copyWikiWeb(null, sourceWiki, targetWiki, language, true, context);
    	
    	// Replace documents contents to include
    	String database = context.getDatabase();
    	try {
    		context.setDatabase(targetWiki);
    		
    		Collection docsNameToInclude = getDocsNameToInclude(sourceWiki, context);
    		for (Iterator it = docsNameToInclude.iterator(); it.hasNext();) {
    			String docFullName = (String)it.next();
    			XWikiDocument targetDoc = xwiki.getDocument(docFullName, context);
    			
    			targetDoc.setContent("#includeInContext(\"" + sourceWiki + ":" + docFullName + "\")");
    		}
    	} finally {
    		context.setDatabase(database);
    	}
    }

    public XWikiServer createNewWikiFromPackage(XWikiServer userWikiSuperDoc, String packageName,
        boolean failOnExist, XWikiContext context) throws XWikiException
    {
        return createNewWiki(userWikiSuperDoc, failOnExist, null, packageName, context);
    }

    public XWikiServer createNewWikiFromTemplate(XWikiServer userWikiSuperDoc, String templateWikiName,
        boolean failOnExist, XWikiContext context) throws XWikiException
    {
        return createNewWiki(userWikiSuperDoc, failOnExist, templateWikiName, null, context);
    }
    
    public XWikiServer createNewWiki(XWikiServer userWikiSuperDoc, boolean failOnExist, XWikiContext context) throws XWikiException
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
                throw new WikiManagerException(WikiManagerException.ERROR_MULTIWIKI_WIKI_NAME_FORBIDDEN,
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
                        throw new WikiManagerException(WikiManagerException.ERROR_MULTIWIKI_WIKISERVER_ALREADY_EXISTS,
                            "Wiki \"" + userWikiSuperDoc.getFullName() + "\" document already exist");
                    } else if (LOG.isWarnEnabled())
                        LOG.warn("Wiki creation (" + userWikiSuperDoc + ") failed: "
                            + "wiki server page already exists");

                }

                wikiSuperDocToSave = (XWikiServer)XWikiServerClass.getInstance(context).newSuperDocument(docToSave, context);

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
                throw new WikiManagerException(WikiManagerException.ERROR_MULTIWIKI_WIKISERVER_ALREADY_EXISTS,
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
                    throw new WikiManagerException(XWikiException.ERROR_XWIKI_UNKNOWN,
                        "Package " + packageName + " does not exists.");

                // Change database
                context.setDatabase(newWikiName);

                // Import
                PackageAPI importer =
                    ((PackageAPI) context.getWiki().getPluginApi("package", context));

                try {
                    importer.Import(packFile.getContent(context));
                } catch (IOException e) {
                    throw new WikiManagerException(XWikiException.ERROR_XWIKI_UNKNOWN,
                        "Fail to import package " + packageName,
                        e);
                }

                if (importer.install() == DocumentInfo.INSTALL_IMPOSSIBLE)
                    throw new WikiManagerException(XWikiException.ERROR_XWIKI_UNKNOWN,
                        "Fail to import package " + packageName);
            }

            // Create user page in his wiki
            // Let's not create it anymore.. this makes the creator loose super admin rights on
            // his wiki
            //xwiki.copyDocument(userWikiSuperDoc.getOwner(), database, newWikiName, language, context);

            // Return to root database
            context.setDatabase(context.getMainXWiki());

            wikiSuperDocToSave.save();
            
            return wikiSuperDocToSave;
        } finally {
            context.setDatabase(database);
        }
    }

    public void deleteWiki(String wikiNameToDelete, XWikiContext context) throws XWikiException
    {
        XWikiServer doc = getWiki(wikiNameToDelete, context, true);

        doc.delete(context);
    }

    public XWikiServer getWiki(String wikiName, XWikiContext context, boolean validate)
        throws XWikiException
    {
        return XWikiServerClass.getInstance(context).getWikiServer(wikiName, context, validate);
    }

    public List getWikiDocumentList(XWikiContext context) throws XWikiException
    {
        return XWikiServerClass.getInstance(context).searchItemDocuments(context);
    }
    
    public List getWikiList(XWikiContext context) throws XWikiException
    {
        List documentList = getWikiDocumentList(context);

        List applicationList = new ArrayList(documentList.size());

        for (Iterator it = documentList.iterator(); it.hasNext();) {
            applicationList.add(XWikiServerClass.getInstance(context).newSuperDocument((XWikiDocument) it.next(), context));
        }

        return applicationList;
    }

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

    public XWikiServer getWikiTemplate(String wikiName, XWikiContext context, boolean validate)
        throws XWikiException
    {
        return XWikiServerClass.getInstance(context).getWikiTemplateServer(wikiName, context, validate);
    }

    public List getWikiTemplateList(XWikiContext context) throws XWikiException
    {
        return XWikiServerClass.getInstance(context).searchItemDocumentsByField(
            XWikiServerClass.FIELD_visibility, XWikiServerClass.FIELDL_visibility_template,
            "StringProperty", context);
    }

    public void createWikiTemplate(XWikiServer wikiSuperDocument, String packageName,
        XWikiContext context) throws XWikiException
    {
        wikiSuperDocument.setVisibility(XWikiServerClass.FIELDL_visibility_template);

        // Create empty wiki
        WikiManager.getInstance().createNewWikiFromPackage(wikiSuperDocument, packageName, false,
            context);
    }
}
