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
import java.util.Collection;
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
import com.xpn.xwiki.notify.XWikiDocChangeNotificationInterface;
import com.xpn.xwiki.notify.XWikiNotificationRule;
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.objects.classes.ListClass;
import com.xpn.xwiki.plugin.applicationmanager.core.plugin.XWikiPluginMessageTool;
import com.xpn.xwiki.plugin.applicationmanager.doc.XWikiApplication;
import com.xpn.xwiki.plugin.applicationmanager.doc.XWikiApplicationClass;
import com.xpn.xwiki.plugin.packaging.DocumentInfo;
import com.xpn.xwiki.plugin.packaging.DocumentInfoAPI;
import com.xpn.xwiki.plugin.packaging.PackageAPI;

/**
 * Hidden toolkit used by the plugin API that make all the plugins actions.
 * 
 * @version $Id: $
 */
final class ApplicationManager implements XWikiDocChangeNotificationInterface
{
    /**
     * The logging tool.
     */
    protected static final Log LOG = LogFactory.getLog(ApplicationManager.class);

    /**
     * Wiki preferences document and class full name.
     */
    private static final String XWIKIPREFERENCES = "XWiki.XWikiPreferences";

    /**
     * "documentBundles" list field name of the XWiki.XWikiPreferences class.
     */
    private static final String XWIKIPREFERENCES_DOCUMENTBUNDLES = "documentBundles";

    /**
     * "documentBundles" list field separator of the XWiki.XWikiPreferences class.
     */
    private static final String XWIKIPREFERENCES_DOCUMENTBUNDLES_SEP = ",";

    /**
     * The name of the internal packaging plugin.
     */
    private static final String PACKAGEPLUGIN_NAME = "package";

    /**
     * Unique instance of ApplicationManager.
     */
    private static ApplicationManager instance;

    // ////////////////////////////////////////////////////////////////////////////

    /**
     * Hidden constructor of ApplicationManager only access via getInstance().
     */
    private ApplicationManager()
    {
    }

    /**
     * @return a unique instance of ApplicationManager.
     */
    public static ApplicationManager getInstance()
    {
        synchronized (ApplicationManager.class) {
            if (instance == null) {
                instance = new ApplicationManager();
            }
        }

        return instance;
    }

    /**
     * Get the {@link XWikiPluginMessageTool} to use with ApplicationManager.
     * 
     * @param context the XWiki context.
     * @return a translated strings manager.
     */
    public XWikiPluginMessageTool getMessageTool(XWikiContext context)
    {
        return ApplicationManagerMessageTool.getDefault(context);
    }

    /**
     * {@inheritDoc}
     * 
     * @see com.xpn.xwiki.notify.XWikiDocChangeNotificationInterface#notify(com.xpn.xwiki.notify.XWikiNotificationRule,
     *      com.xpn.xwiki.doc.XWikiDocument, com.xpn.xwiki.doc.XWikiDocument, int, com.xpn.xwiki.XWikiContext)
     */
    public void notify(XWikiNotificationRule rule, XWikiDocument newdoc, XWikiDocument olddoc, int event,
        XWikiContext context)
    {
        try {
            if (newdoc != null && XWikiApplicationClass.isApplication(newdoc)) {

                List<XWikiApplication> appList =
                    XWikiApplicationClass.getInstance(context).newXObjectDocumentList(newdoc, context);
                updateApplicationsTranslation(appList, getMessageTool(context).get(
                    ApplicationManagerMessageTool.COMMENT_AUTOUPDATETRANSLATIONS, newdoc.getFullName()), context);
            }
        } catch (XWikiException e) {
            LOG.error(getMessageTool(context).get(ApplicationManagerMessageTool.LOG_AUTOUPDATETRANSLATIONS,
                newdoc.getFullName()), e);
        }
    }

    // ////////////////////////////////////////////////////////////////////////////
    // Applications management

    /**
     * Get the current wiki root application.
     * 
     * @param context the XWiki context.
     * @return the root application descriptor document. If can't find root application return null.
     * @throws XWikiException error when getting root application descriptor document from database.
     */
    public XWikiApplication getRootApplication(XWikiContext context) throws XWikiException
    {
        XWiki xwiki = context.getWiki();

        String docFullName = xwiki.getXWikiPreference("rootapplication", null, context);

        if (docFullName != null) {
            XWikiDocument doc = xwiki.getDocument(docFullName, context);

            if (!doc.isNew()) {
                return XWikiApplicationClass.getInstance(context).newXObjectDocument(doc, 0, context);
            }
        }

        return null;
    }

