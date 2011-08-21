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
package org.xwiki.thumbnails.internal;

import java.text.MessageFormat;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;

import org.slf4j.Logger;
import org.xwiki.bridge.DocumentAccessBridge;
import org.xwiki.bridge.DocumentModelBridge;
import org.xwiki.bridge.event.DocumentUpdatedEvent;
import org.xwiki.cache.Cache;
import org.xwiki.cache.CacheException;
import org.xwiki.cache.CacheManager;
import org.xwiki.cache.config.CacheConfiguration;
import org.xwiki.cache.eviction.LRUEvictionConfiguration;
import org.xwiki.component.annotation.Component;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.AttachmentReference;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.model.reference.ObjectPropertyReference;
import org.xwiki.observation.EventListener;
import org.xwiki.observation.event.Event;
import org.xwiki.thumbnails.NoSuchImageException;
import org.xwiki.thumbnails.ThumbnailURLProvider;

/**
 * Default thumbnail URL provider that checks for an image attachment if its document contains an XObject of class
 * <tt>XWiki.ThumbnailClass</tt> that defines the boundaries of the thumbnail relatively to the image.
 * 
 * @version $Id$
 * @since 2.3-M2
 */
@Component
public class DefaultThumbnailURLProvider implements ThumbnailURLProvider, EventListener
{
    /**
     * The question mark sign (indicates the beginning of query string parameters in a URL).
     */
    private static final String QUESTION_MARK = "?";

    /**
     * Maximum number of entries to cache.
     */
    private static final int CACHE_MAX_ENTRIES = 1000;

    /**
     * Cache manager used to create the URL cache.
     */
    @Inject
    private CacheManager cacheManager;
    
    /**
     * That guy with the chain-saw.
     */
    @Inject
    private Logger logger;
    
    /**
     * Serializer used to generate cache key from entity references.
     */
    @Inject
    @Named("default")
    private EntityReferenceSerializer serializer;
    
    /**
     * Document access bridge used to interact with the XWiki data model.
     */
    @Inject
    private DocumentAccessBridge documentAccessBridge;
        
    /**
     * Cache for documents that holds a property pointing to an image.
     * 
     * Cached values are grouped by document document, in order to ease entry flushing.
     */
    private Cache<Map<String, String>> thumbnailURLCache;
    
    /**
     * {@inheritDoc}
     * 
     * @see ThumbnailURLProvider#getURL(ObjectPropertyReference)
     */
    public String getURL(ObjectPropertyReference reference) throws NoSuchImageException
    {
        return this.getURL(reference, Collections.<String, Object> emptyMap());
    }

    /**
     * {@inheritDoc}
     * 
     * @see ThumbnailURLProvider#getURL(ObjectPropertyReference, Map<String,String>)
     */
    public String getURL(ObjectPropertyReference reference, Map<String, Object> extraParameters)
        throws NoSuchImageException
    {
        if (reference == null) {
            throw new NoSuchImageException();
        }
        DocumentReference documentReference = (DocumentReference) reference.extractReference(EntityType.DOCUMENT);
        
        // TODO caching for property values
        String imageName = (String) this.documentAccessBridge.getProperty(reference);
        
        AttachmentReference ref = new AttachmentReference(imageName, documentReference);
        
        return this.getURL(ref, extraParameters);
    }

    /**
     * Gets a boundary property (x, y, width or height) from the thumbnail object.
     * 
     * @param docRef the reference of the document to get the property from
     * @param classRef the reference of the class of the object holding the property
     * @param objectNumber the number of the object of this class in the document
     * @param parameterName the name of the property
     * @return the value for that property
     */
    private String getBoundaryProperty(DocumentReference docRef, DocumentReference classRef, int objectNumber,
        String parameterName)
    {
        return ((Integer) this.documentAccessBridge.getProperty(docRef, classRef, objectNumber, parameterName))
            .toString();
    }
    
    /**
     * Appends parameters to an existing URL.
     * 
     * @param url the URL to append query string parameters to
     * @param extraParameters the parameters to appen
     * @return the URL, possibly enriched with some extra query string parameters
     */
    private String appendExtraParameters(String url, Map<String, Object> extraParameters)
    {
        if (url == null) {
            return url;
        }
        String result = url;
        if (extraParameters != null && !extraParameters.isEmpty()) {
            if (result.indexOf(QUESTION_MARK) < 0) {
                result += QUESTION_MARK;
            }
            for (String parameter : extraParameters.keySet()) {
                Object value = extraParameters.get(parameter);
                Collection<Object> values;
                if (value instanceof Collection) {
                    values = (Collection<Object>) value;
                } else {
                    values = Arrays.asList(value);
                }
                for (Object v : values) {
                    result += "&" + parameter + "=" + v;
                }
            }
        }
        return result;
    }

    /**
     * {@inheritDoc}
     * 
     * @see ThumbnailURLProvider#getURL(AttachmentReference)
     */
    public String getURL(AttachmentReference reference) throws NoSuchImageException
    {
        return this.getURL(reference, Collections.<String, Object> emptyMap());
    }

