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
package org.xwiki.instance;

import java.util.UUID;

/**
 * Represents an XWiki instance using a unique id.
 *
 * Note that we need this class so that Hibernate can persist and read the id.
 *
 * @version $Id$
 * @since 5.2M2
 */
public class InstanceId
{
    /**
     * @see #getInstanceId()
     */
    private UUID uuid;

    /**
     * Default constructor. It is need for Hibernate.
     */
    public InstanceId()
    {
    }

    /**
     * @param id the unique id of this instance
     */
    public InstanceId(String id)
    {
        setInstanceId(id);
    }

    /**
     * @return the unique id of this instance
     */
    public String getInstanceId()
    {
        return this.uuid.toString();
    }

    /**
     * @param id the unique id represented as a String
     */
    private void setInstanceId(String id)
    {
        this.uuid = UUID.fromString(id);
    }

    @Override
    public String toString()
    {
        return this.uuid.toString();
    }

    @Override
    public int hashCode()
    {
        return this.uuid.hashCode();
    }

    @Override
    public boolean equals(Object o)
    {
        return this.uuid.equals(o);
    }
}
