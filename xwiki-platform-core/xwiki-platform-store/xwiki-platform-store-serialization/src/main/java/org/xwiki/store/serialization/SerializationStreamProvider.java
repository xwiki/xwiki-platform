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

import org.xwiki.store.StreamProvider;

/**
 * A stream provider which provides a stream from serializing an object.
 *
 * @param <R> The class of object which the serializer can serialize (what it requires).
 * @version $Id$
 * @since 3.0M3
 */
public class SerializationStreamProvider<R> implements StreamProvider
{
    /**
     * The serializer for converting the list of attachments into a stream of metadata.
     */
    private final Serializer<R, ?> serializer;

    /**
     * The list of attachments to get the stream of metadata from.
     */
    private final R toSerialize;

    /**
     * The Constructor.
     *
     * @param serializer the serializer for converting the object into a stream of data.
     * @param toSerialize the object to serialize.
     */
    public SerializationStreamProvider(final Serializer<R, ?> serializer,
        final R toSerialize)
    {
        this.serializer = serializer;
        this.toSerialize = toSerialize;
    }

    @Override
    public InputStream getStream() throws IOException
    {
        return this.serializer.serialize(this.toSerialize);
    }
}
