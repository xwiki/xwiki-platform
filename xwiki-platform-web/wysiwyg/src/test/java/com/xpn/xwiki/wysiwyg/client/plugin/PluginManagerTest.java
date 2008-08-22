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
package com.xpn.xwiki.wysiwyg.client.plugin;

import com.xpn.xwiki.wysiwyg.client.WysiwygClientTest;
import com.xpn.xwiki.wysiwyg.client.plugin.internal.MockPlugin;
import com.xpn.xwiki.wysiwyg.client.plugin.internal.MockPluginFactory;
import com.xpn.xwiki.wysiwyg.client.plugin.internal.MockPluginFactoryManager;
import com.xpn.xwiki.wysiwyg.client.plugin.internal.MockUIExtension;

/**
 * Unit tests for any concrete implementation of {@link PluginManagerTest}.
 */
public abstract class PluginManagerTest extends WysiwygClientTest
{
    /**
     * @return A new instance of the concrete {@link PluginManager} being tested.
     */
    protected abstract PluginManager newPluginManager();

    public void testCommonUseCase()
    {
        PluginManager pm = newPluginManager();

        String role = "toolbar";
        String feature = "orderedList";
        UIExtension uie = new MockUIExtension(role, feature);
        Plugin plugin = new MockPlugin(uie);

        String pluginName = "list";
        PluginFactory pf = new MockPluginFactory(pluginName, plugin);
        PluginFactoryManager pfm = new MockPluginFactoryManager(pf);

        pm.setPluginFactoryManager(pfm);
        assertEquals(pfm, pm.getPluginFactoryManager());

        pm.load(pluginName);
        assertEquals(uie, pm.getUIExtension(role, feature));

        pm.unload("list");
        assertNull(pm.getUIExtension(role, feature));
    }
}