    /**
     * Search for all document containing a object of class XWikiApplicationClass.
     * 
     * @param context the XWiki context.
     * @return a list if {@link XWikiApplication}.
     * @throws XWikiException error when searching documents.
     */
    public List<XWikiApplication> getApplicationList(XWikiContext context) throws XWikiException
    {
        return XWikiApplicationClass.getInstance(context).searchXObjectDocuments(context);
    }

    /**
     * Create a new application descriptor base on provided application descriptor.
     * 
     * @param userAppSuperDoc appXObjectDocument the user application descriptor from which new descriptor will be
     *            created.
     * @param failOnExist if true fail if the application descriptor to create already exists.
     * @param comment a comment used when saving application descriptor document.
     * @param context the XWiki Context.
     * @throws XWikiException error when calling for {@link XWiki#getDocument(String, XWikiContext)}
     */
    public void createApplication(XWikiApplication userAppSuperDoc, boolean failOnExist, String comment,
        XWikiContext context) throws XWikiException
    {
        XWiki xwiki = context.getWiki();
        XWikiApplicationClass appClass = XWikiApplicationClass.getInstance(context);

        // Verify is server page already exist
        XWikiDocument docToSave =
            xwiki.getDocument(appClass.getItemDocumentDefaultFullName(userAppSuperDoc.getAppName(), context), context);

        if (!docToSave.isNew() && appClass.isInstance(docToSave)) {
            // If we are not allowed to continue if server page already exists
            if (failOnExist) {
                if (LOG.isErrorEnabled()) {
                    LOG.error(getMessageTool(context).get(ApplicationManagerMessageTool.ERROR_APPPAGEALREADYEXISTS,
                        userAppSuperDoc.getAppName()));
                }

                throw new ApplicationManagerException(ApplicationManagerException.ERROR_AM_APPDOCALREADYEXISTS,
                    getMessageTool(context).get(ApplicationManagerMessageTool.ERROR_APPPAGEALREADYEXISTS,
                        userAppSuperDoc.getAppName()));
            } else if (LOG.isWarnEnabled()) {
                LOG.warn(getMessageTool(context).get(ApplicationManagerMessageTool.ERROR_APPPAGEALREADYEXISTS,
                    userAppSuperDoc.getAppName()));
            }

        }

        XWikiApplication appSuperDocToSave =
            XWikiApplicationClass.getInstance(context).newXObjectDocument(docToSave, 0, context);

        appSuperDocToSave.mergeObject(userAppSuperDoc);

        appSuperDocToSave.save(comment);

        // Update user document with the new document name
        userAppSuperDoc.setFullName(appSuperDocToSave.getFullName());
    }

    /**
     * Delete an application descriptor document.
     * 
     * @param appName the name of the application.
     * @param context the XWiki context.
     * @throws XWikiException error when calling for {@link XWikiApplication#delete()}
     */
    public void deleteApplication(String appName, XWikiContext context) throws XWikiException
    {
        XWikiApplication app = getApplication(appName, context, true);

        app.delete();
    }

    /**
     * Get the application descriptor document of the provided application name.
     * 
     * @param appName the name of the application.
     * @param context the XWiki context.
     * @param validate indicate if it return new XWikiDocument or throw exception if application descriptor does not
     *            exist.
     * @return the XWikiApplication representing application descriptor.
     * @throws XWikiException error when searching for application descriptor document.
     */
    public XWikiApplication getApplication(String appName, XWikiContext context, boolean validate)
        throws XWikiException
    {
        return XWikiApplicationClass.getInstance(context).getApplication(appName, validate, context);
    }

    /**
     * Reload xwiki application. It means :
     * <ul>
     * <li> update XWikiPreferences with application translation documents.
     * </ul>
     * 
     * @param app the application to reload.
     * @param comment the comment to use when saving documents.
     * @param context the XWiki context.
     * @throws XWikiException error when :
     *             <ul>
     *             <li>getting wiki preferences document.</li>
     *             <li>or saving wiki preferences document.</li>
     *             </ul>
     */
    public void reloadApplication(XWikiApplication app, String comment, XWikiContext context) throws XWikiException
    {
        updateApplicationTranslation(app, comment, context);
    }

