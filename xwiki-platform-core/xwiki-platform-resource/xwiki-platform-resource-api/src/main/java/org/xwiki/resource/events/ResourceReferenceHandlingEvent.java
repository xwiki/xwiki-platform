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

import org.xwiki.observation.event.BeginEvent;
import org.xwiki.resource.ResourceReference;

/**
 * Event sent before starting the execution of a {@link org.xwiki.resource.ResourceReferenceHandler}.
 * <p>
 * The event also send the following parameters:
 * </p>
 * <ul>
 * <li>source: the instance of the executed {@link org.xwiki.resource.ResourceReferenceHandler}</li>
 * <li>data: null</li>
 * </ul>
 * 
 * @version $Id$
 * @since 9.11RC1
 */
public class ResourceReferenceHandlingEvent extends AbstractResourceReferenceHandlerEvent implements BeginEvent
{
    /**
     * Match any {@link ResourceReferenceHandlingEvent}.
     */
    public ResourceReferenceHandlingEvent()
    {
        // Empty voluntarily, just here to offer a default constructor
    }

    /**
     * Constructor initializing the reference of the event.
     * 
     * @param reference the reference handled
     */
    public ResourceReferenceHandlingEvent(ResourceReference reference)
    {
        super(reference);
    }
}
