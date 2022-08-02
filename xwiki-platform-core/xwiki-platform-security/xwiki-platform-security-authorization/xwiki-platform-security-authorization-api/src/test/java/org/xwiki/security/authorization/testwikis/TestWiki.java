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
import org.xwiki.model.reference.WikiReference;

/**
 * Public interface of test entities representing wikis.
 *
 * @version $Id$
 * @since 5.0M2
 */
public interface TestWiki extends SecureTestEntity
{
    /**
     * @return the reference to the wiki represented by this test entity.
     */
    WikiReference getWikiReference();

    /**
     * @return a reference to the owner of the wiki represented by this test entity. May be null.
     */
    DocumentReference getOwner();

    /**
     * @return true if the wiki represented by this test entity is the main wiki.
     */
    boolean isMainWiki();

    /**
     * Retrieve a test entity representing a space in this wiki.
     * @param name name of the space.
     * @return a test space entity if found, null otherwise.
     */
    TestSpace getSpace(String name);

    /**
     * Retrieve a test entity representing a space in this wiki.
     * @param reference reference to the space.
     * @return a test space entity if found, null otherwise.
     */
    TestSpace getSpace(SpaceReference reference);

    /**
     * @return a collection of all test entities representing spaces in this wiki.
     */
    Collection<TestSpace> getSpaces();

    /**
     * @return the alternate description for this test document.
     */
    String getDescription();

    /**
     * Retrieve a test entities representing a user in this wiki. Note that groups are users as well.
     * @param name name of the user.
     * @return a test user document entity if found, null otherwise.
     */
    TestUserDocument getUser(String name);

    /**
     * Retrieve a test entities representing a group in this wiki.
     * @param name name of the group.
     * @return a test group document entity if found, null otherwise.
     */
    TestGroupDocument getGroup(String name);

    /**
     * @return a collection of all test entities representing users in this wiki. Note that groups are users as well.
     */
    Collection<TestUserDocument> getUsers();

    /**
     * @return a collection of all test entities representing groups in this wiki.
     */
    Collection<TestGroupDocument> getGroups();
}
