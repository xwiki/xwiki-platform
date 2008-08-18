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
package org.xwiki.rendering.util;

/**
 * Id generator.
 * 
 * @version $Id: $
 */
public interface IdGenerator
{
    /**
     * This component's role, used when code needs to look it up.
     */
    String ROLE = IdGenerator.class.getName();

    /**
     * Randomly generate a unique id. The generated id complies with the XHTML specification. Extract from <a
     * href="http://www.devguru.com/technologies/xhtml/QuickRef/xhtml_attribute_id.html">DevGuru</a>:
     * <p>
     * <quote>
     * "The id attribute is used to assign a identifier value to a tag. Each id must be unique within the document 
     *   and each element can only have one id.
     *   In XHTML, the id attribute has essentially replaced the use of the name attribute. The value of the id must 
     *   start with an alphabetic letter or an underscore. The rest of the value can contain any alpha/numeric 
     *   character."
     * </quote></p> 
     * 
     * @return the unique id
     */
    String generateRandomUniqueId();
}
