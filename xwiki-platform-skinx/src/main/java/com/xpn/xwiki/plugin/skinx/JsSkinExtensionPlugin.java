package com.xpn.xwiki.plugin.skinx;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.xpn.xwiki.XWikiContext;

/**
 * Skin Extension plugin that allows pulling javascript code stored inside wiki documents as
 * <code>XWiki.JavaScriptExtension</code> objects.
 * 
 * @version $Id$
 */
public class JsSkinExtensionPlugin extends AbstractDocumentSkinExtensionPlugin
{
    /** The name of the XClass storing the code for this type of extensions. */
    public static final String JSX_CLASS_NAME = "XWiki.JavaScriptExtension";

    /**
     * The identifier for this plugin; used for accessing the plugin from velocity, and as the action returning the
     * extension content.
     */
    public static final String PLUGIN_NAME = "jsx";

    /** Log helper for logging messages in this class. */
    private static final Log LOG = LogFactory.getLog(JsSkinExtensionPlugin.class);

    /**
     * XWiki plugin constructor.
     * 
     * @param name The name of the plugin, which can be used for retrieving the plugin API from velocity. Unused.
     * @param className The canonical classname of the plugin. Unused.
     * @param context The current request context.
     * @see com.xpn.xwiki.plugin.XWikiDefaultPlugin#XWikiDefaultPlugin(String,String,com.xpn.xwiki.XWikiContext)
     */
    public JsSkinExtensionPlugin(String name, String className, XWikiContext context)
    {
        super(PLUGIN_NAME, className, context);
    }

    /**
     * {@inheritDoc}
     * 
     * @see AbstractSkinExtensionPlugin#getLink(String, XWikiContext)
     */
    @Override
    public String getLink(String documentName, XWikiContext context)
    {
        return "<script type='text/javascript' src='"
            + context.getWiki().getURL(documentName, PLUGIN_NAME,
                "language=" + context.getLanguage() + parametersAsQueryString(documentName, context), context)
            + "'></script>";
    }

    /**
     * {@inheritDoc}
     * 
     * @see AbstractDocumentSkinExtensionPlugin#getExtensionClassName()
     */
    @Override
    protected String getExtensionClassName()
    {
        return JSX_CLASS_NAME;
    }

    /**
     * {@inheritDoc}
     * 
     * @see AbstractDocumentSkinExtensionPlugin#getExtensionName()
     */
    @Override
    protected String getExtensionName()
    {
        return "Javascript";
    }

    /**
     * {@inheritDoc}
     * <p>
     * We must override this method since the plugin manager only calls it for classes that provide their own
     * implementation, and not an inherited one.
     * </p>
     * 
     * @see AbstractSkinExtensionPlugin#endParsing(String, XWikiContext)
     */
    @Override
    public String endParsing(String content, XWikiContext context)
    {
        return super.endParsing(content, context);
    }
}
