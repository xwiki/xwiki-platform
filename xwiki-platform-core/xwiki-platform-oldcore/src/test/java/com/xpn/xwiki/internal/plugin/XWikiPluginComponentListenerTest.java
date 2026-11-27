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
package com.xpn.xwiki.internal.plugin;

import java.util.ArrayList;
import java.util.List;

import jakarta.inject.Named;
import jakarta.inject.Provider;
import jakarta.inject.Singleton;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.annotation.ComponentAnnotationLoader;
import org.xwiki.component.descriptor.ComponentDescriptor;
import org.xwiki.component.embed.EmbeddableComponentManager;
import org.xwiki.component.event.ComponentDescriptorAddedEvent;
import org.xwiki.component.event.ComponentDescriptorRemovedEvent;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.plugin.AbstractXWikiPlugin;
import com.xpn.xwiki.plugin.XWikiDefaultPlugin;
import com.xpn.xwiki.plugin.XWikiPluginInterface;
import com.xpn.xwiki.plugin.XWikiPluginManager;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link XWikiPluginComponentListener}.
 *
 * @version $Id$
 */
@ComponentTest
class XWikiPluginComponentListenerTest
{
    private static final String HINT = "testplugin";

    /**
     * A plugin component remembering the wikis in which it was initialized.
     */
    @Component
    @Named(HINT)
    @Singleton
    public static class TestPlugin extends AbstractXWikiPlugin
    {
        private final List<String> initializedWikis = new ArrayList<>();

        @Override
        public void virtualInit(XWikiContext context)
        {
            this.initializedWikis.add(context.getWikiId());
        }
    }

    @InjectMockComponents
    private XWikiPluginComponentListener listener;

    @MockComponent
    private Provider<XWikiContext> xcontextProvider;

    private XWikiContext xcontext;

    private XWiki xwiki;

    private XWikiPluginManager pluginManager;

    @BeforeEach
    void beforeEach()
    {
        this.xwiki = mock(XWiki.class);
        this.pluginManager = new XWikiPluginManager();
        this.pluginManager.initInterface();
        when(this.xwiki.getPluginManager()).thenReturn(this.pluginManager);
        when(this.xwiki.getVirtualWikiList()).thenReturn(List.of("wiki1", "wiki2"));

        this.xcontext = new XWikiContext();
        this.xcontext.setWiki(this.xwiki);
        this.xcontext.setWikiId("current");
        when(this.xcontextProvider.get()).thenReturn(this.xcontext);
    }

    private ComponentDescriptor<?> registerPlugin(EmbeddableComponentManager componentManager) throws Exception
    {
        ComponentDescriptor<?> descriptor =
            new ComponentAnnotationLoader().getComponentsDescriptors(TestPlugin.class).get(0);
        componentManager.registerComponent(descriptor);

        return descriptor;
    }

    @Test
    void addAndRemovePluginComponent() throws Exception
    {
        EmbeddableComponentManager componentManager = new EmbeddableComponentManager();
        ComponentDescriptor<?> descriptor = registerPlugin(componentManager);

        this.listener.onEvent(new ComponentDescriptorAddedEvent(XWikiPluginInterface.class, HINT), componentManager,
            descriptor);

        TestPlugin plugin = (TestPlugin) componentManager.getInstance(XWikiPluginInterface.class, HINT);
        assertSame(plugin, this.pluginManager.getPlugin(HINT));
        // The plugin is initialized in the wikis which are already initialized
        assertEquals(List.of("wiki1", "wiki2"), plugin.initializedWikis);
        assertEquals("current", this.xcontext.getWikiId());

        this.listener.onEvent(new ComponentDescriptorRemovedEvent(XWikiPluginInterface.class, HINT),
            componentManager, descriptor);

        assertNull(this.pluginManager.getPlugin(HINT));
        assertTrue(this.pluginManager.getPlugins().isEmpty());
    }

    @Test
    void keepPluginWithSameNameAndOtherClassOnRemove() throws Exception
    {
        EmbeddableComponentManager componentManager = new EmbeddableComponentManager();
        ComponentDescriptor<?> descriptor = registerPlugin(componentManager);
        XWikiPluginInterface otherPlugin = new XWikiDefaultPlugin(HINT, HINT, this.xcontext)
        {
        };
        this.pluginManager.addComponentPlugin(HINT, otherPlugin, this.xcontext);

        this.listener.onEvent(new ComponentDescriptorRemovedEvent(XWikiPluginInterface.class, HINT),
            componentManager, descriptor);

        assertSame(otherPlugin, this.pluginManager.getPlugin(HINT));
    }

    @Test
    void ignorePluginComponentInWikiNamespace() throws Exception
    {
        EmbeddableComponentManager componentManager = new EmbeddableComponentManager("wiki:wiki1");
        ComponentDescriptor<?> descriptor = registerPlugin(componentManager);

        this.listener.onEvent(new ComponentDescriptorAddedEvent(XWikiPluginInterface.class, HINT), componentManager,
            descriptor);

        assertTrue(this.pluginManager.getPlugins().isEmpty());
    }

    @Test
    void ignorePluginComponentBeforeXWikiInitialization() throws Exception
    {
        EmbeddableComponentManager componentManager = new EmbeddableComponentManager();
        ComponentDescriptor<?> descriptor = registerPlugin(componentManager);
        when(this.xwiki.getPluginManager()).thenReturn(null);

        this.listener.onEvent(new ComponentDescriptorAddedEvent(XWikiPluginInterface.class, HINT), componentManager,
            descriptor);

        // The plugin will be added when the plugin manager is created
        assertTrue(this.pluginManager.getPlugins().isEmpty());
    }
}
