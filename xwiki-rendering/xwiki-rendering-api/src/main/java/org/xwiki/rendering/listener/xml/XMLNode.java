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
package org.xwiki.rendering.listener.xml;

/**
 * Represents a generic XML node which can be one of {@link XMLElement}, {@link XMLCData} or {@link XMLComment}.
 * 
 * @version $Id$
 * @since 1.7M2
 */
public class XMLNode implements Cloneable
{
    /**
     * @return the type of the XML node.
     * @see XMLNodeType
     */
    public XMLNodeType getNodeType()
    {
        return XMLNodeType.UNKNOWN;
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.lang.Object#clone()
     */
    @Override
    public XMLNode clone()
    {
        XMLNode clone;
        try {
            clone = (XMLNode) super.clone();
        } catch (CloneNotSupportedException e) {
            // Should never happen
            throw new RuntimeException("Failed to clone object", e);
        }
        return clone;
    }
}
