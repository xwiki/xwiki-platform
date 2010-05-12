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

import java.util.List;

import org.xwiki.gwt.wysiwyg.client.wiki.EntityReference.EntityType;

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
     * Creates a new cache proxy for the given service.
     * 
     * @param service the service to be cached.
     */
    public WikiServiceAsyncCacheProxy(WikiServiceAsync service)
    {
        this.service = service;
    }

    /**
     * {@inheritDoc}
     * 
     * @see WikiServiceAsync#getPageNames(String, String, AsyncCallback)
     */
    public void getPageNames(String wikiName, String spaceName, AsyncCallback<List<String>> async)
    {
        service.getPageNames(wikiName, spaceName, async);
    }

    /**
     * {@inheritDoc}
     */
    public void getRecentlyModifiedPages(int start, int count, AsyncCallback<List<WikiPage>> async)
    {
        service.getRecentlyModifiedPages(start, count, async);
    }

    /**
     * {@inheritDoc}
     */
    public void getMatchingPages(String keyword, int start, int count, AsyncCallback<List<WikiPage>> async)
    {
        service.getMatchingPages(keyword, start, count, async);
    }

    /**
     * {@inheritDoc}
     * 
     * @see WikiServiceAsync#getSpaceNames(String, AsyncCallback)
     */
    public void getSpaceNames(String wikiName, AsyncCallback<List<String>> async)
    {
        service.getSpaceNames(wikiName, async);
    }

    /**
     * {@inheritDoc}
     * 
     * @see WikiServiceAsync#getVirtualWikiNames(AsyncCallback)
     */
    public void getVirtualWikiNames(final AsyncCallback<List<String>> async)
    {
        if (virtualWikiNamesList == null) {
            service.getVirtualWikiNames(new AsyncCallback<List<String>>()
            {
                public void onFailure(Throwable caught)
                {
                    async.onFailure(caught);
                }

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

    /**
     * {@inheritDoc}
     * 
     * @see WikiServiceAsync#isMultiWiki(AsyncCallback)
     */
    public void isMultiWiki(final AsyncCallback<Boolean> async)
    {
        if (isMultiWiki == null) {
            service.isMultiWiki(new AsyncCallback<Boolean>()
            {
                public void onFailure(Throwable caught)
                {
                    async.onFailure(caught);
                }

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

    /**
     * {@inheritDoc}
     * 
     * @see WikiServiceAsync#getPageLink(EntityReference, EntityReference, AsyncCallback)
     */
    public void getEntityConfig(EntityReference origin, EntityReference destination, AsyncCallback<EntityConfig> async)
    {
        service.getEntityConfig(origin, destination, async);
    }

    /**
     * {@inheritDoc}
     * 
     * @see WikiServiceAsync#getAttachment(EntityReference, AsyncCallback)
     */
    public void getAttachment(EntityReference attachmentReference, AsyncCallback<Attachment> async)
    {
        service.getAttachment(attachmentReference, async);
    }

    /**
     * {@inheritDoc}
     * 
     * @see WikiServiceAsync#getImageAttachments(EntityReference, AsyncCallback)
     */
    public void getImageAttachments(EntityReference documentReference, AsyncCallback<List<Attachment>> async)
    {
        service.getImageAttachments(documentReference, async);
    }

    /**
     * {@inheritDoc}
     * 
     * @see WikiServiceAsync#getAttachments(EntityReference, AsyncCallback)
     */
    public void getAttachments(EntityReference documentReference, AsyncCallback<List<Attachment>> async)
    {
        service.getAttachments(documentReference, async);
    }

    /**
     * {@inheritDoc}
     * 
     * @see WikiServiceAsync#getUploadURL(EntityReference, AsyncCallback)
     */
    public void getUploadURL(EntityReference documentReference, AsyncCallback<String> async)
    {
        service.getUploadURL(documentReference, async);
    }

    /**
     * {@inheritDoc}
     * 
     * @see WikiServiceAsync#parseLinkReference(String, EntityType, EntityReference, AsyncCallback)
     */
    public void parseLinkReference(String linkReference, EntityType entityType, EntityReference baseReference,
        AsyncCallback<EntityReference> async)
    {
        service.parseLinkReference(linkReference, entityType, baseReference, async);
    }
}
