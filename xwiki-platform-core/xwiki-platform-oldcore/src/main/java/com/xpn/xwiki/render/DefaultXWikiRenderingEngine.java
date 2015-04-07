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
 */
package com.xpn.xwiki.render;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xwiki.cache.Cache;
import org.xwiki.cache.CacheException;
import org.xwiki.cache.CacheManager;
import org.xwiki.cache.config.CacheConfiguration;
import org.xwiki.cache.eviction.LRUEvictionConfiguration;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.component.phase.Initializable;
import org.xwiki.component.phase.InitializationException;
import org.xwiki.configuration.ConfigurationSource;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.monitor.api.MonitorPlugin;
import com.xpn.xwiki.util.Util;
import com.xpn.xwiki.web.Utils;
import com.xpn.xwiki.web.XWikiRequest;

@Component
@Singleton
public class DefaultXWikiRenderingEngine implements XWikiRenderingEngine, Initializable
{
    private static final Logger LOGGER = LoggerFactory.getLogger(XWikiRenderingEngine.class);

    /** The default order in which the rendering engines will be run on the input. */
    private static final String[] DEFAULT_RENDERING_ORDER = new String[] { "macromapping", "groovy", "velocity",
        "plugin", "wiki", "wikiwiki" };

    @Inject
    @Named("xwikicfg")
    private ConfigurationSource configuration;

    @Inject
    private CacheManager cacheManager;

    @Inject
    @Named("context")
    private ComponentManager componentManager;

    @Inject
    @Named("wikiwiki")
    private XWikiRenderer xwikiRenderer;

    private List<XWikiRenderer> renderers;

    private HashMap<String, XWikiRenderer> renderermap;

    private Cache<XWikiRenderingCache> cache;

    public DefaultXWikiRenderingEngine()
    {

    }

    /**
     * @deprecated since 6.1M2, use default {@link XWikiRenderingEngine} component instead
     */
    @Deprecated
    public DefaultXWikiRenderingEngine(XWiki xwiki, XWikiContext context)
    {
        this.configuration = Utils.getComponent(ConfigurationSource.class, "xwikicfg");
        this.cacheManager = Utils.getComponent(CacheManager.class);
        this.componentManager = Utils.getContextComponentManager();
        this.xwikiRenderer = Utils.getComponent(XWikiRenderer.class, "wikiwiki");

        try {
            initialize();
        } catch (InitializationException e) {
            LOGGER.error("Failed to initialize rendering engine", e);
        }
    }

    @Override
    public void initialize() throws InitializationException
    {
        this.renderers = new ArrayList<XWikiRenderer>();
        this.renderermap = new LinkedHashMap<String, XWikiRenderer>();

        String[] renderingOrder = this.configuration.getProperty("xwiki.render.renderingorder", String[].class);
        if (renderingOrder == null || renderingOrder.length == 0) {
            renderingOrder = DEFAULT_RENDERING_ORDER;
        }

        try {
            for (String hint : renderingOrder) {
                if (this.configuration.getProperty("xwiki.render." + hint, "1").equals("1")) {
                    if (this.componentManager.hasComponent(XWikiRenderer.class, hint)) {
                        XWikiRenderer renderer = this.componentManager.getInstance(XWikiRenderer.class, hint);

                        addRenderer(renderer.getId(), renderer);
                    }
                }
            }

            // If there is no wikiwiki renderer, we must add it because it's the base renderer
            if (!this.renderermap.containsKey("xwiki")) {
                addRenderer("xwiki", this.xwikiRenderer);
            }

            initCache(null);
        } catch (Exception e) {
            throw new InitializationException("Failed to initialize rendering engine", e);
        }
    }

    @Override
    public void virtualInit(XWikiContext context)
    {
        XWikiMacrosMappingRenderer mmrendered = (XWikiMacrosMappingRenderer) getRenderer("mapping");
        if (mmrendered != null) {
            mmrendered.loadPreferences();
        }
    }

    public void initCache(XWikiContext context) throws XWikiException
    {
        int iCapacity = 100;
        try {
            String capacity = this.configuration.getProperty("xwiki.render.cache.capacity");
            if (capacity != null) {
                iCapacity = Integer.parseInt(capacity);
            }
        } catch (Exception e) {
        }

        initCache(iCapacity, context);
    }

