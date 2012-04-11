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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.jmock.Expectations;
import org.junit.Before;
import org.junit.Test;
import org.xwiki.cache.Cache;
import org.xwiki.cache.CacheEntry;
import org.xwiki.cache.CacheManager;
import org.xwiki.cache.config.CacheConfiguration;
import org.xwiki.cache.event.CacheEntryEvent;
import org.xwiki.cache.event.CacheEntryListener;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.model.reference.SpaceReference;
import org.xwiki.model.reference.WikiReference;
import org.xwiki.security.AbstractSecurityTestCase;
import org.xwiki.security.DefaultSecurityReferenceFactory;
import org.xwiki.security.GroupSecurityReference;
import org.xwiki.security.SecurityReference;
import org.xwiki.security.UserSecurityReference;
import org.xwiki.security.authorization.SecurityAccessEntry;
import org.xwiki.security.authorization.SecurityRuleEntry;
import org.xwiki.security.authorization.cache.ConflictingInsertionException;
import org.xwiki.security.authorization.cache.ParentEntryEvictedException;
import org.xwiki.test.annotation.MockingRequirement;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.CoreMatchers.sameInstance;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

/**
 * Default security cache Unit Test
 *
 * @version $Id$
 * @since 4.0M2
 */
public class DefaultSecurityCacheTest extends AbstractSecurityTestCase
{
    @MockingRequirement(exceptions = {EntityReferenceSerializer.class})
    private DefaultSecurityCache securityCache;

    @MockingRequirement
    private DefaultSecurityReferenceFactory factory;

    private TestCache<?> cache;

    private SecurityReference aMissingParentRef;
    private SecurityReference aMissingEntityRef;

    private UserSecurityReference aMissingUserRef;
    private GroupSecurityReference aMissingGroupRef;

    class TestCache<T> implements Cache<T>
    {
        private Map<String,T> cache = new HashMap<String,T>();
        private CacheEntryListener<T> listener;
        private String lastInsertedKey;
        
        class TestCacheEntry implements CacheEntry<T>
        {
            private final String key;
            private final T value;
            
            TestCacheEntry(String key, T value)
            {
                this.key = key;
                this.value = value;
            }
            
            @Override
            public Cache<T> getCache()
            {
                return TestCache.this;
            }

            @Override
            public String getKey()
            {
                return key;
            }

            @Override
            public T getValue()
            {
                return value;
            }
        }
        
        private CacheEntryEvent<T> getEvent(final String key, final T value)
        {
            return new CacheEntryEvent<T>()
                {
                    @Override
                    public CacheEntry<T> getEntry()
                    {
                        return new TestCacheEntry(key, value);
                    }
    
                    @Override
                    public Cache<T> getCache()
                    {
                        return TestCache.this;
                    }
                };
        }
        
        
        @Override
        public void set(String key, T value)
        {
            T old = cache.put(key, value);
            if (listener != null && old == null) {
                listener.cacheEntryAdded(getEvent(key, value));
            } else {
                listener.cacheEntryModified(getEvent(key, value));                
            }
            lastInsertedKey = key;
        }

        @Override
        public T get(String key)
        {
            return cache.get(key);
        }

        @Override
        public void remove(String key)
        {
            T value = cache.remove(key);
            if (listener != null) {
                listener.cacheEntryRemoved(getEvent(key, value));
            }
        }

        @Override
        public void removeAll()
        {
            cache.clear();
        }

        @Override
        public void addCacheEntryListener(CacheEntryListener<T> tCacheEntryListener)
        {
            listener = tCacheEntryListener;
        }

        @Override
        public void removeCacheEntryListener(CacheEntryListener<T> tCacheEntryListener)
        {
            assertThat(tCacheEntryListener, equalTo(listener));
            listener = null;
        }

        @Override
        public void dispose()
        {
        }

        public String getLastInsertedKey()
        {
            return lastInsertedKey;
        }
    }

    @Override
    public void configure() throws Exception
    {
        if (cache == null) {
            cache = new TestCache();

            final CacheManager cacheManager = getComponentManager().getInstance(CacheManager.class);

            getMockery().checking(new Expectations() {{
                oneOf (cacheManager).createNewCache(with(any(CacheConfiguration.class))); will(returnValue(cache));
            }});
        }
    }