    /**
     * Reload all xwiki applications. It means :
     * <ul>
     * <li> update XWikiPreferences with application translation documents.
     * </ul>
     * 
     * @param comment the comment to use when saving documents.
     * @param context the XWiki context.
     * @throws XWikiException error when :
     *             <ul>
     *             <li>getting wiki preferences document.</li>
     *             <li>or searching for all applications in the wiki.</li>
     *             <li>or saving wiki preferences document.</li>
     *             </ul>
     */
    public void reloadAllApplications(String comment, XWikiContext context) throws XWikiException
    {
        List<XWikiApplication> applist = getApplicationList(context);

        for (XWikiApplication app : applist) {
            ApplicationManager.getInstance().updateApplicationTranslation(app, comment, context);
        }
    }

    /**
     * Insert in XWiki.XWikiPreferences "documentBundles" field the translation documents of all applications in the
     * context's wiki.
     * 
     * @param applications the applications for which to update translations informations.
     * @param comment a comment used when saving XWiki.
     * @param context the XWiki context.
     * @throws XWikiException error when :
     *             <ul>
     *             <li>getting wiki preferences document.</li>
     *             <li>or searching for all applications in the wiki.</li>
     *             <li>or saving wiki preferences document.</li>
     *             </ul>
     */
    public void updateApplicationsTranslation(Collection<XWikiApplication> applications, String comment,
        XWikiContext context) throws XWikiException
    {
        XWiki xwiki = context.getWiki();

        XWikiDocument prefsDoc = xwiki.getDocument(XWIKIPREFERENCES, context);
        BaseObject prefsObject = prefsDoc.getObject(XWIKIPREFERENCES);

        if (prefsObject != null) {
            String documentBundles = prefsObject.getStringValue(XWIKIPREFERENCES_DOCUMENTBUNDLES);
            List<String> translationPrefs =
                ListClass.getListFromString(documentBundles, XWIKIPREFERENCES_DOCUMENTBUNDLES_SEP, true);

            boolean updateprefs = false;

            for (XWikiApplication app : applications) {
                updateprefs |= updateApplicationTranslation(translationPrefs, app);
            }

            if (updateprefs) {
                prefsObject.setStringValue(XWIKIPREFERENCES_DOCUMENTBUNDLES, StringUtils.join(translationPrefs
                    .toArray(), XWIKIPREFERENCES_DOCUMENTBUNDLES_SEP));
                xwiki.saveDocument(prefsDoc, comment, context);
            }
        }
    }

    /**
     * Insert in XWiki.XWikiPreferences "documentBundles" field the translation documents of all applications in the
     * context's wiki.
     * 
     * @param comment a comment used when saving XWiki.
     * @param context the XWiki context.
     * @throws XWikiException error when :
     *             <ul>
     *             <li>getting wiki preferences document.</li>
     *             <li>or searching for all applications in the wiki.</li>
     *             <li>or saving wiki preferences document.</li>
     *             </ul>
     */
    public void updateAllApplicationTranslation(String comment, XWikiContext context) throws XWikiException
    {
        updateApplicationsTranslation(getApplicationList(context), comment, context);
    }

    /**
     * Insert in XWiki.XWikiPreferences "documentBundles" field the translation documents of the provided application.
     * 
     * @param app the application descriptor.
     * @param comment a comment used when saving XWiki.
     * @param context the XWiki context.
     * @throws XWikiException error when :
     *             <ul>
     *             <li>getting wiki preferences document.</li>
     *             <li>or saving wiki preferences document.</li>
     *             </ul>
     */
    public void updateApplicationTranslation(XWikiApplication app, String comment, XWikiContext context)
        throws XWikiException
    {
        XWiki xwiki = context.getWiki();

        XWikiDocument prefsDoc = xwiki.getDocument(XWIKIPREFERENCES, context);
        BaseObject prefsObject = prefsDoc.getObject(XWIKIPREFERENCES);

        if (prefsObject != null) {
            String documentBundles = prefsObject.getStringValue(XWIKIPREFERENCES_DOCUMENTBUNDLES);
            List<String> translationPrefs =
                ListClass.getListFromString(documentBundles, XWIKIPREFERENCES_DOCUMENTBUNDLES_SEP, true);

            boolean updateprefs = updateApplicationTranslation(translationPrefs, app);

            if (updateprefs) {
                prefsObject.setStringValue(XWIKIPREFERENCES_DOCUMENTBUNDLES, StringUtils.join(translationPrefs
                    .toArray(), XWIKIPREFERENCES_DOCUMENTBUNDLES_SEP));
                xwiki.saveDocument(prefsDoc, comment, context);
            }
        }
    }

