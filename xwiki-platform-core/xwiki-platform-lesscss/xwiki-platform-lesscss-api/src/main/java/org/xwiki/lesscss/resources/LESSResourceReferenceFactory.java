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
package org.xwiki.lesscss.resources;

import org.xwiki.component.annotation.Role;
import org.xwiki.model.reference.ObjectPropertyReference;

/**
 * Create references for LESS resources.
 *  
 * @version $Id$
 * @since 7.0RC1
 */
@Role
public interface LESSResourceReferenceFactory
{
    /**
     * Create a LESSResourceReference pointing to a skin file. 
     * @param fileName name of the skin file
     * @return a LESSResourceReference pointing to the skin file.
     */
    LESSResourceReference createReferenceForSkinFile(String fileName);

    /**
     * Create a LESSResourceReference pointing to an XObject property. 
     * @param objectPropertyReference reference to an XObject property
     * @return a LESSResourceReference pointing to the XObject property
     */
    LESSResourceReference createReferenceForXObjectProperty(ObjectPropertyReference objectPropertyReference);
}
