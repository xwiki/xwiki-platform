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
package com.xpn.xwiki.internal.redirection;

import java.io.IOException;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.commons.lang3.StringUtils;
import org.xwiki.component.annotation.Component;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.WikiReference;
import org.xwiki.resource.ResourceReference;
import org.xwiki.resource.ResourceReferenceManager;
import org.xwiki.resource.entity.EntityResourceReference;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.internal.mandatory.RedirectClassDocumentInitializer;
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.redirection.RedirectionFilter;

/**
 * Perform a redirection based on the presence of a {@code XWiki.RedirectClass} XClass.
 *
 * @version $Id$
 * @since 14.0RC1
 */
@Component
@Singleton
@Named("XWiki.RedirectClass")
public class RedirectClassRedirectionFilter implements RedirectionFilter
{
    @Inject
    private DocumentReferenceResolver<String> resolver;
    
    @Inject
    private ResourceReferenceManager resourceReferenceManager;
    
    @Override
    public boolean redirect(XWikiContext context) throws XWikiException
    {
        WikiReference wikiReference = context.getWikiReference();

        // Look if the document has a redirect object
        XWikiDocument doc = context.getDoc();
        BaseObject redirectObj = doc.getXObject(RedirectClassDocumentInitializer.REFERENCE);
        if (redirectObj == null) {
            return false;
        }

        // Get the location
        String location = redirectObj.getStringValue("location");
        if (StringUtils.isBlank(location)) {
            return false;
        }

        // Resolve the location to get a reference.
        EntityReference locationReference = this.resolver.resolve(location, wikiReference);

        // Get the type of the current target
        ResourceReference resourceReference = this.resourceReferenceManager.getResourceReference();
        EntityResourceReference entityResource = (EntityResourceReference) resourceReference;
        EntityReference entityReference = entityResource.getEntityReference();

        // If the entity is inside a document, compute the new entity with the new document part.
        if (entityReference.getType().ordinal() > EntityType.DOCUMENT.ordinal()) {
            EntityReference parentDocument = entityReference.extractReference(EntityType.DOCUMENT);
            locationReference = entityReference.replaceParent(parentDocument, locationReference);
        }

        String url = context.getWiki().getURL(locationReference, context.getAction(),
            context.getRequest().getQueryString(), null, context);

        // Send the redirection
        try {
            context.getResponse().sendRedirect(url);
        } catch (IOException e) {
            throw new XWikiException("Failed to redirect.", e);
        }

        return true;
    }
}
