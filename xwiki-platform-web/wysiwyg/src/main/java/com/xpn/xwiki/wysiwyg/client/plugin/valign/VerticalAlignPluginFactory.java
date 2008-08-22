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
package com.xpn.xwiki.wysiwyg.client.plugin.valign;

import com.xpn.xwiki.wysiwyg.client.plugin.Plugin;
import com.xpn.xwiki.wysiwyg.client.plugin.PluginFactory;
import com.xpn.xwiki.wysiwyg.client.plugin.internal.AbstractPluginFactory;

public final class VerticalAlignPluginFactory extends AbstractPluginFactory
{
    private static VerticalAlignPluginFactory instance;

    private VerticalAlignPluginFactory()
    {
        super("valign");
    }

    public static synchronized VerticalAlignPluginFactory getInstance()
    {
        if (instance == null) {
            instance = new VerticalAlignPluginFactory();
        }
        return instance;
    }

    /**
     * {@inheritDoc}
     * 
     * @see PluginFactory#newInstance()
     */
    public Plugin newInstance()
    {
        return new VerticalAlignPlugin();
    }
}
