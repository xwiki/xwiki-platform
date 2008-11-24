package com.xpn.xwiki.plugin.skinx;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;

public class CssSkinExtensionPlugin extends AbstractDocumentSkinExtensionPlugin
{
    /** Log object to log messages in this class. */
    private static final Log LOG = LogFactory.getLog(CssSkinExtensionPlugin.class);

    public static final String SSX_CLASS_NAME = "XWiki.StyleSheetExtension";

    public CssSkinExtensionPlugin(String name, String className, XWikiContext context)
    {
        super(name, className, context);
        init(context);
    }

    /**
     * {@inheritDoc}
     * 
     * @see com.xpn.xwiki.plugin.XWikiDefaultPlugin#getName()
     */
    @Override
    public String getName()
    {
        return "ssx";
    }

    /**
     * {@inheritDoc}
     * 
     * @see SkinExtensionPlugin#getLink(String, XWikiContext)
     */
    @Override
    public String getLink(String documentName, XWikiContext context)
    {
        try {
            return "<link rel='stylesheet' type='text/css' href='"
                + context.getWiki().getURL(documentName, "ssx", "lang=" + context.getLanguage(), context) + "'/>";
        } catch (XWikiException e) {
            LOG.warn("Cannot link to CSS extension: " + documentName);
            return "";
        }
    }

    @Override
    protected String getExtensionClassName()
    {
        return SSX_CLASS_NAME;
    }

    @Override
    protected String getExtensionName()
    {
        return "Stylesheet";
    }

    @Override
    public void beginParsing(XWikiContext context)
    {
        super.beginParsing(context);
    }

    @Override
    public String endParsing(String content, XWikiContext context)
    {
        return super.endParsing(content, context);
    }

    @Override
    public void init(XWikiContext context)
    {
        super.init(context);
    }

    @Override
    public void virtualInit(XWikiContext context)
    {
        super.virtualInit(context);
    }
}
