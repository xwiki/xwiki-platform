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
package org.xwiki.gwt.wysiwyg.client.plugin.submit;

import org.xwiki.gwt.wysiwyg.client.plugin.Plugin;
import org.xwiki.gwt.wysiwyg.client.plugin.internal.AbstractPluginFactory;

import com.google.gwt.core.client.GWT;

/**
 * Factory for {@link SubmitPlugin}.
 * 
 * @version $Id$
 */
public final class SubmitPluginFactory extends AbstractPluginFactory
{
    /**
     * The singleton factory instance.
     */
    private static SubmitPluginFactory instance;

    /**
     * Default constructor.
     */
    private SubmitPluginFactory()
    {
        super("submit");
    }

    /**
     * @return the singleton factory instance
     */
    public static synchronized SubmitPluginFactory getInstance()
    {
        if (instance == null) {
            instance = new SubmitPluginFactory();
        }
        return instance;
    }

    @Override
    public Plugin newInstance()
    {
        return GWT.create(SubmitPlugin.class);
    }
}
