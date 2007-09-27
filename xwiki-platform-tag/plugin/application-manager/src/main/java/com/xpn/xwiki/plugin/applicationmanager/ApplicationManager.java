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

package com.xpn.xwiki.plugin.applicationmanager;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiAttachment;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.objects.classes.ListClass;
import com.xpn.xwiki.plugin.applicationmanager.doc.XWikiApplication;
import com.xpn.xwiki.plugin.applicationmanager.doc.XWikiApplicationClass;
import com.xpn.xwiki.plugin.packaging.DocumentInfo;
import com.xpn.xwiki.plugin.packaging.DocumentInfoAPI;
import com.xpn.xwiki.plugin.packaging.PackageAPI;

public class ApplicationManager
{
    protected static final Log LOG = LogFactory.getLog(ApplicationManager.class);

    // ////////////////////////////////////////////////////////////////////////////

    private ApplicationManager()
    {
    }

    private static ApplicationManager _instance = null;

    public static ApplicationManager getInstance()
    {
        synchronized (ApplicationManager.class) {
            if (_instance == null)
                _instance = new ApplicationManager();
        }

        return _instance;
    }

    // ////////////////////////////////////////////////////////////////////////////
    // Applications management

    public XWikiApplication getRootApplication(XWikiContext context) throws XWikiException
    {
        XWiki xwiki = context.getWiki();

        String docFullName = xwiki.getXWikiPreference("rootapplication", null, context);

        if (docFullName != null) {
            XWikiDocument doc = xwiki.getDocument(docFullName, context);

            if (!doc.isNew()) {
                return (XWikiApplication)XWikiApplicationClass.getInstance(context).newSuperDocument(doc, context);
            }
        }

        return null;
    }

    public List getApplicationDocumentList(XWikiContext context) throws XWikiException
    {
        return XWikiApplicationClass.getInstance(context).searchItemDocuments(context);
    }

    public List getApplicationList(XWikiContext context) throws XWikiException
    {
        List documentList = getApplicationDocumentList(context);

        List applicationList = new ArrayList(documentList.size());

        for (Iterator it = documentList.iterator(); it.hasNext();) {
            applicationList.add(XWikiApplicationClass.getInstance(context).newSuperDocument((XWikiDocument) it.next(), context));
        }

        return applicationList;
    }

    public void createApplication(XWikiApplication userAppSuperDoc, boolean failOnExist,
        XWikiContext context) throws XWikiException
    {
        XWiki xwiki = context.getWiki();
        XWikiApplicationClass appClass = XWikiApplicationClass.getInstance(context);

        // Verify is server page already exist
        XWikiDocument docToSave =
            xwiki.getDocument(appClass.getItemDocumentDefaultFullName(userAppSuperDoc
                .getAppName(), context), context);

        if (!docToSave.isNew() && appClass.isInstance(docToSave, context)) {
            // If we are not allowed to continue if server page already exists
            if (failOnExist) {
                if (LOG.isErrorEnabled())
                    LOG.error("Wiki creation (" + userAppSuperDoc + ") failed: "
                        + "wiki server page already exists");
                throw new ApplicationManagerException(ApplicationManagerException.ERROR_APPLICATIONMANAGER_APPDOC_ALREADY_EXISTS,
                    "Application \"" + userAppSuperDoc.getAppName() + "\" document already exist");
            } else if (LOG.isWarnEnabled())
                LOG.warn("Application creation (" + userAppSuperDoc + ") failed: "
                    + "application page already exists");

        }

        XWikiApplication appSuperDocToSave = (XWikiApplication)XWikiApplicationClass.getInstance(context).newSuperDocument(docToSave, context);

        appSuperDocToSave.mergeBaseObject(userAppSuperDoc);

        appSuperDocToSave.save();

        // Update user document with the new document name
        userAppSuperDoc.setFullName(appSuperDocToSave.getFullName());
    }

    public void deleteApplication(String appName, XWikiContext context) throws XWikiException
    {
        XWikiApplication app = getApplication(appName, context, true);

        app.delete(context);
    }

    public XWikiApplication getApplication(String appName, XWikiContext context, boolean validate)
        throws XWikiException
    {
        return XWikiApplicationClass.getInstance(context).getApplication(appName, context,
            validate);
    }

