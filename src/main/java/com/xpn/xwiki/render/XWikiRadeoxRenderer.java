/*
 * Copyright 2006, XpertNet SARL, and individual contributors as indicated
 * by the contributors.txt.
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
 *
 * @author ludovic
 * @author sdumitriu
 */


package com.xpn.xwiki.render;

import java.util.Locale;

import org.radeox.api.engine.context.InitialRenderContext;
import org.radeox.api.engine.context.RenderContext;
import org.radeox.engine.context.BaseInitialRenderContext;
import org.radeox.engine.context.BaseRenderContext;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.util.Util;


public class XWikiRadeoxRenderer  implements XWikiRenderer {
    private boolean removePre = true;

    public XWikiRadeoxRenderer() {
    }

    public XWikiRadeoxRenderer(boolean removePre) {
        setRemovePre(removePre);
    }

    public String render(String content, XWikiDocument contentdoc, XWikiDocument doc, XWikiContext context) {
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
            // This is needed so that our local config is used
            InitialRenderContext ircontext = new BaseInitialRenderContext();
            Locale locale = new Locale("xwiki", "xwiki");
            ircontext.set(RenderContext.INPUT_LOCALE, locale);
            ircontext.set(RenderContext.OUTPUT_LOCALE, locale);

            XWikiRadeoxRenderEngine radeoxengine = new XWikiRadeoxRenderEngine(ircontext, context);
            rcontext.setRenderEngine(radeoxengine);

        }
        String result = rcontext.getRenderEngine().render(content, rcontext);
        return preTagSubst.insertNonWikiText(result);
    }

    public void flushCache() {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public boolean isRemovePre() {
        return removePre;
    }

    public void setRemovePre(boolean removePre) {
        this.removePre = removePre;
    }

    public String convertMultiLine(String macroname, String params, String data, String allcontent, XWikiVirtualMacro macro, XWikiContext context) {
        return allcontent;
    }

    public String convertSingleLine(String macroname, String params, String allcontent, XWikiVirtualMacro macro, XWikiContext context) {
        return allcontent;
    }    
}
