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

import java.util.HashMap;
import java.util.Iterator;
import java.util.Locale;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.radeox.api.engine.context.InitialRenderContext;
import org.radeox.api.engine.context.RenderContext;
import org.radeox.engine.context.BaseInitialRenderContext;
import org.radeox.engine.context.BaseRenderContext;
import org.radeox.filter.Filter;
import org.radeox.filter.FilterPipe;
import org.radeox.util.Service;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.render.filter.XWikiFilter;
import com.xpn.xwiki.util.Util;
import com.xpn.xwiki.web.Utils;

public class XWikiRadeoxRenderer implements XWikiRenderer
{
    /** Logging helper object. */
    private static final Log LOG = LogFactory.getLog(XWikiRadeoxRenderer.class);

    private boolean removePre = true;

    private InitialRenderContext initialRenderContext;

    private FilterPipe filterPipe;

    public XWikiRadeoxRenderer()
    {
        initRadeoxEngine();
    }

    public XWikiRadeoxRenderer(boolean removePre)
    {
        this();
        setRemovePre(removePre);
    }

    private void initRadeoxEngine()
    {
        // This is needed so that our local config is used
        InitialRenderContext ircontext = new BaseInitialRenderContext();
        Locale locale = new Locale("xwiki", "xwiki");
        ircontext.set(RenderContext.INPUT_LOCALE, locale);
        ircontext.set(RenderContext.OUTPUT_LOCALE, locale);
        ircontext.set(RenderContext.LANGUAGE_LOCALE, locale);
        ircontext.setParameters(new HashMap());

        this.initialRenderContext = ircontext;
        this.filterPipe = initFilterPipe(ircontext);
    }

    /**
     * We override this method from {@link org.radeox.engine.BaseRenderEngine} in order to provide our own
     * initialization of Filters. In this manner we can load our filter definition from the
     * META-INF/services/com.xpn.xwiki.render.filter.XWikiFilter file.
     */
    private FilterPipe initFilterPipe(InitialRenderContext initialRenderContext)
    {
        FilterPipe fp = new FilterPipe(initialRenderContext);

        Iterator iterator = Service.providers(XWikiFilter.class);
        while (iterator.hasNext()) {
            try {
                Filter filter = (Filter) iterator.next();
                fp.addFilter(filter);
                LOG.debug("Radeox filter [" + filter.getClass().getName() + "] loaded");
            } catch (Exception e) {
                LOG.error("Failed to load Radeox filter", e);
            }
        }

        fp.init();

        return fp;
    }

    public String render(String content, XWikiDocument contentdoc, XWikiDocument contextdoc, XWikiContext context)
    {
        Util util = context.getUtil();
        // Remove the content that is inside "{pre}"
        PreTagSubstitution preTagSubst = new PreTagSubstitution(util, isRemovePre());
        content = preTagSubst.substitute(content);

        RenderContext rcontext = (RenderContext) context.get("rcontext");
        if (rcontext == null) {
            rcontext = new BaseRenderContext();
            // This is needed as otherwise some macros throw exceptions
            rcontext.setParameters(new HashMap());
            rcontext.set("xcontext", context);
        }
        if (rcontext.getRenderEngine() == null) {
            XWikiRadeoxRenderEngine radeoxengine =
                new XWikiRadeoxRenderEngine(this.initialRenderContext, this.filterPipe, context);
            rcontext.setRenderEngine(radeoxengine);
        }
        // If global placeholders are not enabled, then use local placeholders.
        boolean useLocalPlaceholders = !Utils.arePlaceholdersEnabled(context);
        if (useLocalPlaceholders) {
            Utils.enablePlaceholders(context);
        }
        String result = rcontext.getRenderEngine().render(content, rcontext);
        if (useLocalPlaceholders) {
            result = Utils.replacePlaceholders(result, context);
            Utils.disablePlaceholders(context);
        }
        return preTagSubst.insertNonWikiText(result);
    }

    public void flushCache()
    {
        // No need to flush anything yet
    }

    public boolean isRemovePre()
    {
        return this.removePre;
    }

    public void setRemovePre(boolean removePre)
    {
        this.removePre = removePre;
    }

    public String convertMultiLine(String macroname, String params, String data, String allcontent,
        XWikiVirtualMacro macro, XWikiContext context)
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

    public String convertSingleLine(String macroname, String params, String allcontent, XWikiVirtualMacro macro,
        XWikiContext context)
    {
        // Do not render anything here as otherwise the Radeox renderer will be executed twice.
        return allcontent;
    }
}
