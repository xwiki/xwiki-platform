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
package org.xwiki.user.internal.group;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.xwiki.cache.CacheException;
import org.xwiki.cache.CacheManager;
import org.xwiki.cache.internal.MapCache;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.WikiReference;
import org.xwiki.test.annotation.BeforeComponent;
import org.xwiki.test.annotation.ComponentList;
import org.xwiki.test.junit5.mockito.InjectComponentManager;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;
import org.xwiki.test.mockito.MockitoComponentManager;
import org.xwiki.user.group.GroupException;
import org.xwiki.user.group.WikiTarget;
import org.xwiki.wiki.descriptor.WikiDescriptorManager;
import org.xwiki.wiki.manager.WikiManagerException;

import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.test.MockitoOldcore;
import com.xpn.xwiki.test.junit5.mockito.InjectMockitoOldcore;
import com.xpn.xwiki.test.junit5.mockito.OldcoreTest;
import com.xpn.xwiki.test.reference.ReferenceComponentList;
import com.xpn.xwiki.user.api.XWikiGroupService;

import static com.xpn.xwiki.test.mockito.OldcoreMatchers.isContextWiki;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.when;

/**
 * Validate {@link DefaultGroupManager}.
 * 
 * @version $Id$
 */
@OldcoreTest
@ComponentList(value = { MemberGroupsCache.class, GroupMembersCache.class, WikiGroupCache.class })
@ReferenceComponentList
class DefaultGroupManagerTest
{
    private static final DocumentReference GLOBAL_USER_1 = new DocumentReference("xwiki", "XWiki", "user1");

    private static final DocumentReference GLOBAL_GROUP_1 = new DocumentReference("xwiki", "XWiki", "group1");

    private static final DocumentReference GLOBAL_GROUP_2 = new DocumentReference("xwiki", "XWiki", "group2");

    private static final List<String> GLOBAL_GROUPS = List.of("XWiki.group1", "XWiki.group2");

    private static final DocumentReference WIKI_GROUP_1 = new DocumentReference("wiki", "XWiki", "group1");

    private static final DocumentReference WIKI_GROUP_2 = new DocumentReference("wiki", "XWiki", "group2");

    private static final List<String> WIKI_GROUPS = List.of("XWiki.group1", "XWiki.group2");

    @InjectMockitoOldcore
    private MockitoOldcore oldcore;

    @MockComponent
    private CacheManager cacheManager;

    @InjectMockComponents
    private DefaultGroupManager manager;

    @InjectComponentManager
    private MockitoComponentManager componentManager;

    private WikiGroupCache wikiGroupCache;

    private GroupMembersCache groupMembersCache;

    private XWikiGroupService groupService;

    private Set<String> wikis = new LinkedHashSet<>();

    @BeforeComponent
    public void beforeComponent() throws CacheException
    {
        when(this.cacheManager.createNewCache(any())).then(invocation -> {
            return new MapCache<>();
        });
    }

    @BeforeEach
    void beforeEach() throws WikiManagerException, ComponentLookupException
    {
        this.wikis.add(this.oldcore.getXWikiContext().getWikiId());

        this.groupService = this.oldcore.getMockGroupService();

        WikiDescriptorManager wikiManager = this.oldcore.getWikiDescriptorManager();

        when(wikiManager.getAllIds()).thenAnswer((invocation) -> {
            return wikis;
        });
        when(wikiManager.getCurrentWikiId()).thenAnswer((invocation) -> {
            return oldcore.getXWikiContext().getWikiId();
        });

        this.wikiGroupCache = this.componentManager.getInstance(WikiGroupCache.class);
        this.groupMembersCache = this.componentManager.getInstance(GroupMembersCache.class);

        mockGroups(GLOBAL_GROUP_1.getWikiReference(), GLOBAL_GROUPS);
        mockGroups(WIKI_GROUP_1.getWikiReference(), WIKI_GROUPS);
    }

    private void mockGroups(WikiReference wiki, List<String> groups)
    {
        this.wikis.add(wiki.getName());
        try {
            when(this.groupService.getAllMatchedGroups(isNull(), eq(false), eq(0), eq(0), isNull(),
                isContextWiki(wiki.getName()))).thenReturn((List) groups);
        } catch (XWikiException e) {
            // Cannot happen
        }
    }

    private void mockGroups(String wiki, DocumentReference user, List<DocumentReference> groups)
    {
        this.wikis.add(wiki);
        try {
            when(this.groupService.getAllGroupsReferencesForMember(same(user), anyInt(), anyInt(), isContextWiki(wiki)))
                .thenReturn(groups);
        } catch (XWikiException e) {
            // Cannot happen
        }
    }

    private void assertGetGroups(DocumentReference expected, DocumentReference reference, Object wikiTarget,
        boolean recurse) throws GroupException
    {
        assertGetGroups(Arrays.asList(expected), reference, wikiTarget, recurse);
    }

