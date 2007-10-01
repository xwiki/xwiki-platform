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
import java.util.Iterator;
import java.util.List;

import com.xpn.xwiki.plugin.applicationmanager.core.api.XWikiExceptionApi;
import com.xpn.xwiki.plugin.PluginApi;
import com.xpn.xwiki.plugin.applicationmanager.doc.XWikiApplication;
import com.xpn.xwiki.plugin.applicationmanager.doc.XWikiApplicationClass;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Plugin for managing applications: installation, export, creation. The plugin uses the concept of
 * an Application Descriptor describing an application (its version, the documents it contains, the
 * translations, etc).
 * 
 * @version $Id: $
 * @see com.xpn.xwiki.plugin.applicationmanager.ApplicationManagerPlugin
 */
public class ApplicationManagerPluginApi extends PluginApi
{
    protected static final Log LOG = LogFactory.getLog(ApplicationManagerPluginApi.class);

    private XWikiExceptionApi defaultException;

    public ApplicationManagerPluginApi(ApplicationManagerPlugin plugin, XWikiContext context)
    {
        super(plugin, context);

        defaultException =
            new XWikiExceptionApi(ApplicationManagerException.getDefaultException(), context);
    }

    public XWikiExceptionApi getDefaultException()
    {
        return defaultException;
    }

    // ////////////////////////////////////////////////////////////////////////////
    // Applications management

    /**
     * Create empty application document
     * 
     * @return an empty application descriptor document.
     * @throws XWikiException
     */
    public XWikiApplication createApplicationDocument() throws XWikiException
    {
        return (XWikiApplication) XWikiApplicationClass.getInstance(context).newSuperDocument(
            context);
    }

    /**
     * Create a new application descriptor base on provided application descriptor.
     * 
     * @param appSuperDocument the user application descriptor from which new descriptor will be
     *            created.
     * @param failOnExist if true fail if the application descriptor to create already exists.
     * @return error code . If there is error, it add error code in context "lasterrorcode" field
     *         and exception in context's "lastexception" field.
     *         <p>
     *         Error codes can be :
     *         <ul>
     *         <li> {@link XWikiExceptionApi#ERROR_NOERROR} : action finished with no error.
     *         <li> {@link XWikiException#ERROR_XWIKI_ACCESS_DENIED} : context's user don't have
     *         rights to do this action.
     *         <li>
     *         {@link ApplicationManagerException#ERROR_APPLICATIONMANAGER_APPDOC_ALREADY_EXISTS} :
     *         application descriptor already exists.
     *         </ul>
     * @throws XWikiException
     */
    public int createApplication(XWikiApplication appSuperDocument, boolean failOnExist)
        throws XWikiException
    {
        int returncode = XWikiExceptionApi.ERROR_NOERROR;

        // TODO : check rights

        try {
            ApplicationManager.getInstance().createApplication(appSuperDocument, failOnExist,
                context);
        } catch (ApplicationManagerException e) {
            LOG.error("Try to create application \"" + appSuperDocument + "\"", e);

            context.put("lasterrorcode", new Integer(e.getCode()));
            context.put("lastexception", new XWikiExceptionApi(e, context));

            returncode = e.getCode();
        }

        return returncode;
    }

    /**
     * Delete an application descriptor document.
     * 
     * @param appName the name of the application.
     * @return error code . If there is error, it add error code in context "lasterrorcode" field
     *         and exception in context's "lastexception" field.
     *         <p>
     *         Error codes can be :
     *         <ul>
     *         <li> {@link XWikiExceptionApi#ERROR_NOERROR} : action finished with no error.
     *         <li> {@link XWikiException#ERROR_XWIKI_ACCESS_DENIED} : context's user don't have
     *         rights to do this action.
     *         <li> {@link ApplicationManagerException#ERROR_APPLICATIONMANAGER_DOES_NOT_EXIST} :
     *         provided application does not exist.
     *         </ul>
     * @throws XWikiException
     */
    public int deleteApplication(String appName) throws XWikiException
    {
        int returncode = XWikiExceptionApi.ERROR_NOERROR;

        // TODO : check rights

        try {
            ApplicationManager.getInstance().deleteApplication(appName, context);
        } catch (ApplicationManagerException e) {
            LOG.error("Try to delete application \"" + appName + "\"", e);

            context.put("lasterrorcode", new Integer(e.getCode()));
            context.put("lastexception", new XWikiExceptionApi(e, context));

            returncode = e.getCode();
        }

        return returncode;
    }

