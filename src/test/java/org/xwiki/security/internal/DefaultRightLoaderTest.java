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

import static org.junit.Assert.*;
import org.junit.Test;
import org.junit.Before;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.States;

import org.xwiki.security.*;
import static org.xwiki.security.Right.*;
import static org.xwiki.security.RightState.*;

import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.EntityType;

import static java.util.Arrays.asList;
import java.util.List;
import static java.util.Collections.EMPTY_LIST;
import static java.util.Collections.EMPTY_SET;

public class DefaultRightLoaderTest extends AbstractTestCase
{
    @Test 
    public void testRightLoader()
    {
        DocumentReference userX = docRefResolver.resolve("wikiY:XWiki.userX");
        DocumentReference userY = docRefResolver.resolve("wikiY:XWiki.userY");
        DocumentReference userZ = docRefResolver.resolve("xwiki:XWiki.userZ");
        DocumentReference admin = docRefResolver.resolve("xwiki:XWiki.Admin");

        wiki.add(new MockDocument(userX, "xwiki:XWiki.Admin")
                 .allowLocal(asList(new Right[]{EDIT }),
                             asList(new String[]{"wikiY:XWiki.userX"}),
                             EMPTY_LIST ))
            .add(new MockDocument(userY, "xwiki:XWiki.Admin")
                 .allowLocal(asList(new Right[]{EDIT }),
                             asList(new String[]{"wikiY:XWiki.userY"}),
                             EMPTY_LIST ));

        try {
            mockery.checking(new Expectations() {{
                allowing(mockGroupService)
                    .getAllGroupsNamesForMember("wikiY:XWiki.userX", Integer.MAX_VALUE, 0, xwikiContext);
                will(returnValue(asList(new String[]{"XWiki.XWikiAllGroup"})));
                allowing(mockGroupService)
                    .getAllGroupsNamesForMember("wikiY:XWiki.userY", Integer.MAX_VALUE, 0, xwikiContext);
                will(returnValue(asList(new String[]{"XWiki.XWikiAllGroup"})));
            }});

            RightLoader loader = getComponentManager().lookup(RightLoader.class);
            RightCache  cache  = getComponentManager().lookup(RightCache.class);

            AccessLevel edit = AccessLevel.DEFAULT_ACCESS_LEVEL.clone();
            edit.allow(EDIT);

            AccessLevel level = loader.load(userX, userX);
            System.out.println("Level is " + level + ", expected " + edit);
            assertTrue(level.equals(edit));

            RightCacheEntry entry = cache.get(cache.getRightCacheKey(userX), cache.getRightCacheKey(userX));
            assertTrue(entry != null);
            assertTrue(entry.equals(level));

            entry = cache.get(cache.getRightCacheKey(userX));
            assertTrue(entry != null);
            assertTrue(entry.getType() == RightCacheEntry.Type.HAVE_OBJECTS);

            entry = cache.get(cache.getRightCacheKey(userX.getParent()));
            assertTrue(entry != null);
            assertTrue(entry.getType() == RightCacheEntry.Type.HAVE_NO_OBJECTS);

            entry = cache.get(cache.getRightCacheKey(userX.getParent().getParent()));
            assertTrue(entry != null);
            assertTrue(entry.getType() == RightCacheEntry.Type.HAVE_NO_OBJECTS);

            wiki.add(new MockDocument("wikiY:XWiki.WebPreferences", "xwiki:XWiki.Admin")
                     .denyGlobal(asList(new Right[]{COMMENT }),
                                 EMPTY_LIST,
                                 asList(new String[]{"XWiki.XWikiAllGroup"}))
                     );

            cache.remove(cache.getRightCacheKey(new EntityReference("xwiki", EntityType.WIKI, null)));

            entry = cache.get(cache.getRightCacheKey(userX), cache.getRightCacheKey(userX));
            assertTrue(entry == null);

            entry = cache.get(cache.getRightCacheKey(userX));
            assertTrue(entry == null);

            entry = cache.get(cache.getRightCacheKey(userX.getParent()));
            assertTrue(entry == null);

            entry = cache.get(cache.getRightCacheKey(userX.getParent().getParent()));
            assertTrue(entry == null);

            AccessLevel editNoComment = edit.clone();
            editNoComment.deny(COMMENT);
            level = loader.load(userX, userX);
            assertTrue(level.equals(editNoComment));

        } catch (Exception e) {
            LOG.error("Caught exception.", e);
            assert false;
        }
    }

}