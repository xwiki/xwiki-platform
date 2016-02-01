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
package org.xwiki.rendering.internal.parser.reference;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.commons.lang3.StringUtils;
import org.xwiki.component.annotation.Component;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.EntityReferenceProvider;
import org.xwiki.model.reference.EntityReferenceResolver;
import org.xwiki.rendering.listener.reference.ResourceReference;
import org.xwiki.rendering.parser.ResourceReferenceTypeParser;

/**
 * XWiki specific implementation that overrides and extends {@link DefaultUntypedLinkReferenceParser} by also adding the
 * special handling of "WebHome" links which should always be handled as document links (i.e. resolving to
 * {@code X.WebHome}) and never space links (i.e. resolving to {@code X.WebHome.WebHome}).
 *
 * @version $Id$
 * @since 7.4.1, 8.0M1
 */
@Component
@Named("link/untyped")
@Singleton
public class XWikiUntypedLinkReferenceParser extends DefaultUntypedLinkReferenceParser
{
    @Inject
    private EntityReferenceProvider defaultReferenceProvider;

    /**
     * Parser to parse link references pointing to documents.
     */
    @Inject
    @Named("doc")
    private ResourceReferenceTypeParser documentResourceReferenceTypeParser;

    /**
     * Parser to parse link references pointing to spaces.
     */
    @Inject
    @Named("space")
    private ResourceReferenceTypeParser spaceResourceReferenceTypeParser;

    @Inject
    @Named("relative")
    private EntityReferenceResolver<String> relativeReferenceResolver;

    /**
     * {@inheritDoc}
     * <p>
     * Overridden to check if the reference is relative to the current space. In which case we need to make sure any
     * space reference fallback will still be relative to current space.
     * 
     * @see org.xwiki.rendering.internal.parser.reference.DefaultUntypedLinkReferenceParser#getWikiResource(java.lang.String)
     */
    // TODO: improve reusability between XWikiUntypedLinkReferenceParser and DefaultUntypedLinkReferenceParser
    @Override
    protected ResourceReference getWikiResource(String rawReference)
    {
        // Default is document
        ResourceReference documentResourceRefence = this.documentResourceReferenceTypeParser.parse(rawReference);

        // Empty reference means self reference, no need to check more
        if (StringUtils.isEmpty(rawReference)) {
            return documentResourceRefence;
        }

        ResourceReference reference;

        // It can be a link to an existing terminal document
        if (resolveDocumentResource(documentResourceRefence)) {
            // It's a link to a terminal document
            reference = documentResourceRefence;
        } else {
            // Otherwise, treat it as a link to an existing or inexistent space. If the space does not exist, it will be
            // a wanted link.
            String rawSpaceReference = rawReference;

            // If the reference target a page in the same space keep the same logic for non final page
            if (isRelativeDocumentReference(rawReference)) {
                // Make the space reference relative to current space
                rawSpaceReference = '.' + rawSpaceReference;
            }

            reference = spaceResourceReferenceTypeParser.parse(rawSpaceReference);
        }

        return reference;
    }

    @Override
    protected boolean resolveDocumentResource(ResourceReference resourceReference)
    {
        boolean result = super.resolveDocumentResource(resourceReference);

        if (!result) {
            // Consider explicit "WebHome" references (i.e. the ones ending in "WebHome").
            String defaultDocumentName = defaultReferenceProvider.getDefaultReference(EntityType.DOCUMENT).getName();
            result = StringUtils.endsWith(resourceReference.getReference(), defaultDocumentName);
        }

        return result;
    }

    private boolean isRelativeDocumentReference(String rawReference)
    {
        // Resolve the current reference.
        EntityReference entityReference = this.relativeReferenceResolver.resolve(rawReference, EntityType.DOCUMENT);

        return entityReference.getParent() == null;
    }
}