    public void initCache(int iCapacity, XWikiContext context) throws XWikiException
    {
        try {
            CacheConfiguration configuration = new CacheConfiguration();
            configuration.setConfigurationId("xwiki.renderingcache");
            LRUEvictionConfiguration lru = new LRUEvictionConfiguration();
            lru.setMaxEntries(iCapacity);
            configuration.put(LRUEvictionConfiguration.CONFIGURATIONID, lru);

            this.cache = this.cacheManager.createNewCache(configuration);
        } catch (CacheException e) {
            throw new XWikiException(XWikiException.MODULE_XWIKI_CACHE, XWikiException.ERROR_CACHE_INITIALIZING,
                "Failed to create cache");
        }
    }

    public Cache<XWikiRenderingCache> getCache()
    {
        return this.cache;
    }

    @Override
    public void addRenderer(String name, XWikiRenderer renderer)
    {
        this.renderers.add(renderer);
        this.renderermap.put(name, renderer);
    }

    @Override
    public XWikiRenderer getRenderer(String name)
    {
        return this.renderermap.get(name);
    }

    @Override
    public List<XWikiRenderer> getRendererList()
    {
        return new ArrayList<XWikiRenderer>(this.renderers);
    }

    @Override
    public List<String> getRendererNames()
    {
        return new LinkedList<String>(this.renderermap.keySet());
    }

    protected XWikiRenderer removeRenderer(String name)
    {
        XWikiRenderer result = this.renderermap.remove(name);
        if (result != null) {
            this.renderers.remove(result);
        }
        return result;
    }

    @Override
    public String renderDocument(XWikiDocument doc, XWikiContext context) throws XWikiException
    {
        return renderText(doc.getTranslatedContent(context), doc, context);
    }

    @Override
    public String renderDocument(XWikiDocument doc, XWikiDocument includingdoc, XWikiContext context)
        throws XWikiException
    {
        return renderText(doc.getTranslatedContent(context), includingdoc, context);
    }

    @Override
    public String renderText(String text, XWikiDocument includingdoc, XWikiContext context)
    {
        return renderText(text, includingdoc, includingdoc, context);
    }

    @Override
    public String interpretText(String text, XWikiDocument includingdoc, XWikiContext context)
    {
        return renderText(text, true, includingdoc, includingdoc, context);
    }

    @SuppressWarnings("unchecked")
    public void addToCached(String key, XWikiContext context)
    {
        List<String> cached = (ArrayList<String>) context.get("render_cached");
        if (cached == null) {
            cached = new ArrayList<String>();
            context.put("render_cached", cached);
        }
        cached.add(key);
    }

    @SuppressWarnings("unchecked")
    public void addToRefreshed(String key, XWikiContext context)
    {
        List<String> cached = (ArrayList<String>) context.get("render_refreshed");
        if (cached == null) {
            cached = new ArrayList<String>();
            context.put("render_refreshed", cached);
        }
        cached.add(key);
    }

    @Override
    public String renderText(String text, XWikiDocument contentdoc, XWikiDocument includingdoc, XWikiContext context)
    {
        return renderText(text, false, contentdoc, includingdoc, context);
    }

