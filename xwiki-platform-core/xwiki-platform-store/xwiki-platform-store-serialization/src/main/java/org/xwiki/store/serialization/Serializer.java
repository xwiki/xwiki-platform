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
package org.xwiki.store.serialization;

import java.io.IOException;
import java.io.InputStream;

import org.xwiki.component.annotation.ComponentRole;

/**
 * A generic thing for converting objects of a known type into InputStreams and back again.
 *
 * @param <R> The class of object which the serializer can serialize (what it requires).
 * @param <P> The class of object which will be provided by this serializer when it parses data.
 * @version $Id$
 * @since 3.0M2
 */
// Note: We cannot replace @ComponentRole with @Role ATM since @Role supports generics and we have
// Serializer<R, P extends R>. Changing it will thus break all code looking up components implementing this role.
@ComponentRole
public interface Serializer<R, P extends R>
{
    /**
     * Parse an InputStream and create a new object.
     *
     * @param stream an InputStream to parse.
     * @return a new object made by parsing the stream.
     * @throws IOException if the InputStream does not contain the type of object
     * handled by this serializer, the object was serialized with
     * a different Serializer, or something goes wrong along the way.
     */
    P parse(InputStream stream) throws IOException;

    /**
     * Serialize the given object and return an InputStream.
     *
     * @param object the thing to serialize.
     * @return an InputStream which can be used to create a new object using parse().
     * @throws IOException if something goes wrong while serializing.
     */
    InputStream serialize(R object) throws IOException;
}
