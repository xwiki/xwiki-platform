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

import java.util.List;
import java.util.Vector;

import jakarta.inject.Named;
import jakarta.inject.Singleton;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.annotation.ComponentAnnotationLoader;
import org.xwiki.component.descriptor.ComponentDescriptor;
import org.xwiki.component.embed.EmbeddableComponentManager;
import org.xwiki.test.LogLevel;
import org.xwiki.test.junit5.LogCaptureExtension;

import com.xpn.xwiki.XWikiContext;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for {@link XWikiPluginManager}.
 *
 * @version $Id$
 */
class XWikiPluginManagerTest
{
    /**
     * A plugin that declares (overrides) {@link #endParsing(String, XWikiContext)}, appending a marker so that we can
     * verify it was actually dispatched to.
     */
    public static class DeclaringPlugin extends XWikiDefaultPlugin
    {
        public DeclaringPlugin(String name, String className, XWikiContext context)
        {
            super(name, className, context);
        }

        @Override
        public String endParsing(String content, XWikiContext context)
        {
            return content + "-parsed";
        }
    }

    /**
     * A plugin that does NOT declare {@code endParsing}, it only inherits it from {@link DeclaringPlugin}. Because
     * {@link XWikiPluginManager} registers a plugin for a function using the plugin class's {@code getDeclaredMethods()}
     * (which excludes inherited methods), such a plugin must not be dispatched to for {@code endParsing}.
     */
    public static class InheritingPlugin extends DeclaringPlugin
    {
        public InheritingPlugin(String name, String className, XWikiContext context)
        {
            super(name, className, context);
        }
    }

    /**
     * A plugin component.
     */
    @Component
    @Named("component")
    @Singleton
    public static class ComponentPlugin extends AbstractXWikiPlugin
    {
        private boolean initialized;

        @Override
        public void init(XWikiContext context)
        {
            this.initialized = true;
        }

        @Override
        public String endParsing(String content, XWikiContext context)
        {
            return content + "-component";
        }
    }

    /**
     * Another plugin component.
     */
    @Component
    @Named("other")
    @Singleton
    public static class OtherComponentPlugin extends AbstractXWikiPlugin
    {
    }

    /**
     * A plugin component failing to initialize.
     */
    @Component
    @Named("failing")
    @Singleton
    public static class FailingComponentPlugin extends AbstractXWikiPlugin
    {
        @Override
        public void init(XWikiContext context)
        {
            throw new IllegalStateException("Failed to initialize");
        }

        @Override
        public String endParsing(String content, XWikiContext context)
        {
            return content + "-failing";
        }
    }

    @RegisterExtension
    private LogCaptureExtension logCapture = new LogCaptureExtension(LogLevel.WARN);

    private XWikiPluginManager manager;

    private XWikiContext context;

    private EmbeddableComponentManager componentManager;

    @BeforeEach
    void setUp()
    {
        this.manager = new XWikiPluginManager();
        this.manager.initInterface();
        // The plugin manager looks up the plugin constructor using the exact context class
        this.context = new XWikiContext();
        this.componentManager = new EmbeddableComponentManager();
    }

    @SuppressWarnings("unchecked")
    private <T extends XWikiPluginInterface> void initPlugin(T plugin) throws Exception
    {
        this.manager.initPlugin(plugin, (Class<XWikiPluginInterface>) (Class<?>) plugin.getClass(), this.context);
    }

    private XWikiPluginInterface registerComponentPlugin(Class<? extends XWikiPluginInterface> pluginClass)
        throws Exception
    {
        List<ComponentDescriptor<?>> descriptors =
            new ComponentAnnotationLoader().getComponentsDescriptors(pluginClass);
        for (ComponentDescriptor<?> descriptor : descriptors) {
            this.componentManager.registerComponent(descriptor);
        }

        return this.componentManager.getInstance(XWikiPluginInterface.class, descriptors.get(0).getRoleHint());
    }

    @Test
    void declaredFunctionIsDispatched() throws Exception
    {
        DeclaringPlugin plugin = new DeclaringPlugin("test", "test", this.context);
        initPlugin(plugin);

        assertTrue(this.manager.getPlugins("endParsing").contains(plugin),
            "A plugin declaring endParsing() should be registered for that function");
        assertEquals("content-parsed", this.manager.endParsing("content", this.context));
    }

    @Test
    void inheritedFunctionIsNotDispatched() throws Exception
    {
        InheritingPlugin plugin = new InheritingPlugin("test", "test", this.context);
        initPlugin(plugin);

        // The plugin only inherits endParsing() (it does not declare it), so getDeclaredMethods() does not see it and
        // the plugin manager must not dispatch endParsing() to it. This is why a plugin class that wants to receive a
        // handler callback must redeclare (override) the method, even if the override only calls super.
        assertFalse(this.manager.getPlugins("endParsing").contains(plugin),
            "A plugin only inheriting endParsing() must not be registered for that function");
        assertEquals("content", this.manager.endParsing("content", this.context));
    }

