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
package org.xwiki.icon;

import java.io.Reader;

import org.xwiki.component.annotation.Role;
import org.xwiki.model.reference.DocumentReference;

/**
 * Component to load an IconSet.
 *
 * @since 6.2M1
 * @version $Id$
 */
@Role
public interface IconSetLoader
{
    /**
     * Load an icon set from a document in the wiki.
     * @param iconSetReference reference to the document holding the icon set
     * @return the loaded icon set
     * @throws IconException if problems occur
     */
    IconSet loadIconSet(DocumentReference iconSetReference) throws IconException;

    /**
     * Load an icon set from any string input.
     * @param input string that describes an icon set
     * @param name name of the icon set to load
     * @return the loaded icon set
     * @throws IconException if problems occur
     */
    IconSet loadIconSet(Reader input, String name) throws IconException;
}
