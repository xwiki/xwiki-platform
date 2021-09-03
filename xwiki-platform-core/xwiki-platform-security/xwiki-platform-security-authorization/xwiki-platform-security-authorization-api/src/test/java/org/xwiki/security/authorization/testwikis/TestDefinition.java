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

import org.xwiki.model.reference.WikiReference;

/**
 * Public interface of test entities representing complete test definition.
 *
 * @version $Id$
 * @since 5.0M2
 */
public interface TestDefinition extends TestEntity
{
    /**
     * Retrieve a test entity representing a wiki in this test definition.
     * @param name name of the wiki.
     * @return a test wiki entity if found, null otherwise.
     */
    TestWiki getWiki(String name);

    /**
     * Retrieve a test entity representing a wiki in this test definition.
     * @param reference a reference to the wiki.
     * @return a test wiki entity if found, null otherwise.
     */
    TestWiki getWiki(WikiReference reference);

    /**
     * @return a collection of all test entities representing wikis in this test definition.
     */
    Collection<TestWiki> getWikis();

    /**
     * @return a test wiki entity representing the main wiki in this test definition.
     */
    TestWiki getMainWiki();
}