    /**
     * {@inheritDoc}
     * 
     * @see ThumbnailURLProvider#getURL(AttachmentReference, Map<String,String>)
     */
    public String getURL(AttachmentReference reference, Map<String, Object> extraParameters)
        throws NoSuchImageException
    {
        if (reference == null) {
            throw new NoSuchImageException();
        }
        DocumentReference documentReference = (DocumentReference) reference.extractReference(EntityType.DOCUMENT);

        Map<String, String> attachmentDocumentCacheEntries =
            this.getThumbnailURLCache().get((String) this.serializer.serialize(documentReference));
        
        if (attachmentDocumentCacheEntries == null || !attachmentDocumentCacheEntries.containsKey(reference)) {
            try {
                if (attachmentDocumentCacheEntries == null) {
                    attachmentDocumentCacheEntries = new HashMap<String, String>();
                }
                String imageName = reference.getName();
                
                if (imageName == null) {
                    throw new NoSuchImageException();
                }
                
                this.getThumbnailURLCache().set((String) this.serializer.serialize(documentReference),
                    this.fillCacheEntry(documentReference, attachmentDocumentCacheEntries, imageName));
                
            } catch (ClassCastException e) {
                // The reference is not valid, or the property is not a string
                throw new NoSuchImageException();
            }
        }
        return this.appendExtraParameters(
            this.getThumbnailURLCache().get((String) this.serializer.serialize(documentReference))
                .get(reference.getName()), extraParameters);
    }


    /**
     * Helper method to fill a cache entry with a pair imageName/URL.
     * 
     * @param reference the reference of the document to update cache entry for.
     * @param entry the existing cache entries for this document.
     * @param imageName the name of the image to add the entry for.
     * @return the updated entry for this document.
     */
    private Map<String, String> fillCacheEntry(DocumentReference reference, Map<String, String> entry, String imageName)
    {
        Map<String, String> result = new HashMap<String, String>(entry);
        DocumentReference thumbnailClass =
            new DocumentReference(reference.getWikiReference().getName(), "XWiki", "ThumbnailClass");
        AttachmentReference attachmentReference = new AttachmentReference(imageName, reference);
        int thumbnailObjectNumber =
            this.documentAccessBridge.getObjectNumber(reference, thumbnailClass, "image", imageName);

        String queryString = "";
        if (thumbnailObjectNumber >= 0) {
            queryString =
                MessageFormat.format("boundaries={0},{1},{2},{3}",
                    this.getBoundaryProperty(reference, thumbnailClass, thumbnailObjectNumber, "x"),
                    this.getBoundaryProperty(reference, thumbnailClass, thumbnailObjectNumber, "y"),
                    this.getBoundaryProperty(reference, thumbnailClass, thumbnailObjectNumber, "width"),
                    this.getBoundaryProperty(reference, thumbnailClass, thumbnailObjectNumber, "height"));
        }
        String url = this.documentAccessBridge.getAttachmentURL(attachmentReference, queryString, false);
        result.put(imageName, url);
        return result;
    }
    
    /**
     * Get (and lazily initializes if needed) the cache of property documents.
     * 
     * @return the thumbnail URL cache.
     */
    private Cache<Map<String, String>> getThumbnailURLCache()
    {
        if (this.thumbnailURLCache == null) {
            CacheConfiguration configuration = new CacheConfiguration();

            configuration.setConfigurationId("xwiki.thumbnails.url");
            // Set cache constraints.
            LRUEvictionConfiguration lru = new LRUEvictionConfiguration();
            configuration.put(LRUEvictionConfiguration.CONFIGURATIONID, lru);

            lru.setMaxEntries(CACHE_MAX_ENTRIES);

            try {
                this.thumbnailURLCache = this.cacheManager.createNewCache(configuration);
            } catch (CacheException e) {
                this.logger.error("Error initializing the property document cache.", e);
            }
        }
        return this.thumbnailURLCache;
    }
    
    /**
     * {@inheritDoc}
     */
    public List<Event> getEvents()
    {
        return Arrays.<Event> asList(new DocumentUpdatedEvent());
    }

    /**
     * {@inheritDoc}
     */
    public String getName()
    {
        return "thumbnailURLProvider";
    }

    /**
     * {@inheritDoc}
     */
    public void onEvent(Event event, Object data, Object arg2)
    {
        try {
            DocumentModelBridge document = (DocumentModelBridge) data;
            if (this.getThumbnailURLCache().get(
                (String) this.serializer.serialize(document.getDocumentReference())) != null) {

                // Pessimistic cache flushing : if a document with a property pointing to an image with
                // thumbnail has been saved ; just remove this document from the cache.

                this.getThumbnailURLCache().remove((String) this.serializer.serialize(document.getDocumentReference()));
            }
            
        } catch (ClassCastException e) {
            // Should not happen, but if it does, silently do nothing !
        }
    }   
}