    @Before
    @Override
    public void setUp() throws Exception
    {
        super.setUp();

        aMissingParentRef = factory.newEntityReference(new SpaceReference("space", new WikiReference("missing")));
        aMissingEntityRef = factory.newEntityReference(new DocumentReference("missingPage",
            xspaceRef.getOriginalSpaceReference()));

        aMissingUserRef = factory.newUserReference(new DocumentReference("missingUser",
            xXWikiSpace.getOriginalSpaceReference()));
        aMissingGroupRef = factory.newGroupReference(new DocumentReference("missingGroup",
            xXWikiSpace.getOriginalSpaceReference()));

        final Logger mockLogger = getMockLogger(DefaultSecurityCache.class);
        getMockery().checking(new Expectations() {{
            allowing (mockLogger).isDebugEnabled(); will(returnValue(false));
        }});
    }

    private List<SecurityRuleEntry> getMockedSecurityRuleEntries(final List<? extends SecurityReference> references)
    {
        final List<SecurityRuleEntry> entries = new ArrayList<SecurityRuleEntry>(references.size());

        for (SecurityReference ref : references) {
            entries.add(getMockery().mock(SecurityRuleEntry.class, "Rules for " + ref.toString()));
        }

        getMockery().checking(new Expectations() {{
            for (int i = 0; i < entries.size(); i++) {
                SecurityReference ref = references.get(i);
                SecurityRuleEntry entry =  entries.get(i);

                allowing (entry).getReference(); will(returnValue(ref));
            }
        }});

        return entries;
    }

    private List<SecurityAccessEntry> getMockedSecurityAccessEntries(final UserSecurityReference user,
        final List<SecurityReference> references)
    {
        final List<SecurityAccessEntry> entries = new ArrayList<SecurityAccessEntry>(references.size());

        for (SecurityReference ref : references) {
            entries.add(getMockery().mock(SecurityAccessEntry.class,
                "Access for " +  user.toString() + " on " + ref.toString()));
        }

        getMockery().checking(new Expectations() {{
            for (int i = 0; i < entries.size(); i++) {
                SecurityReference ref = references.get(i);
                SecurityAccessEntry entry = entries.get(i);

                allowing (entry).getReference(); will(returnValue(ref));
                allowing (entry).getUserReference(); will(returnValue(user));
            }
        }});

        return entries;
    }

    private class CacheFiller
    {
        private Map<SecurityReference, String> keys;
        private Map<SecurityReference, SecurityRuleEntry> entityEntries;
        private Map<SecurityReference, Collection<SecurityAccessEntry>> entityAccessEntries;
        private Map<UserSecurityReference, Collection<SecurityAccessEntry>> userAccessEntries;
        private List<SecurityRuleEntry> allRuleEntries;
        private List<SecurityAccessEntry> allAccessEntries;

        public Map<SecurityReference, String> getKeys()
        {
            return keys;
        }

        public Map<SecurityReference, SecurityRuleEntry> getEntityEntries()
        {
            return entityEntries;
        }

        public Map<SecurityReference, Collection<SecurityAccessEntry>> getEntityAccessEntries()
        {
            return entityAccessEntries;
        }

        public Map<UserSecurityReference, Collection<SecurityAccessEntry>> getUserAccessEntries()
        {
            return userAccessEntries;
        }

        public List<SecurityRuleEntry> getAllRuleEntries()
        {
            return allRuleEntries;
        }

        public List<SecurityAccessEntry> getAllAccessEntries()
        {
            return allAccessEntries;
        }

