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
 *
 */
package org.xwiki.security.internal;

import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;

import org.junit.Test;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.WikiReference;
import org.xwiki.security.AccessLevel;
import org.xwiki.security.Right;
import org.xwiki.security.RightCacheKey;
import org.xwiki.security.RightsObject;

import static java.util.Arrays.asList;
import static org.junit.Assert.assertTrue;
import static org.xwiki.security.Right.ADMIN;
import static org.xwiki.security.Right.DELETE;
import static org.xwiki.security.Right.PROGRAM;
import static org.xwiki.security.RightState.ALLOW;


public class DefaultRightResolverTest  extends AbstractTestCase
{
    @Test
    public void testRightResolver()
    {
        try {
            wiki.add(new MockDocument(docRefResolver.resolve("wikiY:SpaceX.DocY"), "wikiY:XWiki.UserY"));

            Right[] rights = {ADMIN, PROGRAM };
            DocumentReference[] groups = { uResolver.resolve("GroupX", new WikiReference("wikiY")), uResolver.resolve("SpaceY.GroupY") };
            DocumentReference[] users  = { uResolver.resolve("UserX", new WikiReference("wikiY")) };

            RightsObject o = new MockRightsObject(new HashSet<Right>(asList(rights)),
                                                  ALLOW,
                                                  new HashSet<DocumentReference>(asList(users)),
                                                  new HashSet<DocumentReference>(asList(groups)));

            final DocumentReference doc = docRefResolver.resolve("wikiY:SpaceX.DocY");
            DocumentReference userX = uResolver.resolve("UserX", new WikiReference("wikiY"));
            DocumentReference userY = uResolver.resolve("UserY", new WikiReference("wikiY"));
            
            RightCacheKey key = new RightCacheKey()
                { @Override public EntityReference getEntityReference() { return doc; }};

            List<Collection<RightsObject>> docLevel = new LinkedList<Collection<RightsObject>>();
            docLevel.add(new LinkedList<RightsObject>());
            docLevel.add(new LinkedList<RightsObject>());
            Collection<RightsObject> pageRights = new LinkedList<RightsObject>();
            pageRights.add(o);
            docLevel.add(pageRights);

            AccessLevel level = resolver.resolve(userX, doc, key, new LinkedList<DocumentReference>(), docLevel);
            assertTrue(level.equals(AccessLevel.DEFAULT_ACCESS_LEVEL));

            level = resolver.resolve(userY, doc, key, asList(groups), docLevel);
            AccessLevel delete = AccessLevel.DEFAULT_ACCESS_LEVEL.clone();
            delete.allow(DELETE);
            assertTrue(level.equals(delete));

            level = resolver.resolve(userY, doc, key, new LinkedList<DocumentReference>(), docLevel);
            assertTrue(level.equals(delete));

            List<Collection<RightsObject>> spaceLevel = new LinkedList<Collection<RightsObject>>();
            spaceLevel.add(new LinkedList<RightsObject>());
            Collection<RightsObject> spaceRights = new LinkedList<RightsObject>();
            spaceRights.add(o);
            spaceLevel.add(spaceRights);
            spaceLevel.add(new LinkedList<RightsObject>());
            
            
            level = resolver.resolve(userX, doc, key, new LinkedList<DocumentReference>(), spaceLevel);
            AccessLevel expected = AccessLevel.DEFAULT_ACCESS_LEVEL.clone();
            expected.allow(ADMIN);
            expected.allow(DELETE);
            assertTrue(level.equals(expected));

            level = resolver.resolve(userY, doc, key, asList(groups), spaceLevel);
            assertTrue(level.equals(expected));

            level = resolver.resolve(userY, doc, key, new LinkedList<DocumentReference>(), spaceLevel);
            assertTrue(level.equals(delete));

            List<Collection<RightsObject>> wikiLevel = new LinkedList<Collection<RightsObject>>();
            Collection<RightsObject> wikiRights = new LinkedList<RightsObject>();
            wikiRights.add(o);
            wikiLevel.add(wikiRights);
            wikiLevel.add(new LinkedList<RightsObject>());
            wikiLevel.add(new LinkedList<RightsObject>());
            
            level = resolver.resolve(userX, doc, key, new LinkedList<DocumentReference>(), wikiLevel);
            expected.allow(PROGRAM);
            assertTrue(level.equals(expected));

            level = resolver.resolve(userY, doc, key, asList(groups), wikiLevel);
            assertTrue(level.equals(expected));

            level = resolver.resolve(userY, doc, key, new LinkedList<DocumentReference>(), wikiLevel);
            assertTrue(level.equals(delete));
        } catch (Exception e) {
            LOG.error("Caught exception!", e);
            assert false;
        }
    }
}
