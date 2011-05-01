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
package org.xwiki.users;

import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReferenceSerializer;

/**
 * Base class for implementing users.
 * 
 * @version $Id$
 * @since 3.1M2
 */
public abstract class AbstractUser implements User
{
    /** A link to the user profile document. */
    protected DocumentReference profileReference;

    /** Used for serializing the profile reference into a string. */
    protected EntityReferenceSerializer<String> serializer;

    /**
     * {@inheritDoc}
     * 
     * @see User#getId()
     */
    public String getId()
    {
        return (this.profileReference == null) ? "" : this.serializer.serialize(this.profileReference);
    }

    /**
     * {@inheritDoc}
     * 
     * @see User#getUsername()
     */
    public String getUsername()
    {
        return (this.profileReference == null) ? "" : this.profileReference.getName();
    }

    /**
     * {@inheritDoc}
     * 
     * @see User#getProfileDocument()
     */
    public DocumentReference getProfileDocument()
    {
        return this.profileReference;
    }

    /**
     * {@inheritDoc}
     * 
     * @see Object#toString()
     */
    @Override
    public String toString()
    {
        return getName();
    }

    /**
     * {@inheritDoc}
     * 
     * @see Comparable#compareTo(Object)
     */
    public int compareTo(User user)
    {
        return getName().compareTo(user.getName());
    }
}