        public CacheFiller fill() throws ParentEntryEvictedException, ConflictingInsertionException
        {
            keys = new HashMap<SecurityReference, String>();
            entityEntries = new HashMap<SecurityReference, SecurityRuleEntry>();
            entityAccessEntries = new HashMap<SecurityReference, Collection<SecurityAccessEntry>>();
            userAccessEntries = new HashMap<UserSecurityReference, Collection<SecurityAccessEntry>>();
            allRuleEntries = new ArrayList<SecurityRuleEntry>();
            allAccessEntries = new ArrayList<SecurityAccessEntry>();

            for (SecurityRuleEntry entry : getMockedSecurityRuleEntries(entityRefs)) {
                securityCache.add(entry);
                keys.put(entry.getReference(),cache.getLastInsertedKey());
                entityEntries.put(entry.getReference(), entry);
                allRuleEntries.add(entry);
            }

            List<SecurityRuleEntry> spaceEntries = getMockedSecurityRuleEntries(xwikiSpaceRefs);
            for (SecurityRuleEntry entry : spaceEntries) {
                securityCache.add(entry);
                keys.put(entry.getReference(),cache.getLastInsertedKey());
                entityEntries.put(entry.getReference(), entry);
                allRuleEntries.add(entry);
            }

            for (SecurityRuleEntry entry : getMockedSecurityRuleEntries(userRefs)) {
                securityCache.add(entry);
                keys.put(entry.getReference(),cache.getLastInsertedKey());
                entityEntries.put(entry.getReference(), entry);
                allRuleEntries.add(entry);
            }

            final List<SecurityRuleEntry> groupEntries = getMockedSecurityRuleEntries(new ArrayList<SecurityReference>(groupRefs.keySet()));
            for (SecurityRuleEntry entry : groupEntries) {
                securityCache.add(entry);
                keys.put(entry.getReference(),cache.getLastInsertedKey());
                entityEntries.put(entry.getReference(), entry);
                allRuleEntries.add(entry);
            }

            final List<SecurityRuleEntry> userEntries = getMockedSecurityRuleEntries(groupUserRefs);
            securityCache.add(userEntries.get(0), Arrays.asList(groupRef));
            keys.put(userEntries.get(0).getReference(),cache.getLastInsertedKey());
            securityCache.add(userEntries.get(1), Arrays.asList(anotherGroupRef));
            keys.put(userEntries.get(1).getReference(),cache.getLastInsertedKey());
            securityCache.add(userEntries.get(2), Arrays.asList(groupRef, anotherGroupRef));
            keys.put(userEntries.get(2).getReference(),cache.getLastInsertedKey());
            for (SecurityRuleEntry userEntry : userEntries) {
                entityEntries.put(userEntry.getReference(), userEntry);
            }
            allRuleEntries.addAll(userEntries);

            for (UserSecurityReference userRef : new ArrayList<UserSecurityReference>(){{ addAll(userRefs); addAll(groupUserRefs); }}) {
                for (SecurityAccessEntry entry : getMockedSecurityAccessEntries(userRef, entityRefs)) {
                    securityCache.add(entry);
                    Collection<SecurityAccessEntry> entries = entityAccessEntries.get(entry.getReference());
                    if (entries == null) {
                        entries = new ArrayList<SecurityAccessEntry>(userRefs.size());
                        entityAccessEntries.put(entry.getReference(), entries);
                    }
                    entries.add(entry);
                    entries = userAccessEntries.get(entry.getUserReference());
                    if (entries == null) {
                        entries = new ArrayList<SecurityAccessEntry>(entityRefs.size());
                        userAccessEntries.put(entry.getUserReference(), entries);
                    }
                    entries.add(entry);
                    allAccessEntries.add(entry);
                }
            }
            return this;
        }
    }

