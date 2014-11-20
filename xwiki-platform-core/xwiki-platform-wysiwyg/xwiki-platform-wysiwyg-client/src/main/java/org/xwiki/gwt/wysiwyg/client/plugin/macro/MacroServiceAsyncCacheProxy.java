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
package org.xwiki.gwt.wysiwyg.client.plugin.macro;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.gwt.user.client.rpc.AsyncCallback;

/**
 * Cache proxy for {@link MacroServiceAsync}.
 * 
 * @version $Id$
 */
public class MacroServiceAsyncCacheProxy implements MacroServiceAsync
{
    /**
     * The cached service.
     */
    private final MacroServiceAsync service;

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
     * @param service the service to be cached
     */
    public MacroServiceAsyncCacheProxy(MacroServiceAsync service)
    {
        this.service = service;
    }

    @Override
    public void getMacroDescriptor(final String macroId, final String syntaxId, final String wikiId,
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

        // FIXME/TODO: Do we need an extra wiki level of caching?

        // The macro descriptor wasn't found in the cache. We have to make the request to the server.
        service.getMacroDescriptor(macroId, syntaxId, wikiId, new AsyncCallback<MacroDescriptor>()
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
     * {@inheritDoc}
     * 
     * @see MacroServiceAsync#getMacroDescriptor(String, String, AsyncCallback)
     */
    public void getMacroDescriptor(final String macroId, final String syntaxId,
        final AsyncCallback<MacroDescriptor> async)
    {
        getMacroDescriptor(macroId, syntaxId, null, async);
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

    @Override
    public void getMacroDescriptors(final String syntaxId, final String wikiId,
        final AsyncCallback<List<MacroDescriptor>> async)
    {
        List<MacroDescriptor> macroDescriptorListForSyntax = macroDescriptorList.get(syntaxId);
        if (macroDescriptorListForSyntax != null) {
            async.onSuccess(macroDescriptorListForSyntax);
        } else {
            service.getMacroDescriptors(syntaxId, wikiId, new AsyncCallback<List<MacroDescriptor>>()
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

    /**
     * {@inheritDoc}
     * 
     * @see MacroServiceAsync#getMacroDescriptors(String, AsyncCallback)
     */
    public void getMacroDescriptors(final String syntaxId, final AsyncCallback<List<MacroDescriptor>> async)
    {
        getMacroDescriptors(syntaxId, null, async);
    }
}
