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
package com.xpn.xwiki.plugin.applicationmanager.internal;

import java.io.IOException;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;

import org.xwiki.component.annotation.Component;
import org.xwiki.context.Execution;
import org.xwiki.context.ExecutionContext;
import org.xwiki.script.service.ScriptService;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.plugin.applicationmanager.ApplicationManagerMessageTool;
import com.xpn.xwiki.plugin.applicationmanager.ApplicationManagerPlugin;
import com.xpn.xwiki.plugin.applicationmanager.ApplicationManagerPluginApi;
import com.xpn.xwiki.plugin.applicationmanager.core.api.XWikiExceptionApi;
import com.xpn.xwiki.plugin.applicationmanager.doc.XWikiApplication;
import com.xpn.xwiki.web.XWikiMessageTool;

/**
 * Bridge used to access plugin APi as a script service.
 * 
 * @version $Id$
 */
@Component
@Named("applicationmanager")
public class ApplicationManagerScriptService implements ScriptService
{
    private static final String PLUGINAPI_KEY = "applicationmanagerapi";

    private static final String PLUGIN_ID = "applicationmanager";

    /**
     * Used to access XWiki context.
     */
    @Inject
    private Execution execution;

    /**
     * USed to access current {@link XWikiContext}.
     */
    private Provider<XWikiContext> xcontextProvider;

    /**
     * The plugin.
     */
    private ApplicationManagerPlugin plugin;

    /**
     * @param xcontext the XWiki context
     * @return the plugin
     */
    private ApplicationManagerPlugin getPlugin(XWikiContext xcontext)
    {
        this.plugin = (ApplicationManagerPlugin) xcontext.getWiki().getPlugin(PLUGIN_ID, xcontext);
        if (this.plugin == null) {
            this.plugin = new ApplicationManagerPlugin(PLUGIN_ID, ApplicationManagerPlugin.class.getName(), xcontext);
            this.plugin.init(xcontext);
        }

        return this.plugin;
    }

    /**
     * @return the real API
     */
    private ApplicationManagerPluginApi getAPI()
    {
        ExecutionContext econtext = this.execution.getContext();

        ApplicationManagerPluginApi api = (ApplicationManagerPluginApi) econtext.getProperty(PLUGINAPI_KEY);

        if (api == null) {
            XWikiContext xcontext = this.xcontextProvider.get();

            api = (ApplicationManagerPluginApi) getPlugin(xcontext).getPluginApi(this.plugin, xcontext);
            econtext.setProperty(PLUGINAPI_KEY, api);
        }

        return api;
    }

    // ////////////////////////////////////////////////////////////////////////////////////////////:
    // ApplicationManagerPluginApi

    public XWikiExceptionApi getDefaultException()
    {
        return getAPI().getDefaultException();
    }

    @Deprecated
    public XWikiMessageTool getMessageTool()
    {
        return ApplicationManagerMessageTool.getDefault(this.xcontextProvider.get());
    }

    public XWikiApplication createApplicationDocument() throws XWikiException
    {
        return getAPI().createApplicationDocument();
    }

    public int createApplication(XWikiApplication appXObjectDocument, boolean failOnExist) throws XWikiException
    {
        return getAPI().createApplication(appXObjectDocument, failOnExist);
    }

    public int deleteApplication(String appName) throws XWikiException
    {
        return getAPI().deleteApplication(appName);
    }

    public List<XWikiApplication> getApplicationDocumentList() throws XWikiException
    {
        return getAPI().getApplicationDocumentList();
    }

    public XWikiApplication getApplicationDocument(String appName) throws XWikiException
    {
        return getAPI().getApplicationDocument(appName);
    }

    public int exportApplicationXAR(String appName) throws XWikiException, IOException
    {
        return getAPI().exportApplicationXAR(appName);
    }

    public int exportApplicationXAR(String appName, boolean recurse, boolean withDocHistory) throws XWikiException,
        IOException
    {
        return getAPI().exportApplicationXAR(appName, recurse, withDocHistory);
    }

    public int importApplication(String packageName) throws XWikiException
    {
        return getAPI().importApplication(packageName);
    }

    public int reloadApplication(String appName) throws XWikiException
    {
        return getAPI().reloadApplication(appName);
    }

    public int reloadAllApplications() throws XWikiException
    {
        return getAPI().reloadAllApplications();
    }

    public XWikiApplication getRootApplication() throws XWikiException
    {
        return getAPI().getRootApplication();
    }

    // ////////////////////////////////////////////////////////////////////////////////////////////:
    // PluginApi

    public ApplicationManagerPlugin getInternalPlugin()
    {
        return getAPI().getInternalPlugin();
    }

    // ////////////////////////////////////////////////////////////////////////////////////////////:
    // Api

    public boolean hasProgrammingRights()
    {
        return getAPI().hasProgrammingRights();
    }

    public boolean hasAdminRights()
    {
        return getAPI().hasAdminRights();
    }

    public boolean hasWikiAdminRights()
    {
        return getAPI().hasWikiAdminRights();
    }

    public boolean hasAccessLevel(String right, String docname) throws XWikiException
    {
        return getAPI().hasAccessLevel(right, docname);
    }
}