    public void updateAllApplicationTranslation(XWikiContext context) throws XWikiException
    {
        XWiki xwiki = context.getWiki();

        XWikiDocument prefsDoc = xwiki.getDocument("XWiki.XWikiPreferences", context);
        BaseObject prefsObject = prefsDoc.getObject("XWiki.XWikiPreferences");

        String documentBundles = prefsObject.getStringValue("documentBundles");
        List translationPrefs = ListClass.getListFromString(documentBundles, ",", true);

        boolean updateprefs = false;

        List applist = getApplicationList(context);

        for (Iterator it = applist.iterator(); it.hasNext();) {
            XWikiApplication app = (XWikiApplication) it.next();

            updateprefs |= updateApplicationTranslation(translationPrefs, app, context);
        }

        if (updateprefs) {
            prefsObject.setStringValue("documentBundles", StringUtils.join(translationPrefs
                .toArray(), ","));
            xwiki.saveDocument(prefsDoc, context);
        }
    }

    public void updateApplicationTranslation(XWikiApplication app, XWikiContext context)
        throws XWikiException
    {
        XWiki xwiki = context.getWiki();

        XWikiDocument prefsDoc = xwiki.getDocument("XWiki.XWikiPreferences", context);
        BaseObject prefsObject = prefsDoc.getObject("XWiki.XWikiPreferences");

        String documentBundles = prefsObject.getStringValue("documentBundles");
        List translationPrefs = ListClass.getListFromString(documentBundles, ",", true);

        boolean updateprefs = updateApplicationTranslation(translationPrefs, app, context);

        if (updateprefs) {
            prefsObject.setStringValue("documentBundles", StringUtils.join(translationPrefs
                .toArray(), ","));
            xwiki.saveDocument(prefsDoc, context);
        }
    }

    public boolean updateApplicationTranslation(List translationPrefs, XWikiApplication app,
        XWikiContext context)
    {
        boolean updateprefs = false;

        List translationDocs = app.getTranslationDocs();
        for (Iterator it2 = translationDocs.iterator(); it2.hasNext();) {
            String translationDoc = (String) it2.next();

            if (!translationPrefs.contains(translationDoc)) {
                translationPrefs.add(translationDoc);
                updateprefs = true;
            }
        }

        return updateprefs;
    }

    public void exportApplicationXAR(String appName, boolean recurse, boolean withDocHistory, XWikiContext context) throws XWikiException,
        IOException
    {
        XWikiApplication app =
            ApplicationManager.getInstance().getApplication(appName, context, true);

        PackageAPI export = ((PackageAPI) context.getWiki().getPluginApi("package", context));

        export.setName(app.getAppName() + "-" + app.getAppVersion());

        Set documents = app.getDocumentsNames(recurse, true, context);
        for (Iterator it = documents.iterator(); it.hasNext();) {
            export.add((String) it.next(), DocumentInfo.ACTION_OVERWRITE);
        }
        
        export.setWithVersions(withDocHistory);

        export.export();
    }

    public void importApplication(XWikiDocument packageDoc, String packageName,
        XWikiContext context) throws XWikiException
    {
        XWiki xwiki = context.getWiki();

        XWikiAttachment packFile = packageDoc.getAttachment(packageName);

        if (packFile == null)
            throw new ApplicationManagerException(XWikiException.ERROR_XWIKI_UNKNOWN, "Package "
                + packageName + " does not exists.");

        // Import
        PackageAPI importer = ((PackageAPI) context.getWiki().getPluginApi("package", context));

        try {
            importer.Import(packFile.getContent(context));
        } catch (IOException e) {
            throw new ApplicationManagerException(XWikiException.ERROR_XWIKI_UNKNOWN,
                "Fail to import package " + packageName,
                e);
        }

        if (importer.install() == DocumentInfo.INSTALL_IMPOSSIBLE)
            throw new ApplicationManagerException(XWikiException.ERROR_XWIKI_UNKNOWN,
                "Fail to import package " + packageName);

        // Update translation documents
        XWikiDocument prefsDoc = xwiki.getDocument("XWiki.XWikiPreferences", context);
        BaseObject prefsObject = prefsDoc.getObject("XWiki.XWikiPreferences");

        String documentBundles = prefsObject.getStringValue("documentBundles");
        List translationPrefs = ListClass.getListFromString(documentBundles, ",", true);

        boolean updateprefs = false;

        for (Iterator it = importer.getFiles().iterator(); it.hasNext();) {
            DocumentInfoAPI docinfo = (DocumentInfoAPI) it.next();
            XWikiDocument doc = docinfo.getDocInfo().getDoc();

            if (XWikiApplicationClass.getInstance(context).isInstance(doc, context))
                updateprefs |=
                    updateApplicationTranslation(translationPrefs, (XWikiApplication)XWikiApplicationClass.getInstance(context).newSuperDocument(doc,
                        context), context);
        }

        if (updateprefs) {
            prefsObject.setStringValue("documentBundles", StringUtils.join(translationPrefs
                .toArray(), ","));
            xwiki.saveDocument(prefsDoc, context);
        }
    }
}
