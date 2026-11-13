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
package org.xwiki.user.group;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.xwiki.model.reference.DocumentReference;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for the default methods of {@link GroupManager}.
 *
 * @version $Id$
 */
class GroupManagerTest
{
    private static final DocumentReference USER = new DocumentReference("xwiki", "XWiki", "user");

    private static final DocumentReference GLOBAL_GROUP = new DocumentReference("xwiki", "XWiki", "globalGroup");

    private static final DocumentReference SUBWIKI_GROUP = new DocumentReference("subwiki", "XWiki", "subwikiGroup");

    /**
     * A {@link GroupManager} backed by a fixed membership graph (USER is in GLOBAL_GROUP, which is in SUBWIKI_GROUP),
     * which only returns the groups located in the searched wikis, like the real implementation.
     */
    private final GroupManager manager = new GroupManager()
    {
        private final Map<DocumentReference, List<DocumentReference>> directGroups =
            Map.of(USER, List.of(GLOBAL_GROUP), GLOBAL_GROUP, List.of(SUBWIKI_GROUP));

        @Override
        public Collection<DocumentReference> getGroups(DocumentReference member, Object wikiTarget, boolean recurse)
        {
            Collection<?> wikis = wikiTarget instanceof Collection<?> collection ? collection : List.of(wikiTarget);
            Set<DocumentReference> groups = new LinkedHashSet<>();
            for (DocumentReference group : this.directGroups.getOrDefault(member, List.of())) {
                if (wikis.contains(group.getWikiReference())) {
                    groups.add(group);
                    if (recurse) {
                        groups.addAll(getGroups(group, wikiTarget, true));
                    }
                }
            }
            return groups;
        }

        @Override
        public Collection<DocumentReference> getMembers(DocumentReference group, boolean recurse)
        {
            throw new UnsupportedOperationException("isMember must not materialize the members of the group");
        }
    };

    @Test
    void isMember() throws GroupException
    {
        assertTrue(this.manager.isMember(USER, GLOBAL_GROUP, false));
        assertTrue(this.manager.isMember(USER, GLOBAL_GROUP, true));
        assertTrue(this.manager.isMember(GLOBAL_GROUP, SUBWIKI_GROUP, false));
        assertFalse(this.manager.isMember(GLOBAL_GROUP, USER, true));
    }

    @Test
    void isMemberThroughGroupOfAnotherWiki() throws GroupException
    {
        // USER is in SUBWIKI_GROUP only through GLOBAL_GROUP, which lives in USER's wiki, not in SUBWIKI_GROUP's wiki.
        assertFalse(this.manager.isMember(USER, SUBWIKI_GROUP, false));
        assertTrue(this.manager.isMember(USER, SUBWIKI_GROUP, true));
    }

    @Test
    void isMemberWithGuestOrNoGroup() throws GroupException
    {
        assertFalse(this.manager.isMember(null, GLOBAL_GROUP, true));
        assertFalse(this.manager.isMember(USER, null, true));
    }

    @Test
    void isMemberIgnoresLocale() throws GroupException
    {
        assertTrue(this.manager.isMember(new DocumentReference(USER, Locale.FRENCH),
            new DocumentReference(GLOBAL_GROUP, Locale.FRENCH), true));
    }
}
