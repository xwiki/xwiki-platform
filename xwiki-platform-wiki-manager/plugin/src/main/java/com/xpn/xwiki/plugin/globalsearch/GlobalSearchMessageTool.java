package com.xpn.xwiki.plugin.globalsearch;

import java.util.Locale;
import java.util.ResourceBundle;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.plugin.applicationmanager.core.plugin.XWikiPluginMessageTool;

/**
 * Global Search plugin translation messages manager.
 * <p>
 * The main use of this class is construct {@link XWikiPluginMessageTool} with the correct
 * {@link java.util.ResourceBundle} and to list all the message keys used internally in the plugin.
 * 
 * @version $Id$
 */
public class GlobalSearchMessageTool extends XWikiPluginMessageTool
{
    /**
     * Key to use with {@link XWikiContext#get(Object)}.
     */
    public static final String MESSAGETOOL_CONTEXT_KEY = "globalsearchmessagetool";

    /**
     * Used as {@link GlobalSearchException} message when provided field does not exist in the document.
     */
    public static final String ERROR_CANTACCESSFIELD = "globalsearch.plugin.error.cantaccessdocfield";

    /**
     * Used as {@link GlobalSearchException} message when failed to get document translations.
     */
    public static final String ERROR_DOCUMENTTRANSLATIONS = "globalsearch.plugin.error.documenttranslations";

    /**
     * Used as {@link org.apache.commons.logging.Log} log message when trying to search documents.
     */
    public static final String LOG_SEARCHDOCUMENTS = "globalsearch.plugin.log.searchdocuments";

    /**
     * Used as {@link org.apache.commons.logging.Log} log message when trying to get document from name.
     */
    public static final String LOG_GETDOCUMENTFROMNAME = "globalsearch.plugin.log.getdocumentfromname";

    /**
     * Default bundle manager where to find translated messages.
     * 
     * @since 1.1
     */
    private static final GlobalSearchMessageTool DEFAULTMESSAGETOOL = new GlobalSearchMessageTool();

    /**
     * Create default WikiManagerMessageTool. Only look at WikiManager properties file with system {@link Locale}.
     * 
     * @since 1.1
     */
    private GlobalSearchMessageTool()
    {
        super(ResourceBundle.getBundle(GlobalSearchPlugin.PLUGIN_NAME + "/ApplicationResources"));
    }

    /**
     * Call for {@link XWikiPluginMessageTool#XWikiPluginMessageTool(ResourceBundle, XWikiContext)}. Construct
     * ResourceBundle based on {@link GlobalSearchPlugin#PLUGIN_NAME} + "/ApplicationResources".
     * 
     * @param locale the {@link Locale} used to load the {@link ResourceBundle}.
     * @param plugin tyhe plugin.
     * @param context the {@link com.xpn.xwiki.XWikiContext} object, used to get access to XWiki primitives for loading
     *            documents
     */
    GlobalSearchMessageTool(Locale locale, GlobalSearchPlugin plugin, XWikiContext context)
    {
        super(locale, plugin, context);
    }

    /**
     * Get Global Search message tool registered in XWiki context. If not return default.
     * 
     * @param context the XWiki context from which to get message tool.
     * @return the default Global Search message tool.
     * @since 1.1
     */
    public static GlobalSearchMessageTool getDefault(XWikiContext context)
    {
        Object messagetool = context.get(MESSAGETOOL_CONTEXT_KEY);

        return messagetool != null && messagetool instanceof GlobalSearchMessageTool
            ? (GlobalSearchMessageTool) messagetool : DEFAULTMESSAGETOOL;
    }
}
