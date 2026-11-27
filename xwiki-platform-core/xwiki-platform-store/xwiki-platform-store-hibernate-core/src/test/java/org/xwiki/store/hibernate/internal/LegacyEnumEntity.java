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

import java.util.concurrent.TimeUnit;

/**
 * Entity used to test the restored Hibernate 5 {@link org.hibernate.type.EnumType}.
 *
 * @version $Id$
 */
public class LegacyEnumEntity
{
    private long id;

    private TimeUnit ordinalUnit;

    private TimeUnit namedUnit;

    private TimeUnit typedUnit;

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
     * @return the unit stored with its ordinal
     */
    public TimeUnit getOrdinalUnit()
    {
        return this.ordinalUnit;
    }

    /**
     * @param ordinalUnit the unit stored with its ordinal
     */
    public void setOrdinalUnit(TimeUnit ordinalUnit)
    {
        this.ordinalUnit = ordinalUnit;
    }

    /**
     * @return the unit stored with its name
     */
    public TimeUnit getNamedUnit()
    {
        return this.namedUnit;
    }

    /**
     * @param namedUnit the unit stored with its name
     */
    public void setNamedUnit(TimeUnit namedUnit)
    {
        this.namedUnit = namedUnit;
    }

    /**
     * @return the unit stored with its name, deduced from the JDBC type of the column
     */
    public TimeUnit getTypedUnit()
    {
        return this.typedUnit;
    }

    /**
     * @param typedUnit the unit stored with its name, deduced from the JDBC type of the column
     */
    public void setTypedUnit(TimeUnit typedUnit)
    {
        this.typedUnit = typedUnit;
    }
}
