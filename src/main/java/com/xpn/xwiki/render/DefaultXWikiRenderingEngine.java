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

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.cache.api.XWikiCache;
import com.xpn.xwiki.cache.api.XWikiCacheNeedsRefreshException;
import com.xpn.xwiki.cache.api.XWikiCacheService;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.monitor.api.MonitorPlugin;
import com.xpn.xwiki.render.groovy.XWikiGroovyRenderer;
import com.xpn.xwiki.util.Util;
import com.xpn.xwiki.web.XWikiRequest;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.*;

public class DefaultXWikiRenderingEngine implements XWikiRenderingEngine
{
    private static final Log log = LogFactory.getLog(XWikiRenderingEngine.class);

    private List renderers = new ArrayList();

    private HashMap renderermap = new LinkedHashMap();

    private XWikiCache cache;

    public DefaultXWikiRenderingEngine(XWiki xwiki, XWikiContext context) throws XWikiException
    {

        if (xwiki.Param("xwiki.render.macromapping", "1").equals("1")) {
            addRenderer("mapping", new XWikiMacrosMappingRenderer(xwiki, context));
        }
        // addRenderer(new XWikiJSPRenderer());
        if (xwiki.Param("xwiki.render.velocity", "1").equals("1")) {
            addRenderer("velocity", new XWikiVelocityRenderer());
        }
        if (xwiki.Param("xwiki.render.groovy", "1").equals("1")) {
            addRenderer("groovy", new XWikiGroovyRenderer());
        }
        if (xwiki.Param("xwiki.render.plugin", "1").equals("1")) {
            addRenderer("plugin", new XWikiPluginRenderer());
        }
        if (xwiki.Param("xwiki.render.wiki", "1").equals("1")) {
            addRenderer("wiki", new XWikiRadeoxRenderer(false));
        }
        if (xwiki.Param("xwiki.render.wikiwiki", "0").equals("1")) {
            addRenderer("xwiki", new XWikiWikiBaseRenderer(true, true));
        } else {
            addRenderer("xwiki", new XWikiWikiBaseRenderer(false, true));
        }

        initCache(context);
    }

    public void initCache(XWikiContext context) throws XWikiException
    {
        int iCapacity = 100;
        try {
            String capacity = context.getWiki().Param("xwiki.render.cache.capacity");
            if (capacity != null) {
                iCapacity = Integer.parseInt(capacity);
            }
        } catch (Exception e) {
        }
        initCache(iCapacity, context);
    }

    public void initCache(int iCapacity, XWikiContext context) throws XWikiException
    {
        XWikiCacheService cacheService = context.getWiki().getCacheService();
        cache = cacheService.newCache("xwiki.rendering.cache", iCapacity);
    }

    public XWikiCache getCache()
    {
        return cache;
    }

    public void addRenderer(String name, XWikiRenderer renderer)
    {
        renderers.add(renderer);
        renderermap.put(name, renderer);
    }

    public XWikiRenderer getRenderer(String name)
    {
        return (XWikiRenderer) renderermap.get(name);
    }

    public List getRendererList()
    {
        return (List) ((ArrayList) renderers).clone();
    }

    public List getRendererNames()
    {
        return new LinkedList(renderermap.keySet());
    }

    protected XWikiRenderer removeRenderer(String name)
    {
        XWikiRenderer result = (XWikiRenderer) renderermap.remove(name);
        if (result != null) {
            renderers.remove(result);
        }
        return result;
    }

    public String renderDocument(XWikiDocument doc, XWikiContext context) throws XWikiException
    {
        return renderText(doc.getTranslatedContent(context), doc, context);
    }

    public String renderDocument(XWikiDocument doc, XWikiDocument includingdoc,
        XWikiContext context) throws XWikiException
    {
        return renderText(doc.getTranslatedContent(context), includingdoc, context);
    }

