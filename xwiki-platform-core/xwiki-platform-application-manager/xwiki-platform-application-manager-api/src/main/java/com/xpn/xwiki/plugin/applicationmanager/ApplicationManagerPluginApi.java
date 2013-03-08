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
import java.util.Collections;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xwiki.localization.ContextualLocalizationManager;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.plugin.PluginApi;
import com.xpn.xwiki.plugin.applicationmanager.core.api.XWikiExceptionApi;
import com.xpn.xwiki.plugin.applicationmanager.doc.XWikiApplication;
import com.xpn.xwiki.plugin.applicationmanager.doc.XWikiApplicationClass;
import com.xpn.xwiki.web.Utils;
import com.xpn.xwiki.web.XWikiMessageTool;

/**
 * Plugin for managing applications: installation, export, creation. The plugin uses the concept of an Application
 * Descriptor describing an application (its version, the documents it contains, the translations, etc).
 * 
 * @version $Id$
 * @see com.xpn.xwiki.plugin.applicationmanager.ApplicationManagerPlugin
 */
public class ApplicationManagerPluginApi extends PluginApi<ApplicationManagerPlugin>
{
    /**
     * Field name of the last error code inserted in context.
     */
    public static final String CONTEXT_LASTERRORCODE = "lasterrorcode";

    /**
     * Field name of the last api exception inserted in context.
     */
    public static final String CONTEXT_LASTEXCEPTION = "lastexception";

    /**
     * The logging tool.
     */
    protected static final Logger LOGGER = LoggerFactory.getLogger(ApplicationManagerPluginApi.class);

    /**
     * The default ApplicationManager managed exception.
     */
    private XWikiExceptionApi defaultException;

    /**
     * Protected API for managing applications.
     */
    private ApplicationManager applicationManager;

    /**
     * Protected API for installing/exporting applications.
     */
    private ApplicationPackager applicationPackager;

    /**
     * Used to access translations.
     */
    private ContextualLocalizationManager localizationManager;

    /**
     * Create an instance of the Application Manager plugin user api.
     * 
     * @param plugin the entry point of the Application Manager plugin.
     * @param context the XWiki context.
     */
    public ApplicationManagerPluginApi(ApplicationManagerPlugin plugin, XWikiContext context)
    {
        super(plugin, context);

        // Default Exception
        this.defaultException = new XWikiExceptionApi(ApplicationManagerException.getDefaultException(), context);

        this.applicationManager = new ApplicationManager();
        this.applicationPackager = new ApplicationPackager();

        this.localizationManager = Utils.getComponent(ContextualLocalizationManager.class);
    }

    /**
     * @return the default plugin api exception.
     */
    public XWikiExceptionApi getDefaultException()
    {
        return this.defaultException;
    }

    /**
     * @return the plugin internationalization service.
     */
    @Deprecated
    public XWikiMessageTool getMessageTool()
    {
        return ApplicationManagerMessageTool.getDefault(context);
    }

    /**
     * Log error and store details in the context.
     * 
     * @param errorMessage error message.
     * @param e the catched exception.
     */
    public void logError(String errorMessage, XWikiException e)
    {
        LOGGER.error(errorMessage, e);

        context.put(CONTEXT_LASTERRORCODE, Integer.valueOf(e.getCode()));
        context.put(CONTEXT_LASTEXCEPTION, new XWikiExceptionApi(e, context));
    }

    // ////////////////////////////////////////////////////////////////////////////
    // Applications management

    /**
     * Create empty application document.
     * 
     * @return an empty application descriptor document.
     * @throws XWikiException all error that does not caused by user of this method.
     */
    public XWikiApplication createApplicationDocument() throws XWikiException
    {
        return XWikiApplicationClass.getInstance(context).newXObjectDocument(context);
    }