    @Test
    public void testAddSecurityRuleEntry() throws Exception
    {
        final List<SecurityRuleEntry> entries = getMockedSecurityRuleEntries(entityRefs);

        // Insert and check insertion individually
        for (SecurityRuleEntry entry : entries) {
            assertThat(securityCache.get(entry.getReference()), is(nullValue()));
            securityCache.add(entry);
            assertThat(securityCache.get(entry.getReference()), sameInstance(entry));
        }

        // Check all insertions
        for (SecurityRuleEntry entry : entries) {
            assertThat(securityCache.get(entry.getReference()), sameInstance(entry));
        }
        
        // XWiki spaces are required to load user entries
        List<SecurityRuleEntry> spaceEntries = getMockedSecurityRuleEntries(Arrays.asList(xwikiSpace));
        for (SecurityRuleEntry entry : spaceEntries) {
            securityCache.add(entry);
        }

        // Insert some groups
        final List<SecurityRuleEntry> groupEntries = getMockedSecurityRuleEntries(new ArrayList<SecurityReference>(groupRefs.keySet()));
        for (SecurityRuleEntry entry : groupEntries) {
            securityCache.add(entry);
        }

        // Check inserting users in groups
        final List<SecurityRuleEntry> userEntries = getMockedSecurityRuleEntries(groupUserRefs);
        for (SecurityRuleEntry userEntry : userEntries) {
            assertThat(securityCache.get(userEntry.getReference()), is(nullValue()));
        }
        securityCache.add(userEntries.get(0), Arrays.asList(groupRef));
        securityCache.add(userEntries.get(1), Arrays.asList(anotherGroupRef));
        securityCache.add(userEntries.get(2), Arrays.asList(groupRef, anotherGroupRef));
        for (SecurityRuleEntry userEntry : userEntries) {
            assertThat(securityCache.get(userEntry.getReference()), sameInstance(userEntry));
        }

        // Check a non-conflicting duplicate insertion
        try {
            securityCache.add(entries.get(0));
        } catch (ConflictingInsertionException e) {
            fail("Inserting the same rule entry twice should NOT throw a ConflictingInsertionException.");
        }

        // Check a conflicting duplicate insertion
        final SecurityRuleEntry aDifferentEntry = getMockery().mock(SecurityRuleEntry.class, "aDifferentEntry");
        getMockery().checking(new Expectations() {{
            allowing (aDifferentEntry).getReference(); will(returnValue(entries.get(0).getReference()));
        }});

        try {
            securityCache.add(aDifferentEntry);
            fail(
                "Inserting a different rule entry for the same reference should throw a ConflictingInsertionException.");
        } catch (ConflictingInsertionException ignore) {
            // Expected.
        }

        // Check an insertion of an entry without inserting all its parents first
        final SecurityRuleEntry aMissingParentEntry = getMockery().mock(SecurityRuleEntry.class, "aMissingParentEntry");
        getMockery().checking(new Expectations() {{
            allowing (aMissingParentEntry).getReference(); will(returnValue(aMissingParentRef));
        }});

        try {
            securityCache.add(aMissingParentEntry);
            fail(
                "Inserting a rule entry without its parents should throw a ParentEntryEvictedException.");
        } catch (ParentEntryEvictedException ignore) {
            // Expected.
        }

        // Check an insertion of a user without inserting all its groups first
        final SecurityRuleEntry aMissingParentUser = getMockery().mock(SecurityRuleEntry.class, "aMissingUser");
        getMockery().checking(new Expectations() {{
            allowing (aMissingParentUser).getReference(); will(returnValue(anotherWikiUserRef));
        }});

        try {
            securityCache.add(aMissingParentUser, Arrays.asList(groupRef, anotherGroupRef));
            fail(
                "Inserting a user entry without its parents should throw a ParentEntryEvictedException.");
        } catch (ParentEntryEvictedException ignore) {
            // Expected.
        }

        // Check an insertion of a user without inserting all its groups first
        final SecurityRuleEntry aMissingGroupUser = getMockery().mock(SecurityRuleEntry.class, "aMissingGroupUser");
        getMockery().checking(new Expectations() {{
            allowing (aMissingGroupUser).getReference(); will(returnValue(userRef));
        }});

        try {
            securityCache.add(aMissingGroupUser, Arrays.asList(groupRef, aMissingGroupRef));
            fail(
                "Inserting a user entry without all its groups should throw a ParentEntryEvictedException.");
        } catch (ParentEntryEvictedException ignore) {
            // Expected.
        }
    }

