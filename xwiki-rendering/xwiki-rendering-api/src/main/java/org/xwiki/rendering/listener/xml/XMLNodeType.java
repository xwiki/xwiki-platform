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
 * Defines what is the content of an {@link XWMLNode} instance.
 * 
 * @version $Id$
 * @since 1.7M2
 */
public enum XMLNodeType
{
    /**
     * The XML node is an element.
     */
    ELEMENT,

    /**
     * The XML node is a comment.
     */
    COMMENT,

    /**
     * The XML node is a CDATA verbatim text.
     */
    CDATA,

    /**
     * The XML node is none of the known types.
     */
    UNKNOWN;
}
