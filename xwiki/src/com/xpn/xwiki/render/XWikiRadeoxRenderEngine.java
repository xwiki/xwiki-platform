package com.xpn.xwiki.render;

import org.radeox.engine.BaseRenderEngine;
import org.radeox.api.engine.WikiRenderEngine;
import org.apache.commons.lang.StringUtils;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocInterface;
/**
 * Created by IntelliJ IDEA.
 * User: ludovic
 * Date: 8 mars 2004
 * Time: 08:55:40
 * To change this template use File | Settings | File Templates.
 */
public class XWikiRadeoxRenderEngine extends BaseRenderEngine implements WikiRenderEngine {
    private XWikiContext context;

    public XWikiRadeoxRenderEngine(XWikiContext context) {
        this.setContext(context);
    }

    public boolean exists(String name) {
        try {
            XWikiDocInterface currentdoc = ((XWikiDocInterface) context.get("doc"));
            String newname = StringUtils.replace(name, " ", "");
            XWikiDocInterface doc = context.getWiki().getDocument(
                                        (currentdoc!=null) ? currentdoc.getWeb() : "Main",
                                        newname, context);
            if ((doc==null)||doc.isNew())
                return false;
            else
                return true;
        }
        catch (Exception e) {
            e.printStackTrace();
            return false;
        }

    }

    public boolean showCreate() {
        return true;
    }

    public void appendLink(StringBuffer buffer, String name, String view, String anchor, boolean create) {
        XWikiContext context = getContext();
        buffer.append("<a href=\"");
        buffer.append(context.getWiki().getBase(context));
        buffer.append("view");
        buffer.append("/");

        String newname = StringUtils.replace(name, " ", "");

        if (newname.indexOf(".")!=-1) {
           newname = StringUtils.replace(newname, ".","/", 1);
        } else {
           newname = ((XWikiDocInterface)context.get("doc")).getWeb() + "/" + newname;
        }

        buffer.append(newname);
        if (anchor!=null) {
            buffer.append("#");
            buffer.append(anchor);
        }

        if (create==true) {
            XWikiDocInterface currentdoc = ((XWikiDocInterface) context.get("doc"));
            if (currentdoc!=null) {
                buffer.append("?parent=");
                buffer.append(currentdoc.getFullName());
            }
        }

        buffer.append("\">");
        buffer.append(view);
        buffer.append("</a>");
    }

    public void appendLink(StringBuffer buffer, String name, String view, String anchor) {
        appendLink(buffer, name, view, anchor, false);
    }

    public void appendLink(StringBuffer buffer, String name, String view) {
        appendLink(buffer, name, view, null, false);
    }

    public void appendCreateLink(StringBuffer buffer, String name, String view) {
        appendLink(buffer, name, view, null, true);
    }

    public XWikiContext getContext() {
        return context;
    }

    public void setContext(XWikiContext context) {
        this.context = context;
    }
}