    private void assertGetGroupsEmpty(DocumentReference reference, Object wikiTarget, boolean recurse)
        throws GroupException
    {
        assertGetGroups(Collections.emptyList(), reference, wikiTarget, recurse);
    }

    private void assertGetGroups(Collection<DocumentReference> expected, DocumentReference reference, Object wikiTarget,
        boolean recurse) throws GroupException
    {
        Collection<DocumentReference> actual = this.manager.getGroups(reference, wikiTarget, recurse);

        assertEquals(new HashSet<>(expected), new HashSet<>(actual));

        // Make sure the result is cached
        assertSame(actual, this.manager.getGroups(reference, wikiTarget, recurse), "The result hasn't been cached");
    }

    private void mockMembers(DocumentReference group, List<DocumentReference> groups)
    {
        try {
            when(this.groupService.getAllMembersNamesForGroup(eq(group.toString()), anyInt(), anyInt(), any()))
                .thenReturn(groups.stream().map(element -> element.toString()).collect(Collectors.toList()));
        } catch (XWikiException e) {
            // Cannot happen
        }
    }

    private void assertGetMembersEmpty(DocumentReference reference, boolean recurse) throws GroupException
    {
        assertGetMembers(Collections.emptyList(), reference, recurse);
    }

    private void assertGetMembers(DocumentReference expected, DocumentReference reference, boolean recurse)
        throws GroupException
    {
        assertGetMembers(Arrays.asList(expected), reference, recurse);
    }

    private void assertGetMembers(Collection<DocumentReference> expected, DocumentReference reference, boolean recurse)
        throws GroupException
    {
        Collection<DocumentReference> actual = this.manager.getMembers(reference, recurse);

        assertEquals(new HashSet<>(expected), new HashSet<>(actual));

        // Make sure the result is cached
        assertSame(actual, this.manager.getMembers(reference, recurse), "The result hasn't been cached");
    }

    @Test
    void getGroupsWhenNoGroup() throws GroupException
    {
        assertGetGroupsEmpty(GLOBAL_USER_1, WikiTarget.ENTITY, false);
        assertGetGroupsEmpty(GLOBAL_USER_1, WikiTarget.ENTITY, true);
        assertGetGroupsEmpty(GLOBAL_USER_1, null, false);
        assertGetGroupsEmpty(GLOBAL_USER_1, null, true);
        assertGetGroupsEmpty(GLOBAL_USER_1, "xwiki", false);
        assertGetGroupsEmpty(GLOBAL_USER_1, "xwiki", true);
    }

    @Test
    void getGroupsWhenOneDirect() throws GroupException
    {
        mockGroups("xwiki", GLOBAL_USER_1, Arrays.asList(GLOBAL_GROUP_1));

        assertGetGroups(GLOBAL_GROUP_1, GLOBAL_USER_1, WikiTarget.ENTITY, false);
        assertGetGroups(GLOBAL_GROUP_1, GLOBAL_USER_1, WikiTarget.ENTITY, true);
        assertGetGroups(GLOBAL_GROUP_1, GLOBAL_USER_1, null, false);
        assertGetGroups(GLOBAL_GROUP_1, GLOBAL_USER_1, null, true);
        assertGetGroups(GLOBAL_GROUP_1, GLOBAL_USER_1, "xwiki", false);
        assertGetGroups(GLOBAL_GROUP_1, GLOBAL_USER_1, "xwiki", true);
        assertGetGroupsEmpty(GLOBAL_USER_1, "otherwiki", false);
        assertGetGroupsEmpty(GLOBAL_USER_1, "otherwiki", true);
        assertGetGroups(GLOBAL_GROUP_1, GLOBAL_USER_1, Arrays.asList("xwiki", "otherwiki"), false);
        assertGetGroups(GLOBAL_GROUP_1, GLOBAL_USER_1, Arrays.asList("xwiki", "otherwiki"), true);
    }

