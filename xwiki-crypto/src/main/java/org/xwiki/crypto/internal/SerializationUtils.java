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
package org.xwiki.crypto.internal;

import java.io.ByteArrayOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ObjectOutputStream;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.io.IOException;


/**
 * Utilities for serialization and deserialization.
 *
 * @since 2.5M1
 * @version $Id$
 */
public final class SerializationUtils
{
    /** Private Constructor since this is a utility class. */
    private SerializationUtils()
    {
    }

    /**
     * This method will accept a serialized version of any object defined in the system.
     *
     * @param serialized the byte array to create the Object from.
     * @return an Object made from deserializing the given array.
     * @throws IOException if something goes wrong in the serialization framework.
     * @throws ClassNotFoundException if the required Object class is not present on the system.
     */
    public static Object deserialize(final byte[] serialized)
        throws IOException,
               ClassNotFoundException
    {
        final ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(serialized));
        return ois.readObject();
    }

    /**
     * Convert the given Object to a byte array which when passed to deserialize() will make the same Object.
     *
     * @param <T> a serializable class.
     * @param toSerialize the Object to convert into a byte array.
     * @return the given Object as a byte array.
     * @throws IOException if something goes wrong in the serialization framework.
     */
    public static <T extends Serializable> byte[] serialize(T toSerialize) throws IOException
    {
        final ByteArrayOutputStream out = new ByteArrayOutputStream(128);
        final ObjectOutputStream oos = new ObjectOutputStream(out);
        oos.writeObject(toSerialize);
        oos.flush();
        return out.toByteArray();
    }
}