    /**
     * Get all applications descriptors documents.
     * 
     * @return a list of XWikiApplication.
     * @throws XWikiException
     */
    public List getApplicationDocumentList() throws XWikiException
    {
        List listDocument = Collections.EMPTY_LIST;

        try {
            listDocument = ApplicationManager.getInstance().getApplicationList(this.context);
        } catch (ApplicationManagerException e) {
            LOG.error("Try to get all applications documents", e);

            context.put("lasterrorcode", new Integer(e.getCode()));
            context.put("lastexception", new XWikiExceptionApi(e, context));
        }

        return listDocument;
    }

    /**
     * Get the application descriptor document of the provided application.
     * 
     * @param appName the name of the application.
     * @return the application descriptor document. If there is error, it add error code in context
     *         "lasterrorcode" field and exception in context's "lastexception" field. Error codes
     *         can be :
     *         <ul>
     *         <li> {@link ApplicationManagerException#ERROR_APPLICATIONMANAGER_DOES_NOT_EXIST} :
     *         provided application does not exist.
     *         </ul>
     * @throws XWikiException
     */
    public XWikiApplication getApplicationDocument(String appName) throws XWikiException
    {
        XWikiApplication app = null;

        try {
            app = ApplicationManager.getInstance().getApplication(appName, context, true);
        } catch (ApplicationManagerException e) {
            LOG.error("Try to get application document", e);

            context.put("lasterrorcode", new Integer(e.getCode()));
            context.put("lastexception", new XWikiExceptionApi(e, context));
        }

        return app;
    }

    /**
     * Export an application into XAR using Packaging plugin.
     * 
     * @param appName the name of the application.
     * @return error code . If there is error, it add error code in context "lasterrorcode" field
     *         and exception in context's "lastexception" field.
     *         <p>
     *         Error codes can be :
     *         <ul>
     *         <li> {@link XWikiExceptionApi#ERROR_NOERROR} : action finished with no error.
     *         <li> {@link ApplicationManagerException#ERROR_APPLICATIONMANAGER_DOES_NOT_EXIST} :
     *         provided application does not exist.
     *         </ul>
     * @throws XWikiException
     * @throws IOException
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
     * @return error code . If there is error, it add error code in context "lasterrorcode" field
     *         and exception in context's "lastexception" field.
     *         <p>
     *         Error codes can be :
     *         <ul>
     *         <li> {@link XWikiExceptionApi#ERROR_NOERROR} : action finished with no error.
     *         <li> {@link ApplicationManagerException#ERROR_APPLICATIONMANAGER_DOES_NOT_EXIST} :
     *         provided application does not exist.
     *         </ul>
     * @throws XWikiException
     * @throws IOException
     */
    public int exportApplicationXAR(String appName, boolean recurse, boolean withDocHistory)
        throws XWikiException, IOException
    {
        int returncode = XWikiExceptionApi.ERROR_NOERROR;

        try {
            ApplicationManager.getInstance().exportApplicationXAR(appName, recurse,
                withDocHistory, context);
        } catch (ApplicationManagerException e) {
            LOG.error("Try to get application document", e);

            context.put("lasterrorcode", new Integer(e.getCode()));
            context.put("lastexception", new XWikiExceptionApi(e, context));

            returncode = e.getCode();
        }

        return returncode;
    }

    /**
     * Import attached application XAR into current wiki and do all actions needed to installation
     * an application. See {@link #reloadApplication(String)} for more.
     * 
     * @param packageName the name of the attached XAR file.
     * @return error code . If there is error, it add error code in context "lasterrorcode" field
     *         and exception in context's "lastexception" field.
     *         <p>
     *         Error codes can be :
     *         <ul>
     *         <li> {@link XWikiExceptionApi#ERROR_NOERROR} : action finished with no error.
     *         <li> {@link XWikiException#ERROR_XWIKI_ACCESS_DENIED} : context's user don't have
     *         rights to do this action.
     *         <li> {@link ApplicationManagerException#ERROR_APPLICATIONMANAGER_DOES_NOT_EXIST} :
     *         provided application does not exist.
     *         </ul>
     * @throws XWikiException
     * @see {@link #reloadApplication(String)}
     */
    public int importApplication(String packageName) throws XWikiException
    {
        if (!hasAdminRights())
            return XWikiException.ERROR_XWIKI_ACCESS_DENIED;

        int returncode = XWikiExceptionApi.ERROR_NOERROR;

        try {
            ApplicationManager.getInstance().importApplication(context.getDoc(), packageName,
                context);
        } catch (ApplicationManagerException e) {
            LOG.error("Try to get application document", e);

            context.put("lasterrorcode", new Integer(e.getCode()));
            context.put("lastexception", new XWikiExceptionApi(e, context));

            returncode = e.getCode();
        }

        return returncode;
    }