    @Test
    public void testAddSecurityAccessEntry() throws Exception
    {
        // Access rule is required  as parent for entities that will receive access level
        List<SecurityRuleEntry> entityEntries = getMockedSecurityRuleEntries(entityRefs);
        for (SecurityRuleEntry entry : entityEntries) {
            securityCache.add(entry);
        }

        // XWiki spaces are required to load user entries
        List<SecurityRuleEntry> spaceEntries = getMockedSecurityRuleEntries(xwikiSpaceRefs);
        for (SecurityRuleEntry entry : spaceEntries) {
            securityCache.add(entry);
        }

        // User entries are require as parent for their access level entries
        List<SecurityRuleEntry> userEntries = getMockedSecurityRuleEntries(userRefs);
        for (SecurityRuleEntry entry : userEntries) {
            securityCache.add(entry);
        }

        final List<SecurityAccessEntry> allEntries = new ArrayList<SecurityAccessEntry>(entityRefs.size()*userRefs.size());

        // Insert and check insertion individually
        for (UserSecurityReference userRef : userRefs) {
            List<SecurityAccessEntry> entries = getMockedSecurityAccessEntries(userRef, entityRefs);
    
            for (SecurityAccessEntry entry : entries) {
                assertThat(securityCache.get(entry.getUserReference(), entry.getReference()), is(nullValue()));
                securityCache.add(entry);
                assertThat(securityCache.get(entry.getUserReference(), entry.getReference()), sameInstance(entry));
            }
            
            allEntries.addAll(entries);
        }

        // Check all insertions
        for (SecurityAccessEntry entry : allEntries) {
            assertThat(securityCache.get(entry.getUserReference(), entry.getReference()), sameInstance(entry));
        }

        // Check a non-conflicting duplicate insertion
        try {
            securityCache.add(allEntries.get(0));
        } catch (ConflictingInsertionException e) {
            fail("Inserting the same access entry twice should NOT throw a ConflictingInsertionException.");
        }

        // Check a conflicting duplicate insertion
        final SecurityAccessEntry aDifferentEntry = getMockery().mock(SecurityAccessEntry.class, "aDifferentEntry");
        getMockery().checking(new Expectations() {{
            allowing (aDifferentEntry).getUserReference(); will(returnValue(allEntries.get(0).getUserReference()));
            allowing (aDifferentEntry).getReference(); will(returnValue(allEntries.get(0).getReference()));
        }});

        try {
            securityCache.add(aDifferentEntry);
            fail("Inserting a different access entry for the same reference should throw a ConflictingInsertionException.");
        } catch (ConflictingInsertionException ignore) {
            // Expected.
        }

        // Check insertion of entries without inserting either the entity or the user first
        final SecurityAccessEntry aMissingEntityEntry = getMockery().mock(SecurityAccessEntry.class, "aMissingEntityEntry");
        final SecurityAccessEntry aMissingUserEntry = getMockery().mock(SecurityAccessEntry.class, "aMissingUserEntry");
        getMockery().checking(new Expectations() {{
            allowing (aMissingEntityEntry).getUserReference(); will(returnValue(xuserRef));
            allowing (aMissingEntityEntry).getReference(); will(returnValue(aMissingEntityRef));
            allowing (aMissingUserEntry).getUserReference(); will(returnValue(aMissingUserRef));
            allowing (aMissingUserEntry).getReference(); will(returnValue(xdocRef));
        }});

        try {
            securityCache.add(aMissingEntityEntry);
            fail("Inserting a access entry without inserting its entity first should throw a ParentEntryEvictedException.");
        } catch (ParentEntryEvictedException ignore) {
            // Expected.
        }

        try {
            securityCache.add(aMissingUserEntry);
            fail("Inserting a access entry without inserting its user first should throw a ParentEntryEvictedException.");
        } catch (ParentEntryEvictedException ignore) {
            // Expected.
        }
    }

