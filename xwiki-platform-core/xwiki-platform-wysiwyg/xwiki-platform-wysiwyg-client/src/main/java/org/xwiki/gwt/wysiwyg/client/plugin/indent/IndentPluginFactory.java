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
package org.xwiki.gwt.wysiwyg.client.plugin.indent;

import org.xwiki.gwt.wysiwyg.client.plugin.Plugin;
import org.xwiki.gwt.wysiwyg.client.plugin.internal.AbstractPluginFactory;

/**
 * Factory for {@link IndentPlugin}.
 * 
 * @version $Id$
 */
public final class IndentPluginFactory extends AbstractPluginFactory
{
    /**
     * The singleton factory instance.
     */
    private static IndentPluginFactory instance;

    /**
     * Default constructor.
     */
    private IndentPluginFactory()
    {
        super("indent");
    }

    /**
     * @return the singleton factory instance
     */
    public static synchronized IndentPluginFactory getInstance()
    {
        if (instance == null) {
            instance = new IndentPluginFactory();
        }
        return instance;
    }

    @Override
    public Plugin newInstance()
    {
        return new IndentPlugin();
    }
}
