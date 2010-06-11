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

import junit.framework.TestCase;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.States;

import static org.junit.Assert.*;
import org.junit.Test;
import org.junit.Before;

import org.xwiki.security.*;
import static org.xwiki.security.Right.*;
import static org.xwiki.security.RightState.*;

import java.util.List;
import java.util.LinkedList;
import java.util.HashSet;
import java.util.Collection;
import static java.util.Arrays.asList;

import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReference;


public class DefaultRightResolverTest  extends AbstractTestCase
{
    @Test
    public void testRightResolver()
    {
        try {
            wiki.add(new MockDocument(docRefResolver.resolve("wikiY:SpaceX.DocY"), "wikiX:XWiki.UserY"));

            Right[] rights = {ADMIN, PROGRAM };
            DocumentReference[] groups = { uResolver.resolve("GroupX", "wikiX"), uResolver.resolve("SpaceY.GroupY") };
            DocumentReference[] users  = { uResolver.resolve("UserX", "wikiX") };

            RightsObject o = new MockRightsObject(new HashSet(asList(rights)),
                                                  ALLOW,
                                                  new HashSet(asList(users)),
                                                  new HashSet(asList(groups)));

            final DocumentReference doc = docRefResolver.resolve("wikiY:SpaceX.DocY");
            DocumentReference userX = uResolver.resolve("UserX", "wikiX");
            DocumentReference userY = uResolver.resolve("UserY", "wikiX");
            
            RightCacheKey key = new RightCacheKey() { public EntityReference getEntityReference() { return doc; }};

            List<Collection<RightsObject>> docLevel = new LinkedList();
            docLevel.add(new LinkedList());
            docLevel.add(new LinkedList());
            Collection<RightsObject> pageRights = new LinkedList();
            pageRights.add(o);
            docLevel.add(pageRights);

            AccessLevel level = resolver.resolve(userX, doc, key, new LinkedList(), docLevel);
            assertTrue(level.equals(AccessLevel.DEFAULT_ACCESS_LEVEL));

            level = resolver.resolve(userY, doc, key, asList(groups), docLevel);
            AccessLevel delete = AccessLevel.DEFAULT_ACCESS_LEVEL.clone();
            delete.allow(DELETE);
            assertTrue(level.equals(delete));

            level = resolver.resolve(userY, doc, key, new LinkedList(), docLevel);
            assertTrue(level.equals(delete));

            List<Collection<RightsObject>> spaceLevel = new LinkedList();
            spaceLevel.add(new LinkedList());
            Collection<RightsObject> spaceRights = new LinkedList();
            spaceRights.add(o);
            spaceLevel.add(spaceRights);
            spaceLevel.add(new LinkedList());
            
            
            level = resolver.resolve(userX, doc, key, new LinkedList(), spaceLevel);
            AccessLevel expected = AccessLevel.DEFAULT_ACCESS_LEVEL.clone();
            expected.allow(ADMIN);
            expected.allow(DELETE);
            assertTrue(level.equals(expected));

            level = resolver.resolve(userY, doc, key, asList(groups), spaceLevel);
            assertTrue(level.equals(expected));

            level = resolver.resolve(userY, doc, key, new LinkedList(), spaceLevel);
            assertTrue(level.equals(delete));

            List<Collection<RightsObject>> wikiLevel = new LinkedList();
            Collection<RightsObject> wikiRights = new LinkedList();
            wikiRights.add(o);
            wikiLevel.add(wikiRights);
            wikiLevel.add(new LinkedList());
            wikiLevel.add(new LinkedList());
            
            level = resolver.resolve(userX, doc, key, new LinkedList(), wikiLevel);
            expected.allow(PROGRAM);
            assertTrue(level.equals(expected));

            level = resolver.resolve(userY, doc, key, asList(groups), wikiLevel);
            assertTrue(level.equals(expected));

            level = resolver.resolve(userY, doc, key, new LinkedList(), wikiLevel);
            assertTrue(level.equals(delete));
        } catch (Exception e) {
            LOG.error("Caught exception!", e);
            assert false;
        }
    }
}