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

import java.util.Locale;
import java.util.ResourceBundle;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.plugin.applicationmanager.core.plugin.XWikiPluginMessageTool;

/**
 * Wiki Manager plugin translation messages manager.
 * <p>
 * The main use of this class is construct {@link XWikiPluginMessageTool} with the correct
 * {@link java.util.ResourceBundle} and to list all the message keys used internally in the plugin.
 * 
 * @version $Id$
 */
public class WikiManagerMessageTool extends XWikiPluginMessageTool
{
    /**
     * Key to use with {@link XWikiContext#get(Object)}.
     * 
     * @since 1.1
     */
    public static final String MESSAGETOOL_CONTEXT_KEY = "wikimanagermessagetool";

    /**
     * Used as comment when creating a new empty wiki.
     */
    public static final String COMMENT_CREATEEMPTYWIKI = "wikimanager.plugin.comment.createwiki";

    /**
     * Used as comment when creating a new wiki from wiki template.
     */
    public static final String COMMENT_CREATEWIKIFROMTEMPLATE = "wikimanager.plugin.comment.createwikifromtemplate";

    /**
     * Used as comment when creating a new wiki from XAR package.
     */
    public static final String COMMENT_CREATEWIKIFROMPACKAGE = "wikimanager.plugin.comment.createwikifrompackage";

    /**
     * Used as comment when creating a new wiki template.
     */
    public static final String COMMENT_CREATEWIKITEMPLATE = "wikimanager.plugin.comment.createwikitemplate";

    /**
     * Used as {@link WikiManagerException} message when trying to make action that require virtual mode in a wiki not
     * in virtual mode.
     */
    public static final String ERROR_XWIKINOTVIRTUAL = "wikimanager.plugin.error.xwikinotvirtual";

    /**
     * Used as {@link WikiManagerException} message when provided user does not exists.
     */
    public static final String ERROR_USERDOESNOTEXIST = "wikimanager.plugin.error.userdoesnotexists";

    /**
     * Used as {@link WikiManagerException} message when provided user is not active.
     */
    public static final String ERROR_USERNOTACTIVE = "wikimanager.plugin.error.usernotactive";

    /**
     * Used as {@link WikiManagerException} message when tring to create a new wiki with a wiki name forbidden.
     */
    public static final String ERROR_WIKINAMEFORBIDDEN = "wikimanager.plugin.error.wikinameforbidden";

    /**
     * Used as {@link WikiManagerException} message when trying to create a new wiki with a wiki descriptor that already
     * exist.
     */
    public static final String ERROR_DESCRIPTORALREADYEXISTS = "wikimanager.plugin.error.descriptoralreadyexists";

    /**
     * Used as {@link WikiManagerException} message when call to
     * {@link com.xpn.xwiki.XWiki#updateDatabase(String, XWikiContext)} failed.
     */
    public static final String ERROR_UPDATEDATABASE = "wikimanager.plugin.error.updatedatabase";

    /**
     * Used as {@link WikiManagerException} message when provided XAR package does not exists.
     */
    public static final String ERROR_PACKAGEDOESNOTEXISTS = "wikimanager.plugin.error.packagedoesnotexists";

    /**
     * Used as {@link WikiManagerException} message when failed to load XAR package as list of
     * {@link com.xpn.xwiki.doc.XWikiDocument}.
     */
    public static final String ERROR_PACKAGEIMPORT = "wikimanager.plugin.error.packageimport";

    /**
     * Used as {@link WikiManagerException} message when failed to insert loaded {@link com.xpn.xwiki.doc.XWikiDocument}
     * from package into database.
     */
    public static final String ERROR_PACKAGEINSTALL = "wikimanager.plugin.error.packageinstall";

    /**
     * Used as {@link WikiManagerException} message when trying to create wiki when it's not administrator user.
     * 
     * @since 1.1
     */
    public static final String ERROR_RIGHTTOCREATEWIKI = "wikimanager.plugin.error.righttocreatewiki";

    /**
     * Used as {@link WikiManagerException} message when trying to delete wiki when it's not administrator user.
     * 
     * @since 1.1
     */
    public static final String ERROR_RIGHTTODELETEWIKI = "wikimanager.plugin.error.righttodeletewiki";

    /**
     * Used as {@link WikiManagerException} message when trying to get a wiki alias which does not exists.
     * 
     * @since 1.1
     */
    public static final String ERROR_WIKIALIASDOESNOTEXISTS = "wikimanager.plugin.error.wikialiasdoesnotexists";

    /**
     * Used as {@link WikiManagerException} message when trying to get a wiki which does not exists.
     * 
     * @since 1.1
     */
    public static final String ERROR_WIKIDOESNOTEXISTS = "wikimanager.plugin.error.wikidoesnotexists";

