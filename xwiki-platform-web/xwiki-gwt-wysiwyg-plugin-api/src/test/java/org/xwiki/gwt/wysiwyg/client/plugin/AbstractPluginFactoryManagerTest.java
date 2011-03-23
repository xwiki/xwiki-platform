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
package org.xwiki.gwt.wysiwyg.client.plugin;

import org.xwiki.gwt.wysiwyg.client.plugin.internal.MockPluginFactory;


/**
 * Unit tests for any concrete implementation of {@link PluginFactoryManager}.
 * 
 * @version $Id$
 */
public abstract class AbstractPluginFactoryManagerTest extends PluginTestCase
{
    /**
     * @return A new instance of the concrete {@link PluginFactoryManager} being tested.
     */
    protected abstract PluginFactoryManager newPluginFactoryManager();

    /**
     * Test the basic operations: add, get and remove a plugin factory.
     */
    public void testCommonUseCase()
    {
        PluginFactoryManager pfm = newPluginFactoryManager();

        String pluginName = "image";
        PluginFactory pf = new MockPluginFactory(pluginName, null);

        pfm.addPluginFactory(pf);
        assertEquals(pf, pfm.getPluginFactory(pluginName));

        pfm.removePluginFactory(pluginName);
        assertNull(pfm.getPluginFactory(pluginName));
    }
}
