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
package org.xwiki.extension.event;

import org.xwiki.extension.ExtensionId;

/**
 * Base class for all {@link ExtensionEvent}.
 * 
 * @version $Id$
 */
public abstract class AbstractExtensionEvent implements ExtensionEvent
{
    /**
     * The event related extension identifier.
     */
    private ExtensionId extensionId;

    /**
     * Default constructor.
     */
    public AbstractExtensionEvent()
    {

    }

    /**
     * @param extensionId the event related extension identifier
     */
    protected AbstractExtensionEvent(ExtensionId extensionId)
    {
        this.extensionId = extensionId;
    }

    // ExtensionEvent

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.extension.event.ExtensionEvent#getExtensionId()
     */
    public ExtensionId getExtensionId()
    {
        return this.extensionId;
    }

    // Event

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.observation.event.Event#matches(java.lang.Object)
     */
    public boolean matches(Object event)
    {
        return this.getClass() == event.getClass()
            && (this.extensionId == null || macthesExtensionId(((AbstractExtensionEvent) event).getExtensionId()));
    }

    /**
     * @param extensionId the event related extension identifier
     * @return <code>true</code> if the passed event matches this event, <code>false</code> otherwise.
     */
    private boolean macthesExtensionId(ExtensionId extensionId)
    {
        return this.extensionId.equals(extensionId)
            || (this.extensionId.getVersion() == null && this.extensionId.getId().equals(extensionId.getId()));
    }
}
