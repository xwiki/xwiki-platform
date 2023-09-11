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
package org.xwiki.eventstream.query;

import org.apache.commons.lang3.builder.HashCodeBuilder;

/**
 * A condition related to the association indicating if an entity should receive a mail for an event.
 * 
 * @version $Id$
 * @since 12.6
 */
public class MailEntityQueryCondition extends AbstractEntityQueryCondition
{
    /**
     * @param statusEntityId the entity associated with the events
     * @param reversed true if the condition should be reversed
     */
    public MailEntityQueryCondition(String statusEntityId, boolean reversed)
    {
        super(statusEntityId, reversed);
    }

    @Override
    public int hashCode()
    {
        HashCodeBuilder builder = new HashCodeBuilder();

        builder.appendSuper(super.hashCode());

        return builder.build();
    }

    @Override
    public boolean equals(Object obj)
    {
        if (obj == this) {
            return true;
        }

        if (obj instanceof MailEntityQueryCondition) {
            return super.equals(obj);
        }

        return false;
    }
}
