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
package com.xpn.xwiki.internal.context;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.ObjectStreamException;
import java.io.Serializable;

import javax.servlet.http.HttpSession;

/**
 * This class is used to wrap an {@link HttpSession} object in order to make it serializable. The HTTP session is
 * serialized as the session id. Deserialization is not supported. This is used to save the HTTP session in the
 * {@code ContextStore} and restore it before a job is executed. The serialization (session id) also ends up in the keys
 * used by the {@code AsyncRendererCache}, which is invalidated when the session is destroyed.
 * 
 * @version $Id$
 * @since 14.10.18
 * @since 15.5.3
 * @since 15.9RC1
 */
public class SerializableHttpSessionWrapper implements Serializable
{
    private final HttpSession session;

    /**
     * Wraps the given HTTP session.
     * 
     * @param session the HTTP session to wrap
     */
    public SerializableHttpSessionWrapper(HttpSession session)
    {
        this.session = session;
    }

    /**
     * @return the wrapped HTTP session
     */
    public HttpSession getSession()
    {
        return session;
    }

    private void writeObject(ObjectOutputStream out) throws IOException
    {
        // Serialize only the session id.
        out.writeObject(session.getId());
    }

    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException
    {
        readObjectNoData();
    }

    private void readObjectNoData() throws ObjectStreamException
    {
        throw new UnsupportedOperationException("Deserialization of the HTTP session is not supported.");
    }
}
