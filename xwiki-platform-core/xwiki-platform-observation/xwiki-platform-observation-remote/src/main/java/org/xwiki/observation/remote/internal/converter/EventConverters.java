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
package org.xwiki.observation.remote.internal.converter;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.observation.remote.converter.LocalEventConverter;
import org.xwiki.observation.remote.converter.RemoteEventConverter;

/**
 * Hold the local and remote events converters.
 * 
 * @version $Id$
 * @since 18.3.0RC1
 * @since 17.10.8
 */
@Component(roles = EventConverters.class)
@Singleton
public class EventConverters
{
    @Inject
    private ComponentManager componentManager;

    @Inject
    private Logger logger;

    /**
     * The local events converters.
     */
    private List<LocalEventConverter> localEventConverters;

    /**
     * The remote events converters.
     */
    private List<RemoteEventConverter> remoteEventConverters;

    /**
     * @return the local event converters
     */
    public List<LocalEventConverter> getLocalEventConverters()
    {
        if (this.localEventConverters == null) {
            this.localEventConverters =
                loadConverters(LocalEventConverter.class, (c1, c2) -> c1.getPriority() - c2.getPriority());
        }

        return this.localEventConverters;
    }

    /**
     * @return the remote event converters
     */
    public List<RemoteEventConverter> getRemoteEventConverters()
    {
        if (this.remoteEventConverters == null) {
            this.remoteEventConverters =
                loadConverters(RemoteEventConverter.class, (c1, c2) -> c1.getPriority() - c2.getPriority());
        }

        return this.remoteEventConverters;
    }

    /**
     * Reset the cached list of local event converter components. This is useful when the list of converters has changed
     * and we want to reload it.
     */
    public void resetLocalEventConverters()
    {
        this.localEventConverters = null;
    }

    /**
     * Reset the cached list of remote event converter components. This is useful when the list of converters has
     * changed and we want to reload it.
     */
    public void resetRemoteEventConverters()
    {
        this.remoteEventConverters = null;
    }

    private <T> List<T> loadConverters(Class<T> converterType, Comparator<T> c)
    {
        // Load converters lazily to avoid cycles if any of those inject directly or indirectly the converter manager
        try {
            List<T> converters = this.componentManager.getInstanceList(converterType);
            Collections.sort(converters, c);
            return converters;
        } catch (ComponentLookupException e) {
            this.logger.error("Failed to lookup event converters", e);
        }

        return List.of();
    }
}