    @Test
    public void testRemoveSecurityRuleEntry() throws Exception
    {
        // Fill the cache
        CacheFiller cacheFiller = new CacheFiller().fill();
        List<SecurityRuleEntry> allRuleEntries = cacheFiller.getAllRuleEntries();
        Map<SecurityReference, SecurityRuleEntry> entityEntries = cacheFiller.getEntityEntries();
        Map<SecurityReference, Collection<SecurityAccessEntry>> entityAccessEntries =
            cacheFiller.getEntityAccessEntries();
        List<SecurityAccessEntry> allAccessEntries = cacheFiller.getAllAccessEntries();
        Map<UserSecurityReference, Collection<SecurityAccessEntry>> userAccessEntries =
            cacheFiller.getUserAccessEntries();

        // Remove an document entity
        securityCache.remove(docRef);
        allRuleEntries.remove(entityEntries.get(docRef));
        assertThat(securityCache.get(docRef), nullValue());
        for (SecurityAccessEntry entry : entityAccessEntries.get(docRef)) {
            allAccessEntries.remove(entry);
            assertThat(securityCache.get(entry.getUserReference(), entry.getReference()), nullValue());
        }
        for (SecurityAccessEntry entry : allAccessEntries) {
            assertThat(securityCache.get(entry.getUserReference(), entry.getReference()), sameInstance(entry));
        }
        for (SecurityRuleEntry entry : allRuleEntries) {
            assertThat(securityCache.get(entry.getReference()), sameInstance(entry));
        }

        // Remove an user entity
        securityCache.remove(anotherWikiUserRef);
        allRuleEntries.remove(entityEntries.get(anotherWikiUserRef));
        assertThat(securityCache.get(anotherWikiUserRef), nullValue());
        for (SecurityAccessEntry entry : userAccessEntries.get(anotherWikiUserRef)) {
            allAccessEntries.remove(entry);
            assertThat(securityCache.get(entry.getUserReference(), entry.getReference()), nullValue());
        }
        for (SecurityAccessEntry entry : allAccessEntries) {
            assertThat(securityCache.get(entry.getUserReference(), entry.getReference()), sameInstance(entry));
        }
        for (SecurityRuleEntry entry : allRuleEntries) {
            assertThat(securityCache.get(entry.getReference()), sameInstance(entry));
        }

        // Remove a group entity
        securityCache.remove(groupRef);
        for(SecurityReference ref : Arrays.asList(groupRef, groupUserRef, bothGroupUserRef)) {
            allRuleEntries.remove(entityEntries.get(ref));
            assertThat(securityCache.get(ref), nullValue());
        }
        for (SecurityAccessEntry entry : userAccessEntries.get(groupUserRef)) {
            allAccessEntries.remove(entry);
            assertThat(securityCache.get(entry.getUserReference(), entry.getReference()), nullValue());
        }
        for (SecurityAccessEntry entry : userAccessEntries.get(bothGroupUserRef)) {
            allAccessEntries.remove(entry);
            assertThat(securityCache.get(entry.getUserReference(), entry.getReference()), nullValue());
        }
        for (SecurityAccessEntry entry : allAccessEntries) {
            assertThat(securityCache.get(entry.getUserReference(), entry.getReference()), sameInstance(entry));
        }
        for (SecurityRuleEntry entry : allRuleEntries) {
            assertThat(securityCache.get(entry.getReference()), sameInstance(entry));
        }

        // Remove a space entry
        securityCache.remove(spaceRef);
        for (SecurityReference ref : Arrays.asList(anotherDocRef, spaceRef)) {
            allRuleEntries.remove(entityEntries.get(ref));
            assertThat(securityCache.get(ref), nullValue());
            for (SecurityAccessEntry entry : entityAccessEntries.get(ref)) {
                allAccessEntries.remove(entry);
                assertThat(securityCache.get(entry.getUserReference(), entry.getReference()), nullValue());
            }
        }
        for (SecurityAccessEntry entry : allAccessEntries) {
            assertThat(securityCache.get(entry.getUserReference(), entry.getReference()), sameInstance(entry));
        }
        for (SecurityRuleEntry entry : allRuleEntries) {
            assertThat(securityCache.get(entry.getReference()), sameInstance(entry));
        }


        // Remove an wiki entity
        securityCache.remove(wikiRef);
        allRuleEntries.remove(entityEntries.get(xwikiSpace));
        assertThat(securityCache.get(xwikiSpace), nullValue());
        allRuleEntries.remove(entityEntries.get(anotherGroupRef));
        assertThat(securityCache.get(anotherGroupRef), nullValue());
        for (SecurityReference ref : Arrays.asList(anotherSpaceDocRef, anotherSpaceRef, wikiRef)) {
            allRuleEntries.remove(entityEntries.get(ref));
            assertThat(securityCache.get(ref), nullValue());
            for (SecurityAccessEntry entry : entityAccessEntries.get(ref)) {
                allAccessEntries.remove(entry);
                assertThat(securityCache.get(entry.getUserReference(), entry.getReference()), nullValue());
            }
        }
        for (UserSecurityReference ref : Arrays.asList(userRef, anotherUserRef, anotherGroupUserRef)) {
            allRuleEntries.remove(entityEntries.get(ref));
            for (SecurityAccessEntry entry : userAccessEntries.get(ref)) {
                allAccessEntries.remove(entry);
                assertThat(securityCache.get(entry.getUserReference(), entry.getReference()), nullValue());
            }
        }
        for (SecurityAccessEntry entry : allAccessEntries) {
            assertThat(securityCache.get(entry.getUserReference(), entry.getReference()), sameInstance(entry));
        }
        for (SecurityRuleEntry entry : allRuleEntries) {
            assertThat(securityCache.get(entry.getReference()), sameInstance(entry));
        }

        // Remove the main wiki entry
        securityCache.remove(xwikiRef);
        for (SecurityAccessEntry entry : allAccessEntries) {
            assertThat(securityCache.get(entry.getUserReference(), entry.getReference()), nullValue());
        }
        for (SecurityRuleEntry entry : allRuleEntries) {
            assertThat(securityCache.get(entry.getReference()), nullValue());
        }
    }

