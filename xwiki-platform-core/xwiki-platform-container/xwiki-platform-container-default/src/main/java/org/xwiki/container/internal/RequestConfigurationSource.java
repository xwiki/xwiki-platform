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
import org.xwiki.configuration.internal.AbstractPropertiesConfigurationSource;
import org.xwiki.container.Container;

/**
 * Configuration source based on the current request parameters.
 *
 * @version $Id$
 * @since 17.6.0RC1
 */
@Component
@Singleton
@Named("request")
public class RequestConfigurationSource extends AbstractPropertiesConfigurationSource
{
    @Inject
    private Container container;

    @Override
    protected boolean containsKeyInternal(String key)
    {
        return this.container.getRequest().getParameter(key) != null;
    }

    @Override
    protected List<String> getKeysInternal()
    {
        return Collections.list(this.container.getRequest().getParameterNames());
    }

    @Override
    protected List<String> getKeysInternal(String prefix)
    {
        return getKeysInternal().stream().filter(key -> key.startsWith(prefix)).toList();
    }

    @SuppressWarnings("unchecked")
    @Override
    protected <T> T getPropertyInternal(String key)
    {
        return (T) this.container.getRequest().getParameter(key);
    }

    @Override
    protected boolean isEmptyInternal()
    {
        return this.container.getRequest().getParameterNames().hasMoreElements();
    }

    @Override
    protected boolean isEmptyInternal(String prefix)
    {
        return getKeysInternal(prefix).isEmpty();
    }
}
