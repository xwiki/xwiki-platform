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
package org.xwiki.template.event;

import java.util.Objects;

import org.xwiki.stability.Unstable;

/**
 * Base class for all events related to templates.
 *
 * @version $Id$
 * @since 7.0M1
 */
@Unstable
public abstract class AbstractTemplateEvent implements TemplateEvent
{
    private String id;

    /**
     * Matches any {@link TemplateEvent}s.
     */
    public AbstractTemplateEvent()
    {

    }

    /**
     * @param id the id of the template
     */
    public AbstractTemplateEvent(String id)
    {
        this.id = id;
    }

    @Override
    public boolean matches(Object otherEvent)
    {
        if (getClass() == otherEvent.getClass()) {
            return Objects.equals(this.id, ((TemplateEvent) otherEvent).getId());
        }

        return false;
    }

    @Override
    public String getId()
    {
        return this.id;
    }
}