    @Test
    public void testCacheEvictedEntries() throws Exception
    {
        // Fill the cache
        CacheFiller cacheFiller = new CacheFiller().fill();
        Map<SecurityReference, String> keys = cacheFiller.getKeys();
        List<SecurityRuleEntry> allRuleEntries = cacheFiller.getAllRuleEntries();
        Map<SecurityReference, SecurityRuleEntry> entityEntries = cacheFiller.getEntityEntries();
        Map<SecurityReference, Collection<SecurityAccessEntry>> entityAccessEntries =
            cacheFiller.getEntityAccessEntries();
        List<SecurityAccessEntry> allAccessEntries = cacheFiller.getAllAccessEntries();
        Map<UserSecurityReference, Collection<SecurityAccessEntry>> userAccessEntries =
            cacheFiller.getUserAccessEntries();

        // Remove a document entry
        cache.remove(keys.get(anotherWikiDocRef));
        for (SecurityReference ref : Arrays.asList(anotherWikiDocRef)) {
            allRuleEntries.remove(entityEntries.get(ref));
            assertThat(securityCache.get(ref), nullValue());
            for (SecurityAccessEntry entry : entityAccessEntries.get(ref)) {
                allAccessEntries.remove(entry);
                assertThat(securityCache.get(entry.getUserReference(), entry.getReference()), nullValue());
            }
        }
        for (SecurityRuleEntry entry : allRuleEntries) {
            assertThat(securityCache.get(entry.getReference()), sameInstance(entry));
        }
        for (SecurityAccessEntry entry : allAccessEntries) {
            assertThat(securityCache.get(entry.getUserReference(), entry.getReference()), sameInstance(entry));
        }

        // Remove a user entry
        cache.remove(keys.get(anotherWikiUserRef));
        for (SecurityReference ref : Arrays.asList(anotherWikiUserRef)) {
            allRuleEntries.remove(entityEntries.get(ref));
            assertThat(securityCache.get(ref), nullValue());
        }
        for (SecurityAccessEntry entry : userAccessEntries.get(anotherWikiUserRef)) {
            allAccessEntries.remove(entry);
            assertThat(securityCache.get(entry.getUserReference(), entry.getReference()), nullValue());
        }
        for (SecurityRuleEntry entry : allRuleEntries) {
            assertThat(securityCache.get(entry.getReference()), sameInstance(entry));
        }
        for (SecurityAccessEntry entry : allAccessEntries) {
            assertThat(securityCache.get(entry.getUserReference(), entry.getReference()), sameInstance(entry));
        }

        // Remove a space entry
        cache.remove(keys.get(spaceRef));
        for (SecurityReference ref : Arrays.asList(docRef, anotherDocRef, spaceRef)) {
            allRuleEntries.remove(entityEntries.get(ref));
            assertThat(securityCache.get(ref), nullValue());
            for (SecurityAccessEntry entry : entityAccessEntries.get(ref)) {
                allAccessEntries.remove(entry);
                assertThat(securityCache.get(entry.getUserReference(), entry.getReference()), nullValue());
            }
        }
        for (SecurityRuleEntry entry : allRuleEntries) {
            assertThat(securityCache.get(entry.getReference()), sameInstance(entry));
        }
        for (SecurityAccessEntry entry : allAccessEntries) {
            assertThat(securityCache.get(entry.getUserReference(), entry.getReference()), sameInstance(entry));
        }

        // Remove a wiki entry
        cache.remove(keys.get(wikiRef));
        allRuleEntries.remove(entityEntries.get(xwikiSpace));
        assertThat(securityCache.get(xwikiSpace), nullValue());
        allRuleEntries.remove(entityEntries.get(groupRef));
        assertThat(securityCache.get(groupRef), nullValue());
        allRuleEntries.remove(entityEntries.get(anotherGroupRef));
        assertThat(securityCache.get(anotherGroupRef), nullValue());
        for (SecurityReference ref : Arrays.asList(anotherSpaceDocRef, anotherSpaceRef, wikiRef)) {
            allRuleEntries.remove(entityEntries.get(ref));
            assertThat(securityCache.get(ref), nullValue());
            for (SecurityAccessEntry entry : entityAccessEntries.get(ref)) {
                allAccessEntries.remove(entry);
                assertThat(securityCache.get(entry.getUserReference(), entry.getReference()), nullValue());
            }
        }
        for (UserSecurityReference ref : Arrays.asList(userRef, anotherUserRef, groupUserRef, anotherGroupUserRef, bothGroupUserRef)) {
            allRuleEntries.remove(entityEntries.get(ref));
            assertThat(securityCache.get(ref), nullValue());
            for (SecurityAccessEntry entry : userAccessEntries.get(ref)) {
                allAccessEntries.remove(entry);
                assertThat(securityCache.get(entry.getUserReference(), entry.getReference()), nullValue());
            }
        }
        for (SecurityRuleEntry entry : allRuleEntries) {
            assertThat(securityCache.get(entry.getReference()), sameInstance(entry));
        }
        for (SecurityAccessEntry entry : allAccessEntries) {
            assertThat(securityCache.get(entry.getUserReference(), entry.getReference()), sameInstance(entry));
        }

        // Remove the main wiki entry
        cache.remove(keys.get(xwikiRef));
        for (SecurityRuleEntry entry : allRuleEntries) {
            assertThat(securityCache.get(entry.getReference()), nullValue());
        }
        for (SecurityAccessEntry entry : allAccessEntries) {
            assertThat(securityCache.get(entry.getUserReference(), entry.getReference()), nullValue());
        }
    }

