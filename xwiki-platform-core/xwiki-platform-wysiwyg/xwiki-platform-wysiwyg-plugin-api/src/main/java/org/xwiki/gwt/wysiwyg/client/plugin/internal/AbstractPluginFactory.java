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

/**
 * Abstract implementation of the {@link PluginFactory} interface. This could serve as a base class for all kind of
 * plug-in factories.
 * 
 * @version $Id$
 */
public abstract class AbstractPluginFactory implements PluginFactory
{
    /**
     * The name of the plugins that will be created by this factory.
     */
    private final String pluginName;

    /**
     * Creates a new factory for the specified plugin.
     * 
     * @param pluginName The name of the plugins that will be created by this factory.
     */
    protected AbstractPluginFactory(String pluginName)
    {
        this.pluginName = pluginName;
    }

    @Override
    public String getPluginName()
    {
        return pluginName;
    }
}
