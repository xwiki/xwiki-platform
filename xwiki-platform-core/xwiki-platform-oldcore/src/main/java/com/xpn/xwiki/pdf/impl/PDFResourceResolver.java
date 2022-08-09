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
package com.xpn.xwiki.pdf.impl;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.util.Collections;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.apache.fop.apps.io.ResourceResolverFactory;
import org.apache.xmlgraphics.io.Resource;
import org.apache.xmlgraphics.io.ResourceResolver;
import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.component.util.DefaultParameterizedType;
import org.xwiki.resource.ResourceLoader;
import org.xwiki.resource.ResourceReference;
import org.xwiki.resource.ResourceReferenceResolver;
import org.xwiki.resource.ResourceType;
import org.xwiki.resource.ResourceTypeResolver;
import org.xwiki.url.ExtendedURL;

import com.xpn.xwiki.XWikiContext;

/**
 * Resolves URIs sent by Apache FOP to embed images in the exported PDF. We bypass the standard resource resolver
 * provided for Resource that XWiki handles since that resolver simpy opens an un-authenticated URL Connection to get
 * the content and if the XWiki resource is protected (e.g. if an attachment is located in a document that requires
 * some permission to access), then the exported content in the PDF will be empty.
 *
 *
 * @version $Id$
 * @since 7.4M2
 */
@Component(roles = PDFResourceResolver.class)
@Singleton
public class PDFResourceResolver implements ResourceResolver
{
    @Inject
    private Logger logger;

    @Inject
    private Provider<XWikiContext> xcontextProvider;

    @Inject
    @Named("context")
    private Provider<ComponentManager> componentManagerProvider;

    @Inject
    private ResourceReferenceResolver<ExtendedURL> resourceReferenceResolver;

    @Inject
    private ResourceTypeResolver<ExtendedURL> resourceTypeResolver;

    private ResourceResolver standardResolver = ResourceResolverFactory.createDefaultResourceResolver();

    @Override
    public Resource getResource(URI uri) throws IOException
    {
        XWikiContext xcontext = this.xcontextProvider.get();

        // Note: handle the resources known by XWiki instead of delegating to the stadard resource provider in order
        // to overcome any permission required to access the resource from its URL. See the javadoc for this class
        // for more details.

        Resource result = null;
        ResourceReference resourceReference = buildTResourceReference(uri, xcontext);
        ResourceLoader<ResourceReference> resourceLoader = null;
        if (resourceReference != null) {
            try {
                resourceLoader = this.componentManagerProvider.get().getInstance(
                    new DefaultParameterizedType(null, ResourceLoader.class, resourceReference.getClass()));
            } catch (ComponentLookupException e) {
                // No resource loader for that resource reference type, fallback to FOP's standard resolver (which won't
                // work properly - ie no content included in the PDF - if the XWiki resource is protected)
            }
        }

        if (resourceLoader != null) {
            InputStream is = resourceLoader.load(resourceReference);
            if (is != null) {
                result = new Resource(is);
            }
        }

        if (result == null) {
            // If the URI points to the XWiki instance, then it's likely that it's going to cause a problem (the
            // resource won't be included in the PDF) if the XWiki instance has protected the access to that resource
            // (rights on a wiki page, etc). This is because FOP's standard resolver simply opens the passed URI
            // without any credentials.
            this.logger.debug("Fall-backing to the standard resolver for URI [{}]", uri);

            result = this.standardResolver.getResource(uri);
        }

        return result;
    }

    @Override
    public OutputStream getOutputStream(URI uri) throws IOException
    {
        // Not easy to implement in for attachment but not really needed in the context of PDF export anyway
        return this.standardResolver.getOutputStream(uri);
    }

    private ResourceReference buildTResourceReference(URI uri, XWikiContext xcontext)
    {
        ResourceReference result = null;

        // To improve performance, only try to recognize an XWiki URL if the scheme is HTTP or HTTPS
        if ("http".equals(uri.getScheme()) || "https".equals(uri.getScheme())) {
            try {
                ExtendedURL extendedURL = new ExtendedURL(uri.toURL(), xcontext.getRequest().getContextPath());
                ResourceType resourceType = this.resourceTypeResolver.resolve(extendedURL, Collections.emptyMap());
                result = this.resourceReferenceResolver.resolve(extendedURL, resourceType, Collections.emptyMap());
            } catch (Exception e) {
                // It may not be an XWiki URL, don't fail and instead return null to signify that we should fallback
                // to the standard resolver.
            }
        }

        return result;
    }
}