    @Test
    void getGroupsWhenOneDirectAndOneRecursive() throws GroupException
    {
        mockGroups("xwiki", GLOBAL_USER_1, Arrays.asList(GLOBAL_GROUP_1));
        mockGroups("xwiki", GLOBAL_GROUP_1, Arrays.asList(GLOBAL_GROUP_2));
        List<DocumentReference> all = Arrays.asList(GLOBAL_GROUP_1, GLOBAL_GROUP_2);

        assertGetGroups(GLOBAL_GROUP_1, GLOBAL_USER_1, WikiTarget.ENTITY, false);
        assertGetGroups(all, GLOBAL_USER_1, WikiTarget.ENTITY, true);
        assertGetGroups(GLOBAL_GROUP_1, GLOBAL_USER_1, null, false);
        assertGetGroups(all, GLOBAL_USER_1, null, true);
        assertGetGroups(GLOBAL_GROUP_1, GLOBAL_USER_1, "xwiki", false);
        assertGetGroups(all, GLOBAL_USER_1, "xwiki", true);
        assertGetGroupsEmpty(GLOBAL_USER_1, "otherwiki", false);
        assertGetGroupsEmpty(GLOBAL_USER_1, "otherwiki", true);
        assertGetGroups(GLOBAL_GROUP_1, GLOBAL_USER_1, Arrays.asList("xwiki", "otherwiki"), false);
        assertGetGroups(all, GLOBAL_USER_1, Arrays.asList("xwiki", "otherwiki"), true);

        assertGetGroups(GLOBAL_GROUP_2, GLOBAL_GROUP_1, WikiTarget.ENTITY, false);
        assertGetGroups(GLOBAL_GROUP_2, GLOBAL_GROUP_1, WikiTarget.ENTITY, true);        
    }

    @Test
    void getGroupsWhenOneDirectOnSeveralWikis() throws GroupException
    {
        mockGroups("xwiki", GLOBAL_USER_1, Arrays.asList(GLOBAL_GROUP_1));
        mockGroups("wiki", GLOBAL_USER_1, Arrays.asList(WIKI_GROUP_1));
        List<DocumentReference> all = Arrays.asList(GLOBAL_GROUP_1, WIKI_GROUP_1);

        assertGetGroups(GLOBAL_GROUP_1, GLOBAL_USER_1, WikiTarget.ENTITY, false);
        assertGetGroups(GLOBAL_GROUP_1, GLOBAL_USER_1, WikiTarget.ENTITY, true);
        assertGetGroups(GLOBAL_GROUP_1, GLOBAL_USER_1, "xwiki", false);
        assertGetGroups(GLOBAL_GROUP_1, GLOBAL_USER_1, "xwiki", true);
        assertGetGroups(WIKI_GROUP_1, GLOBAL_USER_1, "wiki", false);
        assertGetGroups(WIKI_GROUP_1, GLOBAL_USER_1, "wiki", true);
        assertGetGroups(all, GLOBAL_USER_1, null, false);
        assertGetGroups(all, GLOBAL_USER_1, null, true);
        assertGetGroups(all, GLOBAL_USER_1, Arrays.asList("xwiki", "wiki", "otherwiki"), false);
        assertGetGroups(all, GLOBAL_USER_1, Arrays.asList("xwiki", "wiki", "otherwiki"), true);
        this.oldcore.getXWikiContext().setWikiId("wiki");
        assertGetGroups(all, GLOBAL_USER_1, WikiTarget.ENTITY_AND_CURRENT, false);
        assertGetGroups(all, GLOBAL_USER_1, WikiTarget.ENTITY_AND_CURRENT, true);
    }

    @Test
    void getGroupsWhenOneDirectOnRecursiveOnSeveralWikis() throws GroupException
    {
        mockGroups("xwiki", GLOBAL_USER_1, Arrays.asList(GLOBAL_GROUP_1));
        mockGroups("wiki", GLOBAL_GROUP_1, Arrays.asList(WIKI_GROUP_2));
        mockGroups("wiki", GLOBAL_USER_1, Arrays.asList(WIKI_GROUP_1));
        mockGroups("xwiki", WIKI_GROUP_1, Arrays.asList(GLOBAL_GROUP_2));
        List<DocumentReference> allDirect = Arrays.asList(GLOBAL_GROUP_1, WIKI_GROUP_1);
        List<DocumentReference> all = Arrays.asList(GLOBAL_GROUP_1, GLOBAL_GROUP_2, WIKI_GROUP_1, WIKI_GROUP_2);

        assertGetGroups(GLOBAL_GROUP_1, GLOBAL_USER_1, WikiTarget.ENTITY, false);
        assertGetGroups(GLOBAL_GROUP_1, GLOBAL_USER_1, WikiTarget.ENTITY, true);
        assertGetGroups(GLOBAL_GROUP_1, GLOBAL_USER_1, "xwiki", false);
        assertGetGroups(GLOBAL_GROUP_1, GLOBAL_USER_1, "xwiki", true);
        assertGetGroups(WIKI_GROUP_1, GLOBAL_USER_1, "wiki", false);
        assertGetGroups(WIKI_GROUP_1, GLOBAL_USER_1, "wiki", true);
        assertGetGroups(allDirect, GLOBAL_USER_1, null, false);
        assertGetGroups(all, GLOBAL_USER_1, null, true);
        assertGetGroups(allDirect, GLOBAL_USER_1, Arrays.asList("xwiki", "wiki", "otherwiki"), false);
        assertGetGroups(all, GLOBAL_USER_1, Arrays.asList("xwiki", "wiki", "otherwiki"), true);
        this.oldcore.getXWikiContext().setWikiId("wiki");
        assertGetGroups(allDirect, GLOBAL_USER_1, WikiTarget.ENTITY_AND_CURRENT, false);
        assertGetGroups(all, GLOBAL_USER_1, WikiTarget.ENTITY_AND_CURRENT, true);
    }

