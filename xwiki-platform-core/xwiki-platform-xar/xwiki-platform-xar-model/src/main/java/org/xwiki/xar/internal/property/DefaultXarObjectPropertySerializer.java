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
package org.xwiki.xar.internal.property;

import javax.inject.Singleton;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;

import org.xwiki.component.annotation.Component;
import org.xwiki.xar.internal.XarObjectPropertySerializer;

/**
 * Default implementation of {@link XarObjectPropertySerializer}.
 * <p>
 * Assume value is {@link String}.
 * 
 * @version $Id$
 * @since 5.4M1
 */
@Component
@Singleton
public class DefaultXarObjectPropertySerializer implements XarObjectPropertySerializer
{
    @Override
    public Object read(XMLStreamReader reader) throws XMLStreamException
    {
        return reader.getElementText();
    }

    @Override
    public void write(XMLStreamWriter writer, Object value) throws XMLStreamException
    {
        if (value != null) {
            writer.writeCharacters(value.toString());
        }
    }
}
