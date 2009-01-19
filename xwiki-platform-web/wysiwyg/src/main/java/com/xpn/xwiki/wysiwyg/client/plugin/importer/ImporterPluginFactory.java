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
package com.xpn.xwiki.wysiwyg.client.plugin.importer;

import com.xpn.xwiki.wysiwyg.client.plugin.Plugin;
import com.xpn.xwiki.wysiwyg.client.plugin.internal.AbstractPluginFactory;

/**
 * {@link PluginFactory} class for the office importer wysiwyg plugin.
 * 
 * @version $Id$
 */
public final class ImporterPluginFactory extends AbstractPluginFactory
{
    /**
     * Private instance of the plugin factory. Plugin factory is a singleton.
     */
    private static ImporterPluginFactory instance;

    /**
     * Private constructor.
     */
    private ImporterPluginFactory()
    {
        super("importer");
    }

    /**
     * @return The singleton instance of the plugin factory.
     */
    public static synchronized ImporterPluginFactory getInstance()
    {
        if (null == instance) {
            instance = new ImporterPluginFactory();
        }
        return instance;
    }

    /**
     * {@inheritDoc}
     */
    public Plugin newInstance()
    {
        return new ImporterPlugin();
    }
}