    @Test
    public void testRemoveSecurityAccessEntry() throws Exception
    {
        // Fill the cache
        CacheFiller cacheFiller = new CacheFiller().fill();
        List<SecurityRuleEntry> allRuleEntries = cacheFiller.getAllRuleEntries();
        Map<SecurityReference, Collection<SecurityAccessEntry>> entityAccessEntries =
            cacheFiller.getEntityAccessEntries();
        List<SecurityAccessEntry> allAccessEntries = cacheFiller.getAllAccessEntries();

        // Remove the access level for a user on a document
        assertThat(securityCache.get(userRef, docRef), notNullValue());
        securityCache.remove(userRef, docRef);
        assertThat(securityCache.get(userRef, docRef), nullValue());
        for (SecurityAccessEntry entry : entityAccessEntries.get(docRef)) {
            if (entry.getUserReference().equals(userRef)) {
                allAccessEntries.remove(entry);
                assertThat(securityCache.get(entry.getUserReference(), entry.getReference()), nullValue());
            }
        }
        for (SecurityRuleEntry entry : allRuleEntries) {
            assertThat(securityCache.get(entry.getReference()), sameInstance(entry));
        }
        for (SecurityAccessEntry entry : allAccessEntries) {
            assertThat(securityCache.get(entry.getUserReference(), entry.getReference()), sameInstance(entry));
        }
    }
}