    /**
     * Reload xwiki application. It means :
     * <ul>
     * <li> update XWikiPreferences with application translation documents.
     * </ul>
     * 
     * @return error code . If there is error, it add error code in context "lasterrorcode" field
     *         and exception in context's "lastexception" field.
     *         <p>
     *         Error codes can be :
     *         <ul>
     *         <li> {@link XWikiExceptionApi#ERROR_NOERROR} : action finished with no error.
     *         <li> {@link XWikiException#ERROR_XWIKI_ACCESS_DENIED} : context's user don't have
     *         rights to do this action.
     *         <li> {@link ApplicationManagerException#ERROR_APPLICATIONMANAGER_DOES_NOT_EXIST} :
     *         provided application does not exist.
     *         </ul>
     * @throws XWikiException
     */
    public int reloadApplication(String appName) throws XWikiException
    {
        if (!hasAdminRights())
            return XWikiException.ERROR_XWIKI_ACCESS_DENIED;

        int returncode = XWikiExceptionApi.ERROR_NOERROR;

        try {
            XWikiApplication app =
                ApplicationManager.getInstance().getApplication(appName, context, true);
            ApplicationManager.getInstance().updateApplicationTranslation(app, context);
        } catch (ApplicationManagerException e) {
            LOG.error("Try to get application document", e);

            context.put("lasterrorcode", new Integer(e.getCode()));
            context.put("lastexception", new XWikiExceptionApi(e, context));

            returncode = e.getCode();
        }

        return returncode;
    }

    /**
     * Reload all xwiki applications. It means : - update XWikiPreferences with each application
     * translation documents
     * 
     * @return error code.
     *         <ul>
     *         <li> {@link XWikiExceptionApi#ERROR_NOERROR} : action finished with no error.
     *         <li> {@link XWikiException#ERROR_XWIKI_ACCESS_DENIED} : context's user don't have
     *         rights to do this action.
     *         <ul>
     * @throws XWikiException
     */
    public int reloadAllApplications() throws XWikiException
    {
        if (!hasAdminRights())
            return XWikiException.ERROR_XWIKI_ACCESS_DENIED;

        int returncode = XWikiExceptionApi.ERROR_NOERROR;

        try {
            List applist = ApplicationManager.getInstance().getApplicationList(context);
            for (Iterator it = applist.iterator(); it.hasNext();) {
                XWikiApplication app = (XWikiApplication) it.next();
                ApplicationManager.getInstance().updateApplicationTranslation(app, context);
            }
        } catch (ApplicationManagerException e) {
            LOG.error("Try to get application document", e);

            context.put("lasterrorcode", new Integer(e.getCode()));
            context.put("lastexception", new XWikiExceptionApi(e, context));

            returncode = e.getCode();
        }

        return returncode;
    }
    
    /**
     * @deprecated use {@link #getRootApplication()}.
     */
    public XWikiApplication getRootApplication(XWikiContext context) throws XWikiException
    {
        return getRootApplication();
    }

    /**
     * Get the current wiki root application.
     * 
     * @return the root application descriptor document. If can't find root application return null.
     * @throws XWikiException
     */
    public XWikiApplication getRootApplication() throws XWikiException
    {
        XWikiApplication app = null;

        try {
            app = ApplicationManager.getInstance().getRootApplication(context);
        } catch (ApplicationManagerException e) {
            LOG.error("Try to get application document", e);

            context.put("lasterrorcode", new Integer(e.getCode()));
            context.put("lastexception", new XWikiExceptionApi(e, context));
        }

        return app;
    }
}