    /**
     * Used as {@link WikiManagerException} message when trying to delete wiki with not administrator user.
     * 
     * @since 1.1
     */
    public static final String ERROR_WIKITEMPLATEALIASDOESNOTEXISTS =
        "wikimanager.plugin.error.wikitemplatealiasdoesnotexists";

    /**
     * Used as {@link WikiManagerException} message when trying to delete the main wiki.
     * 
     * @since 1.1
     */
    public static final String ERROR_DELETEMAINWIKI = "wikimanager.plugin.error.deletemainwiki";

    /**
     * Used as log message when trying to create a new wiki with a wiki descriptor that already exist.
     */
    public static final String LOG_DESCRIPTORALREADYEXISTS =
        "wikimanager.plugin.log.createwiki.descriptoralreadyexists";

    /**
     * Used as log message when trying to create a new database/schema that already exists.
     */
    public static final String LOG_DATABASEALREADYEXISTS = "wikimanager.plugin.log.createwiki.databasealreadyexist";

    /**
     * Used as log message when database/schema creation failed.
     */
    public static final String LOG_DATABASECREATION = "wikimanager.plugin.log.createwiki.databasecreation";

    /**
     * Used as log message when database/schema creation thrown unknown exception.
     */
    public static final String LOG_DATABASECREATIONEXCEPTION =
        "wikimanager.plugin.log.createwiki.databasecreationexception";

    /**
     * Used as log message when wiki creation failed.
     */
    public static final String LOG_WIKICREATION = "wikimanager.plugin.log.wikicreation";

    /**
     * Used as log message when wiki deletion failed.
     */
    public static final String LOG_WIKIDELETION = "wikimanager.plugin.log.wikideletion";

    /**
     * Used as log message when failed to find wiki alias.
     */
    public static final String LOG_WIKIGET = "wikimanager.plugin.log.wikiget";

    /**
     * Used as log message when failed to find wiki descriptor document.
     */
    public static final String LOG_WIKIALIASGET = "wikimanager.plugin.log.wikialiasget";

    /**
     * Used as log message when failed to find all the wikis descriptors documents.
     */
    public static final String LOG_WIKIGETALL = "wikimanager.plugin.log.wikigetall";

    /**
     * Used as log message when failed to find all the wikis aliases.
     */
    public static final String LOG_WIKIALIASGETALL = "wikimanager.plugin.log.wikialiasgetall";

    /**
     * Used as log message when modification of the "visibility" field of a wiki descriptor failed.
     */
    public static final String LOG_WIKISETVISIBILITY = "wikimanager.plugin.log.wikisetvisibility";

    /**
     * Used as log message when failed to find wiki template descriptor document.
     */
    public static final String LOG_WIKITEMPLATEGET = "wikimanager.plugin.log.wikitemplateget";

    /**
     * Used as log message when failed to find all the wikis templates descriptors documents.
     */
    public static final String LOG_WIKITEMPLATEGETALL = "wikimanager.plugin.log.wikitemplategetall";

    /**
     * Default bundle manager where to find translated messages.
     * 
     * @since 1.1
     */
    private static final WikiManagerMessageTool DEFAULTMESSAGETOOL = new WikiManagerMessageTool();

    /**
     * Create default WikiManagerMessageTool. Only look at WikiManager properties file with system {@link Locale}.
     */
    private WikiManagerMessageTool()
    {
        super(ResourceBundle.getBundle(WikiManagerPlugin.PLUGIN_NAME + "/ApplicationResources"));
    }

    /**
     * Call for {@link XWikiPluginMessageTool#XWikiPluginMessageTool(ResourceBundle, XWikiContext)}. Construct
     * ResourceBundle based on {@link WikiManagerPlugin#PLUGIN_NAME} + "/ApplicationResources".
     * 
     * @param locale the {@link Locale} used to load the {@link ResourceBundle}.
     * @param plugin the plugin.
     * @param context the {@link com.xpn.xwiki.XWikiContext} object, used to get access to XWiki primitives for loading
     *            documents
     */
    WikiManagerMessageTool(Locale locale, WikiManagerPlugin plugin, XWikiContext context)
    {
        super(locale, plugin, context);
    }

    /**
     * Get Wiki Manager message tool registered in XWiki context. If not return default.
     * 
     * @param context the XWiki context from which to get message tool.
     * @return the default Wiki Manager message tool.
     * @since 1.1
     */
    public static WikiManagerMessageTool getDefault(XWikiContext context)
    {
        Object messagetool = context.get(MESSAGETOOL_CONTEXT_KEY);

        return messagetool != null && messagetool instanceof WikiManagerMessageTool
            ? (WikiManagerMessageTool) messagetool : DEFAULTMESSAGETOOL;
    }
}
