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
package com.xpn.xwiki.render.groovy;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.api.Context;
import com.xpn.xwiki.api.Document;
import com.xpn.xwiki.api.XWiki;
import com.xpn.xwiki.api.Util;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.render.XWikiInterpreter;
import com.xpn.xwiki.render.XWikiRenderer;
import com.xpn.xwiki.render.XWikiVirtualMacro;
import com.xpn.xwiki.web.XWikiRequest;
import groovy.lang.GroovyClassLoader;
import groovy.lang.Writable;
import groovy.text.Template;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.groovy.runtime.InvokerHelper;
import org.xwiki.cache.DisposableCacheValue;
import org.xwiki.cache.Cache;
import org.xwiki.cache.CacheException;
import org.xwiki.cache.config.CacheConfiguration;
import org.xwiki.cache.eviction.LRUEvictionConfiguration;

import java.io.Writer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class XWikiGroovyRenderer implements XWikiRenderer, XWikiInterpreter
{
    private static final Log log = LogFactory.getLog(XWikiGroovyRenderer.class);

    private Cache<Template> cache;

    private Cache<CachedGroovyClass> classCache;

    public void flushCache()
    {
        if (cache != null) {
            cache.removeAll();
        }
        if (classCache != null) {
            classCache.removeAll();
        }

        GroovyTemplateEngine.flushCache();

        cache = null;
        classCache = null;
    }

    public Map prepareContext(XWikiContext context)
    {
        prepareCache(context);

        Map gcontext = (Map) context.get("gcontext");
        if (gcontext == null) {
            gcontext = new HashMap();
            gcontext.put("xwiki", new XWiki(context.getWiki(), context));
            gcontext.put("request", context.getRequest());
            gcontext.put("response", context.getResponse());
            gcontext.put("context", new Context(context));
            gcontext.put("util", new Util(context.getWiki(), context));

            // Put the Groovy Context in the context
            // so that includes can use it..
            context.put("gcontext", gcontext);
            // add XWikiMessageTool to the context
            if (context.get("msg") != null) {
                gcontext.put("msg", context.get("msg"));
            } else {
                context.getWiki().prepareResources(context);
            }
        }
        return gcontext;
    }

    public void initCache(XWikiContext context) throws XWikiException
    {
        int iCapacity = 100;
        try {
            String capacity = context.getWiki().Param("xwiki.render.groovy.cache.capacity");
            if (capacity != null) {
                iCapacity = Integer.parseInt(capacity);
            }
        } catch (Exception e) {
        }

        int iClassCapacity = 100;
        try {
            String capacity = context.getWiki().Param("xwiki.render.groovy.classcache.capacity");
            if (capacity != null) {
                iCapacity = Integer.parseInt(capacity);
            }
        } catch (Exception e) {
        }

        initCache(iCapacity, iClassCapacity, context);
    }

    public void initCache(int iCapacity, int iClassCapacity, XWikiContext context) throws XWikiException
    {
        try {
            CacheConfiguration configuration = new CacheConfiguration();
            configuration.setConfigurationId("xwiki.groovy.content");
            LRUEvictionConfiguration lru = new LRUEvictionConfiguration();
            lru.setMaxEntries(iCapacity);
            configuration.put(LRUEvictionConfiguration.CONFIGURATIONID, lru);

            cache = context.getWiki().getLocalCacheFactory().newCache(configuration);

            configuration = new CacheConfiguration();
            configuration.setConfigurationId("xwiki.groovy.class");
            lru = new LRUEvictionConfiguration();
            lru.setMaxEntries(iClassCapacity);
            configuration.put(LRUEvictionConfiguration.CONFIGURATIONID, lru);

            classCache = context.getWiki().getLocalCacheFactory().newCache(configuration);
        } catch (CacheException e) {
            throw new XWikiException(XWikiException.MODULE_XWIKI_CACHE, XWikiException.ERROR_CACHE_INITIALIZING,
                "Failed to initilize caches", e);
        }
    }

    protected void prepareCache(XWikiContext context)
    {
        try {
            if ((cache == null) || (classCache == null)) {
                initCache(context);
            }
        } catch (Exception e) {
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see XWikiInterpreter#interpret(String,XWikiDocument,XWikiContext)
     */
    public String interpret(String content, XWikiDocument contextdoc, XWikiContext context)
    {
        return render(content, contextdoc, contextdoc, context);
    }

    public String evaluate(String content, String name, Map gcontext)
    {
        GroovyTemplateEngine engine = new GroovyTemplateEngine();
        Template template = null;
        boolean refresh = false;

        try {
            XWikiRequest request = (XWikiRequest) gcontext.get("request");
            refresh = "1".equals(request.get("refresh"));
        } catch (Exception e) {
        }
        try {
            if (!refresh) {
                template = cache.get(content);
            }

            if (template == null) {
                template = engine.createTemplate(content);
                cache.set(content, template);
            }

            Writable writable = template.make(gcontext);
            String result = writable.toString();

            return result;
        } catch (Exception e) {
            e.printStackTrace();
            Object[] args = {name};

            String title;
            String text;

            XWikiException xe =
                new XWikiException(XWikiException.MODULE_XWIKI_RENDERING,
                    XWikiException.ERROR_XWIKI_RENDERING_GROOVY_EXCEPTION, "Error while parsing groovy page {0}", e,
                    args);
            title = xe.getMessage();
            text = com.xpn.xwiki.XWiki.getFormEncoded(xe.getFullMessage());

            return "<a href=\"\" onclick=\"document.getElementById('xwikierror').style.display='block'; return false;\">"
                + title + "</a><div id=\"xwikierror\" style=\"display: none;\"><pre>\n" + text + "</pre></div>";
        }
    }

    public String render(String content, XWikiDocument contentdoc, XWikiDocument contextdoc, XWikiContext context)
    {
        if (content.indexOf("<%") == -1) {
            return content;
        }

        if (!context.getWiki().getRightService().hasProgrammingRights(contextdoc, context)) {
            return content;
        }

        prepareCache(context);

        Map gcontext = null;
        try {
            String name = contextdoc.getFullName();
            gcontext = prepareContext(context);
            Document previousdoc = (Document) gcontext.get("doc");
            Writer previouswriter = (Writer) gcontext.get("out");

            try {
                gcontext.put("doc", contextdoc.newDocument(context));
                return evaluate(content, name, gcontext);
            } finally {
                if (previousdoc != null) {
                    gcontext.put("doc", previousdoc);
                }
                if (previouswriter != null) {
                    gcontext.put("out", previouswriter);
                }
            }
        } finally {
        }
    }

    private void generateFunction(StringBuffer result, String param, String data, XWikiVirtualMacro macro)
    {
        Map namedparams = new HashMap();
        List unnamedparams = new ArrayList();
        if ((param != null) && (!param.trim().equals(""))) {
            String[] params = StringUtils.split(param, "|");
            for (int i = 0; i < params.length; i++) {
                String[] rparam = StringUtils.split(params[i], "=");
                if (rparam.length == 1) {
                    unnamedparams.add(params[i]);
                } else {
                    namedparams.put(rparam[0], rparam[1]);
                }
            }
        }

        result.append("<% ");
        result.append(macro.getFunctionName());
        result.append("(");

        List macroparam = macro.getParams();
        int j = 0;
        for (int i = 0; i < macroparam.size(); i++) {
            String name = (String) macroparam.get(i);
            String value = (String) namedparams.get(name);
            if (value == null) {
                try {
                    value = (String) unnamedparams.get(j);
                    j++;
                } catch (Exception e) {
                    value = "";
                }
            }
            if (i > 0) {
                result.append(",");
            }
            result.append("\"");
            result.append(value.replaceAll("\"", "\\\\\""));
            result.append("\"");
        }

        if (data != null) {
            result.append(",");
            result.append("\"");
            result.append(data.replaceAll("\"", "\\\\\""));
            result.append("\"");
        }
        result.append(") %>");
    }

    private void addGroovyMacros(StringBuffer result, XWikiContext context)
    {
        Object macroAdded = context.get("groovyMacrosAdded");
        if (macroAdded == null) {
            context.put("groovyMacrosAdded", "1");
            String inclDocName = context.getWiki().getXWikiPreference("macros_groovy", context);
            try {
                XWikiDocument doc = context.getWiki().getDocument(inclDocName, context);
                result.append(doc.getContent());
            } catch (XWikiException e) {
                if (log.isErrorEnabled()) {
                    log.error("Impossible to load groovy macros doc " + inclDocName);
                }
            }
        }
    }

    public String convertSingleLine(String macroname, String param, String allcontent, XWikiVirtualMacro macro,
        XWikiContext context)
    {
        StringBuffer result = new StringBuffer();
        addGroovyMacros(result, context);
        generateFunction(result, param, null, macro);
        return result.toString();
    }

    public String convertMultiLine(String macroname, String param, String data, String allcontent,
        XWikiVirtualMacro macro, XWikiContext context)
    {
        StringBuffer result = new StringBuffer();
        addGroovyMacros(result, context);
        generateFunction(result, param, data, macro);
        return result.toString();
    }

    public Object parseGroovyFromString(String script, XWikiContext context) throws XWikiException
    {
        prepareCache(context);
        ClassLoader parentClassLoader = (ClassLoader) context.get("parentclassloader");
        try {
            CachedGroovyClass cgc = classCache.get(script);
            Class< ? > gc;

            if (cgc == null) {
                GroovyClassLoader gcl =
                    (parentClassLoader == null) ? new GroovyClassLoader() : new GroovyClassLoader(parentClassLoader);
                gc = gcl.parseClass(script);
                cgc = new CachedGroovyClass(gc);
                classCache.set(script, cgc);
            } else {
                gc = cgc.getGroovyClass();
            }

            return gc.newInstance();
        } catch (Exception e) {
            throw new XWikiException(XWikiException.MODULE_XWIKI_GROOVY,
                XWikiException.ERROR_XWIKI_GROOVY_COMPILE_FAILED, "Failed compiling groovy script", e);
        }
    }

    public class CachedGroovyClass implements DisposableCacheValue
    {
        protected Class cl;

        public CachedGroovyClass(Class cl)
        {
            this.cl = cl;
        }

        public Class getGroovyClass()
        {
            return cl;
        }

        public void dispose() throws Exception
        {
            if (cl != null) {
                InvokerHelper.removeClass(cl);
                GroovyTemplateEngine.removeClass(cl);
            }
        }
    }
}
