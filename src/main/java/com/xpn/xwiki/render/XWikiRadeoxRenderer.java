/**
 * ===================================================================
 *
 * Copyright (c) 2003 Ludovic Dubost, All rights reserved.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details, published at
 * http://www.gnu.org/copyleft/gpl.html or in gpl.txt in the
 * root folder of this distribution.
 * * User: ludovic
 * Date: 8 mars 2004
 * Time: 08:50:54
 */

package com.xpn.xwiki.render;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.util.Util;
import org.radeox.api.engine.context.InitialRenderContext;
import org.radeox.api.engine.context.RenderContext;
import org.radeox.engine.context.BaseInitialRenderContext;
import org.radeox.engine.context.BaseRenderContext;

import java.util.Locale;


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

}
