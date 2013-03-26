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

package org.xwiki.security.authorization.testwikis.internal.entities;

import java.util.Collection;

import org.xwiki.model.EntityType;
import org.xwiki.model.reference.WikiReference;
import org.xwiki.security.authorization.testwikis.TestWiki;
import org.xwiki.security.authorization.testwikis.TestDefinition;

/**
 * Entity representing the root of the hierarchy, and containing wikis.
 *
 * @version $Id$
 * @since 5.0M2
 */
public class DefaultTestDefinition extends AbstractTestEntity implements TestDefinition
{
    /** Entity representing the main wiki. */
    private TestWiki mainWiki;

    /**
     * Set a given wiki as the main wiki.
     * @param wiki wiki that should be taken for the main wiki.
     */
    void setMainWiki(TestWiki wiki)
    {
        mainWiki = wiki;
    }

    @Override
    public EntityType getType()
    {
        return null;
    }

    @Override
    public TestWiki getWiki(String name)
    {
        return getWiki(new WikiReference(name));
    }

    @Override
    public TestWiki getWiki(WikiReference reference)
    {
        return (TestWiki) getEntity(reference);
    }

    @Override
    public Collection<TestWiki> getWikis()
    {
        return TypeFilteredCollection.getNewInstance(getEntities(), TestWiki.class);
    }

    @Override
    public TestWiki getMainWiki()
    {
        return mainWiki;
    }
}
