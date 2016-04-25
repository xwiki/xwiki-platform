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
package org.xwiki.gwt.wysiwyg.client.wiki;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.gwt.user.client.rpc.AsyncCallback;

/**
 * Cache proxy for {@link WikiServiceAsync}. This proxy is used to store on the client a set of values from the server
 * that cannot change without server restart, therefore without the reload of the page that holds the reference to the
 * {@link WikiService}.
 * 
 * @version $Id$
 */
public class WikiServiceAsyncCacheProxy implements WikiServiceAsync
{
    /**
     * The cached service.
     */
    private final WikiServiceAsync service;

    /**
     * Caches the multiwiki property for this wiki.
     */
    private Boolean isMultiWiki;

    /**
     * Caches the list of the virtual wikis from this multiwiki.
     */
    private List<String> virtualWikiNamesList;

    /**
     * The map where we cache the upload URLs.
     */
    private final Map<EntityReference, String> uploadURLCache = new HashMap<EntityReference, String>();

    /**
     * Creates a new cache proxy for the given service.
     * 
     * @param service the service to be cached.
     */
    public WikiServiceAsyncCacheProxy(WikiServiceAsync service)
    {
        this.service = service;
    }

    @Override
    public void getPageNames(String wikiName, String spaceName, AsyncCallback<List<String>> async)
    {
        service.getPageNames(wikiName, spaceName, async);
    }

    @Override
    public void getRecentlyModifiedPages(String wikiName, int start, int count, AsyncCallback<List<WikiPage>> async)
    {
        service.getRecentlyModifiedPages(wikiName, start, count, async);
    }

    @Override
    public void getMatchingPages(String wikiName, String keyword, int start, int count,
        AsyncCallback<List<WikiPage>> async)
    {
        service.getMatchingPages(wikiName, keyword, start, count, async);
    }

    @Override
    public void getSpaceNames(String wikiName, AsyncCallback<List<String>> async)
    {
        service.getSpaceNames(wikiName, async);
    }

    @Override
    public void getVirtualWikiNames(final AsyncCallback<List<String>> async)
    {
        if (virtualWikiNamesList == null) {
            service.getVirtualWikiNames(new AsyncCallback<List<String>>()
            {
                @Override
                public void onFailure(Throwable caught)
                {
                    async.onFailure(caught);
                }

                @Override
                public void onSuccess(List<String> result)
                {
                    virtualWikiNamesList = result;
                    async.onSuccess(virtualWikiNamesList);
                }
            });
        } else {
            async.onSuccess(virtualWikiNamesList);
        }
    }

    @Override
    public void isMultiWiki(final AsyncCallback<Boolean> async)
    {
        if (isMultiWiki == null) {
            service.isMultiWiki(new AsyncCallback<Boolean>()
            {
                @Override
                public void onFailure(Throwable caught)
                {
                    async.onFailure(caught);
                }

                @Override
                public void onSuccess(Boolean result)
                {
                    isMultiWiki = result;
                    async.onSuccess(isMultiWiki);
                }
            });
        } else {
            async.onSuccess(isMultiWiki);
        }
    }

    @Override
    public void getEntityConfig(EntityReference base, ResourceReference target, AsyncCallback<EntityConfig> async)
    {
        service.getEntityConfig(base, target, async);
    }

    @Override
    public void getAttachment(AttachmentReference attachmentReference, AsyncCallback<Attachment> async)
    {
        service.getAttachment(attachmentReference, async);
    }

    @Override
    public void getImageAttachments(WikiPageReference documentReference, AsyncCallback<List<Attachment>> async)
    {
        service.getImageAttachments(documentReference, async);
    }

    @Override
    public void getAttachments(WikiPageReference documentReference, AsyncCallback<List<Attachment>> async)
    {
        service.getAttachments(documentReference, async);
    }

    @Override
    public void getUploadURL(final WikiPageReference documentReference, final AsyncCallback<String> async)
    {
        if (uploadURLCache.containsKey(documentReference.getEntityReference())) {
            async.onSuccess(uploadURLCache.get(documentReference.getEntityReference()));
        } else {
            service.getUploadURL(documentReference, new AsyncCallback<String>()
            {
                @Override
                public void onFailure(Throwable caught)
                {
                    async.onFailure(caught);
                }

                @Override
                public void onSuccess(String result)
                {
                    uploadURLCache.put(documentReference.getEntityReference().clone(), result);
                    async.onSuccess(result);
                }
            });
        }
    }

    @Override
    public void parseLinkReference(String linkReference, EntityReference baseReference,
        AsyncCallback<ResourceReference> async)
    {
        service.parseLinkReference(linkReference, baseReference, async);
    }
}
