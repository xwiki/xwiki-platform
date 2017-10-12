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

import org.xwiki.extension.ExtensionId;
import org.xwiki.extension.version.Version;
import org.xwiki.observation.event.Event;

/**
 * Event triggered when a new extension version is available.
 *
 * @since 9.9RC1
 * @version $Id$
 */
public class NewExtensionVersionAvailableEvent implements Event
{
    private ExtensionId extensionId;

    private Version version;

    /**
     * Constructs a new {@link NewExtensionVersionAvailableEvent}.
     *
     * @param extensionId the {@link ExtensionId} concerned by the event
     * @param version the new available version
     */
    public NewExtensionVersionAvailableEvent(ExtensionId extensionId, Version version)
    {
        this.extensionId = extensionId;
        this.version = version;
    }

    /**
     * @return the ID of the extension concerned by the event
     */
    public ExtensionId getExtensionId()
    {
        return extensionId;
    }

    /**
     * @return the new available version of the extension
     */
    public Version getVersion()
    {
        return version;
    }

    @Override
    public boolean matches(Object otherEvent)
    {
        if (otherEvent instanceof NewExtensionVersionAvailableEvent) {
            NewExtensionVersionAvailableEvent event = (NewExtensionVersionAvailableEvent) otherEvent;
            return (event.getExtensionId().matches(extensionId) && (event.getVersion().compareTo(version) == 0));
        }

        return false;
    }
}