    public String renderText(String text, XWikiDocument includingdoc, XWikiContext context)
    {
        return renderText(text, includingdoc, includingdoc, context);
    }

    public String interpretText(String text, XWikiDocument includingdoc, XWikiContext context)
    {
        return renderText(text, true, includingdoc, includingdoc, context);
    }

    public void addToCached(String key, XWikiContext context)
    {
        List cached = (ArrayList) context.get("render_cached");
        if (cached == null) {
            cached = new ArrayList();
            context.put("render_cached", cached);
        }
        cached.add(key);
    }

    public void addToRefreshed(String key, XWikiContext context)
    {
        List cached = (ArrayList) context.get("render_refreshed");
        if (cached == null) {
            cached = new ArrayList();
            context.put("render_refreshed", cached);
        }
        cached.add(key);
    }

    public String renderText(String text, XWikiDocument contentdoc, XWikiDocument includingdoc,
        XWikiContext context)
    {
        return renderText(text, false, contentdoc, includingdoc, context);
    }

    private String renderText(String text, boolean onlyInterpret, XWikiDocument contentdoc,
        XWikiDocument includingdoc, XWikiContext context)
    {
        String key = getKey(text, contentdoc, includingdoc, context);
        int currentCacheDuration = context.getCacheDuration();

        try {
            if (cache == null) {
                initCache(context);
            }
        } catch (XWikiException e) {
        }

        synchronized (key) {
            try {
                XWikiRenderingCache cacheObject = null;
                try {
                    cacheObject =
                        (cache != null) ? (XWikiRenderingCache) cache.getFromCache(key) : null;
                } catch (XWikiCacheNeedsRefreshException e2) {
                    cache.cancelUpdate(key);
                }
                if (cacheObject != null) {
                    XWikiRequest request = context.getRequest();
                    boolean refresh =
                        (request != null) && ("1".equals(request.get("refresh")))
                            || "inline".equals(context.getAction());
                    if ((cacheObject.isValid() && (!refresh))) {
                        addToCached(key, context);
                        return cacheObject.getContent();
                    } else {
                        addToRefreshed(key, context);
                    }
                }
            } catch (Exception e) {
            }

            MonitorPlugin monitor = Util.getMonitorPlugin(context);
            try {
                // We need to make sure we don't use the cache duretion currently in the system
                context.setCacheDuration(0);
                // Start monitoring timer
                if (monitor != null) {
                    monitor.startTimer("rendering");
                }

                String content = text;

                // Which is the current idoc and sdoc
                XWikiDocument idoc = (XWikiDocument) context.get("idoc");
                XWikiDocument sdoc = (XWikiDocument) context.get("sdoc");
                // We put the including and security doc in the context
                // It will be needed to verify programming rights
                context.put("idoc", includingdoc);
                context.put("sdoc", contentdoc);

                // Let's call the beginRendering loop
                context.getWiki().getPluginManager().beginRendering(context);

                try {
                    for (int i = 0; i < renderers.size(); i++) {
                        XWikiRenderer renderer = ((XWikiRenderer) renderers.get(i));
                        String rendererName = renderer.getClass().getName();
                        if (shouldRender(contentdoc, rendererName, context)) {
                            // Check if only XWikiInterpreter should be executed
                            if (onlyInterpret) {
                                if (XWikiInterpreter.class.isAssignableFrom(renderer.getClass())) {
                                    XWikiInterpreter interpreter = (XWikiInterpreter) renderer;
                                    content =
                                        interpreter.interpret(content, includingdoc, context);
                                }
                            } else {
                                content =
                                    renderer.render(content, contentdoc, includingdoc, context);
                            }
                        } else {
                            if (log.isDebugEnabled()) {
                                log.debug("skip renderer: " + rendererName + " for the document "
                                    + contentdoc.getFullName());
                            }
                        }
                    }
                } finally {
                    // Remove including doc or set the previous one
                    if (idoc == null) {
                        context.remove("idoc");
                    } else {
                        context.put("idoc", idoc);
                    }

                    // Remove security doc or set the previous one
                    if (sdoc == null) {
                        context.remove("sdoc");
                    } else {
                        context.put("sdoc", sdoc);
                    }

                    // Let's call the endRendering loop
                    context.getWiki().getPluginManager().endRendering(context);
                }

                try {
                    int cacheDuration = context.getCacheDuration();
                    if (cacheDuration > 0) {
                        XWikiRenderingCache cacheObject =
                            new XWikiRenderingCache(key, content, cacheDuration, new Date());
                        cache.putInCache(key, cacheObject);
                    }
                } catch (Exception e) {
                    log.error("cache exception", e);
                }
                return content;
            } finally {
                // We need to make sure we reset the cache Duration
                context.setCacheDuration(currentCacheDuration);

                if (monitor != null) {
                    monitor.endTimer("rendering");
                }
            }
        }
    }

