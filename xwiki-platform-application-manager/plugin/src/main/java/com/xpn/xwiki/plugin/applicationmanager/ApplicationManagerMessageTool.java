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

import java.util.Locale;
import java.util.ResourceBundle;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.plugin.applicationmanager.core.plugin.XWikiPluginMessageTool;

/**
 * Application Manager plugin translation messages manager.
 * <p>
 * The main use of this class is construct {@link XWikiPluginMessageTool} with the correct
 * {@link java.util.ResourceBundle} and to list all the message keys used internally in the plugin.
 * 
 * @version $Id$
 * @since 1.1
 */
public class ApplicationManagerMessageTool extends XWikiPluginMessageTool
{
    /**
     * Key to use with {@link XWikiContext#get(Object)}.
     */
    public static final String MESSAGETOOL_CONTEXT_KEY = "applicationmanagermessagetool";

    /**
     * Used as comment when creating a new application.
     */
    public static final String COMMENT_CREATEAPPLICATION = "applicationmanager.plugin.comment.createapplication";

    /**
     * Used as comment when importing a new application.
     */
    public static final String COMMENT_IMPORTAPPLICATION = "applicationmanager.plugin.comment.importapplication";

    /**
     * Used as comment when reloading an application.
     */
    public static final String COMMENT_RELOADAPPLICATION = "applicationmanager.plugin.comment.reloadapplication";

    /**
     * Used as comment when reloading all applications.
     */
    public static final String COMMENT_RELOADALLAPPLICATIONS =
        "applicationmanager.plugin.comment.reloadallapplications";

    /**
     * Used as comment when automatically update application translations pages.
     */
    public static final String COMMENT_AUTOUPDATETRANSLATIONS =
        "applicationmanager.plugin.comment.autoupdatetranslations";

    /**
     * Used as comment when refreshing all applications translations pages.
     */
    public static final String COMMENT_REFRESHALLTRANSLATIONS =
        "applicationmanager.plugin.comment.refreshalltranslations";

    /**
     * Used as {@link ApplicationManagerException} message when application default page name already exists.
     */
    public static final String ERROR_APPPAGEALREADYEXISTS =
        "applicationmanager.plugin.error.applicationpagealreadyexists";

    /**
     * Used as {@link ApplicationManagerException} message when provided XAR package does not exists.
     */
    public static final String ERROR_IMORT_PKGDOESNOTEXISTS =
        "applicationmanager.plugin.error.import.packagedoesnotexists";

    /**
     * Used as {@link ApplicationManagerException} message when failed to load XAR package as list of
     * {@link com.xpn.xwiki.doc.XWikiDocument}.
     */
    public static final String ERROR_IMORT_IMPORT = "applicationmanager.plugin.error.import.import";

    /**
     * Used as {@link ApplicationManagerException} message when failed to insert loaded
     * {@link com.xpn.xwiki.doc.XWikiDocument} from package into database.
     */
    public static final String ERROR_IMORT_INSTALL = "applicationmanager.plugin.error.import.install";

    /**
     * Used as {@link ApplicationManagerException} message when failed to find application from provided application
     * name.
     */
    public static final String ERROR_APPDOESNOTEXISTS = "applicationmanager.plugin.error.applicationdoesnotexists";

    /**
     * Used as {@link org.apache.commons.logging.Log} log message when application creation failed.
     */
    public static final String LOG_CREATEAPP = "applicationmanager.plugin.log.createapplication";

    /**
     * Used as {@link org.apache.commons.logging.Log} log message when application delete failed.
     */
    public static final String LOG_DELETEAPP = "applicationmanager.plugin.log.deleteapplication";

    /**
     * Used as {@link org.apache.commons.logging.Log} log message when getting all application descriptors failed.
     */
    public static final String LOG_GETALLAPPS = "applicationmanager.plugin.log.getallapplications";

    /**
     * Used as {@link org.apache.commons.logging.Log} log message when getting application descriptor failed.
     */
    public static final String LOG_GETAPP = "applicationmanager.plugin.log.getapplication";

    /**
     * Used as {@link org.apache.commons.logging.Log} log message when exporting application failed.
     */
    public static final String LOG_EXPORTAPP = "applicationmanager.plugin.log.exportapplication";

    /**
     * Used as {@link org.apache.commons.logging.Log} log message when importing application failed.
     */
    public static final String LOG_IMPORTAPP = "applicationmanager.plugin.log.importapplication";

    /**
     * Used as {@link org.apache.commons.logging.Log} log message when reloading application failed.
     */
    public static final String LOG_RELOADAPP = "applicationmanager.plugin.log.reloadapplication";

    /**
     * Used as {@link org.apache.commons.logging.Log} log message when reloading all applications failed.
     */
    public static final String LOG_REALOADALLAPPS = "applicationmanager.plugin.log.realoadallapplications";

    /**
     * Used as {@link org.apache.commons.logging.Log} log message when automatically updating application translations
     * informations failed.
     */
    public static final String LOG_AUTOUPDATETRANSLATIONS = "applicationmanager.plugin.log.autoupdatetranslations";

    /**
     * Used as {@link org.apache.commons.logging.Log} log message when refreshing all applications translations pages.
     */
    public static final String LOG_REFRESHALLTRANSLATIONS = "applicationmanager.plugin.log.refreshalltranslations";

    /**
     * Used as {@link org.apache.commons.logging.Log} log message when getting wiki root application failed.
     */
    public static final String LOG_GETROOTAPP = "applicationmanager.plugin.log.getrootapplication";

    /**
     * Default bundle manager where to find translated messages.
     */
    private static final ApplicationManagerMessageTool DEFAULTMESSAGETOOL = new ApplicationManagerMessageTool();

    /**
     * Create default WikiManagerMessageTool. Only look at WikiManager properties file with system {@link Locale}.
     */
    private ApplicationManagerMessageTool()
    {
        super(ResourceBundle.getBundle(ApplicationManagerPlugin.PLUGIN_NAME + "/ApplicationResources"));
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
    ApplicationManagerMessageTool(Locale locale, ApplicationManagerPlugin plugin, XWikiContext context)
    {
        super(locale, plugin, context);
    }

    /**
     * Get Wiki Manager message tool registered in XWiki context. If not return default.
     * 
     * @param context the XWiki context from which to get message tool.
     * @return the default Wiki Manager message tool.
     */
    public static ApplicationManagerMessageTool getDefault(XWikiContext context)
    {
        Object messagetool = context.get(MESSAGETOOL_CONTEXT_KEY);

        return messagetool != null && messagetool instanceof ApplicationManagerMessageTool
            ? (ApplicationManagerMessageTool) messagetool : DEFAULTMESSAGETOOL;
    }
}