    /**
     * Create a new application descriptor base on provided application descriptor.
     * 
     * @param appXObjectDocument the user application descriptor from which new descriptor will be created.
     * @param failOnExist if true fail if the application descriptor to create already exists.
     * @return error code . If there is error, it add error code in context {@link #CONTEXT_LASTERRORCODE} field and
     *         exception in context's {@link #CONTEXT_LASTEXCEPTION} field.
     *         <p>
     *         Error codes can be :
     *         <ul>
     *         <li> {@link XWikiExceptionApi#ERROR_NOERROR} : method succeed with no error.</li>
     *         <li> {@link XWikiException#ERROR_XWIKI_ACCESS_DENIED} : context's user don't have rights to do this
     *         action.</li>
     *         <li> {@link ApplicationManagerException#ERROR_AM_APPDOCALREADYEXISTS} : application descriptor already
     *         exists.</li>
     *         </ul>
     * @throws XWikiException all error that does not caused by user of this method.
     */
    public int createApplication(XWikiApplication appXObjectDocument, boolean failOnExist) throws XWikiException
    {
        int returncode = XWikiExceptionApi.ERROR_NOERROR;

        try {
            this.applicationManager.createApplication(appXObjectDocument, failOnExist, this.localizationManager
                .getTranslationPlain(ApplicationManagerMessageTool.COMMENT_CREATEAPPLICATION,
                    appXObjectDocument.toString()), context);
        } catch (ApplicationManagerException e) {
            logError(this.localizationManager.getTranslationPlain(ApplicationManagerMessageTool.LOG_CREATEAPP,
                appXObjectDocument.toString()), e);

            returncode = e.getCode();
        }

        return returncode;
    }

    /**
     * Delete an application descriptor document.
     * 
     * @param appName the name of the application.
     * @return error code . If there is error, it add error code in context {@link #CONTEXT_LASTERRORCODE} field and
     *         exception in context's {@link #CONTEXT_LASTEXCEPTION} field.
     *         <p>
     *         Error codes can be :
     *         <ul>
     *         <li> {@link XWikiExceptionApi#ERROR_NOERROR} : action finished with no error.
     *         <li> {@link XWikiException#ERROR_XWIKI_ACCESS_DENIED} : context's user don't have rights to do this
     *         action.
     *         <li> {@link ApplicationManagerException#ERROR_AM_DOESNOTEXIST} : provided application does not exist.
     *         </ul>
     * @throws XWikiException all error that does not caused by user of this method.
     */
    public int deleteApplication(String appName) throws XWikiException
    {
        int returncode = XWikiExceptionApi.ERROR_NOERROR;

        try {
            this.applicationManager.deleteApplication(appName, context);
        } catch (ApplicationManagerException e) {
            logError(
                this.localizationManager.getTranslationPlain(ApplicationManagerMessageTool.LOG_DELETEAPP, appName), e);

            returncode = e.getCode();
        }

        return returncode;
    }

    /**
     * Get all applications descriptors documents.
     * 
     * @return a list of XWikiApplication.
     * @throws XWikiException all error that does not caused by user of this method.
     */
    public List<XWikiApplication> getApplicationDocumentList() throws XWikiException
    {
        List<XWikiApplication> listDocument = Collections.emptyList();

        try {
            listDocument = this.applicationManager.getApplicationList(this.context);
        } catch (ApplicationManagerException e) {
            logError(this.localizationManager.getTranslationPlain(ApplicationManagerMessageTool.LOG_GETALLAPPS), e);
        }

        return listDocument;
    }

    /**
     * Get the application descriptor document of the provided application name.
     * 
     * @param appName the name of the application.
     * @return the application descriptor document. If there is error, it add error code in context
     *         {@link #CONTEXT_LASTERRORCODE} field and exception in context's {@link #CONTEXT_LASTEXCEPTION} field.
     *         Error codes can be :
     *         <ul>
     *         <li> {@link ApplicationManagerException#ERROR_AM_DOESNOTEXIST} : provided application does not exist.
     *         </ul>
     * @throws XWikiException all error that does not caused by user of this method.
     */
    public XWikiApplication getApplicationDocument(String appName) throws XWikiException
    {
        XWikiApplication app = null;

        try {
            app = this.applicationManager.getApplication(appName, context, true);
        } catch (ApplicationManagerException e) {
            logError(
                this.localizationManager.getTranslationPlain(ApplicationManagerMessageTool.LOG_GETAPP, appName), e);
        }

        return app;
    }

