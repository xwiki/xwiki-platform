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
package org.xwiki.gwt.wysiwyg.client.plugin.internal;

import org.xwiki.gwt.wysiwyg.client.plugin.PluginFactory;
import org.xwiki.gwt.wysiwyg.client.plugin.PluginFactoryManager;

/**
 * Mock plug-in factory manager to be used in unit tests.
 * 
 * @version $Id$
 */
public class MockPluginFactoryManager implements PluginFactoryManager
{
    /**
     * The only plugin factory known by this factory manager.
     */
    private final PluginFactory factory;

    /**
     * Creates a new mock plugin factory manager that uses only the given plugin factory.
     * 
     * @param factory The only factory that will be used by the newly created manager.
     */
    public MockPluginFactoryManager(PluginFactory factory)
    {
        this.factory = factory;
    }

    /**
     * {@inheritDoc}
     * 
     * @see PluginFactoryManager#addPluginFactory(PluginFactory)
     */
    public PluginFactory addPluginFactory(PluginFactory factory)
    {
        return null;
    }

    /**
     * {@inheritDoc}
     * 
     * @see PluginFactoryManager#getPluginFactory(String)
     */
    public PluginFactory getPluginFactory(String pluginName)
    {
        if (factory.getPluginName().equals(pluginName)) {
            return factory;
        } else {
            return null;
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see PluginFactoryManager#removePluginFactory(String)
     */
    public PluginFactory removePluginFactory(String pluginName)
    {
        return null;
    }
}
