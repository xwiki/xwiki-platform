/*
 * Copyright 2010 Andreas Jonsson
 * 
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

import org.xwiki.model.EntityType;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.DocumentReference;

import org.xwiki.component.annotation.Component;
import org.xwiki.component.annotation.Requirement;
import org.xwiki.component.logging.AbstractLogEnabled;

import org.xwiki.security.RightLoader;
import org.xwiki.security.RightCache;
import org.xwiki.security.RightCacheEntry;
import org.xwiki.security.RightCacheKey;
import org.xwiki.security.RightResolver;
import org.xwiki.security.ParentEntryEvictedException;
import org.xwiki.security.ConflictingInsertionException;
import org.xwiki.security.EntityTypeNotSupportedException;
import org.xwiki.security.RightServiceException;
import org.xwiki.security.RightsObject;
import org.xwiki.security.RightsObjectFactory;
import org.xwiki.security.AccessLevel;

import java.util.List;
import java.util.LinkedList;
import java.util.Collection;

/**
 * The default implementation for the right loader.
 *
 * @version $Id$
 */
@Component
public class DefaultRightLoader extends AbstractLogEnabled implements RightLoader
{
    /** Maximum number of attempts at loading an entry. */
    private static final int MAX_RETRIES = 5;

    /** Resolver for the user, group and rights objects. */
    @Requirement("priority") private RightResolver rightResolver;

    /** The right cache. */
    @Requirement private RightCache rightCache;

    /** Event listener responsible for invalidating cache entries. */
    @Requirement private RightCacheInvalidator rightCacheInvalidator;

    /** Factory object for producing RightsObject instances from the corresponding xwiki rights objects. */
    @Requirement private RightsObjectFactory rightsObjectFactory;

    @Override
    public AccessLevel load(DocumentReference user, EntityReference entity)
        throws RightServiceException
    {
        int retries = 0;
    RETRY: 
        while (true) {
            rightCacheInvalidator.suspend();

            try {
                retries++;
                return loadRequiredEntries(user, entity);
            } catch (ParentEntryEvictedException e) {
                if (retries < MAX_RETRIES) {
                    getLogger().debug("The parent entry was evicted. Have tried " 
                                      + retries
                                      + " times.  Trying again...");
                    continue RETRY;
                }
            } catch (ConflictingInsertionException e) {
                if (retries < MAX_RETRIES) {
                    getLogger().debug("There were conflicting insertions.  Have tried "
                                      + retries
                                      + " times.  Retrying...");
                    continue RETRY;
                }
            } finally {
                rightCacheInvalidator.resume();
            }
            getLogger().error("Failed to load the cache in "
                              + retries
                              + " attempts.  Giving up.");
            throw new RightServiceException();
        }
    }

    /**
     * @param user The user identity.
     * @param entity The entity that is the object of thi rihghts check.
     * @return The resulting access level for the user at the entity.
     * @throws ParentEntryEvictedException If one of the parent
     * entries are evicted before the load is completed.
     * @throws ConflictingInsertionException When different threads
     * have inserted conflicting entries into the cache.
     * @throws RightServiceException On error.
     */
    private AccessLevel loadRequiredEntries(DocumentReference user, EntityReference entity)
        throws ParentEntryEvictedException, ConflictingInsertionException, RightServiceException
    {
        if (entity == null) {
            return AccessLevel.DEFAULT_ACCESS_LEVEL;
        }

        RightCacheKey entityKey = rightCache.getRightCacheKey(entity);
        RightCacheEntry entry = rightCache.get(entityKey);
        boolean foundObjects;
        if (entry == null) {
            List<Collection<RightsObject>> rightsObjects
                = getRightsObjects(entityKey, entity);
            foundObjects = rightsObjects.get(rightsObjects.size() - 1).size() > 0;
        } else {
            foundObjects = entry.getType() == RightCacheEntry.Type.HAVE_OBJECTS;
        }

        if (foundObjects || entity.getType() == EntityType.WIKI) {
            return loadUserAtEntity(user, entity);
        } else {
            return loadRequiredEntries(user, entity.getParent());
        }
    }

    /**
     * @param user Entity that identifies user.
     * @param entity The entity that is the object of this rights check.
     * @return The access level cache entry that was loaded into the cache.
     * @throws ParentEntryEvictedException If one of the parent
     * entries are evicted before the load is completed.
     * @throws ConflictingInsertionException When different threads
     * have inserted conflicting entries into the cache.
     * @throws RightServiceException On error.
     */
    private AccessLevel loadUserAtEntity(DocumentReference user, EntityReference entity)
        throws ParentEntryEvictedException, ConflictingInsertionException, RightServiceException
    {
        /*
         * Make sure the group entries are loaded
         */
        Collection<DocumentReference> groups = loadGroupEntries(user);
        EntityReference userParent = user.getParent().clone();
        userParent.setChild(null);
        /*
         * Make sure the parent of the user document is loaded.
         */
        RightCacheKey userParentKey = rightCache.getRightCacheKey(userParent);
        getRightsObjects(userParentKey, userParent);
        /*
         * Parent entries of the user entry are group entries in
         * addition to the space entry of the user page.
         */
        Collection<RightCacheKey> parents = new LinkedList();
        parents.add(userParentKey);
        for (DocumentReference group : groups) {
            parents.add(rightCache.getRightCacheKey(group));
        }

        RightCacheKey userKey   = rightCache.getRightCacheKey(user);
        RightCacheKey entityKey = rightCache.getRightCacheKey(entity);
        RightCacheEntry entry = loadRightsObjects(user);
        rightCache.addWithMultipleParents(userKey, parents, entry);

        List<Collection<RightsObject>> rightsObjects
            = getRightsObjects(entityKey, entity);

        AccessLevel accessLevel = rightResolver.resolve(user, entity, entityKey, groups, rightsObjects);
        getLogger().debug("Adding "
                          + userKey.getEntityReference() + "@"
                          + entityKey.getEntityReference() + ": "
                          + accessLevel);
        rightCache.addUserAtEntity(userKey, entityKey, accessLevel);
        return accessLevel;
    }