    @Test
    void addComponentPlugins() throws Exception
    {
        ComponentPlugin plugin = (ComponentPlugin) registerComponentPlugin(ComponentPlugin.class);
        XWikiPluginInterface otherPlugin = registerComponentPlugin(OtherComponentPlugin.class);

        this.manager.addComponentPlugins(this.componentManager, this.context);

        // The component hint is the plugin name
        assertSame(plugin, this.manager.getPlugin("component"));
        assertTrue(plugin.initialized);
        assertSame(otherPlugin, this.manager.getPlugin("other"));
        assertEquals(List.of("component", "other"), this.manager.getPlugins().stream().sorted().toList());
        assertEquals("content-component", this.manager.endParsing("content", this.context));
    }

    @Test
    void addComponentPluginWithNameDifferentFromHint()
    {
        assertFalse(this.manager.addComponentPlugin("hint", new DeclaringPlugin("name", "name", this.context),
            this.context));

        assertTrue(this.manager.getPlugins().isEmpty());
        assertEquals("Plugin component [hint] is not available because its name [name] is different from its hint.",
            this.logCapture.getMessage(0));
    }

    @Test
    void addComponentPluginAlreadyRegisteredFromConfiguration()
    {
        this.manager.addPlugin(DeclaringPlugin.class.getName(), DeclaringPlugin.class.getName(), this.context);
        XWikiPluginInterface configuredPlugin = this.manager.getPlugin(DeclaringPlugin.class.getName());

        // Same class
        assertFalse(
            this.manager.addComponentPlugin("hint", new DeclaringPlugin("hint", "hint", this.context), this.context));
        // Same name
        assertFalse(this.manager.addComponentPlugin(DeclaringPlugin.class.getName(),
            new InheritingPlugin(DeclaringPlugin.class.getName(), DeclaringPlugin.class.getName(), this.context),
            this.context));

        assertEquals(List.of(DeclaringPlugin.class.getName()), this.manager.getPlugins());
        assertSame(configuredPlugin, this.manager.getPlugin(DeclaringPlugin.class.getName()));
    }

    @Test
    void removeComponentPlugin() throws Exception
    {
        XWikiPluginInterface plugin = registerComponentPlugin(ComponentPlugin.class);
        assertTrue(this.manager.addComponentPlugin("component", plugin, this.context));
        Vector<XWikiPluginInterface> endParsingPlugins = this.manager.getPlugins("endParsing");

        this.manager.removePlugin("component");

        assertTrue(this.manager.getPlugins().isEmpty());
        assertNull(this.manager.getPlugin("component"));
        assertEquals("content", this.manager.endParsing("content", this.context));
        // A list retrieved before the removal is not modified, so that it can be safely iterated
        assertEquals(List.of(plugin), endParsingPlugins);

        // The same class can be registered again
        this.componentManager.unregisterComponent(XWikiPluginInterface.class, "component");
        XWikiPluginInterface newPlugin = registerComponentPlugin(ComponentPlugin.class);
        assertTrue(this.manager.addComponentPlugin("component", newPlugin, this.context));
        assertSame(newPlugin, this.manager.getPlugin("component"));
    }

    @Test
    void removePlugin()
    {
        this.manager.addPlugin(DeclaringPlugin.class.getName(), DeclaringPlugin.class.getName(), this.context);
        assertEquals("content-parsed", this.manager.endParsing("content", this.context));

        this.manager.removePlugin(DeclaringPlugin.class.getName());

        assertTrue(this.manager.getPlugins().isEmpty());
        assertNull(this.manager.getPlugin(DeclaringPlugin.class.getName()));
        assertEquals("content", this.manager.endParsing("content", this.context));

        // The plugin can be added again
        this.manager.addPlugin(DeclaringPlugin.class.getName(), DeclaringPlugin.class.getName(), this.context);
        assertEquals("content-parsed", this.manager.endParsing("content", this.context));
    }

    @Test
    void addComponentPluginFailingToInitialize() throws Exception
    {
        XWikiPluginInterface plugin = registerComponentPlugin(FailingComponentPlugin.class);

        assertFalse(this.manager.addComponentPlugin("failing", plugin, this.context));

        assertTrue(this.manager.getPlugins().isEmpty());
        assertNull(this.manager.getPlugin("failing"));
        assertEquals("content", this.manager.endParsing("content", this.context));
        assertEquals("Cannot initialize plugin component [failing]. This plugin will not be available.",
            this.logCapture.getMessage(0));
    }

    @Test
    void abstractPluginNameWithoutComponentManager()
    {
        assertNull(new OtherComponentPlugin().getName());
    }
}
