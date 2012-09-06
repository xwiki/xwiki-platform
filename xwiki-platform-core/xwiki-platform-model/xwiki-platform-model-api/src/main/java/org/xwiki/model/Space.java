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
package org.xwiki.model;

public interface Space extends Object, Extensible
{
    /**
     * @return the list of top level Space objects in this Space (excluding nested spaces)
     */
    EntityIterator<Space> getSpaces();

    /**
     * @param spaceName the name of the nested space to look for
     * @return the nested space whose name is passed as parameter
     */
    Space getSpace(String spaceName);

    /**
     * Add a nested space.
     */
    Space addSpace(String spaceName);

    /**
     * Remove a nested space.
     */
    void removeSpace(String spaceName);

    boolean hasSpace(String spaceName);

    EntityIterator<Document> getDocuments();

    boolean hasDocument(String documentName);

    Document getDocument(String documentName);

    Document addDocument(String documentName);

    void removeDocument(String documentName);
}
