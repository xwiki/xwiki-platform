package com.xpn.xwiki.render;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.doc.XWikiDocInterface;
import com.xpn.xwiki.util.Util;
import org.radeox.api.engine.context.RenderContext;
import org.radeox.engine.context.BaseRenderContext;

/**
 * Created by IntelliJ IDEA.
 * User: ludovic
 * Date: 8 mars 2004
 * Time: 08:50:54
 * To change this template use File | Settings | File Templates.
 */
public class XWikiRadeoxRenderer  implements XWikiRenderer {
    private boolean removePre = true;

    public XWikiRadeoxRenderer() {
    }

    public XWikiRadeoxRenderer(boolean removePre) {
        setRemovePre(removePre);
    }

    public String render(String content, XWikiDocInterface doc, XWikiContext context) {
        Util util = context.getUtil();
        // Remove the content that is inside "{pre}"
        PreTagSubstitution preTagSubst = new PreTagSubstitution(util, isRemovePre());
        content = preTagSubst.substitute(content);

        RenderContext rcontext = (RenderContext) context.get("rcontext");
        if (rcontext==null) {
            rcontext = new BaseRenderContext();
            rcontext.set("xcontext", context);
        }
        if (rcontext.getRenderEngine()==null) {
            XWikiRadeoxRenderEngine radeoxengine = new XWikiRadeoxRenderEngine(context);
            rcontext.setRenderEngine(radeoxengine);
        }
        String result = rcontext.getRenderEngine().render(content, rcontext);
        return preTagSubst.insertNonWikiText(result);
    }

    public boolean isRemovePre() {
        return removePre;
    }

    public void setRemovePre(boolean removePre) {
        this.removePre = removePre;
    }

}
