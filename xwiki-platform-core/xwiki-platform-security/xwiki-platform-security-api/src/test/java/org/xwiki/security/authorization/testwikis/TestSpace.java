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

package org.xwiki.security.authorization.testwikis;

import java.util.Collection;

import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.SpaceReference;

/**
 * Public interface of test entities representing spaces.
 *
 * @version $Id$
 * @since 5.0M2
 */
public interface TestSpace extends SecureTestEntity
{
    /**
     * @return a reference to the space represented by this test entity.
     */
    SpaceReference getSpaceReference();

    /**
     * Retrieve a test entity representing a document in this space.
     * @param name name of the document.
     * @return a test document entity if found, null otherwise.
     */
    TestDocument getDocument(String name);

    /**
     * Retrieve a test entity representing a document in this space.
     * @param reference reference to a document.
     * @return a test document entity if found, null otherwise.
     */
    TestDocument getDocument(DocumentReference reference);

    /**
     * @return a collection of all test entities representing documents in this space.
     */
    Collection<TestDocument> getDocuments();

    /**
     * @return the alternate description for this test document.
     */
    String getDescription();
}