    @Test
    void getGroupsWhenCrossReference() throws GroupException
    {
        mockGroups("xwiki", GLOBAL_GROUP_1, Arrays.asList(GLOBAL_GROUP_2));
        mockGroups("xwiki", GLOBAL_GROUP_2, Arrays.asList(GLOBAL_GROUP_1));

        assertGetGroups(GLOBAL_GROUP_1, GLOBAL_GROUP_2, WikiTarget.ENTITY, false);
        assertGetGroups(GLOBAL_GROUP_2, GLOBAL_GROUP_1, WikiTarget.ENTITY, false);
        assertGetGroups(GLOBAL_GROUP_1, GLOBAL_GROUP_2, WikiTarget.ENTITY, true);
        assertGetGroups(GLOBAL_GROUP_2, GLOBAL_GROUP_1, WikiTarget.ENTITY, true);
    }

    @Test
    void getMembersWhenNotGroup() throws GroupException
    {
        assertGetMembersEmpty(GLOBAL_USER_1, false);
        assertGetMembersEmpty(GLOBAL_USER_1, true);

        // Make sure the result is not cached in the member cache
        assertNull(this.groupMembersCache.getCacheEntry(GLOBAL_USER_1, false));
     }

    @Test
    void getMembersWhenNoMembers() throws GroupException
    {
        assertGetMembersEmpty(GLOBAL_GROUP_1, false);
        assertGetMembersEmpty(GLOBAL_GROUP_1, true);
    }

    @Test
    void getMembersWhenOneDirect() throws GroupException
    {
        mockMembers(GLOBAL_GROUP_1, Arrays.asList(GLOBAL_USER_1));

        assertGetMembers(GLOBAL_USER_1, GLOBAL_GROUP_1, false);
        assertGetMembers(GLOBAL_USER_1, GLOBAL_GROUP_1, true);
    }

    @Test
    void getMembersWhenOneDirectAndOneRecursive() throws GroupException
    {
        mockMembers(GLOBAL_GROUP_1, Arrays.asList(GLOBAL_USER_1));
        mockMembers(GLOBAL_GROUP_2, Arrays.asList(GLOBAL_GROUP_1));

        assertGetMembers(GLOBAL_GROUP_1, GLOBAL_GROUP_2, false);
        assertGetMembers(Arrays.asList(GLOBAL_GROUP_1, GLOBAL_USER_1), GLOBAL_GROUP_2, true);
        
        assertGetMembers(GLOBAL_USER_1, GLOBAL_GROUP_1, false);
        assertGetMembers(GLOBAL_USER_1, GLOBAL_GROUP_1, true);
    }

    @Test
    void getMembersWhenCrossReference() throws GroupException
    {
        mockMembers(GLOBAL_GROUP_1, Arrays.asList(GLOBAL_GROUP_2));
        mockMembers(GLOBAL_GROUP_2, Arrays.asList(GLOBAL_GROUP_1));

        assertGetMembers(GLOBAL_GROUP_1, GLOBAL_GROUP_2, false);
        assertGetMembers(GLOBAL_GROUP_2, GLOBAL_GROUP_1, false);
        assertGetMembers(GLOBAL_GROUP_1, GLOBAL_GROUP_2, true);
        assertGetMembers(GLOBAL_GROUP_2, GLOBAL_GROUP_1, true);
    }

    /**
     * Ensure that calling the API with guest user {@code null} reference doesn't lead to NPE.
     */
    @Test
    void getGroupsWithGuest() throws GroupException
    {
        Collection<DocumentReference> groups = this.manager.getGroups(null, null, false);
        assertTrue(groups.isEmpty());

        groups = this.manager.getGroups(null, null, true);
        assertTrue(groups.isEmpty());
    }

    @Test
    void getWikiGroups() throws GroupException
    {
        WikiReference emptyWiki = new WikiReference("empty");
        Set<DocumentReference> groups = this.manager.getGroups(emptyWiki);
        assertEquals(0, groups.size());

        assertSame(groups, this.manager.getGroups(emptyWiki));

        this.wikiGroupCache.invalidate(emptyWiki.getName());

        assertNotSame(groups, this.manager.getGroups(emptyWiki));

        mockGroups(WIKI_GROUP_1.getWikiReference(), List.of("XWiki.group1", "XWiki.group2"));

        assertEquals(Set.of(WIKI_GROUP_1, WIKI_GROUP_2), this.manager.getGroups(WIKI_GROUP_1.getWikiReference()));
    }
}
