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
 *
 */
package org.xwiki.platform.patchservice.api;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.xpn.xwiki.XWikiException;

/**
 * Patch components can be serialized into XML documents, using the W3C DOM specification.
 * 
 * @version $Id$
 * @since XWikiPlatform 1.3
 */
public interface XmlSerializable
{
    /**
     * Serialize this object as a DOM Element that can be inserted in the given DOM Document.
     * 
     * @param doc A DOM Document used for generating a compatible Element. The document is not changed, as the
     *            constructed Element is just returned, not inserted in the document.
     * @return The object exported as a DOM Element.
     * @throws XWikiException If the object is not well defined.
     */
    Element toXml(Document doc) throws XWikiException;

    /**
     * Load the object from an XML.
     * 
     * @param e A DOM Element defining the object.
     * @throws XWikiException If the provided element is not a valid (or compatible) exported object.
     */
    void fromXml(Element e) throws XWikiException;
}
