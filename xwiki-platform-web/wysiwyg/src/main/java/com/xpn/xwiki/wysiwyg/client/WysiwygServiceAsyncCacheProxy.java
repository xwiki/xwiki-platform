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
package com.xpn.xwiki.wysiwyg.client;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.xpn.xwiki.gwt.api.client.Document;
import com.xpn.xwiki.wysiwyg.client.diff.Revision;
import com.xpn.xwiki.wysiwyg.client.plugin.link.LinkConfig;
import com.xpn.xwiki.wysiwyg.client.plugin.macro.MacroDescriptor;
import com.xpn.xwiki.wysiwyg.client.sync.SyncResult;
import com.xpn.xwiki.wysiwyg.client.util.Attachment;

/**
 * Cache proxy for {@link WysiwygServiceAsync}. This proxy is used to store on the client a set of values from the
 * server that cannot change without server restart, therefore without the reload of the page that holds the reference
 * to the {@link WysiwygService}.
 * 
 * @version $Id$
 */
public class WysiwygServiceAsyncCacheProxy implements WysiwygServiceAsync
{
    /**
     * The cached service.
     */
    private final WysiwygServiceAsync service;

    /**
     * Caches the multiwiki property for this wiki.
     */
    private Boolean isMultiWiki;

    /**
     * Caches the list of the virtual wikis from this multiwiki.
     */
    private List<String> virtualWikiNamesList;

    /**
     * Caches the list of macro descriptors for each syntax.
     * 
     * @see #getMacroDescriptors(String, AsyncCallback)
     * @see #getMacroDescriptor(String, String, AsyncCallback)
     */
    private final Map<String, List<MacroDescriptor>> macroDescriptorList = new HashMap<String, List<MacroDescriptor>>();

    /**
     * The cache for macro descriptors. The first key is the syntax identifier and the second is the macro name.
     * 
     * @see #getMacroDescriptor(String, String, AsyncCallback)
     */
    private final Map<String, Map<String, MacroDescriptor>> macroDescriptorMap =
        new HashMap<String, Map<String, MacroDescriptor>>();

    /**
     * Creates a new cache proxy for the given service.
     * 
     * @param service the service to be cached.
     */
    public WysiwygServiceAsyncCacheProxy(WysiwygServiceAsync service)
    {
        this.service = service;
    }

    /**
     * {@inheritDoc}
     * 
     * @see WysiwygServiceAsync#cleanHTML(String, AsyncCallback)
     */
    public void cleanHTML(String dirtyHTML, AsyncCallback<String> async)
    {
        service.cleanHTML(dirtyHTML, async);
    }

    /**
     * {@inheritDoc}
     * 
     * @see WysiwygServiceAsync#cleanOfficeHTML(String, String, Map, AsyncCallback)
     */
    public void cleanOfficeHTML(String htmlPaste, String cleanerHint, Map<String, String> cleaningParams,
        AsyncCallback<String> async)
    {
        service.cleanOfficeHTML(htmlPaste, cleanerHint, cleaningParams, async);
    }

    /**
     * {@inheritDoc}
     * 
     * @see WysiwygServiceAsync#officeToXHTML(String, Map, AsyncCallback)
     */
    public void officeToXHTML(String pageName, Map<String, String> cleaningParams, AsyncCallback<String> async)
    {
        service.officeToXHTML(pageName, cleaningParams, async);
    }
    
    /**
     * {@inheritDoc}
     * 
     * @see WysiwygServiceAsync#officeToXHTML(Attachment, Map, AsyncCallback)
     */
    public void officeToXHTML(Attachment attachment, Map<String, String> cleaningParams, AsyncCallback<String> async)
    {
        service.officeToXHTML(attachment, cleaningParams, async);
    }

    /**
     * {@inheritDoc}
     * 
     * @see WysiwygServiceAsync#fromHTML(String, String, AsyncCallback)
     */
    public void fromHTML(String html, String syntax, AsyncCallback<String> async)
    {
        service.fromHTML(html, syntax, async);
    }

    /**
     * {@inheritDoc}
     * 
     * @see WysiwygServiceAsync#syncEditorContent(Revision, String, int, boolean, AsyncCallback)
     */
    public void syncEditorContent(Revision syncedRevision, String pageName, int version, boolean syncReset,
        AsyncCallback<SyncResult> async)
    {
        service.syncEditorContent(syncedRevision, pageName, version, syncReset, async);
    }

    /**
     * {@inheritDoc}
     * 
     * @see WysiwygServiceAsync#getPageNames(String, String, AsyncCallback)
     */
    public void getPageNames(String wikiName, String spaceName, AsyncCallback<List<String>> async)
    {
        service.getPageNames(wikiName, spaceName, async);
    }

    /**
     * {@inheritDoc}
     */
    public void getRecentlyModifiedPages(int start, int count, AsyncCallback<List<Document>> async)
    {
        service.getRecentlyModifiedPages(start, count, async);
    }

    /**
     * {@inheritDoc}
     */
    public void getMatchingPages(String keyword, int start, int count, AsyncCallback<List<Document>> async)
    {
        service.getMatchingPages(keyword, start, count, async);
    }

