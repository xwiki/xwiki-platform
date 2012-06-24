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
package org.xwiki.extension.repository.xwiki.internal.xhtml;

import java.net.URI;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;

import org.xwiki.component.annotation.Component;
import org.xwiki.component.annotation.InstantiationStrategy;
import org.xwiki.component.descriptor.ComponentInstantiationStrategy;
import org.xwiki.context.Execution;
import org.xwiki.extension.repository.xwiki.Resources;
import org.xwiki.extension.repository.xwiki.UriBuilder;
import org.xwiki.extension.repository.xwiki.internal.reference.ExtensionResourceReference;
import org.xwiki.rendering.internal.renderer.xhtml.link.AbstractXHTMLLinkTypeRenderer;
import org.xwiki.rendering.internal.renderer.xhtml.link.XHTMLLinkRenderer;
import org.xwiki.rendering.listener.reference.ResourceReference;

import com.xpn.xwiki.XWikiContext;

/**
 * Handle XHTML rendering for links to extensions.
 * 
 * @version $Id$
 * @since 3.4RC1
 */
@Component
@Named("extension")
@InstantiationStrategy(ComponentInstantiationStrategy.PER_LOOKUP)
public class ExtensionXHTMLLinkTypeRenderer extends AbstractXHTMLLinkTypeRenderer
{
    @Inject
    private Execution execution;

    private XWikiContext getXWikiContext()
    {
        return (XWikiContext) this.execution.getContext().getProperty(XWikiContext.EXECUTIONCONTEXT_KEY);
    }

    @Override
    protected void beginLinkExtraAttributes(ResourceReference reference, Map<String, String> spanAttributes,
        Map<String, String> anchorAttributes)
    {
        XWikiContext xcontext = getXWikiContext();

        String prefix = xcontext.getWiki().getWebAppPath(xcontext);
        if (prefix.isEmpty() || prefix.charAt(0) != '/') {
            prefix = '/' + prefix;
        }
        if (prefix.charAt(prefix.length() - 1) != '/') {
            prefix += '/';
        }
        // FIXME: need a way to get the rest URL prefix instead of hardcoding it here
        prefix += "rest";

        // Create an URI (because the API only produces URIs) with a stub context we will then remove since we actually
        // want a relative HREF
        UriBuilder builder = new UriBuilder("stubscheme:" + prefix, Resources.EXTENSION_VERSION_FILE);

        ExtensionResourceReference extensionReference = (ExtensionResourceReference) reference;

        if (extensionReference.getRepositoryId() != null) {
            builder.queryParam(ExtensionResourceReference.PARAM_REPOSITORYID, extensionReference.getRepositoryId());
        }
        if (extensionReference.getRepositoryType() != null) {
            builder.queryParam(ExtensionResourceReference.PARAM_REPOSITORYTYPE, extensionReference.getRepositoryType());
        }
        if (extensionReference.getRepositoryURI() != null) {
            builder.queryParam(ExtensionResourceReference.PARAM_REPOSITORYURI, extensionReference.getRepositoryURI());
        }

        URI uri = builder.build(extensionReference.getExtensionId(), extensionReference.getExtensionVersion());

        anchorAttributes.put(XHTMLLinkRenderer.HREF, uri.toString().substring("stubscheme://".length()));
    }
}
