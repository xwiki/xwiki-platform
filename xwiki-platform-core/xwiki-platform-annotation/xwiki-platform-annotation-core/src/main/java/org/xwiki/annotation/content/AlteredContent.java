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

package org.xwiki.annotation.content;

/**
 * This class models an altered content, by providing the translations of offsets from the original content to the
 * altered content.
 *
 * @version $Id$
 * @since 2.3M1
 */
public interface AlteredContent
{
    /**
     * @param i is altered offset to map
     * @return initial offset corresponding to specified altered offset
     */
    int getInitialOffset(int i);

    /**
     * @param i is initial offset to map
     * @return altered offset corresponding to specified initial offset
     */
    int getAlteredOffset(int i);

    /**
     * @return the char sequence representing the altered content
     */
    CharSequence getContent();

    /**
     * @return initial length of content
     */
    int getInitialLength();
}
