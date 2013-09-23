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

import org.xwiki.component.annotation.Role;

/**
 * Service that provides functionality for filtering a sequence of characters and producing an altered content from an
 * original content.
 * 
 * @version $Id$
 * @since 2.3M1
 */
@Role
public interface ContentAlterer
{
    /**
     * @param sequence the characters sequence to alter
     * @return the altered content resulted from altering the passed sequence
     */
    AlteredContent alter(CharSequence sequence);

    /**
     * Provides altering of an already altered content, such allowing for multiple alterers to be composed.
     * 
     * @param alteredContent the already altered content to apply filtering on
     * @return AlteredContent the composed altered content
     */
    AlteredContent alter(AlteredContent alteredContent);
}
