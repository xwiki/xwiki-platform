package com.xpn.xwiki.plugin.skinx;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.classes.BaseClass;

public class JsSkinExtensionPlugin extends SkinExtensionPlugin
{
    /** Log helper for logging messages in this class. */
    private static final Log LOG = LogFactory.getLog(JsSkinExtensionPlugin.class);

    public static final String JSX_CLASS_NAME = "XWiki.JavaScriptExtension";

    public JsSkinExtensionPlugin(String name, String className, XWikiContext context)
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
        return "jsx";
    }

    /**
     * @see com.xpn.xwiki.plugin.XWikiDefaultPlugin#init(com.xpn.xwiki.XWikiContext)
     */
    @Override
    public void init(XWikiContext context)
    {
        super.init(context);
        getJsxClass(context);
    }

    /**
     * @see com.xpn.xwiki.plugin.XWikiDefaultPlugin#virtualInit(com.xpn.xwiki.XWikiContext)
     */
    @Override
    public void virtualInit(XWikiContext context)
    {
        super.virtualInit(context);
        getJsxClass(context);
    }

    @Override
    public String getLink(String documentName, XWikiContext context)
    {
        try {
            return "<script type='text/javascript' src='" + context.getWiki().getURL(documentName, "jsx", context)
                + "'></script>";
        } catch (XWikiException e) {
            LOG.warn("Cannot link to JS extension: " + documentName);
            return "";
        }
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

    public BaseClass getJsxClass(XWikiContext context)
    {
        try {
            XWikiDocument doc = context.getWiki().getDocument(JSX_CLASS_NAME, context);
            boolean needsUpdate = false;

            BaseClass bclass = doc.getxWikiClass();
            if (context.get("initdone") != null) {
                return bclass;
            }

            bclass.setName(JSX_CLASS_NAME);

            needsUpdate |= bclass.addTextField("name", "Name", 30);
            needsUpdate |= bclass.addTextAreaField("code", "Code", 50, 20);
            needsUpdate |= bclass.addStaticListField("use", "Use this extension", "onDemand=On demand|always=Always");
            needsUpdate |= bclass.addBooleanField("parse", "Parse content", "yesno");
            needsUpdate |= bclass.addStaticListField("cache", "Caching policy", "long|short|default|forbid");

            if (StringUtils.isBlank(doc.getAuthor())) {
                needsUpdate = true;
                doc.setAuthor("XWiki.Admin");
            }
            if (StringUtils.isBlank(doc.getCreator())) {
                needsUpdate = true;
                doc.setCreator("XWiki.Admin");
            }
            if (StringUtils.isBlank(doc.getParent())) {
                needsUpdate = true;
                doc.setParent("XWiki.XWikiClasses");
            }
            if (StringUtils.isBlank(doc.getContent())) {
                needsUpdate = true;
                doc.setContent("1 XWiki Stylesheet Extension Class");
            }

            if (needsUpdate) {
                context.getWiki().saveDocument(doc, context);
            }
            return bclass;
        } catch (Exception ex) {
            LOG.error("Cannot initialize JsxClass", ex);
        }
        return null;
    }
}
