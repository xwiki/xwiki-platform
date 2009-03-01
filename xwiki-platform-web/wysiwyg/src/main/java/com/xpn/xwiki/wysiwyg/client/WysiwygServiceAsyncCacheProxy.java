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
import com.xpn.xwiki.wysiwyg.client.diff.Revision;
import com.xpn.xwiki.wysiwyg.client.plugin.image.ImageConfig;
import com.xpn.xwiki.wysiwyg.client.plugin.link.LinkConfig;
import com.xpn.xwiki.wysiwyg.client.plugin.macro.MacroDescriptor;
import com.xpn.xwiki.wysiwyg.client.sync.SyncResult;

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
     * The cache for macro descriptors. The first key is the syntax identifier and the second is the macro name.
     */
    private final Map<String, Map<String, MacroDescriptor>> macroDescriptorsCache =
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
     * @see WysiwygServiceAsync#parseAndRender(String, String, AsyncCallback)
     */
    public void parseAndRender(String html, String syntax, AsyncCallback<String> async)
    {
        service.parseAndRender(html, syntax, async);
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
     * @see WysiwygServiceAsync#officeToXHTML(String, String, Map, AsyncCallback)
     */
    public void officeToXHTML(String pageName, String attachmentName, Map<String, String> cleaningParams,
        AsyncCallback<String> async)
    {
        service.officeToXHTML(pageName, attachmentName, cleaningParams, async);
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
     * @see WysiwygServiceAsync#toHTML(String, String, AsyncCallback)
     */
    public void toHTML(String source, String syntax, AsyncCallback<String> async)
    {
        service.toHTML(source, syntax, async);
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
     * @see WysiwygServiceAsync#getImageAttachments(String, String, String, AsyncCallback)
     */
    public void getImageAttachments(String wikiName, String spaceName, String pageName,
        AsyncCallback<List<ImageConfig>> async)
    {
        service.getImageAttachments(wikiName, spaceName, pageName, async);
    }

    /**
     * {@inheritDoc}
     * 
     * @see WysiwygServiceAsync#getMacroDescriptor(String, String, AsyncCallback)
     */
    public void getMacroDescriptor(final String macroName, final String syntax,
        final AsyncCallback<MacroDescriptor> async)
    {
        // First let's look in the cache.
        Map<String, MacroDescriptor> macroDescriptorsForSyntax = macroDescriptorsCache.get(syntax);
        if (macroDescriptorsForSyntax != null) {
            MacroDescriptor descriptor = macroDescriptorsForSyntax.get(macroName);
            if (descriptor != null) {
                async.onSuccess(descriptor);
                return;
            }
        }
        // The macro descriptor wasn't found in the cache. We have to make the request to the server.
        service.getMacroDescriptor(macroName, syntax, new AsyncCallback<MacroDescriptor>()
        {
            public void onFailure(Throwable caught)
            {
                async.onFailure(caught);
            }

            public void onSuccess(MacroDescriptor result)
            {
                if (result != null) {
                    Map<String, MacroDescriptor> macroDescriptorsForSyntax = macroDescriptorsCache.get(syntax);
                    if (macroDescriptorsForSyntax == null) {
                        macroDescriptorsForSyntax = new HashMap<String, MacroDescriptor>();
                        macroDescriptorsCache.put(syntax, macroDescriptorsForSyntax);
                    }
                    macroDescriptorsForSyntax.put(macroName, result);
                }
                async.onSuccess(result);
            }
        });
    }
}
