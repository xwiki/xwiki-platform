/*
 * See the NOTICE file distributed with this work for additional
 * information regarding copyright ownership.
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
 */


package com.xpn.xwiki.render;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.util.Util;
import org.apache.commons.lang.StringUtils;
import org.radeox.api.engine.context.InitialRenderContext;
import org.radeox.api.engine.context.RenderContext;
import org.radeox.engine.context.BaseInitialRenderContext;
import org.radeox.engine.context.BaseRenderContext;

import java.util.HashMap;
import java.util.Locale;


public class XWikiRadeoxRenderer  implements XWikiRenderer {
    private boolean removePre = true;
    private XWikiRadeoxRenderEngine radeoxEngine;

    public XWikiRadeoxRenderer()
    {
        // This is needed so that our local config is used
        InitialRenderContext ircontext = new BaseInitialRenderContext();
        Locale locale = new Locale("xwiki", "xwiki");
        ircontext.set(RenderContext.INPUT_LOCALE, locale);
        ircontext.set(RenderContext.OUTPUT_LOCALE, locale);
        ircontext.setParameters(new HashMap());

        this.radeoxEngine = new XWikiRadeoxRenderEngine(ircontext);
    }

    public XWikiRadeoxRenderer(boolean removePre) {
        this();
        setRemovePre(removePre);
    }

    public String render(String content, XWikiDocument contentdoc, XWikiDocument contextdoc, XWikiContext context) {
        Util util = context.getUtil();
        // Remove the content that is inside "{pre}"
        PreTagSubstitution preTagSubst = new PreTagSubstitution(util, isRemovePre());
        content = preTagSubst.substitute(content);

        RenderContext rcontext = (RenderContext) context.get("rcontext");
        if (rcontext==null) {
            rcontext = new BaseRenderContext();
            rcontext.setParameters(new HashMap());
            rcontext.set("xcontext", context);
        }
        if (rcontext.getRenderEngine()==null) {
            this.radeoxEngine.setXWikiContext(context);

            // Note: Are there any case where we would want to clone the radeox context?
            rcontext.setRenderEngine(this.radeoxEngine);
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

    public String convertMultiLine(String macroname, String params, String data, String allcontent, XWikiVirtualMacro macro, XWikiContext context)
    {
        String result;

        // This is huge hack to ensure that the {code} macro content is not parsed by the Velocity
        // or Groovy Renderers. It relies on the fact that the MacroMapping executes before these
        // Renderers and the code macro must be defined in the macros.txt file, as otherwise this
        // will not work.
        if (macroname.equals("code")) {
            // Escape the Velocity special characters: '$' and '#' so that they are not interpreted.
            result = StringUtils.replace(allcontent, "#", "&#35;");
            result = StringUtils.replace(result, "$", "&#36;");
            // Escape the Groovy special characters '<%' and '%>' so that the Groovy Renderer
            // doesn't execute.
            result = StringUtils.replace(result, "<%", "&#60;%");
            result = StringUtils.replace(result, "%>", "%&#62;");
        } else {
            result = allcontent;
        }

        return result;
    }

    public String convertSingleLine(String macroname, String params, String allcontent, XWikiVirtualMacro macro, XWikiContext context)
    {
        // Do not render anything here as otherwise the Radeox renderer will be executed twice.
        return allcontent;
    }
}
