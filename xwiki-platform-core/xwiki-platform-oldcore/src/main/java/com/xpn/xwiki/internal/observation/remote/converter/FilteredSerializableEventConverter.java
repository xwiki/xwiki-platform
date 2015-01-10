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
package com.xpn.xwiki.internal.observation.remote.converter;

import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.observation.remote.LocalEventData;
import org.xwiki.observation.remote.RemoteEventData;
import org.xwiki.observation.remote.internal.converter.SerializableEventConverter;

import com.xpn.xwiki.XWikiContext;

/**
 * Overwrite {@link SerializableEventConverter} to filter some known types which are declared as Serialized and are for
 * really being so.
 *
 * @version $Id$
 * @since 4.1.3
 */
// TODO: find a more generic way to filter those bad classes at SerializableEventConverter level
@Component
@Singleton
public class FilteredSerializableEventConverter extends SerializableEventConverter
{
    @Override
    public boolean toRemote(LocalEventData localEvent, RemoteEventData remoteEvent)
    {
        if (localEvent.getData() instanceof XWikiContext || localEvent.getSource() instanceof XWikiContext) {
            return false;
        } else {
            return super.toRemote(localEvent, remoteEvent);
        }
    }
}
