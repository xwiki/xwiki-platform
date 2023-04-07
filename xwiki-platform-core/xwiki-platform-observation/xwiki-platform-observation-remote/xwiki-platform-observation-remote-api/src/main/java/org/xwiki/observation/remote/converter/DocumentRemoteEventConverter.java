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
package org.xwiki.observation.remote.converter;

import java.io.Serializable;
import java.util.Hashtable;

import org.xwiki.component.annotation.Role;
import org.xwiki.bridge.DocumentModelBridge;
import org.xwiki.stability.Unstable;

/**
 * Component helper for serializing standard XWiki elements such as {@code XWikiDocument} or {@code XWikiContext}.
 *
 * @version $Id$
 * @since 15.3RC1
 */
@Unstable
@Role
public interface DocumentRemoteEventConverter
{
    /**
     * Allow to serialize an {@code XWikiContext}. The argument should be an actual instance of
     * {@code com.xpn.xwiki.XWikiContext}.
     *
     * @param context the context to be serialized
     * @return a {@link Serializable} object to be used in a remote event
     */
    Serializable serializeXWikiContext(Hashtable<Object, Object> context);

    /**
     * Allow to unserialize an {@code XWikiContext}. The argument should be a serializable created by
     * {@link #serializeXWikiContext(Hashtable)}. The result will always be an instance of
     * {@code com.xpn.xwiki.XWikiContext}.
     *
     * @param remoteData the serialized data to be deserialized
     * @return an instance of {@code com.xpn.xwiki.XWikiContext}
     * @param <T> com.xpn.xwiki.XWikiContext
     * @throws RemoteEventConverterException in case of problem to perform the deserialization
     */
    <T extends Hashtable<Object, Object>> T unserializeXWikiContext(Serializable remoteData)
        throws RemoteEventConverterException;

    /**
     * Allow to serialize a document to be sent in a remote event.
     *
     * @param document the document to be serialized
     * @return a {@link Serializable} object to be used in a remote event
     */
    Serializable serializeDocument(DocumentModelBridge document);

    /**
     * Unserialize the given data representing a document. The argument should be a serialized object produced by
     * {@link #serializeDocument(DocumentModelBridge)}. Note that this method is looking for a document stored in
     * database, for a document that has been deleted see {@link #unserializeDeletedDocument(Serializable, Hashtable)}.
     *
     * @param remoteData the data to be deserialized
     * @return an instance of {@code com.xpn.xwiki.doc.XWikiDocument}
     * @throws RemoteEventConverterException in case of problem during the deserialization
     */
    DocumentModelBridge unserializeDocument(Serializable remoteData) throws RemoteEventConverterException;

    /**
     * Perform a deserialization of a deleted document. This method will look in the recycle bin to retrieve the
     * document. If it fails to find it, it will still return an empty document.
     *
     * @param remoteData the data of the deleted document produced by {@link #serializeDocument(DocumentModelBridge)}
     * @param context the {@code com.xpn.xwiki.XWikiContext} to use for retrieving the document
     * @return an instance of {@code com.xpn.xwiki.doc.XWikiDocument}
     * @param <T> {@code com.xpn.xwiki.XWikiContext}
     * @throws RemoteEventConverterException in case of problem during the deserialization
     */
    <T extends Hashtable<Object, Object>> DocumentModelBridge unserializeDeletedDocument(Serializable remoteData,
        T context) throws RemoteEventConverterException;
}
