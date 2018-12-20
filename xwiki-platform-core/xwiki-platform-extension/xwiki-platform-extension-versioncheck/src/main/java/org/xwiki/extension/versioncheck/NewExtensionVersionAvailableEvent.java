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
package org.xwiki.extension.versioncheck;

import java.util.Collections;
import java.util.Set;

import org.xwiki.eventstream.RecordableEvent;
import org.xwiki.eventstream.TargetableEvent;

/**
 * Event triggered when a new extension version is available.
 * <p>
 *     The event should also send the following parameters:
 * </p>
 * <ul>
 *     <li>source: the {@link org.xwiki.extension.ExtensionId} that has a new version</li>
 *     <li>data: the new {@link org.xwiki.extension.version.Version}</li>
 * </ul>
 *
 * @since 9.9RC1
 * @version $Id$
 */
public class NewExtensionVersionAvailableEvent implements RecordableEvent, TargetableEvent
{
    @Override
    public boolean matches(Object otherEvent)
    {
        return (otherEvent instanceof NewExtensionVersionAvailableEvent);
    }

    @Override
    public Set<String> getTarget()
    {
        return Collections.singleton("XWikiAdminGroup");
    }
}
