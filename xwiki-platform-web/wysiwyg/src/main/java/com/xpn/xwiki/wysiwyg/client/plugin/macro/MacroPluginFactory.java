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
package com.xpn.xwiki.wysiwyg.client.plugin.macro;

import java.util.MissingResourceException;

import com.google.gwt.core.client.GWT;
import com.google.gwt.i18n.client.Dictionary;
import com.google.gwt.user.client.rpc.ServiceDefTarget;
import com.xpn.xwiki.wysiwyg.client.plugin.Plugin;
import com.xpn.xwiki.wysiwyg.client.plugin.internal.AbstractPluginFactory;

/**
 * Factory for {@link MacroPlugin}.
 * 
 * @version $Id$
 */
public final class MacroPluginFactory extends AbstractPluginFactory
{
    /**
     * The singleton factory instance.
     */
    private static MacroPluginFactory instance;

    /**
     * The macro service used to retrieve macro descriptors.
     */
    private MacroServiceAsync macroService;

    /**
     * Default constructor.
     */
    private MacroPluginFactory()
    {
        super("macro");

        String serviceURL;
        try {
            // Look in the global configuration object.
            serviceURL = Dictionary.getDictionary("GWTConfig").get("serviceURL");
        } catch (MissingResourceException e) {
            serviceURL = "/MacroService";
        }

        macroService = GWT.create(MacroService.class);
        ((ServiceDefTarget) macroService).setServiceEntryPoint(serviceURL);

        // We cache the service calls.
        macroService = new MacroServiceAsyncCacheProxy(macroService);
    }

    /**
     * @return the singleton factory instance.
     */
    public static synchronized MacroPluginFactory getInstance()
    {
        if (instance == null) {
            instance = new MacroPluginFactory();
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
        return new MacroPlugin(macroService);
    }
}
