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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import com.xpn.xwiki.XWikiContext;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;

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

    private XWikiPluginManager manager;

    private XWikiContext context;

    @BeforeEach
    void setUp()
    {
        this.manager = new XWikiPluginManager();
        this.manager.initInterface();
        this.context = Mockito.mock(XWikiContext.class);
    }

    @SuppressWarnings("unchecked")
    private <T extends XWikiPluginInterface> void initPlugin(T plugin) throws Exception
    {
        this.manager.initPlugin(plugin, (Class<XWikiPluginInterface>) (Class<?>) plugin.getClass(), this.context);
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
}
