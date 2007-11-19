package com.xpn.xwiki.plugin.wikimanager;

import java.util.Locale;
import java.util.ResourceBundle;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.plugin.applicationmanager.core.plugin.XWikiPluginMessageTool;

/**
 * Wiki Manager plugin translation messages manager.
 * <p>
 * The main use of this class is construct {@link XWikiPluginMessageTool} with the correct
 * {@link ResourceBundle} and to list all the message keys used internally in the plugin.
 * 
 * @version $Id: $
 */
public class WikiManagerMessageTool extends XWikiPluginMessageTool
{
    /**
     * Used as comment when creating a new empty wiki.
     */
    public static final String COMMENT_CREATEEMPTYWIKI = "wikimanager.plugin.comment.createwiki";

    /**
     * Used as comment when creating a new wiki from wiki template.
     */
    public static final String COMMENT_CREATEWIKIFROMTEMPLATE =
        "wikimanager.plugin.comment.createwikifromtemplate";

    /**
     * Used as comment when creating a new wiki from XAR package.
     */
    public static final String COMMENT_CREATEWIKIFROMPACKAGE =
        "wikimanager.plugin.comment.createwikifrompackage";

    /**
     * Used as comment when creating a new wiki template.
     */
    public static final String COMMENT_CREATEWIKITEMPLATE =
        "wikimanager.plugin.comment.createwikitemplate";

    /**
     * Used as {@link WikiManagerException} message when trying to make action that require virtual
     * mode in a wiki not in virtual mode.
     */
    public static final String ERROR_XWIKINOTVIRTUAL = "wikimanager.plugin.error.xwikinotvirtual";

    /**
     * Used as {@link WikiManagerException} message when provided user does not exists.
     */
    public static final String ERROR_USERDOESNOTEXIST =
        "wikimanager.plugin.error.userdoesnotexists";

    /**
     * Used as {@link WikiManagerException} message when provided user is not active.
     */
    public static final String ERROR_USERNOTACTIVE = "wikimanager.plugin.error.usernotactive";

    /**
     * Used as {@link WikiManagerException} message when tring to create a new wiki with a wiki name
     * forbidden.
     */
    public static final String ERROR_WIKINAMEFORBIDDEN =
        "wikimanager.plugin.error.wikinameforbidden";

    /**
     * Used as {@link WikiManagerException} message when trying to create a new wiki with a wiki
     * descriptor that already exist.
     */
    public static final String ERROR_DESCRIPTORALREADYEXISTS =
        "wikimanager.plugin.error.descriptoralreadyexists";

    /**
     * Used as {@link WikiManagerException} message when call to
     * {@link com.xpn.xwiki.XWiki#updateDatabase(String, XWikiContext)} failed.
     */
    public static final String ERROR_UPDATEDATABASE = "wikimanager.plugin.error.updatedatabase";

    /**
     * Used as {@link WikiManagerException} message when provided XAR package does not exists.
     */
    public static final String ERROR_PACKAGEDOESNOTEXISTS =
        "wikimanager.plugin.error.packagedoesnotexists";

    /**
     * Used as {@link WikiManagerException} message when failed to load XAR package as list of
     * {@link com.xpn.xwiki.doc.XWikiDocument}.
     */
    public static final String ERROR_PACKAGEIMPORT = "wikimanager.plugin.error.packageimport";

    /**
     * Used as {@link WikiManagerException} message when failed to insert loaded
     * {@link com.xpn.xwiki.doc.XWikiDocument} from package into database.
     */
    public static final String ERROR_PACKAGEINSTALL = "wikimanager.plugin.error.packageinstall";

    /**
     * Used as {@link org.apache.commons.logging.Log} log message when trying to create a new wiki
     * with a wiki descriptor that already exist.
     */
    public static final String LOG_DESCRIPTORALREADYEXISTS =
        "wikimanager.plugin.log.createwiki.descriptoralreadyexists";

    /**
     * Used as {@link org.apache.commons.logging.Log} log message when trying to create a new
     * database/schema that already exists.
     */
    public static final String LOG_DATABASEALREADYEXISTS =
        "wikimanager.plugin.log.createwiki.databasealreadyexist";

    /**
     * Used as {@link org.apache.commons.logging.Log} log message when database/schema creation
     * failed.
     */
    public static final String LOG_DATABASECREATION =
        "wikimanager.plugin.log.createwiki.databasecreation";

    /**
     * Used as {@link org.apache.commons.logging.Log} log message when database/schema creation
     * thrown unknown exception.
     */
    public static final String LOG_DATABASECREATIONEXCEPTION =
        "wikimanager.plugin.log.createwiki.databasecreationexception";

    /**
     * Used as {@link org.apache.commons.logging.Log} log message when wiki creation failed.
     */
    public static final String LOG_WIKICREATION = "wikimanager.plugin.log.wikicreation";

    /**
     * Used as {@link org.apache.commons.logging.Log} log message when wiki deletion failed.
     */
    public static final String LOG_WIKIDELETION = "wikimanager.plugin.log.wikideletion";

    /**
     * Used as {@link org.apache.commons.logging.Log} log message when failed to find wiki alias.
     */
    public static final String LOG_WIKIGET = "wikimanager.plugin.log.wikiget";
    
    /**
     * Used as {@link org.apache.commons.logging.Log} log message when failed to find wiki
     * descriptor document.
     */
    public static final String LOG_WIKIALIASGET = "wikimanager.plugin.log.wikialiasget";

    /**
     * Used as {@link org.apache.commons.logging.Log} log message when failed to find all the wikis
     * descriptors documents.
     */
    public static final String LOG_WIKIGETALL = "wikimanager.plugin.log.wikigetall";

    /**
     * Used as {@link org.apache.commons.logging.Log} log message when failed to find all the wikis
     * aliases.
     */
    public static final String LOG_WIKIALIASGETALL = "wikimanager.plugin.log.wikialiasgetall";

    /**
     * Used as {@link org.apache.commons.logging.Log} log message when modification of the
     * "visibility" field of a wiki descriptor failed.
     */
    public static final String LOG_WIKISETVISIBILITY = "wikimanager.plugin.log.wikisetvisibility";

    /**
     * Used as {@link org.apache.commons.logging.Log} log message when failed to find wiki template
     * descriptor document.
     */
    public static final String LOG_WIKITEMPLATEGET = "wikimanager.plugin.log.wikitemplateget";

    /**
     * Used as {@link org.apache.commons.logging.Log} log message when failed to find all the wikis
     * templates descriptors documents.
     */
    public static final String LOG_WIKITEMPLATEGETALL =
        "wikimanager.plugin.log.wikitemplategetall";

    /**
     * Create default WikiManagerMessageTool. Only look at WikiManager properties file with system
     * {@link Locale}.
     */
    WikiManagerMessageTool()
    {
        super(null, null);
    }

    /**
     * Call for {@link XWikiPluginMessageTool#XWikiPluginMessageTool(ResourceBundle, XWikiContext)}.
     * Construct ResourceBundle based on {@link WikiManagerPlugin#PLUGIN_NAME} +
     * "/ApplicationResources".
     * 
     * @param locale the {@link Locale} used to load the {@link ResourceBundle}.
     * @param context the {@link com.xpn.xwiki.XWikiContext} object, used to get access to XWiki
     *            primitives for loading documents
     */
    WikiManagerMessageTool(Locale locale, XWikiContext context)
    {
        super(ResourceBundle.getBundle(WikiManagerPlugin.PLUGIN_NAME + "/ApplicationResources",
            locale == null ? Locale.ENGLISH : locale), context);
    }
}
