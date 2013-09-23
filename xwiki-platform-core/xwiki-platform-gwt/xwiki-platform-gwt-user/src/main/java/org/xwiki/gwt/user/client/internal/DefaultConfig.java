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
package org.xwiki.gwt.user.client.internal;

import java.util.Collections;
import java.util.MissingResourceException;
import java.util.Set;

import org.xwiki.gwt.dom.client.JavaScriptObject;
import org.xwiki.gwt.dom.client.Window;
import org.xwiki.gwt.user.client.Config;

import com.google.gwt.i18n.client.Dictionary;

/**
 * The default implementation of {@link Config} interface. This implementation wraps a {@link Dictionary} instance build
 * from a JavaScript object in the host HTML page.
 * 
 * @version $Id$
 */
public final class DefaultConfig implements Config
{
    /**
     * Empty configuration.
     */
    public static final DefaultConfig DEFAULT = new DefaultConfig();

    /**
     * This is build from a JavaScript object in the container HTML page.
     */
    private final Dictionary params;

    /**
     * Creates a new empty configuration object.
     */
    private DefaultConfig()
    {
        params = null;
    }

    /**
     * Creates a new configuration object based on the given {@link JavaScriptObject} by putting it in the global name
     * space and then looking it up using GWT's {@link Dictionary} mechanism.
     * 
     * @param jso the source for the configuration object
     */
    public DefaultConfig(JavaScriptObject jso)
    {
        // Generate a random name for the JavaScript object because the Dictionary class uses a cache.
        String name = DefaultConfig.class.getName() + "@" + Math.round(Math.random() * 1000);
        // Place the JavaScript object in the global name space so that the Dictionary can look it up.
        Window.get().set(name, jso);
        // Create a dictionary based on the given JavaScript object.
        Dictionary dictionary = null;
        try {
            dictionary = Dictionary.getDictionary(name);
        } catch (MissingResourceException e) {
            // empty configuration
        }
        params = dictionary;
        // Remove the JavaScript object from the global name space once the dictionary has been created.
        Window.get().remove(name);
    }

    /**
     * Creates a new configuration object based on the given dictionary.
     * 
     * @param params a dictionary.
     */
    public DefaultConfig(Dictionary params)
    {
        this.params = params;
    }

    @Override
    public String getParameter(String paramName)
    {
        return getParameter(paramName, null);
    }

    @Override
    public String getParameter(String paramName, String defaultValue)
    {
        try {
            Object paramValue = params.get(paramName);
            return (paramValue == null) ? defaultValue : String.valueOf(paramValue);
        } catch (Exception e) {
            return defaultValue;
        }
    }

    @Override
    public Set<String> getParameterNames()
    {
        if (params != null) {
            return params.keySet();
        } else {
            return Collections.emptySet();
        }
    }
}