    /**
     * Make sure that the group entries for this user's groups are loaded.
     * @param user The user.
     * @return The collection of groups.
     * @throws ParentEntryEvictedException if any of the parent entries of the group
     * were evicted.
     * @throws ConflictingInsertionException When different threads
     * have inserted conflicting entries into the cache.
     * @throws RightServiceException on error.
     */
    private Collection<DocumentReference> loadGroupEntries(DocumentReference user)
        throws ParentEntryEvictedException, ConflictingInsertionException, RightServiceException
    {
        Collection<DocumentReference> groups = XWikiUtils.getGroupsForUser(user);
                                              
        for (DocumentReference group : groups) {
            EntityReference parent = group.getParent().clone();
            parent.setChild(null);
            /*
             * Make sure the parent entries of the group is cached.
             */
            getRightsObjects(rightCache.getRightCacheKey(parent), parent);

            RightCacheKey groupKey = rightCache.getRightCacheKey(group);
            RightCacheEntry entry = rightCache.get(groupKey);
            if (entry == null) {
                entry = loadRightsObjects(group);
                rightCache.add(groupKey, entry);
            }
        }
        return groups;
    }

    /**
     * Make sure that the entity and its parents have entries in the
     * cache, and gather the rights objects of these.
     *
     * Note that we need to pass both the RightCacheKey, which is used
     * for stepping through the document hierarcy and may therefore
     * have the main wiki prepended, and the entity, which is
     * used for accessing the rights objects as parameters.
     * 
     * @param entityKey The entity represented by a right cache key.
     * @param entity The entity represented by an entity reference.
     * @return A collection of rights objects.
     * @exception RightServiceException if an error occurs
     * @exception ParentEntryEvictedException if any parent entry is
     * evicted before the operation completes.
     * @throws ConflictingInsertionException When different threads
     * have inserted conflicting entries into the cache.
     */
    private List<Collection<RightsObject>> getRightsObjects(RightCacheKey entityKey, EntityReference entity)
        throws RightServiceException, ParentEntryEvictedException, ConflictingInsertionException
    {
        List<Collection<RightsObject>> rightsObjects = new LinkedList();
        EntityReference hierarchy = entityKey.getEntityReference();
        for (EntityReference ref = hierarchy.getRoot(); ref != null; ref = ref.getChild()) {
            Collection<RightsObject> thisLevel = new LinkedList();
            rightsObjects.add(thisLevel);
            RightCacheEntry entry = rightCache.get(rightCache.getRightCacheKey(ref));
            if (entry == null) {
                entry = loadRightsObjects(ref);
                rightCache.add(rightCache.getRightCacheKey(ref), entry);
            }
            if (entry.getType() != RightCacheEntry.Type.HAVE_OBJECTS) {
                continue;
            }
            switch (ref.getType()) {
                case SPACE:
                case WIKI:
                    thisLevel.addAll(entry.getObjects(GlobalRightsObject.class));
                    break;
                case DOCUMENT:
                    thisLevel.addAll(entry.getObjects(LocalRightsObject.class));
                    break;
                default:
                    String message = "There is an entry of type "
                        + ref.getType()
                        + " in the right cache!";
                    getLogger().error(message);
                    throw new RightServiceException(message);
            }
        }
        return rightsObjects;
    }

    /**
     * Load the global rights object of a wiki or space.
     * @param entity Either a document, space or wiki entity referenced.
     * @return the right cache entry that should be loaded into the cache.
     * @throws RightServiceException On error.
     */
    private RightCacheEntry loadRightsObjects(EntityReference entity)
        throws RightServiceException
    {
        DocumentReference docRef;
        boolean global;
        switch (entity.getType()) {
            case SPACE:
                EntityReference spaceDoc = new EntityReference(XWikiUtils.SPACE_DOC,
                                                               EntityType.DOCUMENT,
                                                               entity.clone());
                docRef = new DocumentReference(spaceDoc);
                global = true;
                break;
            case WIKI:
                EntityReference space = new EntityReference(XWikiUtils.WIKI_SPACE,
                                                            EntityType.SPACE,
                                                            entity.clone());
                EntityReference wikiDoc = new EntityReference(XWikiUtils.WIKI_DOC,
                                                              EntityType.DOCUMENT,
                                                              space);
                docRef = new DocumentReference(wikiDoc);
                global = true;
                break;
            case DOCUMENT:
                docRef = new DocumentReference(entity);
                global = false;
                break;
            default:
                getLogger().error("Rights on entities of type "
                                  + entity.getType()
                                  + " is not supported by this loader!");
                throw new EntityTypeNotSupportedException(entity.getType(), this);
        }

        RightCacheEntry entry;
        Collection<RightsObject> objs = rightsObjectFactory.getInstances(docRef, global);

        if (objs.size() == 0 && entity.getType() != EntityType.WIKI) {
            entry = RightCacheEntry.HAVE_NO_RIGHT_OBJECT_ENTRY;
        } else {
            ObjectEntry objEntry = new ObjectEntry();
            for (RightsObject obj : objs) {
                objEntry.addObject(obj);
            }
            entry = objEntry;
        }
        return entry;
    }

}
