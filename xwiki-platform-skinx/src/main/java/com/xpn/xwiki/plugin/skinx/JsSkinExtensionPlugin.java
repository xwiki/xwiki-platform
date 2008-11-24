package com.xpn.xwiki.plugin.skinx;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;

public class JsSkinExtensionPlugin extends AbstractDocumentSkinExtensionPlugin
{
    public static final String JSX_CLASS_NAME = "XWiki.JavaScriptExtension";

    public JsSkinExtensionPlugin(String name, String className, XWikiContext context)
    {
        super(name, className, context);
    }

    /**
     * {@inheritDoc}
     * 
     * @see com.xpn.xwiki.plugin.XWikiDefaultPlugin#getName()
     */
    @Override
    public String getName()
    {
        return "jsx";
    }

    @Override
    public String getLink(String documentName, XWikiContext context)
    {
        try {
            return "<script type='text/javascript' src='"
                + context.getWiki().getURL(documentName, "jsx", "lang=" + context.getLanguage(), context)
                + "'></script>";
        } catch (XWikiException e) {
            LOG.warn("Cannot link to JS extension: " + documentName);
            return "";
        }
    }

    @Override
    protected String getExtensionClassName()
    {
        return JSX_CLASS_NAME;
    }

    @Override
    protected String getExtensionName()
    {
        return "Javascript";
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
