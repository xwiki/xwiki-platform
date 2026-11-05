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
package org.xwiki.store.hibernate.internal;

/**
 * Entity used to test the restored Hibernate 5 APIs.
 *
 * @version $Id$
 */
public class LegacyEntity
{
    private long id;

    private String name;

    /**
     * Used by Hibernate.
     */
    public LegacyEntity()
    {
    }

    /**
     * @param id the identifier
     * @param name the name
     */
    public LegacyEntity(long id, String name)
    {
        this.id = id;
        this.name = name;
    }

    /**
     * @return the identifier
     */
    public long getId()
    {
        return this.id;
    }

    /**
     * @param id the identifier
     */
    public void setId(long id)
    {
        this.id = id;
    }

    /**
     * @return the name
     */
    public String getName()
    {
        return this.name;
    }

    /**
     * @param name the name
     */
    public void setName(String name)
    {
        this.name = name;
    }
}