    /**
     * Export an application into XAR using Packaging plugin.
     * 
     * @param appName the name of the application to export.
     * @return error code . If there is error, it add error code in context {@link #CONTEXT_LASTERRORCODE} field and
     *         exception in context's {@link #CONTEXT_LASTEXCEPTION} field.
     *         <p>
     *         Error codes can be :
     *         <ul>
     *         <li> {@link XWikiExceptionApi#ERROR_NOERROR} : action finished with no error.
     *         <li> {@link ApplicationManagerException#ERROR_AM_DOESNOTEXIST} : provided application does not exist.
     *         </ul>
     * @throws XWikiException all error that does not caused by user of this method.
     * @throws IOException all error that does not caused by user of this method.
     */
    public int exportApplicationXAR(String appName) throws XWikiException, IOException
    {
        return exportApplicationXAR(appName, true, false);
    }

    /**
     * Export an application into XAR using Packaging plugin.
     * 
     * @param appName the name of the application.
     * @param recurse if true include all dependencies applications into XAR.
     * @param withDocHistory if true export with documents history.
     * @return error code . If there is error, it add error code in context {@link #CONTEXT_LASTERRORCODE} field and
     *         exception in context's {@link #CONTEXT_LASTEXCEPTION} field.
     *         <p>
     *         Error codes can be :
     *         <ul>
     *         <li> {@link XWikiExceptionApi#ERROR_NOERROR} : action finished with no error.
     *         <li> {@link ApplicationManagerException#ERROR_AM_DOESNOTEXIST} : provided application does not exist.
     *         </ul>
     * @throws XWikiException all error that does not caused by user of this method.
     * @throws IOException all error that does not caused by user of this method.
     */
    public int exportApplicationXAR(String appName, boolean recurse, boolean withDocHistory) throws XWikiException,
        IOException
    {
        int returncode = XWikiExceptionApi.ERROR_NOERROR;

        try {
            this.applicationPackager.exportApplicationXAR(appName, recurse, withDocHistory, context);
        } catch (ApplicationManagerException e) {
            logError(
                this.localizationManager.getTranslationPlain(ApplicationManagerMessageTool.LOG_EXPORTAPP, appName), e);

            returncode = e.getCode();
        }

        return returncode;
    }

    /**
     * Import attached application XAR into current wiki and do all actions needed to installation an application. See
     * {@link #reloadApplication(String)} for more.
     * 
     * @param packageName the name of the attached XAR file to import.
     * @return error code . If there is error, it add error code in context {@link #CONTEXT_LASTERRORCODE} field and
     *         exception in context's {@link #CONTEXT_LASTEXCEPTION} field.
     *         <p>
     *         Error codes can be :
     *         <ul>
     *         <li> {@link XWikiExceptionApi#ERROR_NOERROR} : action finished with no error.
     *         <li> {@link XWikiException#ERROR_XWIKI_ACCESS_DENIED} : context's user don't have rights to do this
     *         action.
     *         <li> {@link ApplicationManagerException#ERROR_AM_DOESNOTEXIST} : provided application does not exist.
     *         </ul>
     * @throws XWikiException all error that does not caused by user of this method.
     */
    public int importApplication(String packageName) throws XWikiException
    {
        if (!hasAdminRights()) {
            return XWikiException.ERROR_XWIKI_ACCESS_DENIED;
        }

        int returncode = XWikiExceptionApi.ERROR_NOERROR;

        try {
            this.applicationPackager.importApplication(context.getDoc(), packageName, this.localizationManager
                .getTranslationPlain(ApplicationManagerMessageTool.COMMENT_IMPORTAPPLICATION, packageName), context);
        } catch (ApplicationManagerException e) {
            logError(
                this.localizationManager.getTranslationPlain(ApplicationManagerMessageTool.LOG_IMPORTAPP, packageName),
                e);

            returncode = e.getCode();
        }

        return returncode;
    }

