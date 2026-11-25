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
import java.util.Map;
import java.util.Vector;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.stability.Unstable;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiAttachment;

/**
 * Hold the plugins listed in the configuration ({@code xwiki.cfg} and the wiki preferences) and the ones registered as
 * {@link XWikiPluginInterface} components in the root component manager.
 * <p>
 * The lists of plugins can change at any time (when a plugin component is registered or unregistered) so they are
 * never modified in place: each modification replaces them with a modified copy, which allows to safely iterate over
 * them without any lock.
 *
 * @version $Id$
 */
public class XWikiPluginManager
{
    /** Log helper for logging messages in this class. */
    private static final Logger LOGGER = LoggerFactory.getLogger(XWikiPluginManager.class);

    private final AtomicReference<Vector<String>> plugins = new AtomicReference<>(new Vector<>());

    private final AtomicReference<Vector<String>> pluginClassNames = new AtomicReference<>(new Vector<>());

    private final Map<String, XWikiPluginInterface> pluginsClasses = new ConcurrentHashMap<>();

    private final Map<String, Vector<XWikiPluginInterface>> functionList = new ConcurrentHashMap<>();

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
        if (this.pluginClassNames.get().contains(className)) {
            LOGGER.info("Skipping already registered plugin [{}]", name);
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
                registerPlugin(plugin, className, pluginClass, context);
            }
        } catch (Exception ex) {
            // Log an error but do not fail
            LOGGER.error("Cannot initialize plugin [{}]. This plugin will not be available.", className, ex);
        }
    }

    /**
     * Add a plugin registered as a {@link XWikiPluginInterface} component.
     * <p>
     * The component hint must be the plugin name (which {@link AbstractXWikiPlugin} guarantees), a plugin with a name
     * different from the hint is rejected. The plugin is also skipped if a plugin with the same name or class is
     * already registered (for example because it's also listed in {@code xwiki.cfg}).
     *
     * @param hint the component hint of the plugin
     * @param plugin the plugin component instance
     * @param context the current request context
     * @return {@code true} if the plugin was added
     * @since 18.10.0
     */
    @Unstable
    @SuppressWarnings("unchecked")
    public synchronized boolean addComponentPlugin(String hint, XWikiPluginInterface plugin, XWikiContext context)
    {
        if (!hint.equals(plugin.getName())) {
            LOGGER.error("Plugin component [{}] is not available because its name [{}] is different from its hint.",
                hint, plugin.getName());
            return false;
        }

        String className = plugin.getClass().getName();
        if (this.pluginsClasses.containsKey(hint) || this.pluginClassNames.get().contains(className)) {
            LOGGER.info("Skipping already registered plugin [{}]", hint);
            return false;
        }

        try {
            registerPlugin(plugin, className, (Class<XWikiPluginInterface>) plugin.getClass(), context);

            return true;
        } catch (Exception e) {
            // Don't keep a plugin which failed to initialize
            removePlugin(hint);

            // Log an error but do not fail
            LOGGER.error("Cannot initialize plugin component [{}]. This plugin will not be available.", hint, e);

            return false;
        }
    }

    /**
     * Add all the plugins registered as {@link XWikiPluginInterface} components in the passed component manager.
     *
     * @param componentManager the component manager where to search for plugin components
     * @param context the current request context
     * @since 18.10.0
     */
    @Unstable
    public void addComponentPlugins(ComponentManager componentManager, XWikiContext context)
    {
        for (var descriptor : componentManager.getComponentDescriptorList(XWikiPluginInterface.class)) {
            try {
                addComponentPlugin(descriptor.getRoleHint(),
                    componentManager.getInstance(XWikiPluginInterface.class, descriptor.getRoleHint()), context);
            } catch (ComponentLookupException e) {
                LOGGER.error("Failed to lookup plugin component [{}]. This plugin will not be available.",
                    descriptor.getRoleHint(), e);
            }
        }
    }

    private synchronized void registerPlugin(XWikiPluginInterface plugin, String className,
        Class<XWikiPluginInterface> pluginClass, XWikiContext context) throws XWikiException
    {
        Vector<String> newPlugins = new Vector<>(this.plugins.get());
        newPlugins.add(plugin.getName());
        this.plugins.set(newPlugins);
        this.pluginsClasses.put(plugin.getName(), plugin);
        Vector<String> newPluginClassNames = new Vector<>(this.pluginClassNames.get());
        newPluginClassNames.add(className);
        this.pluginClassNames.set(newPluginClassNames);
        initPlugin(plugin, pluginClass, context);
    }

    /**
     * Remove a plugin.
     *
     * @param name the name of the plugin to remove, as returned by {@link XWikiPluginInterface#getName()}
     */
    public synchronized void removePlugin(String name)
    {
        XWikiPluginInterface plugin = this.pluginsClasses.remove(name);

        Vector<String> newPlugins = new Vector<>(this.plugins.get());
        newPlugins.remove(name);
        this.plugins.set(newPlugins);

        if (plugin != null) {
            Vector<String> newPluginClassNames = new Vector<>(this.pluginClassNames.get());
            newPluginClassNames.remove(plugin.getClass().getName());
            this.pluginClassNames.set(newPluginClassNames);

            this.functionList.replaceAll((function, pluginList) -> {
                Vector<XWikiPluginInterface> newPluginList = new Vector<>(pluginList);
                newPluginList.remove(plugin);
                return newPluginList;
            });
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
        return this.pluginsClasses.get(className);
    }

    public Vector<String> getPlugins()
    {
        return this.plugins.get();
    }

    public void setPlugins(Vector<String> plugins)
    {
        this.plugins.set(plugins);
    }

    public void initInterface()
    {
        for (Method method : XWikiPluginInterface.class.getMethods()) {
            String name = method.getName();
            this.functionList.putIfAbsent(name, new Vector<XWikiPluginInterface>());
        }
    }

    public synchronized void initPlugin(Object plugin, Class<XWikiPluginInterface> pluginClass, XWikiContext context)
        throws XWikiException
    {
        for (Method method : pluginClass.getDeclaredMethods()) {
            String name = method.getName();
            this.functionList.computeIfPresent(name, (function, pluginList) -> {
                Vector<XWikiPluginInterface> newPluginList = new Vector<>(pluginList);
                newPluginList.add((XWikiPluginInterface) plugin);
                return newPluginList;
            });
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
                // TODO: log a warning instead of ignoring this exception.
                // A plugin failing must not prevent the other plugins from being called.
            }
        }
    }

    public void flushCache(XWikiContext context)
    {
        for (XWikiPluginInterface plugin : getPlugins("flushCache")) {
            try {
                plugin.flushCache(context);
            } catch (Exception e) {
                LOGGER.error("Failed to flush cache in plugin [{}]", plugin.getClass(), e);
            }
        }
    }

    public String commonTagsHandler(String text, XWikiContext context)
    {
        for (XWikiPluginInterface plugin : getPlugins("commonTagsHandler")) {
            try {
                text = plugin.commonTagsHandler(text, context);
            } catch (Exception e) {
                // TODO: log a warning instead of ignoring this exception.
                // A plugin failing must not prevent the other plugins from being called.
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
                // TODO: log a warning instead of ignoring this exception.
                // A plugin failing must not prevent the other plugins from being called.
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
                // TODO: log a warning instead of ignoring this exception.
                // A plugin failing must not prevent the other plugins from being called.
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
                // TODO: log a warning instead of ignoring this exception.
                // A plugin failing must not prevent the other plugins from being called.
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
                // TODO: log a warning instead of ignoring this exception.
                // A plugin failing must not prevent the other plugins from being called.
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
                // TODO: log a warning instead of ignoring this exception.
                // A plugin failing must not prevent the other plugins from being called.
            }
        }
    }

    public void endRendering(XWikiContext context)
    {
        for (XWikiPluginInterface plugin : getPlugins("endRendering")) {
            try {
                plugin.endRendering(context);
            } catch (Exception e) {
                // TODO: log a warning instead of ignoring this exception.
                // A plugin failing must not prevent the other plugins from being called.
            }
        }
    }

    public void beginParsing(XWikiContext context)
    {
        for (XWikiPluginInterface plugin : getPlugins("beginParsing")) {
            try {
                plugin.beginParsing(context);
            } catch (Exception e) {
                // TODO: log a warning instead of ignoring this exception.
                // A plugin failing must not prevent the other plugins from being called.
            }
        }
    }

    public String endParsing(String content, XWikiContext context)
    {
        for (XWikiPluginInterface plugin : getPlugins("endParsing")) {
            try {
                content = plugin.endParsing(content, context);
            } catch (Exception e) {
                // TODO: log a warning instead of ignoring this exception.
                // A plugin failing must not prevent the other plugins from being called.
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
                LOGGER.warn("downloadAttachment failed for plugin [{}]: [{}]", plugin.getName(),
                    ExceptionUtils.getRootCauseMessage(ex));
            }
        }
        return attach;
    }
}
