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
import java.util.Locale;
import java.util.Objects;

import org.xwiki.stability.Unstable;

/**
 * Represents a document tag, identified by its name. Tags are compared ignoring case, like the tag cloud and the tag
 * notification filter do, but the name is kept as given.
 *
 * @param name the name of the tag
 * @version $Id$
 * @since 18.10.0RC1
 */
@Unstable
public record Tag(String name) implements Serializable
{
    /**
     * @return the name of the tag, so that a tag is displayed as its name
     */
    @Override
    public String toString()
    {
        return this.name;
    }

    @Override
    public boolean equals(Object object)
    {
        return object instanceof Tag(String otherName) && Objects.equals(normalize(this.name), normalize(otherName));
    }

    @Override
    public int hashCode()
    {
        return Objects.hashCode(normalize(this.name));
    }

    private static String normalize(String name)
    {
        return name == null ? null : name.toLowerCase(Locale.ROOT);
    }
}
