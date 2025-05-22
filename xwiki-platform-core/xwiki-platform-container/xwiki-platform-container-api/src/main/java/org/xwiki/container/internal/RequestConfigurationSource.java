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
package org.xwiki.container.internal;

import java.util.Collections;
import java.util.List;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.configuration.internal.AbstractConfigurationSource;
import org.xwiki.container.Container;

/**
 * Configuration source based on the current request parameters.
 *
 * @version $Id$
 * @since 17.4.0
 */
@Component
@Singleton
@Named("request")
// TODO: Extend AbstractPropertiesConfigurationSource once we move to 17.5.0.
public class RequestConfigurationSource extends AbstractConfigurationSource
{
    @Inject
    private Container container;

    @Override
    public boolean containsKey(String key)
    {
        return this.container.getRequest().getParameter(key) != null;
    }

    @Override
    public List<String> getKeys()
    {
        return Collections.list(this.container.getRequest().getParameterNames());
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> T getProperty(String key)
    {
        return (T) this.container.getRequest().getParameter(key);
    }

    @Override
    public <T> T getProperty(String key, T defaultValue)
    {
        if (containsKey(key)) {
            return getProperty(key);
        }
        return defaultValue;
    }

    @Override
    public <T> T getProperty(String key, Class<T> valueClass)
    {
        return getProperty(key);
    }

    @Override
    public boolean isEmpty()
    {
        return this.container.getRequest().getParameterNames().hasMoreElements();
    }
}
