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
package org.xwiki.xar.internal;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;

import org.xwiki.component.annotation.Role;

/**
 * Read and write object property value in XAR 1.x XML format.
 * 
 * @version $Id$
 * @since 5.4M1
 */
@Role
public interface XarObjectPropertySerializer
{
    /**
     * @param reader the current XML reader
     * @return the value converted from XML content
     * @throws XMLStreamException when failing to read XML content
     */
    Object read(XMLStreamReader reader) throws XMLStreamException;

    /**
     * @param writer the current XML writer
     * @param value the value to convert to XML
     * @throws XMLStreamException when failing to write ML content
     */
    void write(XMLStreamWriter writer, Object value) throws XMLStreamException;
}
