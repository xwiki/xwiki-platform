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
package com.xpn.xwiki.web;

import java.io.IOException;

import org.apache.commons.lang3.StringUtils;
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

/**
 * Add a backward compatibility layer to {@link XWikiAction}.
 * 
 * @version $Id$
 */
public privileged aspect XWikiActionCompatibilityAspect
{
    // handleRedirectObject and handleRedirectObject while previously {@code protected} are now {@code public} due of 
    // technical limitations of AspectJ.
    // See https://doanduyhai.wordpress.com/2011/12/12/advanced-aspectj-part-ii-inter-type-declaration/
    // "If their is one thing to remember from access modifier, it’s that their semantic applies with respect to the
    // declaring aspect, and not to the target."
    // They must still be used as if {@code protected}.
    /**
     * Indicate if the XWiki.RedirectClass is handled by the action (see handleRedirectObject()).
     * 
     * @deprecated since 14.0RC1, see {@link XWikiAction#supportRedirections()}
     */
    @Deprecated
    public boolean XWikiAction.handleRedirectObject = false;

    /**
     * Redirect the user to an other location if the document holds an XWiki.RedirectClass instance (used when a
     * document is moved).
     *
     * @param context the XWiki context
     * @return either or not a redirection have been sent
     * @throws XWikiException if error occurs
     * @since 8.0RC1
     * @since 7.4.2
     * 
     * @deprecated since 14.0RC1, not used anymore
     */
    @Deprecated
    public boolean XWikiAction.handleRedirectObject(XWikiContext context) throws XWikiException
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

        // Resolve the location to get a reference
        DocumentReferenceResolver<String> resolver = Utils.getComponent(DocumentReferenceResolver.TYPE_STRING);
        EntityReference locationReference = resolver.resolve(location, wikiReference);

        // Get the type of the current target
        ResourceReference resourceReference = Utils.getComponent(ResourceReferenceManager.class).getResourceReference();
        EntityResourceReference entityResource = (EntityResourceReference) resourceReference;
        EntityReference entityReference = entityResource.getEntityReference();

        // If the entity is inside a document, compute the new entity with the new document part.
        if (entityReference.getType().ordinal() > EntityType.DOCUMENT.ordinal()) {
            EntityReference parentDocument = entityReference.extractReference(EntityType.DOCUMENT);
            locationReference = entityReference.replaceParent(parentDocument, locationReference);
        }

        // Get the URL corresponding to the location
        // Note: the anchor part is lost in the process, because it is not sent to the server
        // (see: http://stackoverflow.com/a/4276491)
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
