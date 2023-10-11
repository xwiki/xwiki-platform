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
package org.xwiki.security.authorization.cache.internal;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.xwiki.cache.CacheException;
import org.xwiki.cache.CacheManager;
import org.xwiki.cache.config.CacheConfiguration;
import org.xwiki.model.EntityType;
import org.xwiki.model.internal.reference.DefaultStringEntityReferenceSerializer;
import org.xwiki.model.internal.reference.DefaultSymbolScheme;
import org.xwiki.model.internal.reference.EntityReferenceFactory;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.SpaceReference;
import org.xwiki.model.reference.WikiReference;
import org.xwiki.security.AbstractSecurityTestCase;
import org.xwiki.security.DefaultSecurityReferenceFactory;
import org.xwiki.security.GroupSecurityReference;
import org.xwiki.security.SecurityReference;
import org.xwiki.security.UserSecurityReference;
import org.xwiki.security.authorization.SecurityAccessEntry;
import org.xwiki.security.authorization.SecurityEntry;
import org.xwiki.security.authorization.SecurityRuleEntry;
import org.xwiki.security.authorization.cache.ConflictingInsertionException;
import org.xwiki.security.authorization.cache.ParentEntryEvictedException;
import org.xwiki.security.authorization.cache.SecurityShadowEntry;
import org.xwiki.security.internal.XWikiBridge;
import org.xwiki.test.annotation.BeforeComponent;
import org.xwiki.test.annotation.ComponentList;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.CoreMatchers.sameInstance;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Default security cache Unit Test.
 *
 * @version $Id$
 */
@ComponentTest
@ComponentList({DefaultStringEntityReferenceSerializer.class, DefaultSymbolScheme.class, EntityReferenceFactory.class})
class DefaultSecurityCacheTest extends AbstractSecurityTestCase
{
    @MockComponent
    private CacheManager cacheManager;

    @InjectMockComponents
    private DefaultSecurityCache securityCache;

    @MockComponent
    private XWikiBridge xWikiBridge;

    @InjectMockComponents
    private DefaultSecurityReferenceFactory factory;

    private TestCache<Object> cache;

    private SecurityReference aMissingParentRef;

    private SecurityReference aMissingEntityRef;

    private UserSecurityReference aMissingUserRef;

    private GroupSecurityReference aMissingGroupRef;

    private SecurityReference aMissingWikiRef;

    @BeforeComponent
    public void configureCacheManager() throws CacheException
    {
        if (this.cache == null) {
            this.cache = new TestCache<>();
            when(this.cacheManager.createNewCache(any(CacheConfiguration.class))).thenReturn(this.cache);
        }
    }

