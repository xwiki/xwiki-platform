package com.xpn.xwiki.plugin.skinx;

import com.xpn.xwiki.XWikiContext;

/**
 * Skin Extension plugin that allows pulling CSS code stored inside wiki documents as
 * <code>XWiki.StyleSheetExtension</code> objects.
 * 
 * @version $Id$
 */
public class CssSkinExtensionPlugin extends AbstractDocumentSkinExtensionPlugin
{
    /** The name of the XClass storing the code for this type of extensions. */
    public static final String SSX_CLASS_NAME = "XWiki.StyleSheetExtension";

    /**
     * The identifier for this plugin; used for accessing the plugin from velocity, and as the action returning the
     * extension content.
     */
    public static final String PLUGIN_NAME = "ssx";

    /**
     * XWiki plugin constructor.
     * 
     * @param name The name of the plugin, which can be used for retrieving the plugin API from velocity. Unused.
     * @param className The canonical classname of the plugin. Unused.
     * @param context The current request context.
     * @see com.xpn.xwiki.plugin.XWikiDefaultPlugin#XWikiDefaultPlugin(String,String,com.xpn.xwiki.XWikiContext)
     */
    public CssSkinExtensionPlugin(String name, String className, XWikiContext context)
    {
        super(PLUGIN_NAME, className, context);
    }

    /**
     * {@inheritDoc}
     * <p>
     * We must override this method since the plugin manager only calls it for classes that provide their own
     * implementation, and not an inherited one.
     * </p>
     * 
     * @see com.xpn.xwiki.plugin.XWikiPluginInterface#virtualInit(com.xpn.xwiki.XWikiContext)
     */
    @Override
    public void virtualInit(XWikiContext context)
    {
        super.virtualInit(context);
    }

    /**
     * {@inheritDoc}
     * 
     * @see AbstractSkinExtensionPlugin#getLink(String, XWikiContext)
     */
    @Override
    public String getLink(String documentName, XWikiContext context)
    {
        return "<link rel='stylesheet' type='text/css' href='"
            + context.getWiki().getURL(documentName, PLUGIN_NAME,
                "language=" + context.getLanguage() + parametersAsQueryString(documentName, context), context) + "'/>";
    }

    /**
     * {@inheritDoc}
     * 
     * @see AbstractDocumentSkinExtensionPlugin#getExtensionClassName()
     */
    @Override
    protected String getExtensionClassName()
    {
        return SSX_CLASS_NAME;
    }

    /**
     * {@inheritDoc}
     * 
     * @see AbstractDocumentSkinExtensionPlugin#getExtensionName()
     */
    @Override
    protected String getExtensionName()
    {
        return "Stylesheet";
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
