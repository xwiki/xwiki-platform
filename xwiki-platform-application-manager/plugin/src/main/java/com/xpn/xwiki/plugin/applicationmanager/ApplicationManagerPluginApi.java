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

package com.xpn.xwiki.plugin.applicationmanager;

import java.io.IOException;
import java.util.ArrayList;
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
 * API for managing multiwikis
 * 
 * @version $Id: $
 * @see com.xpn.xwiki.plugin.multiwiki.MultiWikiPlugin
 */
public class ApplicationManagerPluginApi extends PluginApi
{
    protected static final Log LOG = LogFactory.getLog(ApplicationManagerPluginApi.class);
    
    private XWikiExceptionApi defaultException;

    public ApplicationManagerPluginApi(ApplicationManagerPlugin plugin, XWikiContext context)
    {
        super(plugin, context);
        
        defaultException = new XWikiExceptionApi(ApplicationManagerException.getDefaultException(), context);
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
     * @return Document Empty application document
     * @throws XWikiException
     */
    public XWikiApplication createApplicationDocument() throws XWikiException
    {
        return (XWikiApplication)XWikiApplicationClass.getInstance(context).newSuperDocument(context);
    }

    public int createApplication(XWikiApplication appSuperDocument, boolean failOnExist) throws XWikiException
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

    public List getApplicationDocumentList() throws XWikiException
    {
        List listDocument = new ArrayList();

        try {
            listDocument = ApplicationManager.getInstance().getApplicationList(this.context);
        } catch (ApplicationManagerException e) {
            LOG.error("Try to get all applications documents", e);

            context.put("lasterrorcode", new Integer(e.getCode()));
            context.put("lastexception", new XWikiExceptionApi(e, context));
        }

        return listDocument;
    }

    public XWikiApplication getApplicationDocument(String appName) throws XWikiException
    {
        XWikiApplication app = null;

        try {
            app =
                ApplicationManager.getInstance().getApplication(appName, context, true);
        } catch (ApplicationManagerException e) {
            LOG.error("Try to get application document", e);

            context.put("lasterrorcode", new Integer(e.getCode()));
            context.put("lastexception", new XWikiExceptionApi(e, context));
        }

        return app;
    }

    public int exportApplicationXAR(String appName) throws XWikiException, IOException
    {
        int returncode = XWikiExceptionApi.ERROR_NOERROR;

        try {
            ApplicationManager.getInstance().exportApplicationXAR(appName, false, context);
        } catch (ApplicationManagerException e) {
            LOG.error("Try to get application document", e);

            context.put("lasterrorcode", new Integer(e.getCode()));
            context.put("lastexception", new XWikiExceptionApi(e, context));
            
            returncode = e.getCode();
        }

        return returncode;
    }
    
    public int importApplication(String packageName) throws XWikiException
    {
        if (!hasAdminRights())
            return XWikiException.ERROR_XWIKI_ACCESS_DENIED;
        
        int returncode = XWikiExceptionApi.ERROR_NOERROR;

        try {
            ApplicationManager.getInstance().importApplication(context.getDoc(), packageName, context);
        } catch (ApplicationManagerException e) {
            LOG.error("Try to get application document", e);

            context.put("lasterrorcode", new Integer(e.getCode()));
            context.put("lastexception", new XWikiExceptionApi(e, context));
            
            returncode = e.getCode();
        }

        return returncode;
    }
    
    /**
	 * Reload xwiki application. It means : - update XWikiPreferences with
	 * application translation documents
	 * 
	 * @return int Error code.
	 * 
	 * @throws XWikiException
	 */
	public int reloadApplication(String appName) throws XWikiException {
		if (!hasAdminRights())
			return XWikiException.ERROR_XWIKI_ACCESS_DENIED;

		int returncode = XWikiExceptionApi.ERROR_NOERROR;

		try {
			XWikiApplication app = ApplicationManager.getInstance()
					.getApplication(appName, context, true);
			ApplicationManager.getInstance().updateApplicationTranslation(app,
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
	 * Reload all xwiki applications. It means : - update XWikiPreferences with
	 * each application translation documents
	 * 
	 * @return int Error code.
	 * 
	 * @throws XWikiException
	 */
	public int reloadAllApplications() throws XWikiException {
		if (!hasAdminRights())
			return XWikiException.ERROR_XWIKI_ACCESS_DENIED;

		int returncode = XWikiExceptionApi.ERROR_NOERROR;

		try {
			List applist = ApplicationManager.getInstance().getApplicationList(
					context);
			for (Iterator it = applist.iterator(); it.hasNext();) {
				XWikiApplication app = (XWikiApplication) it.next();
				ApplicationManager.getInstance().updateApplicationTranslation(
						app, context);
			}
		} catch (ApplicationManagerException e) {
			LOG.error("Try to get application document", e);

			context.put("lasterrorcode", new Integer(e.getCode()));
			context.put("lastexception", new XWikiExceptionApi(e, context));

			returncode = e.getCode();
		}

		return returncode;
	}
    
    public XWikiApplication getRootApplication(XWikiContext context) throws XWikiException
    {
        XWikiApplication app = null;

        try {
            app =
                ApplicationManager.getInstance().getRootApplication(context);
        } catch (ApplicationManagerException e) {
            LOG.error("Try to get application document", e);

            context.put("lasterrorcode", new Integer(e.getCode()));
            context.put("lastexception", new XWikiExceptionApi(e, context));
        }

        return app;
    }
}