    private String renderText(String text, boolean onlyInterpret, XWikiDocument contentdoc, XWikiDocument includingdoc,
        XWikiContext context)
    {
        String key = getKey(text, contentdoc, includingdoc, context);
        int currentCacheDuration = context.getCacheDuration();

        try {
            if (this.cache == null) {
                initCache(context);
            }
        } catch (XWikiException e) {
        }

        try {
            XWikiRenderingCache cacheObject = (this.cache != null) ? this.cache.get(key) : null;

            if (cacheObject != null) {
                XWikiRequest request = context.getRequest();
                boolean refresh =
                    (request != null) && ("1".equals(request.get("refresh"))) || "inline".equals(context.getAction())
                        || "admin".equals(context.getAction());
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
            context.setCacheDuration(this.configuration.getProperty("xwiki.rendering.defaultCacheDuration", 0));
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
                for (int i = 0; i < this.renderers.size(); i++) {
                    XWikiRenderer renderer = (this.renderers.get(i));
                    String rendererName = renderer.getClass().getName();
                    if (shouldRender(contentdoc, rendererName, context)) {
                        // Check if only XWikiInterpreter should be executed
                        if (onlyInterpret) {
                            if (XWikiInterpreter.class.isAssignableFrom(renderer.getClass())) {
                                XWikiInterpreter interpreter = (XWikiInterpreter) renderer;
                                content = interpreter.interpret(content, includingdoc, context);
                            }
                        } else {
                            content = renderer.render(content, contentdoc, includingdoc, context);
                        }
                    } else {
                        if (LOGGER.isDebugEnabled()) {
                            LOGGER.debug("skip renderer: " + rendererName + " for the document "
                                + contentdoc.getFullName());
                        }
                    }
                }
                content = Utils.replacePlaceholders(content, context);
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
                    XWikiRenderingCache cacheObject = new XWikiRenderingCache(key, content, cacheDuration, new Date());
                    this.cache.set(key, cacheObject);
                }
            } catch (Exception e) {
                LOGGER.error("cache exception", e);
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

    private boolean shouldRender(XWikiDocument doc, String rendererName, XWikiContext context)
    {
        try {
            if (rendererName.indexOf('.') >= 0) {
                rendererName = rendererName.substring(rendererName.lastIndexOf(".") + 1);
            }
            String render = context.getWiki().getSpacePreference("render" + rendererName, context);
            if (render != null && render.length() > 0) {
                return render.equals("1");
            }

            render = context.getWiki().getXWikiPreference("render" + rendererName, context);
            if (render != null && render.length() > 0) {
                return render.equals("1");
            }
            return true;
        } catch (Exception e) {
            LOGGER.error("Error in the function shouldRender", e);
            return true;
        }
    }

    private String getKey(String text, XWikiDocument contentdoc, XWikiDocument includingdoc, XWikiContext context)
    {
        String qs = ((context == null || context.getRequest() == null) ? "" : context.getRequest().getQueryString());
        if (qs != null) {
            qs = qs.replaceAll("refresh=1&?", "");
            qs = qs.replaceAll("&?refresh=1", "");
        }
        String db = ((context == null) ? "xwiki" : context.getWikiId());
        String cdoc =
            ((contentdoc == null) ? "" : contentdoc.getDatabase() + ":" + contentdoc.getFullName() + ":"
                + contentdoc.getRealLanguage() + ":" + contentdoc.getVersion());
        String idoc =
            ((includingdoc == null) ? "" : includingdoc.getDatabase() + ":" + includingdoc.getFullName() + ":"
                + includingdoc.getRealLanguage() + ":" + includingdoc.getVersion());
        String action = ((context == null) ? "view" : context.getAction());
        String lang = ((context == null) ? "" : context.getLanguage());
        lang += ((contentdoc == null) ? "" : ":" + contentdoc.getRealLanguage());
        return db + "-" + cdoc + "-" + idoc + "-" + qs + "-" + action + "-" + lang + "-" + text.hashCode();
    }

    @Override
    public void flushCache()
    {
        for (int i = 0; i < this.renderers.size(); i++) {
            (this.renderers.get(i)).flushCache();
        }
        if (this.cache != null) {
            this.cache.dispose();
            this.cache = null;
        }
    }

    @Override
    public String convertMultiLine(String macroname, String params, String data, String allcontent,
        XWikiVirtualMacro macro, XWikiContext context)
    {
        String language = macro.getLanguage();
        XWikiRenderer renderer = this.renderermap.get(language);
        if (renderer == null) {
            return allcontent;
        } else {
            return renderer.convertMultiLine(macroname, params, data, allcontent, macro, context);
        }
    }

    @Override
    public String convertSingleLine(String macroname, String params, String allcontent, XWikiVirtualMacro macro,
        XWikiContext context)
    {
        String language = macro.getLanguage();
        XWikiRenderer renderer = this.renderermap.get(language);
        if (renderer == null) {
            return allcontent;
        } else {
            return renderer.convertSingleLine(macroname, params, allcontent, macro, context);
        }
    }
}
