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
package org.xwiki.store.serialization.xml;

import java.io.IOException;

import org.dom4j.Element;
import org.xwiki.component.annotation.ComponentRole;
import org.xwiki.store.serialization.Serializer;
import org.xwiki.store.serialization.xml.internal.XMLWriter;

/**
 * A Serializer which converts objects into XML Elements and back.
 *
 * @param <R> The class of object which the serializer can serialize (what it requires).
 * @param <P> The class of object which will be provided by this serializer when it parses data.
 * @version $Id$
 * @since 3.0M2
 */
// Note: We cannot replace @ComponentRole with @Role ATM since @Role supports generics and we have
// XMLSerializer<R, P extends R>. Changing it will thus break all code looking up components implementing this role.
@ComponentRole
public interface XMLSerializer<R, P extends R> extends Serializer<R, P>
{
    /**
     * Deserialize from an XML Element.
     *
     * @param xmlElement the root element of a serialized object.
     * @return a new object made by deserializing the XML Element.
     * @throws IOException if something goes wrong.
     */
    P parse(Element xmlElement) throws IOException;

    /**
     * Serialize to an XMLWriter.
     *
     * @param object the object to serialize.
     * @param writeTo write output to this.
     * @throws IOException if something goes wrong.
     */
    void serialize(R object, XMLWriter writeTo) throws IOException;
}
