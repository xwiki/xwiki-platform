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
package com.xpn.xwiki.plugin;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.Vector;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiAttachment;

public class XWikiPluginManager
{
    /** Log helper for logging messages in this class. */
    private static final Logger LOGGER = LoggerFactory.getLogger(XWikiPluginManager.class);

    private Vector<String> plugins = new Vector<String>();

    private Vector<String> pluginClassNames = new Vector<String>();

    private Map<String, XWikiPluginInterface> plugins_classes = new HashMap<String, XWikiPluginInterface>();

    private Map<String, Vector<XWikiPluginInterface>> functionList =
        new HashMap<String, Vector<XWikiPluginInterface>>();

    public XWikiPluginManager()
    {
    }

    public XWikiPluginManager(String classList, XWikiContext context)
    {
        String[] classNames = StringUtils.split(classList, " ,");
        addPlugins(classNames, context);
    }

    public XWikiPluginManager(String[] classNames, XWikiContext context)
    {
        addPlugins(classNames, context);
    }

    @SuppressWarnings("unchecked")
    public void addPlugin(String name, String className, XWikiContext context)
    {
        if (this.pluginClassNames.contains(className)) {
            if (LOGGER.isInfoEnabled()) {
                LOGGER.info(String.format("Skipping already registered plugin [%s]", name));
            }
            return;
        }
        try {
            Class<?>[] classes = new Class<?>[3];
            classes[0] = String.class;
            classes[1] = String.class;
            classes[2] = context.getClass();
            Object[] args = new Object[3];
            args[0] = name;
            args[1] = className;
            args[2] = context;
            Class<XWikiPluginInterface> pluginClass =
                (Class<XWikiPluginInterface>) Class.forName(className, true, Thread.currentThread()
                    .getContextClassLoader());
            XWikiPluginInterface plugin = pluginClass.getConstructor(classes).newInstance(args);
            if (plugin != null) {
                this.plugins.add(plugin.getName());
                this.plugins_classes.put(plugin.getName(), plugin);
                this.pluginClassNames.add(className);
                initPlugin(plugin, pluginClass, context);
            }
        } catch (Exception ex) {
            // Log an error but do not fail
            LOGGER.error("Cannot initialize plugin [" + className + "]. This plugin will not be available.", ex);
        }
    }

    public void removePlugin(String className)
    {
        this.plugins.remove(className);
        Object plugin = this.plugins_classes.get(className);
        this.plugins_classes.remove(className);

        for (String name : this.functionList.keySet()) {
            Vector<XWikiPluginInterface> pluginList = this.functionList.get(name);
            pluginList.remove(plugin);
        }
    }

    public void addPlugins(String[] classNames, XWikiContext context)
    {
        if (context.getURLFactory() == null) {
            context
                .setURLFactory(context.getWiki().getURLFactoryService().createURLFactory(context.getMode(), context));
        }
        initInterface();
        for (String className : classNames) {
            addPlugin(className, className, context);
        }
    }

    public XWikiPluginInterface getPlugin(String className)
    {
        return this.plugins_classes.get(className);
    }

    public Vector<String> getPlugins()
    {
        return this.plugins;
    }

    public void setPlugins(Vector<String> plugins)
    {
        this.plugins = plugins;
    }

    public void initInterface()
    {
        for (Method method : XWikiPluginInterface.class.getMethods()) {
            String name = method.getName();
            this.functionList.put(name, new Vector<XWikiPluginInterface>());
        }
    }

    public void initPlugin(Object plugin, Class<XWikiPluginInterface> pluginClass, XWikiContext context)
        throws XWikiException
    {
        for (Method method : pluginClass.getDeclaredMethods()) {
            String name = method.getName();
            if (this.functionList.containsKey(name)) {
                this.functionList.get(name).add((XWikiPluginInterface) plugin);
            }
        }
        ((XWikiPluginInterface) plugin).init(context);
    }

    public Vector<XWikiPluginInterface> getPlugins(String functionName)
    {
        return this.functionList.get(functionName);
    }

    public void virtualInit(XWikiContext context)
    {
        for (XWikiPluginInterface plugin : getPlugins("virtualInit")) {
            try {
                plugin.virtualInit(context);
            } catch (Exception e) {
            }
        }
    }

    public void flushCache(XWikiContext context)
    {
        for (XWikiPluginInterface plugin : getPlugins("flushCache")) {
            try {
                plugin.flushCache(context);
            } catch (Exception e) {
                LOGGER.error("Failed to flush cache in plugin [" + plugin.getClass() + "]", e);
            }
        }
    }

    public String commonTagsHandler(String text, XWikiContext context)
    {
        for (XWikiPluginInterface plugin : getPlugins("commonTagsHandler")) {
            try {
                text = plugin.commonTagsHandler(text, context);
            } catch (Exception e) {
            }
        }
        return text;
    }

    public String startRenderingHandler(String text, XWikiContext context)
    {
        for (XWikiPluginInterface plugin : getPlugins("startRenderingHandler")) {
            try {
                text = plugin.startRenderingHandler(text, context);
            } catch (Exception e) {
            }
        }
        return text;
    }

    public String outsidePREHandler(String text, XWikiContext context)
    {
        for (XWikiPluginInterface plugin : getPlugins("outsidePREHandler")) {
            try {
                text = plugin.outsidePREHandler(text, context);
            } catch (Exception e) {
            }
        }
        return text;
    }

    public String insidePREHandler(String text, XWikiContext context)
    {
        for (XWikiPluginInterface plugin : getPlugins("insidePREHandler")) {
            try {
                text = plugin.insidePREHandler(text, context);
            } catch (Exception e) {
            }
        }
        return text;
    }

    public String endRenderingHandler(String text, XWikiContext context)
    {
        for (XWikiPluginInterface plugin : getPlugins("endRenderingHandler")) {
            try {
                text = plugin.endRenderingHandler(text, context);
            } catch (Exception e) {
            }
        }
        return text;
    }

    public void beginRendering(XWikiContext context)
    {
        for (XWikiPluginInterface plugin : getPlugins("beginRendering")) {
            try {
                plugin.beginRendering(context);
            } catch (Exception e) {
            }
        }
    }

    public void endRendering(XWikiContext context)
    {
        for (XWikiPluginInterface plugin : getPlugins("endRendering")) {
            try {
                plugin.endRendering(context);
            } catch (Exception e) {
            }
        }
    }

    public void beginParsing(XWikiContext context)
    {
        for (XWikiPluginInterface plugin : getPlugins("beginParsing")) {
            try {
                plugin.beginParsing(context);
            } catch (Exception e) {
            }
        }
    }

    public String endParsing(String content, XWikiContext context)
    {
        for (XWikiPluginInterface plugin : getPlugins("endParsing")) {
            try {
                content = plugin.endParsing(content, context);
            } catch (Exception e) {
            }
        }
        return content;
    }

    public XWikiAttachment downloadAttachment(XWikiAttachment attachment, XWikiContext context)
    {
        XWikiAttachment attach = attachment;
        for (XWikiPluginInterface plugin : getPlugins("downloadAttachment")) {
            try {
                attach = plugin.downloadAttachment(attach, context);
            } catch (Exception ex) {
                LOGGER.warn("downloadAttachment failed for plugin [" + plugin.getName() + "]: " + ex.getMessage());
            }
        }
        return attach;
    }
}
