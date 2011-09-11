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

import java.util.Collection;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.objects.classes.ListClass;
import com.xpn.xwiki.plugin.applicationmanager.core.plugin.XWikiPluginMessageTool;
import com.xpn.xwiki.plugin.applicationmanager.doc.XWikiApplication;
import com.xpn.xwiki.plugin.applicationmanager.doc.XWikiApplicationClass;

/**
 * Hidden toolkit used by the plugin API that make all the plugins actions.
 * 
 * @version $Id$
 */
public final class ApplicationManager
{
    /**
     * The logging tool.
     */
    protected static final Logger LOGGER = LoggerFactory.getLogger(ApplicationManager.class);

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
     * The message tool to use to generate error or comments.
     */
    private XWikiPluginMessageTool messageTool;

    // ////////////////////////////////////////////////////////////////////////////

    /**
     * @param messageTool the message tool
     */
    public ApplicationManager(XWikiPluginMessageTool messageTool)
    {
        this.messageTool = messageTool;
    }

    /**
     * Get the {@link XWikiPluginMessageTool} to use with ApplicationManager.
     * 
     * @param context the XWiki context.
     * @return a translated strings manager.
     */
    public XWikiPluginMessageTool getMessageTool(XWikiContext context)
    {
        return this.messageTool != null ? this.messageTool : ApplicationManagerMessageTool.getDefault(context);
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
        return XWikiApplicationClass.getInstance(context, false).searchXObjectDocuments(context);
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
                if (LOGGER.isErrorEnabled()) {
                    LOGGER.error(getMessageTool(context).get(ApplicationManagerMessageTool.ERROR_APPPAGEALREADYEXISTS,
                        userAppSuperDoc.getAppName()));
                }

                throw new ApplicationManagerException(ApplicationManagerException.ERROR_AM_APPDOCALREADYEXISTS,
                    getMessageTool(context).get(ApplicationManagerMessageTool.ERROR_APPPAGEALREADYEXISTS,
                        userAppSuperDoc.getAppName()));
            } else if (LOGGER.isWarnEnabled()) {
                LOGGER.warn(getMessageTool(context).get(ApplicationManagerMessageTool.ERROR_APPPAGEALREADYEXISTS,
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
        return XWikiApplicationClass.getInstance(context, false).getApplication(appName, validate, context);
    }

    /**
     * Reload xwiki application. It means :
     * <ul>
     * <li>update XWikiPreferences with application translation documents.
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
     * <li>update XWikiPreferences with application translation documents.
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
            updateApplicationTranslation(app, comment, context);
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
                prefsObject.setStringValue(XWIKIPREFERENCES_DOCUMENTBUNDLES, StringUtils.join(
                    translationPrefs.toArray(), XWIKIPREFERENCES_DOCUMENTBUNDLES_SEP));
                xwiki.saveDocument(prefsDoc, comment, context);
            }
        }
    }

    /**
     * Insert in XWiki.XWikiPreferences "documentBundles" field the translation documents of all applications in the
     * context's wiki.
     * 
     * @param context the XWiki context.
     * @throws XWikiException error when :
     *             <ul>
     *             <li>getting wiki preferences document.</li>
     *             <li>or searching for all applications in the wiki.</li>
     *             <li>or saving wiki preferences document.</li>
     *             </ul>
     */
    public void updateAllApplicationTranslation(XWikiContext context) throws XWikiException
    {
        updateApplicationsTranslation(getApplicationList(context), getMessageTool(context).get(
            ApplicationManagerMessageTool.COMMENT_REFRESHALLTRANSLATIONS), context);
    }

    /**
     * Insert in XWiki.XWikiPreferences "documentBundles" field the translation documents of all applications in the
     * context's wiki.
     * 
     * @param document the document containing the applications descriptors
     * @param context the XWiki context.
     * @throws XWikiException error when :
     *             <ul>
     *             <li>getting wiki preferences document.</li>
     *             <li>or searching for all applications in the wiki.</li>
     *             <li>or saving wiki preferences document.</li>
     *             </ul>
     * @since 1.9
     */
    public void updateApplicationsTranslation(XWikiDocument document, XWikiContext context) throws XWikiException
    {
        List<XWikiApplication> appList =
            XWikiApplicationClass.getInstance(context).newXObjectDocumentList(document, context);
        updateApplicationsTranslation(appList, getMessageTool(context).get(
            ApplicationManagerMessageTool.COMMENT_AUTOUPDATETRANSLATIONS, document.getFullName()), context);
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
                prefsObject.setStringValue(XWIKIPREFERENCES_DOCUMENTBUNDLES, StringUtils.join(
                    translationPrefs.toArray(), XWIKIPREFERENCES_DOCUMENTBUNDLES_SEP));
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
}