    /**
     * Insert in <code>translationPrefs</code> the translation documents of the provided application.
     * 
     * @param translationPrefs the list of translation documents to complete.
     * @param app the application's descriptor.
     * @return true if at least one document has been inserted in <code>translationPrefs</code>.
     */
    public boolean updateApplicationTranslation(List<String> translationPrefs, XWikiApplication app)
    {
        boolean updateprefs = false;

        List<String> translationDocs = app.getTranslationDocs();
        for (String translationDoc : translationDocs) {
            if (!translationPrefs.contains(translationDoc)) {
                translationPrefs.add(translationDoc);
                updateprefs = true;
            }
        }

        return updateprefs;
    }

    /**
     * Export an application into XAR using Packaging plugin.
     * 
     * @param appName the name of the application to export.
     * @param recurse indicate if dependencies applications has to be included in the package.
     * @param withDocHistory indicate if history of documents is exported.
     * @param context the XWiki context.
     * @throws XWikiException error when :
     *             <ul>
     *             <li>getting application descriptor document to export.</li>
     *             <li>or getting application's documents to export.</li>
     *             <li>or when apply export.</li>
     *             </ul>
     * @throws IOException error when apply export.
     */
    public void exportApplicationXAR(String appName, boolean recurse, boolean withDocHistory, XWikiContext context)
        throws XWikiException, IOException
    {
        XWikiApplication app = ApplicationManager.getInstance().getApplication(appName, context, true);

        PackageAPI export = ((PackageAPI) context.getWiki().getPluginApi(PACKAGEPLUGIN_NAME, context));

        export.setName(app.getAppName() + "-" + app.getAppVersion());

        Set<String> documents = app.getDocumentsNames(recurse, true);
        for (String documentName : documents) {
            export.add(documentName, DocumentInfo.ACTION_OVERWRITE);
        }

        export.setWithVersions(withDocHistory);

        export.export();
    }

    /**
     * Import attached application XAR into current wiki and do all actions needed to installation an application. See
     * {@link #reloadApplication(XWikiApplication, String, XWikiContext)} for more.
     * 
     * @param packageDoc the document where package to import is attached.
     * @param packageName the name of the attached XAR file to import.
     * @param comment a comment used update XWiki.XWikiPreferences.
     * @param context the XWiki context.
     * @throws XWikiException error when :
     *             <ul>
     *             <li>getting attached package file.</li>
     *             <li>or load package in memory.</li>
     *             <li>or installing loaded document in database</li>
     *             <li>or apply application initialization for each application descriptor document.</li>
     *             </ul>
     */
    public void importApplication(XWikiDocument packageDoc, String packageName, String comment, XWikiContext context)
        throws XWikiException
    {
        XWikiAttachment packFile = packageDoc.getAttachment(packageName);

        if (packFile == null) {
            throw new ApplicationManagerException(XWikiException.ERROR_XWIKI_UNKNOWN, getMessageTool(context).get(
                ApplicationManagerMessageTool.ERROR_IMORT_PKGDOESNOTEXISTS, packageName));
        }

        // Import
        PackageAPI importer = ((PackageAPI) context.getWiki().getPluginApi(PACKAGEPLUGIN_NAME, context));

        try {
            importer.Import(packFile.getContent(context));
        } catch (IOException e) {
            throw new ApplicationManagerException(XWikiException.ERROR_XWIKI_UNKNOWN, getMessageTool(context).get(
                ApplicationManagerMessageTool.ERROR_IMORT_IMPORT, packageName), e);
        }

        if (importer.install() == DocumentInfo.INSTALL_IMPOSSIBLE) {
            throw new ApplicationManagerException(XWikiException.ERROR_XWIKI_UNKNOWN, getMessageTool(context).get(
                ApplicationManagerMessageTool.ERROR_IMORT_INSTALL, packageName));
        }

        // Apply applications installation
        for (DocumentInfoAPI docinfo : importer.getFiles()) {
            XWikiDocument doc = docinfo.getDocInfo().getDoc();

            if (XWikiApplicationClass.getInstance(context).isInstance(doc)) {
                reloadApplication(XWikiApplicationClass.getInstance(context).newXObjectDocument(doc, 0, context),
                    comment, context);
            }
        }
    }
}
