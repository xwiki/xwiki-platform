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
package org.xwiki.resource.events;

import org.xwiki.observation.event.EndEvent;
import org.xwiki.resource.ResourceReference;
import org.xwiki.resource.ResourceReferenceHandlerException;
import org.xwiki.stability.Unstable;

/**
 * Event sent after the execution of a {@link org.xwiki.resource.ResourceReferenceHandler}.
 * <p>
 * The event also send the following parameters:
 * </p>
 * <ul>
 * <li>source: the instance of the executed {@link org.xwiki.resource.ResourceReferenceHandler}</li>
 * <li>data: the {@link ResourceReferenceHandlerException} if any</li>
 * </ul>
 * 
 * @version $Id$
 * @since 9.11RC1
 */
@Unstable
public class ResourceReferenceHandledEvent extends AbstractResourceReferenceHandlerEvent implements EndEvent
{
    /**
     * Match any {@link ResourceReferenceHandledEvent}.
     */
    public ResourceReferenceHandledEvent()
    {
        // Empty voluntarily, just here to offer a default constructor
    }

    /**
     * Constructor initializing the reference of the event.
     * 
     * @param reference the reference handled
     */
    public ResourceReferenceHandledEvent(ResourceReference reference)
    {
        super(reference);
    }
}
