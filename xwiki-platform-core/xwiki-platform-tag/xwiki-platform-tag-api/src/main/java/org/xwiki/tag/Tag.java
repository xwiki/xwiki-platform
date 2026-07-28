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
package org.xwiki.tag;

import java.io.Serializable;
import java.util.Objects;

import org.xwiki.stability.Unstable;

/**
 * Represents a document tag, identified by its name.
 *
 * @version $Id$
 * @since 18.7RC1
 */
@Unstable
public class Tag implements Serializable
{
    private static final long serialVersionUID = 1L;

    private final String name;

    /**
     * @param name the name of the tag
     */
    public Tag(String name)
    {
        this.name = name;
    }

    /**
     * @return the name of the tag
     */
    public String getName()
    {
        return this.name;
    }

    @Override
    public boolean equals(Object obj)
    {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof Tag)) {
            return false;
        }
        return Objects.equals(this.name, ((Tag) obj).name);
    }

    @Override
    public int hashCode()
    {
        return Objects.hashCode(this.name);
    }

    @Override
    public String toString()
    {
        return this.name;
    }
}
