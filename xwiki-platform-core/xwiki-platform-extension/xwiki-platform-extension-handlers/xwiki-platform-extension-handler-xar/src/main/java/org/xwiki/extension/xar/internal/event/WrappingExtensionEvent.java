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
package org.xwiki.extension.xar.internal.event;

import org.xwiki.extension.ExtensionId;
import org.xwiki.extension.event.ExtensionEvent;

/**
 * Wraps an {@link ExtensionEvent}.
 * 
 * @version $Id$
 * @since 11.10
 */
public class WrappingExtensionEvent implements ExtensionEvent
{
    private ExtensionEvent originalEvent;

    /**
     * Wraps the given extension event.
     * 
     * @param originalEvent the event to wrap
     */
    public WrappingExtensionEvent(ExtensionEvent originalEvent)
    {
        this.originalEvent = originalEvent;
    }

    @Override
    public boolean matches(Object otherEvent)
    {
        return this.getClass() == otherEvent.getClass()
            && this.originalEvent.matches(((WrappingExtensionEvent) otherEvent).originalEvent);
    }

    @Override
    public ExtensionId getExtensionId()
    {
        return this.originalEvent.getExtensionId();
    }

    @Override
    public String getNamespace()
    {
        return this.originalEvent.getNamespace();
    }

    @Override
    public boolean hasNamespace()
    {
        return this.originalEvent.hasNamespace();
    }
}
