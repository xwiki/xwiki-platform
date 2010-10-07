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
package org.xwiki.rendering.listener;

/**
 * Represents a reference to an image. Note that this representation is independent of any wiki syntax.
 * 
 * @version $Id$
 * @since 1.7M3
 */
public interface Image extends Cloneable
{
    /**
     * @return the image type (image located in a Document or image located at a URL)
     */
    ImageType getType();

    /**
     * @return the reference to where the image is located. The format used is independent of the Rendering module.
     *         For XWiki the syntax is of the type {@code wiki:page.space@filename}.
     *
     * Note that the reason we store the reference as a String and not as an Entity Reference is because we want
     * the Rendering module independent of the XWiki Model so that it can be used independently of XWiki.
     */
    String getReference();

    /**
     * @return a copy of the image
     */
    Image clone();
}
