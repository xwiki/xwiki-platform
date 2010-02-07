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
package org.xwiki.gwt.wysiwyg.client.plugin.sync;

import org.xwiki.gwt.wysiwyg.client.plugin.Plugin;
import org.xwiki.gwt.wysiwyg.client.plugin.internal.AbstractPluginFactory;

import com.google.gwt.core.client.GWT;

/**
 * Factory for {@link SyncPlugin}.
 * 
 * @version $Id$
 */
public final class SyncPluginFactory extends AbstractPluginFactory
{
    /**
     * The singleton factory instance.
     */
    private static SyncPluginFactory instance;

    /**
     * The service used to synchronize the content of multiple editors.
     */
    private final SyncServiceAsync syncService = GWT.create(SyncService.class);

    /**
     * Default constructor.
     */
    private SyncPluginFactory()
    {
        super("sync");
    }

    /**
     * @return the singleton factory instance
     */
    public static synchronized SyncPluginFactory getInstance()
    {
        if (instance == null) {
            instance = new SyncPluginFactory();
        }
        return instance;
    }

    /**
     * {@inheritDoc}
     * 
     * @see AbstractPluginFactory#newInstance()
     */
    public Plugin newInstance()
    {
        return new SyncPlugin(syncService);
    }
}
