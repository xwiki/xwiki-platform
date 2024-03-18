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
package org.xwiki.notifications.filters.internal.livedata.system;

import java.util.Collection;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.livedata.LiveDataConfiguration;
import org.xwiki.livedata.LiveDataException;
import org.xwiki.livedata.LiveDataPropertyDescriptor;
import org.xwiki.livedata.LiveDataPropertyDescriptorStore;

/**
 * Descriptor for the {@link NotificationSystemFiltersLiveDataSource}.
 *
 * @version $Id$
 * @since 16.3.0RC1
 */
@Component
@Singleton
@Named(NotificationSystemFiltersLiveDataSource.NAME)
public class NotificationSystemFiltersLiveDataPropertyDescriptorStore implements LiveDataPropertyDescriptorStore
{
    @Inject
    @Named(NotificationSystemFiltersLiveDataSource.NAME)
    private Provider<LiveDataConfiguration> liveDataConfigurationProvider;

    @Override
    public Collection<LiveDataPropertyDescriptor> get() throws LiveDataException
    {
        return liveDataConfigurationProvider.get().getMeta().getPropertyDescriptors();
    }
}
