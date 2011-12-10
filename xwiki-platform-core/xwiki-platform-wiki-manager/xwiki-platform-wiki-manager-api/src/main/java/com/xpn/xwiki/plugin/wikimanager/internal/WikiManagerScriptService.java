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
package com.xpn.xwiki.plugin.wikimanager.internal;

import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;

import org.xwiki.component.annotation.Component;
import org.xwiki.context.Execution;
import org.xwiki.context.ExecutionContext;
import org.xwiki.script.service.ScriptService;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.plugin.applicationmanager.core.api.XWikiExceptionApi;
import com.xpn.xwiki.plugin.globalsearch.GlobalSearchPluginApi;
import com.xpn.xwiki.plugin.wikimanager.WikiManagerPlugin;
import com.xpn.xwiki.plugin.wikimanager.WikiManagerPluginApi;
import com.xpn.xwiki.plugin.wikimanager.doc.Wiki;
import com.xpn.xwiki.plugin.wikimanager.doc.XWikiServer;
import com.xpn.xwiki.web.XWikiMessageTool;

/**
 * Bridge used to access plugin APi as a script service.
 * 
 * @version $Id$
 */
@Component
@Named("wikimanager")
public class WikiManagerScriptService implements ScriptService
{
    private static final String PLUGINAPI_KEY = "wikimanagerapi";

    private static final String PLUGIN_ID = "wikimanager";

    /**
     * Used to access XWiki context.
     */
    @Inject
    private Execution execution;

    /**
     * The plugin.
     */
    private WikiManagerPlugin plugin;

    /**
     * @param xcontext the XWiki context
     * @return the plugin
     */
    private WikiManagerPlugin getPlugin(XWikiContext xcontext)
    {
        this.plugin = (WikiManagerPlugin) xcontext.getWiki().getPlugin(PLUGIN_ID, xcontext);
        if (this.plugin == null) {
            this.plugin = new WikiManagerPlugin(PLUGIN_ID, WikiManagerPlugin.class.getName(), xcontext);
            this.plugin.init(xcontext);
        }

        return this.plugin;
    }

    /**
     * @return the real API
     */
    private WikiManagerPluginApi getAPI()
    {
        ExecutionContext econtext = this.execution.getContext();

        WikiManagerPluginApi api = (WikiManagerPluginApi) econtext.getProperty(PLUGINAPI_KEY);

        if (api == null) {
            XWikiContext xcontext = (XWikiContext) econtext.getProperty(XWikiContext.EXECUTIONCONTEXT_KEY);

            api = (WikiManagerPluginApi) getPlugin(xcontext).getPluginApi(this.plugin, xcontext);
            econtext.setProperty(PLUGINAPI_KEY, api);
        }

        return api;
    }

    // ////////////////////////////////////////////////////////////////////////////////////////////:
    // WikiManagerPluginApi

    public XWikiExceptionApi getDefaultException()
    {
        return getAPI().getDefaultException();
    }

    public XWikiMessageTool getMessageTool()
    {
        return getAPI().getMessageTool();
    }

    public GlobalSearchPluginApi getSearchApi()
    {
        return getAPI().getSearchApi();
    }

    public boolean canCreateWiki()
    {
        return getAPI().canCreateWiki();
    }

    public boolean canEditWiki()
    {
        return getAPI().canEditWiki();
    }

    public boolean canDeleteWiki()
    {
        return getAPI().canDeleteWiki();
    }

    // ////////////////////////////////////////////////////////////////////////////
    // Wikis management

    public int createNewWiki(String wikiName, String templateWiki, String pkgName, XWikiServer wikiXObjectDocument,
        boolean failOnExist) throws XWikiException
    {
        return getAPI().createNewWiki(wikiName, templateWiki, pkgName, wikiXObjectDocument, failOnExist);
    }

    public int deleteWiki(String wikiName, boolean deleteDatabase) throws XWikiException
    {
        return getAPI().deleteWiki(wikiName, deleteDatabase);
    }

    public int deleteWiki(String wikiName) throws XWikiException
    {
        return getAPI().deleteWiki(wikiName);
    }

    public int deleteWikiAlias(String wikiName, int objectId) throws XWikiException
    {
        return getAPI().deleteWikiAlias(wikiName, objectId);
    }

    public Wiki getWikiFromName(String wikiName) throws XWikiException
    {
        return getAPI().getWikiFromName(wikiName);
    }

    public List<Wiki> getAllWikis() throws XWikiException
    {
        return getAPI().getAllWikis();
    }

    public Wiki getWikiFromDocumentName(String documentFullName) throws XWikiException
    {
        return getAPI().getWikiFromDocumentName(documentFullName);
    }

    public XWikiServer getWikiDocument(String wikiName) throws XWikiException
    {
        return getAPI().getWikiDocument(wikiName);
    }

    public XWikiServer getWikiDocument(String wikiName, int objectId) throws XWikiException
    {
        return getAPI().getWikiDocument(wikiName, objectId);
    }

    public List<XWikiServer> getWikiDocumentList() throws XWikiException
    {
        return getAPI().getWikiDocumentList();
    }

    public XWikiServer createWikiDocument() throws XWikiException
    {
        return getAPI().createWikiDocument();
    }

    public boolean isWikiExist(String wikiName)
    {
        return getAPI().isWikiExist(wikiName);
    }

    public boolean isWikiExist(String wikiName, int objectId)
    {
        return getAPI().isWikiExist(wikiName, objectId);
    }

    public boolean isWikiNameAvailable(String wikiName) throws XWikiException
    {
        return getAPI().isWikiNameAvailable(wikiName);
    }

    public int setWikiVisibility(String wikiName, String visibility) throws XWikiException
    {
        return getAPI().setWikiVisibility(wikiName, visibility);
    }

    public int setWikiVisibility(String wikiName, int objectId, String visibility) throws XWikiException
    {
        return getAPI().setWikiVisibility(wikiName, objectId, visibility);
    }

    public int setIsWikiTemplate(String wikiName, boolean isWikiTemplate) throws XWikiException
    {
        return getAPI().setIsWikiTemplate(wikiName, isWikiTemplate);
    }

    public int createWikiTemplate(String templateName, String templateDescription, String packageName)
        throws XWikiException
    {
        return getAPI().createWikiTemplate(templateName, templateDescription, packageName);
    }

    public XWikiServer getWikiTemplateDocument(String wikiName) throws XWikiException
    {
        return getAPI().getWikiTemplateDocument(wikiName);
    }

    public XWikiServer getWikiTemplateDocument(String wikiName, int objectId) throws XWikiException
    {
        return getAPI().getWikiTemplateDocument(wikiName, objectId);
    }

    public List<XWikiServer> getWikiTemplateList() throws XWikiException
    {
        return getAPI().getWikiTemplateList();
    }

    // ////////////////////////////////////////////////////////////////////////////////////////////:
    // PluginApi

    public WikiManagerPlugin getInternalPlugin()
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
