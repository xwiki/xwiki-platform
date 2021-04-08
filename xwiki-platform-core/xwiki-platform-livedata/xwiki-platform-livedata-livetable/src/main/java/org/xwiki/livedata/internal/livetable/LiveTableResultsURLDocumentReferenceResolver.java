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
package org.xwiki.livedata.internal.livetable;

import java.net.URL;
import java.util.Collections;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.resource.ResourceReference;
import org.xwiki.resource.ResourceReferenceResolver;
import org.xwiki.resource.ResourceType;
import org.xwiki.resource.ResourceTypeResolver;
import org.xwiki.resource.entity.EntityResourceReference;
import org.xwiki.url.ExtendedURL;

import com.xpn.xwiki.XWikiContext;

/**
 * Attempts to extract the target document reference from the live table results URL.
 * 
 * @version $Id$
 */
@Component(roles = LiveTableResultsURLDocumentReferenceResolver.class)
@Singleton
public class LiveTableResultsURLDocumentReferenceResolver
{
    @Inject
    private Logger logger;

    @Inject
    private Provider<XWikiContext> xcontextProvider;

    @Inject
    private ResourceTypeResolver<ExtendedURL> typeResolver;

    @Inject
    private ResourceReferenceResolver<ExtendedURL> resourceResolver;

    @Inject
    @Named("current")
    private DocumentReferenceResolver<EntityReference> currentEntityDocumentReferenceResolver;

    @Inject
    private EntityReferenceSerializer<String> defaultEntityReferenceSerializer;

    /**
     * Attempts to extract the target document reference from the live table results URL.
     * 
     * @param liveTableResultsURL the live table results URL
     * @return the reference of the target document
     */
    public String resolve(String liveTableResultsURL)
    {
        XWikiContext xcontext = this.xcontextProvider.get();

        try {
            URL url = new URL(xcontext.getURL(), liveTableResultsURL);
            ExtendedURL extendedURL = new ExtendedURL(url, xcontext.getRequest().getContextPath());
            ResourceType type = this.typeResolver.resolve(extendedURL, Collections.emptyMap());
            ResourceReference reference = this.resourceResolver.resolve(extendedURL, type, Collections.emptyMap());
            if (reference instanceof EntityResourceReference) {
                EntityReference entityReference = ((EntityResourceReference) reference).getEntityReference();
                DocumentReference documentReference =
                    this.currentEntityDocumentReferenceResolver.resolve(entityReference);
                return this.defaultEntityReferenceSerializer.serialize(documentReference);
            }
        } catch (Exception e) {
            this.logger.debug("Failed to extract a document reference from [{}].", liveTableResultsURL, e);
        }

        return null;
    }
}
