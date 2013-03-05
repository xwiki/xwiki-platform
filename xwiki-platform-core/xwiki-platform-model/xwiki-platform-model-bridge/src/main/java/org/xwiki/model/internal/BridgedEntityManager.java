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
package org.xwiki.model.internal;

import java.net.MalformedURLException;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.xwiki.cache.Cache;
import org.xwiki.cache.CacheManager;
import org.xwiki.cache.config.CacheConfiguration;
import org.xwiki.cache.eviction.LRUEvictionConfiguration;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.phase.Initializable;
import org.xwiki.component.phase.InitializationException;
import org.xwiki.context.Execution;
import org.xwiki.model.Entity;
import org.xwiki.model.EntityManager;
import org.xwiki.model.EntityType;
import org.xwiki.model.ModelException;
import org.xwiki.model.ModelRuntimeException;
import org.xwiki.model.UniqueReference;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.ObjectReference;
import org.xwiki.model.reference.WikiReference;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.objects.BaseProperty;

/**
 * @since 5.0M1
 */
@Component
@Named("bridge")
@Singleton
public class BridgedEntityManager implements EntityManager, Initializable
{
    @Inject
    private Execution execution;

    @Inject
    private CacheManager cacheManager;

    /**
     * Cache holding modified entities not yet saved to persistent storage.
     */
    private Cache<Entity> modifiedEntityCache;

    @Override
    public void initialize() throws InitializationException
    {
        CacheConfiguration cacheConfiguration = new CacheConfiguration();
        cacheConfiguration.setConfigurationId("model");
        LRUEvictionConfiguration lru = new LRUEvictionConfiguration();
        cacheConfiguration.put(LRUEvictionConfiguration.CONFIGURATIONID, lru);

        try {
            this.modifiedEntityCache = this.cacheManager.getCacheFactory().newCache(cacheConfiguration);
        } catch (Exception e) {
            throw new ModelRuntimeException("Failed to create Entity Cache", e);
        }
    }

    @Override
    public <T extends Entity> T getEntity(UniqueReference uniqueReference) throws ModelException
    {
        T result = null;

        EntityReference reference = uniqueReference.getReference();
        switch (reference.getType()) {
            case DOCUMENT:
                result = (T) new BridgedDocumentEntity(getXWikiDocument(reference), getXWikiContext());
                // Note: We don't need to set isNew since this is supported by the old model directly.
                break;
            case SPACE:
                // A space exists if there's at least one document in it.
                try {
                    // TODO: Improve performance by issuing a query that only looks for documents in the specified
                    // space instead of all docs in all spaces...
                    List<String> spaces = getXWiki().getSpaces(getXWikiContext());
                    if (spaces.contains(reference.getName())) {
                        BridgedSpaceEntity bs = new BridgedSpaceEntity(getXWikiContext());
                        bs.setNew(false);
                        result = (T) bs;
                    }
                } catch (XWikiException e) {
                    throw new ModelException("Error verifying existence of space [%s]", e, reference);
                }
                break;
            case WIKI:
                // TODO: Need to load the wiki details. FTM only checking if it exists
                if (hasEntity(uniqueReference)) {
                    BridgedWikiEntity bwe = new BridgedWikiEntity(getXWikiContext());
                    bwe.setNew(false);
                    result = (T) bwe;
                }
                break;
            case OBJECT:
                BaseObject xObject = getXWikiObject(reference);
                if (xObject != null) {
                    BridgedObjectEntity bo = new BridgedObjectEntity(xObject, getXWikiContext());
                    bo.setNew(false);
                    result = (T) bo;
                }
                break;
            case OBJECT_PROPERTY:
                BaseObject xobject = getXWikiObject(reference.getParent());
                if (xobject != null) {
                    try {
                        BridgedObjectPropertyEntity bop = new BridgedObjectPropertyEntity(
                            (BaseProperty) xobject.get(reference.getName()), getXWikiContext());
                        bop.setNew(false);
                        result = (T) bop;
                    } catch (XWikiException e) {
                        throw new ModelException("Failed to retrieve object property [%s]", e, reference);
                    }
                }
                break;
            default:
                throw new ModelRuntimeException("Not supported");
        }

        return result;
    }

    private BaseObject getXWikiObject(EntityReference reference) throws ModelException
    {
        // Find the reference to the document containing the object (it's the parent of the passed
        // reference) and Load the parent document since objects are loaded at the same time in the old model.
        XWikiDocument xdoc = getXWikiDocument(reference.getParent());
        // Get the requested object if the document isn't new. If the document is new or the document exists but
        // doesn't have that XObject then we return null. Note that we cannot create an empty XObject since we don't
        // have a reference to the XClass at this moment.
        return xdoc.getXObject(new ObjectReference(reference));
    }

    private XWikiDocument getXWikiDocument(EntityReference reference) throws ModelException
    {
        XWikiDocument result;

        try {
            // Since the old model API always return a XWikiDocument even if it doesn't exist, we need to check
            // if the document is new or not.
            result = getXWiki().getDocument(new DocumentReference(reference), getXWikiContext());
        } catch (XWikiException e) {
            throw new ModelException("Error loading document [%s]", e, reference);
        }

        return result;
    }

    @Override
    public boolean hasEntity(UniqueReference uniqueReference)
    {
        boolean result;

        // TODO: should we return true if the entity has been created but not saved in the DB?

        EntityReference reference = uniqueReference.getReference();
        switch (reference.getType()) {
            case DOCUMENT:
                result = getXWiki().exists(new DocumentReference(reference), getXWikiContext());
                break;
            case WIKI:
                try {
                    result = getXWiki().getServerURL(new WikiReference(reference).getName(), getXWikiContext()) != null;
                } catch (MalformedURLException e) {
                    result = false;
                }
                break;
            default:
                throw new ModelRuntimeException("Not supported");
        }
        return result;
    }

    @Override
    public <T extends Entity> T addEntity(UniqueReference uniqueReference)
    {
        T result;

        EntityReference reference = uniqueReference.getReference();
        if (reference.getType().equals(EntityType.WIKI)) {
            result = (T) new BridgedWikiEntity(getXWikiContext());
        } else {
            throw new ModelRuntimeException("Not supported");
        }

        // Save the Entity in the cache
        // TODO: in the future send it through an event
        this.modifiedEntityCache.set(uniqueReference.toString(), result);

        return result;
    }

    @Override
    public void removeEntity(UniqueReference uniqueReference)
    {
        throw new ModelRuntimeException("Not supported");
    }

    public XWiki getXWiki()
    {
        return getXWikiContext().getWiki();
    }

    public XWikiContext getXWikiContext()
    {
        return (XWikiContext) this.execution.getContext().getProperty("xwikicontext");
    }
}