    @BeforeEach
    public void configure() throws Exception
    {
        when(this.xWikiBridge.getMainWikiReference()).thenReturn(new WikiReference("xwiki"));
        when(this.xWikiBridge.toCompatibleEntityReference(any(EntityReference.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

        aMissingParentRef = factory.newEntityReference(new SpaceReference("space", new WikiReference("missing")));
        aMissingEntityRef =
            factory.newEntityReference(new DocumentReference("missingPage", xspaceRef.getOriginalSpaceReference()));

        aMissingUserRef =
            factory.newUserReference(new DocumentReference("missingUser", xXWikiSpace.getOriginalSpaceReference()));
        aMissingGroupRef =
            factory.newGroupReference(new DocumentReference("missingGroup", xXWikiSpace.getOriginalSpaceReference()));
        aMissingWikiRef = factory.newEntityReference(new WikiReference("missingWiki"));
    }

    private SecurityRuleEntry mockSecurityRuleEntry(final SecurityReference ref)
    {
        SecurityRuleEntry entry = mock(SecurityRuleEntry.class, "Rules for " + ref.toString());
        when(entry.getReference()).thenReturn(ref);
        return entry;
    }

    private SecurityShadowEntry mockSecurityShadowEntry(final UserSecurityReference user, final SecurityReference wiki)
    {
        SecurityShadowEntry entry =
            mock(SecurityShadowEntry.class, "Shadow for " + user.toString() + " on " + wiki.toString());
        when(entry.getReference()).thenReturn(user);
        when(entry.getWikiReference()).thenReturn(wiki);
        return entry;
    }

    private SecurityAccessEntry mockSecurityAccessEntry(final SecurityReference ref, final UserSecurityReference user)
    {
        SecurityAccessEntry entry =
            mock(SecurityAccessEntry.class, "Access for " + user.toString() + " on " + ref.toString());
        when(entry.getReference()).thenReturn(ref);
        when(entry.getUserReference()).thenReturn(user);
        return entry;
    }

    private String AddAccessEntry(final SecurityAccessEntry entry)
        throws ParentEntryEvictedException, ConflictingInsertionException
    {
        WikiReference entityWiki =
            (WikiReference) entry.getReference().getOriginalReference().extractReference(EntityType.WIKI);
        WikiReference userWiki = entry.getUserReference().getWikiReference().getOriginalWikiReference();
        if (!Objects.equals(entityWiki, userWiki)) {
            if (entry.getUserReference().isGlobal()) {
                securityCache.add(entry, factory.newEntityReference(entityWiki));
                return cache.getLastInsertedKey();
            } else {
                return null;
            }
        } else {
            securityCache.add(entry);
            return cache.getLastInsertedKey();
        }
    }

    private String AddUserEntry(SecurityRuleEntry entry, Collection<GroupSecurityReference> groups)
        throws ParentEntryEvictedException, ConflictingInsertionException
    {
        securityCache.add(entry, groups);
        return cache.getLastInsertedKey();
    }

    private String AddRuleEntry(SecurityRuleEntry entry)
        throws ParentEntryEvictedException, ConflictingInsertionException
    {
        if (groupUserRefs.contains(entry.getReference())) {
            final List<GroupSecurityReference> groups = new ArrayList<GroupSecurityReference>();
            for (GroupSecurityReference group : groupRefs.keySet()) {
                if (groupRefs.get(group).contains(entry.getReference())) {
                    if (group.getOriginalReference().getWikiReference()
                        .equals(entry.getReference().getOriginalDocumentReference().getWikiReference())) {
                        groups.add(group);
                    }
                }
            }
            AddUserEntry(entry, groups);
        } else if (userRefs.contains(entry.getReference())) {
            AddUserEntry(entry, null);
        } else {
            securityCache.add(entry);
        }
        return cache.getLastInsertedKey();
    }

    private String AddUserEntry(SecurityShadowEntry user)
        throws ParentEntryEvictedException, ConflictingInsertionException
    {
        if (groupUserRefs.contains(user.getReference())) {
            final List<GroupSecurityReference> groups = new ArrayList<GroupSecurityReference>();
            for (GroupSecurityReference group : groupRefs.keySet()) {
                if (groupRefs.get(group).contains(user.getReference())) {
                    if (group.getOriginalReference().getWikiReference()
                        .equals(user.getWikiReference().getOriginalWikiReference())
                        || group.isGlobal())
                    {
                        groups.add(group);
                    }
                }
            }
            securityCache.add(user, groups);
        } else {
            securityCache.add(user, null);
        }
        return cache.getLastInsertedKey();
    }

    private Map<String, SecurityEntry> InsertUsersWithouShadow()
        throws ConflictingInsertionException, ParentEntryEvictedException
    {
        Map<String, SecurityEntry> entries = new HashMap<String, SecurityEntry>();

        // Add wikis
        for (SecurityReference ref : wikiRefs) {
            SecurityRuleEntry entry = mockSecurityRuleEntry(ref);
            entries.put(AddRuleEntry(entry), entry);
        }

        // XWiki spaces are required to load user entries
        for (SecurityReference ref : xwikiSpaceRefs) {
            SecurityRuleEntry entry = mockSecurityRuleEntry(ref);
            entries.put(AddRuleEntry(entry), entry);
        }

        // Insert some users
        for (SecurityReference ref : userRefs) {
            SecurityRuleEntry entry = mockSecurityRuleEntry(ref);
            entries.put(AddRuleEntry(entry), entry);
        }

        // Insert some groups
        for (SecurityReference ref : groupRefs.keySet()) {
            SecurityRuleEntry entry = mockSecurityRuleEntry(ref);
            entries.put(AddRuleEntry(entry), entry);
        }

        // Insert users in groups
        for (SecurityReference ref : groupUserRefs) {
            SecurityRuleEntry entry = mockSecurityRuleEntry(ref);
            entries.put(AddRuleEntry(entry), entry);
        }

        return entries;
    }

    private Map<String, SecurityEntry> InsertUsers() throws ConflictingInsertionException, ParentEntryEvictedException
    {
        Map<String, SecurityEntry> entries = InsertUsersWithouShadow();

        // Check inserting shadow users
        for (UserSecurityReference ref : userRefs) {
            if (ref.isGlobal()) {
                for (SecurityReference wiki : Arrays.asList(wikiRef, anotherWikiRef)) {
                    SecurityShadowEntry entry = mockSecurityShadowEntry(ref, wiki);
                    entries.put(AddUserEntry(entry), entry);
                }
            }
        }

        // Insert some groups
        for (GroupSecurityReference ref : groupRefs.keySet()) {
            if (ref.isGlobal()) {
                for (SecurityReference wiki : Arrays.asList(wikiRef, anotherWikiRef)) {
                    SecurityShadowEntry entry = mockSecurityShadowEntry(ref, wiki);
                    entries.put(AddUserEntry(entry), entry);
                }
            }
        }

        // Insert shadow users in shadow groups
        for (UserSecurityReference ref : groupUserRefs) {
            if (ref.isGlobal()) {
                for (SecurityReference wiki : Arrays.asList(wikiRef, anotherWikiRef)) {
                    SecurityShadowEntry entry = mockSecurityShadowEntry(ref, wiki);
                    entries.put(AddUserEntry(entry), entry);
                }
            }
        }

        return entries;
    }

    private Map<String, SecurityEntry> InsertEntities()
        throws ConflictingInsertionException, ParentEntryEvictedException
    {
        Map<String, SecurityEntry> entries = new HashMap<String, SecurityEntry>();

        for (SecurityReference ref : entityRefs) {
            if (securityCache.get(ref) == null) {
                SecurityRuleEntry entry = mockSecurityRuleEntry(ref);
                entries.put(AddRuleEntry(entry), entry);
            }
        }

        return entries;
    }

    private Map<String, SecurityEntry> InsertAccess() throws ConflictingInsertionException, ParentEntryEvictedException
    {
        Map<String, SecurityEntry> entries = new HashMap<String, SecurityEntry>();

        // Insert access for simple users
        for (UserSecurityReference user : userRefs) {
            for (SecurityReference ref : entityRefs) {
                SecurityAccessEntry entry = mockSecurityAccessEntry(ref, user);
                String key = AddAccessEntry(entry);
                if (key != null)
                    entries.put(key, entry);
            }
            SecurityAccessEntry entry = mockSecurityAccessEntry(user, user);
            String key = AddAccessEntry(entry);
            if (key != null)
                entries.put(key, entry);
        }

        // Insert access for group users
        for (UserSecurityReference user : groupUserRefs) {
            for (SecurityReference ref : entityRefs) {
                SecurityAccessEntry entry = mockSecurityAccessEntry(ref, user);
                String key = AddAccessEntry(entry);
                if (key != null)
                    entries.put(key, entry);
            }
            SecurityAccessEntry entry = mockSecurityAccessEntry(user, user);
            String key = AddAccessEntry(entry);
            if (key != null)
                entries.put(key, entry);
        }

        return entries;
    }

    interface KeepEntries
    {
        boolean keepRule(SecurityRuleEntry entry);

        boolean keepAccess(SecurityAccessEntry entry);

        boolean keepShadow(SecurityShadowEntry entry);
    }

    class Keeper implements KeepEntries
    {
        public boolean keepRule(SecurityRuleEntry entry)
        {
            return true;
        }

        public boolean keepAccess(SecurityAccessEntry entry)
        {
            return true;
        }

        public boolean keepShadow(SecurityShadowEntry entry)
        {
            return true;
        }
    }

    private void checkEntries(Map<String, SecurityEntry> entries, KeepEntries keeper)
    {
        for (Iterator<Map.Entry<String, SecurityEntry>> it = entries.entrySet().iterator(); it.hasNext();) {
            Map.Entry<String, SecurityEntry> entry = it.next();
            if (entry.getValue() instanceof SecurityRuleEntry) {
                SecurityRuleEntry sentry = (SecurityRuleEntry) entry.getValue();
                if (keeper.keepRule(sentry)) {
                    assertThat(this.securityCache.get(entry.getKey()), sameInstance(entry.getValue()));
                } else {
                    it.remove();
                    assertThat(this.securityCache.get(entry.getKey()), nullValue());
                }
            } else if (entry.getValue() instanceof SecurityAccessEntry) {
                SecurityAccessEntry sentry = (SecurityAccessEntry) entry.getValue();
                if (keeper.keepAccess(sentry)) {
                    assertThat(this.securityCache.get(entry.getKey()), sameInstance(entry.getValue()));
                } else {
                    it.remove();
                    assertThat(this.securityCache.get(entry.getKey()), nullValue());
                }
            } else {
                SecurityShadowEntry sentry = (SecurityShadowEntry) entry.getValue();
                if (keeper.keepShadow(sentry)) {
                    assertThat(this.securityCache.get(entry.getKey()), sameInstance(entry.getValue()));
                } else {
                    it.remove();
                    assertThat(this.securityCache.get(entry.getKey()), nullValue());
                }
            }
        }
    }

    interface Remover
    {
        void remove(SecurityReference ref);
    }

    private void removerTest(Map<String, SecurityEntry> entries, Remover remover)
    {

        // Remove an document entity
        remover.remove(docRef);
        checkEntries(entries, new Keeper()
        {
            public boolean keepRule(SecurityRuleEntry entry)
            {
                return entry.getReference() != docRef;
            }

            public boolean keepAccess(SecurityAccessEntry entry)
            {
                return entry.getReference() != docRef;
            }
        });

        // Remove an user entity
        remover.remove(anotherWikiUserRef);
        checkEntries(entries, new Keeper()
        {
            public boolean keepRule(SecurityRuleEntry entry)
            {
                return entry.getReference() != anotherWikiUserRef;
            }

            public boolean keepAccess(SecurityAccessEntry entry)
            {
                return entry.getUserReference() != anotherWikiUserRef;
            }
        });

        // Remove a global user entity with shadow entry
        remover.remove(anotherGroupXUserRef);
        checkEntries(entries, new Keeper()
        {
            public boolean keepRule(SecurityRuleEntry entry)
            {
                return entry.getReference() != anotherGroupXUserRef;
            }

            public boolean keepAccess(SecurityAccessEntry entry)
            {
                return entry.getUserReference() != anotherGroupXUserRef;
            }

            public boolean keepShadow(SecurityShadowEntry entry)
            {
                return entry.getReference() != anotherGroupXUserRef;
            }
        });

        // Remove a group entity
        remover.remove(groupRef);
        checkEntries(entries, new Keeper()
        {
            public boolean keepRule(SecurityRuleEntry entry)
            {
                return (entry.getReference() != groupRef && (!groupRefs.get(groupRef).contains(entry.getReference())
                    || entry.getReference().getOriginalReference().extractReference(EntityType.WIKI) != wikiRef
                        .getOriginalWikiReference()));
            }

            public boolean keepAccess(SecurityAccessEntry entry)
            {
                return (!groupRefs.get(groupRef).contains(entry.getUserReference()) || entry.getReference()
                    .getOriginalReference().extractReference(EntityType.WIKI) != wikiRef.getOriginalWikiReference());
            }

            public boolean keepShadow(SecurityShadowEntry entry)
            {
                return (!groupRefs.get(groupRef).contains(entry.getReference()) || entry.getWikiReference() != wikiRef);
            }
        });

        // Remove a space entry
        remover.remove(spaceRef);
        checkEntries(entries, new Keeper()
        {
            public boolean keepRule(SecurityRuleEntry entry)
            {
                return (entry.getReference().getOriginalReference().extractReference(EntityType.SPACE) != spaceRef
                    .getOriginalSpaceReference());
            }

            public boolean keepAccess(SecurityAccessEntry entry)
            {
                return (entry.getReference().getOriginalReference().extractReference(EntityType.SPACE) != spaceRef
                    .getOriginalSpaceReference());
            }
        });

        // Remove an wiki entity
        remover.remove(wikiRef);
        checkEntries(entries, new Keeper()
        {
            public boolean keepRule(SecurityRuleEntry entry)
            {
                return (entry.getReference().getOriginalReference().extractReference(EntityType.WIKI) != wikiRef
                    .getOriginalWikiReference());
            }

            public boolean keepAccess(SecurityAccessEntry entry)
            {
                return (entry.getReference().getOriginalReference().extractReference(EntityType.WIKI) != wikiRef
                    .getOriginalWikiReference());
            }

            public boolean keepShadow(SecurityShadowEntry entry)
            {
                return (entry.getWikiReference() != wikiRef);
            }

        });

        // Remove the main wiki entry
        remover.remove(xwikiRef);
        checkEntries(entries, new Keeper()
        {
            public boolean keepRule(SecurityRuleEntry entry)
            {
                return false;
            }

            public boolean keepAccess(SecurityAccessEntry entry)
            {
                return false;
            }

            public boolean keepShadow(SecurityShadowEntry entry)
            {
                return false;
            }

        });
    }

    @Test
    void testAddSecurityRuleEntry() throws Exception
    {
        final List<SecurityRuleEntry> ruleEntries = new ArrayList<SecurityRuleEntry>();

        // Insert and check insertion individually
        for (SecurityReference ref : entityRefs) {
            assertThat(securityCache.get(ref), is(nullValue()));
            SecurityRuleEntry entry = mockSecurityRuleEntry(ref);
            AddRuleEntry(entry);
            assertThat(securityCache.get(ref), sameInstance(entry));
            ruleEntries.add(entry);
        }

        // XWiki spaces are required to load user entries
        for (SecurityReference ref : xwikiSpaceRefs) {
            SecurityRuleEntry entry = mockSecurityRuleEntry(ref);
            AddRuleEntry(entry);
            assertThat(securityCache.get(ref), sameInstance(entry));
            ruleEntries.add(entry);
        }

        // Check inserting users
        for (SecurityReference ref : userRefs) {
            SecurityRuleEntry entry = mockSecurityRuleEntry(ref);
            AddRuleEntry(entry);
            assertThat(securityCache.get(ref), sameInstance(entry));
            ruleEntries.add(entry);
        }

        // Insert some groups
        for (SecurityReference ref : groupRefs.keySet()) {
            SecurityRuleEntry entry = mockSecurityRuleEntry(ref);
            AddRuleEntry(entry);
            assertThat(securityCache.get(ref), sameInstance(entry));
            ruleEntries.add(entry);
        }

        // Check inserting users in groups
        for (SecurityReference ref : groupUserRefs) {
            SecurityRuleEntry entry = mockSecurityRuleEntry(ref);
            AddRuleEntry(entry);
            assertThat(securityCache.get(ref), sameInstance(entry));
            ruleEntries.add(entry);
        }

        // Check all insertions
        for (SecurityRuleEntry entry : ruleEntries) {
            assertThat(securityCache.get(entry.getReference()), sameInstance(entry));
        }

        // Check a non-conflicting duplicate insertion
        try {
            AddRuleEntry(ruleEntries.get(0));
        } catch (ConflictingInsertionException e) {
            fail("Inserting the same rule entry twice should NOT throw a ConflictingInsertionException.");
        }

        // Check a conflicting duplicate insertion
        try {
            final SecurityReference ref = ruleEntries.get(0).getReference();
            SecurityRuleEntry entry =
                mock(SecurityRuleEntry.class, "Another entry for " + ruleEntries.get(0).getReference().toString());
            when(entry.getReference()).thenReturn(ref);

            AddRuleEntry(entry);
            fail("Inserting a different rule entry for the same reference should throw"
                + " a ConflictingInsertionException.");
        } catch (ConflictingInsertionException ignore) {
            // Expected.
        }

        // Check an insertion of an entry without inserting all its parents first
        try {
            AddRuleEntry(mockSecurityRuleEntry(aMissingParentRef));
            fail("Inserting a rule entry without its parents should throw a ParentEntryEvictedException.");
        } catch (ParentEntryEvictedException ignore) {
            // Expected.
        }

        // Check an insertion of a user without inserting all its groups first
        try {
            AddUserEntry(mockSecurityRuleEntry(aMissingUserRef), Arrays.asList(groupRef, aMissingGroupRef));
            fail("Inserting a user entry without its parents should throw a ParentEntryEvictedException.");
        } catch (ParentEntryEvictedException ignore) {
            // Expected.
        }
    }

    @Test
    void testAddSecurityShadowEntry() throws Exception
    {
        InsertUsersWithouShadow();

        final List<SecurityShadowEntry> allEntries = new ArrayList<SecurityShadowEntry>();

        // Check inserting shadow users
        for (UserSecurityReference ref : userRefs) {
            if (ref.isGlobal()) {
                for (SecurityReference wiki : Arrays.asList(wikiRef, anotherWikiRef)) {
                    SecurityShadowEntry entry = mockSecurityShadowEntry(ref, wiki);
                    assertThat(securityCache.get(AddUserEntry(entry)), sameInstance((SecurityEntry) entry));
                    allEntries.add(entry);
                }
            }
        }

        // Check inserting some shadow groups
        for (GroupSecurityReference ref : groupRefs.keySet()) {
            if (ref.isGlobal()) {
                for (SecurityReference wiki : Arrays.asList(wikiRef, anotherWikiRef)) {
                    SecurityShadowEntry entry = mockSecurityShadowEntry(ref, wiki);
                    assertThat(securityCache.get(AddUserEntry(entry)), sameInstance((SecurityEntry) entry));
                    allEntries.add(entry);
                }
            }
        }

        // Check inserting shadow users in shadow groups
        for (UserSecurityReference ref : groupUserRefs) {
            if (ref.isGlobal()) {
                for (SecurityReference wiki : Arrays.asList(wikiRef, anotherWikiRef)) {
                    SecurityShadowEntry entry = mockSecurityShadowEntry(ref, wiki);
                    assertThat(securityCache.get(AddUserEntry(entry)), sameInstance((SecurityEntry) entry));
                    allEntries.add(entry);
                }
            }
        }

        // Check a duplicate insertion
        try {
            AddUserEntry(allEntries.get(0));
        } catch (ConflictingInsertionException e) {
            fail("Inserting the same shadow entry twice should NOT throw a ConflictingInsertionException.");
        }

        // Check inserting a shadow for a missing user in an existing wiki
        try {
            AddUserEntry(mockSecurityShadowEntry(aMissingUserRef, wikiRef));
            fail("Inserting a shadow entry without inserting its global user first should throw"
                + " a ParentEntryEvictedException.");
        } catch (ParentEntryEvictedException ignore) {
            // Expected.
        }

        // Check inserting a shadow for a existing user in a missing wiki
        try {
            AddUserEntry(mockSecurityShadowEntry(xuserRef, aMissingWikiRef));
            fail("Inserting a shadow entry without inserting its wiki first should throw"
                + " a ParentEntryEvictedException.");
        } catch (ParentEntryEvictedException ignore) {
            // Expected.
        }
    }

    @Test
    public void testAddSecurityAccessEntry() throws Exception
    {
        InsertUsers();
        InsertEntities();

        final List<SecurityAccessEntry> allEntries = new ArrayList<SecurityAccessEntry>();

        // Insert and check insertion individually for simple users
        for (UserSecurityReference user : userRefs) {
            for (SecurityReference ref : entityRefs) {
                assertThat(securityCache.get(user, ref), is(nullValue()));
                SecurityAccessEntry entry = mockSecurityAccessEntry(ref, user);
                if (AddAccessEntry(entry) != null) {
                    assertThat(securityCache.get(user, ref), sameInstance(entry));
                    allEntries.add(entry);
                }
            }
        }

        // Insert and check insertion individually for group users
        for (UserSecurityReference user : groupUserRefs) {
            for (SecurityReference ref : entityRefs) {
                assertThat(securityCache.get(user, ref), is(nullValue()));
                SecurityAccessEntry entry = mockSecurityAccessEntry(ref, user);
                if (AddAccessEntry(entry) != null) {
                    assertThat(securityCache.get(user, ref), sameInstance(entry));
                    allEntries.add(entry);
                }
            }
        }

        // Check all insertions
        for (SecurityAccessEntry entry : allEntries) {
            assertThat(securityCache.get(entry.getUserReference(), entry.getReference()), sameInstance(entry));
        }

        // Check a non-conflicting duplicate insertion
        try {
            AddAccessEntry(allEntries.get(0));
        } catch (ConflictingInsertionException e) {
            fail("Inserting the same access entry twice should NOT throw a ConflictingInsertionException.");
        }

        // Check a conflicting duplicate insertion
        try {
            final SecurityReference ref = allEntries.get(0).getReference();
            final UserSecurityReference user = allEntries.get(0).getUserReference();
            SecurityAccessEntry entry =
                mock(SecurityAccessEntry.class, "Another access for " + allEntries.get(0).getUserReference().toString()
                    + " on " + allEntries.get(0).getReference().toString());
            when(entry.getUserReference()).thenReturn(user);
            when(entry.getReference()).thenReturn(ref);

            AddAccessEntry(entry);
            fail("Inserting a different access entry for the same reference should throw"
                + " a ConflictingInsertionException.");
        } catch (ConflictingInsertionException ignore) {
            // Expected.
        }

        // Check insertion of entries without inserting either the entity or the user first
        try {
            AddAccessEntry(mockSecurityAccessEntry(aMissingEntityRef, xuserRef));
            fail("Inserting a access entry without inserting its entity first should throw"
                + " a ParentEntryEvictedException.");
        } catch (ParentEntryEvictedException ignore) {
            // Expected.
        }

        try {
            AddAccessEntry(mockSecurityAccessEntry(xdocRef, aMissingUserRef));
            fail("Inserting a access entry without inserting its user first should throw"
                + " a ParentEntryEvictedException.");
        } catch (ParentEntryEvictedException ignore) {
            // Expected.
        }
    }

    @Test
    void testRemoveSecurityRuleEntry() throws Exception
    {
        // Fill the cache
        Map<String, SecurityEntry> entries = InsertUsers();
        entries.putAll(InsertEntities());
        entries.putAll(InsertAccess());

        removerTest(entries, new Remover()
        {
            @Override
            public void remove(SecurityReference ref)
            {
                securityCache.remove(ref);
            }
        });
    }

    @Test
    void testCacheEvictedEntries() throws Exception
    {
        // Fill the cache
        Map<String, SecurityEntry> entries = InsertUsers();
        entries.putAll(InsertEntities());
        entries.putAll(InsertAccess());

        for (Map.Entry<String, SecurityEntry> entry : entries.entrySet()) {
            // Remove all "internal" entries and remove one access entry for the docRef/userRef.
            if (!(entry.getValue() instanceof SecurityAccessEntry)
                || (entry.getValue().getReference() == this.docRef
                && ((SecurityAccessEntry) entry.getValue()).getUserReference() == this.userRef))
            {
                this.cache.remove(entry.getKey());
            }
        }

        // Force a GC to try emptying the reference map as much as possible to validate that it indeed keeps the
        // referenced entries.
        // All "internal" entries should be kept as they should be referenced through parents of the access entries
        // still in the cache.
        System.gc();

        checkEntries(entries, new Keeper()
        {
            public boolean keepAccess(SecurityAccessEntry entry)
            {
                return entry.getReference() != docRef || entry.getUserReference() != userRef;
            }
        });
    }

    @Test
    void testRemoveSecurityAccessEntry() throws Exception
    {
        // Fill the cache
        Map<String, SecurityEntry> entries = InsertUsers();
        entries.putAll(InsertEntities());
        entries.putAll(InsertAccess());

        // Remove the access level for a user on a document
        securityCache.remove(userRef, docRef);
        checkEntries(entries, new Keeper()
        {
            public boolean keepAccess(SecurityAccessEntry entry)
            {
                return entry.getReference() != docRef || entry.getUserReference() != userRef;
            }
        });
    }

    @Test
    void testKeyCollisions() throws Exception
    {
        this.securityCache.add(mockSecurityRuleEntry(newEntityReference(null)));
        this.securityCache.add(mockSecurityRuleEntry(newEntityReference(new WikiReference("wiki"))));
        this.securityCache.add(mockSecurityRuleEntry(newEntityReference(new SpaceReference("wiki", "page1"))));

        SecurityReference spaceReference = newEntityReference(new SpaceReference("wiki", "page1", "page2"));
        SecurityRuleEntry page1Space = mockSecurityRuleEntry(spaceReference);

        // Add a cache entry associated to SPACE wiki:page1.page2
        this.securityCache.add(page1Space);

        assertSame(page1Space, this.securityCache.get(spaceReference));

        SecurityReference documentReference = newEntityReference(new DocumentReference("wiki", "page1", "page2"));

        assertNull(this.securityCache.get(documentReference));

        SecurityRuleEntry page1Document = mockSecurityRuleEntry(documentReference);

        // Add a cache entry associated to DOCUMENT wiki:page1.page2
        this.securityCache.add(page1Document);

        assertSame(page1Space, this.securityCache.get(spaceReference));
        assertSame(page1Document, this.securityCache.get(documentReference));
    }

    @Test
    void testRejectAccessEntryWithNonUserParent() throws Exception
    {
        // Insert the hierarchy for the user into the cache, but not as user.
        this.securityCache.add(mockSecurityRuleEntry(newEntityReference(null)));
        this.securityCache.add(mockSecurityRuleEntry(newEntityReference(new WikiReference("wiki"))));
        this.securityCache.add(mockSecurityRuleEntry(newEntityReference(new SpaceReference("wiki", "XWiki"))));
        DocumentReference userDocRef = new DocumentReference("wiki", "XWiki", "nonUserParent");
        this.securityCache.add(mockSecurityRuleEntry(newEntityReference(userDocRef)));
        UserSecurityReference userSecurityReference = newUserReference(userDocRef);

        DocumentReference documentReference = new DocumentReference("wiki", "XWiki", "page1");
        SecurityReference documentSecurityReference = newEntityReference(documentReference);
        this.securityCache.add(mockSecurityRuleEntry(documentSecurityReference));

        // Create an access entry with the user with non-user parent
        SecurityAccessEntry entry = mockSecurityAccessEntry(documentSecurityReference, userSecurityReference);

        // Try to add the access entry to the cache
        ParentEntryEvictedException exception = assertThrows(ParentEntryEvictedException.class,
            () -> AddAccessEntry(entry));
        assertEquals("The second parent [Rules for Document wiki:XWiki.nonUserParent] for the entry "
                + "[Access for Document wiki:XWiki.nonUserParent on Document wiki:XWiki.page1] with wiki [null] "
                + "is not a user entry.",
            exception.getMessage());
    }

    @Test
    void testRejectUserEntryWithNonGroupGroup() throws Exception
    {
        // Insert the hierarchy for the group and the user into the cache.
        this.securityCache.add(mockSecurityRuleEntry(newEntityReference(null)));
        this.securityCache.add(mockSecurityRuleEntry(newEntityReference(new WikiReference("wiki"))));
        this.securityCache.add(mockSecurityRuleEntry(newEntityReference(new SpaceReference("wiki", "XWiki"))));

        // Insert the group entry but as regular document.
        DocumentReference groupDocRef = new DocumentReference("wiki", "XWiki", "group");
        this.securityCache.add(mockSecurityRuleEntry(newEntityReference(groupDocRef)));
        GroupSecurityReference groupSecurityReference = newGroupReference(groupDocRef);

        // Insert the user as user entry.
        DocumentReference userDocRef = new DocumentReference("wiki", "XWiki", "user");
        ParentEntryEvictedException exception = assertThrows(ParentEntryEvictedException.class, () ->
            this.securityCache.add(mockSecurityRuleEntry(newUserReference(userDocRef)),
                Collections.singleton(groupSecurityReference)));
        assertEquals("The parent [Rules for Document wiki:XWiki.group] is not a group entry.",
            exception.getMessage());
    }

    @Test
    void testAcceptsMainWikiAsUserParent() throws Exception
    {
        // Insert the main wiki entry into the cache, but not as user.
        this.securityCache.add(mockSecurityRuleEntry(newEntityReference(null)));

        this.securityCache.add(mockSecurityRuleEntry(newEntityReference(new SpaceReference("xwiki", "XWiki"))));
        DocumentReference documentReference = new DocumentReference("xwiki", "XWiki", "page1");
        SecurityReference documentSecurityReference = newEntityReference(documentReference);
        this.securityCache.add(mockSecurityRuleEntry(documentSecurityReference));

        // Create an access entry with the user
        UserSecurityReference userSecurityReference = newUserReference(null);
        SecurityAccessEntry entry = mockSecurityAccessEntry(documentSecurityReference, userSecurityReference);

        // Try to add the access entry to the cache
        AddAccessEntry(entry);

        // Check that the entry was added
        assertSame(entry, this.securityCache.get(userSecurityReference, documentSecurityReference));
    }

    @Test
    void testAcceptsMainWikiShadowAsUserParent() throws Exception
    {
        // Insert the main wiki entry into the cache, but not as user.
        this.securityCache.add(mockSecurityRuleEntry(newEntityReference(null)));
        SecurityReference subWikiReference = newEntityReference(new WikiReference("wiki"));
        this.securityCache.add(mockSecurityRuleEntry(subWikiReference));
        // Insert the shadow entry for the subwiki
        this.securityCache.add(mockSecurityShadowEntry(newUserReference(null), subWikiReference), null);
        UserSecurityReference userSecurityReference = newUserReference(null);

        this.securityCache.add(mockSecurityRuleEntry(newEntityReference(new SpaceReference("wiki", "XWiki"))));
        DocumentReference documentReference = new DocumentReference("wiki", "XWiki", "page1");
        SecurityReference documentSecurityReference = newEntityReference(documentReference);
        this.securityCache.add(mockSecurityRuleEntry(documentSecurityReference));

        // Create an access entry with the user
        SecurityAccessEntry entry = mockSecurityAccessEntry(documentSecurityReference, userSecurityReference);

        // Try to add the access entry to the cache
        AddAccessEntry(entry);

        // Check that the entry was added
        assertSame(entry, this.securityCache.get(userSecurityReference, documentSecurityReference));
    }
}