    /**
     * {@inheritDoc}
     * 
     * @see WysiwygServiceAsync#getSpaceNames(String, AsyncCallback)
     */
    public void getSpaceNames(String wikiName, AsyncCallback<List<String>> async)
    {
        service.getSpaceNames(wikiName, async);
    }

    /**
     * {@inheritDoc}
     * 
     * @see WysiwygServiceAsync#getVirtualWikiNames(AsyncCallback)
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
     * @see WysiwygServiceAsync#isMultiWiki(AsyncCallback)
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
     * @see WysiwygServiceAsync#getPageLink(String, String, String, String, String, AsyncCallback)
     */
    public void getPageLink(String wikiName, String spaceName, String pageName, String revision, String anchor,
        AsyncCallback<LinkConfig> async)
    {
        service.getPageLink(wikiName, spaceName, pageName, revision, anchor, async);
    }

    /**
     * {@inheritDoc}
     * 
     * @see WysiwygServiceAsync#getAttachment(String, String, String, String, AsyncCallback)
     */
    public void getAttachment(String wikiName, String spaceName, String pageName, String attachmentName,
        AsyncCallback<Attachment> async)
    {
        service.getAttachment(wikiName, spaceName, pageName, attachmentName, async);
    }

    /**
     * {@inheritDoc}
     * 
     * @see WysiwygServiceAsync#getImageAttachments(String, String, String, AsyncCallback)
     */
    public void getImageAttachments(String wikiName, String spaceName, String pageName,
        AsyncCallback<List<Attachment>> async)
    {
        service.getImageAttachments(wikiName, spaceName, pageName, async);
    }

    /**
     * {@inheritDoc}
     * 
     * @see WysiwygServiceAsync#getAttachments(String, String, String, AsyncCallback)
     */
    public void getAttachments(String wikiName, String spaceName, String pageName,
        AsyncCallback<List<Attachment>> async)
    {
        service.getAttachments(wikiName, spaceName, pageName, async);
    }

    /**
     * {@inheritDoc}
     * 
     * @see WysiwygServiceAsync#getMacroDescriptor(String, String, AsyncCallback)
     */
    public void getMacroDescriptor(final String macroId, final String syntaxId,
        final AsyncCallback<MacroDescriptor> async)
    {
        // First let's look in the cache.
        Map<String, MacroDescriptor> macroDescriptorMapForSyntax = macroDescriptorMap.get(syntaxId);
        if (macroDescriptorMapForSyntax != null) {
            MacroDescriptor descriptor = macroDescriptorMapForSyntax.get(macroId);
            if (descriptor != null) {
                async.onSuccess(descriptor);
                return;
            }
        }
        List<MacroDescriptor> macroDescriptorListForSyntax = macroDescriptorList.get(syntaxId);
        if (macroDescriptorListForSyntax != null) {
            for (MacroDescriptor descriptor : macroDescriptorListForSyntax) {
                if (macroId.equals(descriptor.getId())) {
                    cacheMacroDescriptor(descriptor, syntaxId);
                    async.onSuccess(descriptor);
                    return;
                }
            }
        }
        // The macro descriptor wasn't found in the cache. We have to make the request to the server.
        service.getMacroDescriptor(macroId, syntaxId, new AsyncCallback<MacroDescriptor>()
        {
            public void onFailure(Throwable caught)
            {
                async.onFailure(caught);
            }

            public void onSuccess(MacroDescriptor result)
            {
                if (result != null) {
                    cacheMacroDescriptor(result, syntaxId);
                }
                async.onSuccess(result);
            }
        });
    }

    /**
     * Caches a macro descriptor.
     * 
     * @param descriptor the macro descriptor to be cached
     * @param syntaxId the syntax for which the macro was defined
     */
    private void cacheMacroDescriptor(MacroDescriptor descriptor, String syntaxId)
    {
        Map<String, MacroDescriptor> macroDescriptorsForSyntax = macroDescriptorMap.get(syntaxId);
        if (macroDescriptorsForSyntax == null) {
            macroDescriptorsForSyntax = new HashMap<String, MacroDescriptor>();
            macroDescriptorMap.put(syntaxId, macroDescriptorsForSyntax);
        }
        macroDescriptorsForSyntax.put(descriptor.getId(), descriptor);
    }

    /**
     * {@inheritDoc}
     * 
     * @see WysiwygServiceAsync#getMacroDescriptors(String, AsyncCallback)
     */
    public void getMacroDescriptors(final String syntaxId, final AsyncCallback<List<MacroDescriptor>> async)
    {
        List<MacroDescriptor> macroDescriptorListForSyntax = macroDescriptorList.get(syntaxId);
        if (macroDescriptorListForSyntax != null) {
            async.onSuccess(macroDescriptorListForSyntax);
        } else {
            service.getMacroDescriptors(syntaxId, new AsyncCallback<List<MacroDescriptor>>()
            {
                public void onFailure(Throwable caught)
                {
                    async.onFailure(caught);
                }

                public void onSuccess(List<MacroDescriptor> result)
                {
                    if (result != null) {
                        macroDescriptorList.put(syntaxId, result);
                    }
                    async.onSuccess(result);
                }
            });
        }
    }
}
