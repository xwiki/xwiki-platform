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

package org.xwiki.security.authorization.testwikis.internal.parser;

import java.util.List;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

/**
 * Interface to be implemented by factories for the XML parser.
 *
 * @version $Id$
 * @since 5.0M2
 */
public interface EntityFactory
{
    /**
     * @return list of tags handled by the factory.
     */
    List<String> getTagNames();

    /**
     * Called by the parser at the start of a matching element.
     * @param parser the ElementParser calling for action.
     * @param name of the tag currently starting
     * @param attributes Attributes of this tags.
     * @throws SAXException on error.
     */
    void newElement(ElementParser parser, String name, Attributes attributes) throws SAXException;
}