    private boolean shouldRender(XWikiDocument doc, String rendererName, XWikiContext context)
    {
        try {
            if (rendererName.indexOf('.') >= 0) {
                rendererName = rendererName.substring(rendererName.lastIndexOf(".") + 1);
            }
            String render = context.getWiki().getWebPreference("render" + rendererName, context);
            if (render != null && render.length() > 0) {
                return render.equals("1");
            }

            render = context.getWiki().getXWikiPreference("render" + rendererName, context);
            if (render != null && render.length() > 0) {
                return render.equals("1");
            }
            return true;
        } catch (Exception e) {
            log.error("Error in the function shouldRender", e);
            return true;
        }
    }

    private String getKey(String text, XWikiDocument contentdoc, XWikiDocument includingdoc,
        XWikiContext context)
    {
        String qs =
            ((context == null || context.getRequest() == null) ? "" : context.getRequest()
                .getQueryString());
        if (qs != null) {
            qs = qs.replaceAll("refresh=1&?", "");
            qs = qs.replaceAll("&?refresh=1", "");
        }
        String db = ((context == null) ? "xwiki" : context.getDatabase());
        String cdoc =
            ((contentdoc == null) ? "" : contentdoc.getDatabase() + ":"
                + contentdoc.getFullName() + ":" + contentdoc.getRealLanguage());
        String idoc =
            ((includingdoc == null) ? "" : includingdoc.getDatabase() + ":"
                + includingdoc.getFullName() + ":" + includingdoc.getRealLanguage());
        String action = ((context == null) ? "view" : context.getAction());
        String lang = ((context == null) ? "" : context.getLanguage());
        lang += ((contentdoc == null) ? "" : ":" + contentdoc.getRealLanguage());
        return db + "-" + cdoc + "-" + idoc + "-" + qs + "-" + action + "-" + lang + "-"
            + text.hashCode();
    }

    public void flushCache()
    {
        for (int i = 0; i < renderers.size(); i++) {
            ((XWikiRenderer) renderers.get(i)).flushCache();
        }
        if (cache != null) {
            cache.flushAll();
            cache = null;
        }
    }

    public String convertMultiLine(String macroname, String params, String data,
        String allcontent, XWikiVirtualMacro macro, XWikiContext context)
    {
        String language = macro.getLanguage();
        XWikiRenderer renderer = (XWikiRenderer) renderermap.get(language);
        if (renderer == null) {
            return allcontent;
        } else {
            return renderer.convertMultiLine(macroname, params, data, allcontent, macro, context);
        }
    }

    public String convertSingleLine(String macroname, String params, String allcontent,
        XWikiVirtualMacro macro, XWikiContext context)
    {
        String language = macro.getLanguage();
        XWikiRenderer renderer = (XWikiRenderer) renderermap.get(language);
        if (renderer == null) {
            return allcontent;
        } else {
            return renderer.convertSingleLine(macroname, params, allcontent, macro, context);
        }
    }

}