    /**
     * Reload xwiki application. It means :
     * <ul>
     * <li>update XWikiPreferences with application translation documents.
     * </ul>
     * 
     * @param appName the name of the application to reload.
     * @return error code . If there is error, it add error code in context {@link #CONTEXT_LASTERRORCODE} field and
     *         exception in context's {@link #CONTEXT_LASTEXCEPTION} field.
     *         <p>
     *         Error codes can be :
     *         <ul>
     *         <li> {@link XWikiExceptionApi#ERROR_NOERROR} : action finished with no error.
     *         <li> {@link XWikiException#ERROR_XWIKI_ACCESS_DENIED} : context's user don't have rights to do this
     *         action.
     *         <li> {@link ApplicationManagerException#ERROR_AM_DOESNOTEXIST} : provided application does not exist.
     *         </ul>
     * @throws XWikiException all error that does not caused by user of this method.
     */
    public int reloadApplication(String appName) throws XWikiException
    {
        if (!hasAdminRights()) {
            return XWikiException.ERROR_XWIKI_ACCESS_DENIED;
        }

        int returncode = XWikiExceptionApi.ERROR_NOERROR;

        try {
            XWikiApplication app = this.applicationManager.getApplication(appName, context, true);
            this.applicationManager.reloadApplication(app, this.localizationManager.getTranslationPlain(
                ApplicationManagerMessageTool.COMMENT_RELOADAPPLICATION, app.getAppName()), context);
        } catch (ApplicationManagerException e) {
            logError(
                this.localizationManager.getTranslationPlain(ApplicationManagerMessageTool.LOG_RELOADAPP, appName), e);

            returncode = e.getCode();
        }

        return returncode;
    }

    /**
     * Reload all xwiki applications. It means : - update XWikiPreferences with each application translation documents
     * 
     * @return error code.
     *         <ul>
     *         <li> {@link XWikiExceptionApi#ERROR_NOERROR} : action finished with no error.
     *         <li>
     *         {@link XWikiException#ERROR_XWIKI_ACCESS_DENIED} : context's user don't have rights to do this
     *         action.
     *         </ul>
     * @throws XWikiException all error that does not caused by user of this method.
     */
    public int reloadAllApplications() throws XWikiException
    {
        if (!hasAdminRights()) {
            return XWikiException.ERROR_XWIKI_ACCESS_DENIED;
        }

        int returncode = XWikiExceptionApi.ERROR_NOERROR;

        try {
            this.applicationManager.reloadAllApplications(this.localizationManager
                .getTranslationPlain(ApplicationManagerMessageTool.COMMENT_RELOADALLAPPLICATIONS), context);
        } catch (ApplicationManagerException e) {
            logError(this.localizationManager.getTranslationPlain(ApplicationManagerMessageTool.LOG_REALOADALLAPPS), e);

            returncode = e.getCode();
        }

        return returncode;
    }

    /**
     * Get the current wiki root application.
     * 
     * @return the root application descriptor document. If can't find root application return null.
     * @throws XWikiException all error that does not caused by user of this method.
     */
    public XWikiApplication getRootApplication() throws XWikiException
    {
        XWikiApplication app = null;

        try {
            app = this.applicationManager.getRootApplication(context);
        } catch (ApplicationManagerException e) {
            logError(this.localizationManager.getTranslationPlain(ApplicationManagerMessageTool.LOG_GETROOTAPP), e);
        }

        return app;
    }
}
