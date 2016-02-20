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
package org.xwiki.rendering.internal.renderer;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.commons.lang3.StringUtils;
import org.xwiki.bridge.DocumentAccessBridge;
import org.xwiki.bridge.DocumentModelBridge;
import org.xwiki.component.annotation.Component;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.EntityReferenceProvider;
import org.xwiki.model.reference.EntityReferenceResolver;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.rendering.configuration.RenderingConfiguration;
import org.xwiki.rendering.listener.reference.ResourceReference;
import org.xwiki.rendering.renderer.reference.link.LinkLabelGenerator;

/**
 * Resolve configuration link format using the defined Rendering Configuration
 * (see {@link org.xwiki.rendering.configuration.RenderingConfiguration}).
 *
 * @version $Id$
 * @since 2.0M1
 */
@Component
@Singleton
public class XWikiLinkLabelGenerator implements LinkLabelGenerator
{
    private static final String NP_LOWER = "%np";

    private static final String NP_UPPER = "%NP";

    private static final String T_LOWER = "%t";

    private static final String P_UPPER = "%P";

    @Inject
    private RenderingConfiguration renderingConfiguration;

    @Inject
    private DocumentAccessBridge documentAccessBridge;

    @Inject
    private EntityReferenceResolver<ResourceReference> resourceReferenceResolver;

    @Inject
    private EntityReferenceProvider defaultEntityReferenceProvider;

    @Inject
    @Named("local")
    private EntityReferenceSerializer<String> localReferenceSerializer;

    /**
     * {@inheritDoc}
     * @since 2.5RC1
     */
    @Override
    public String generate(ResourceReference reference)
    {
        String result;

        String format = this.renderingConfiguration.getLinkLabelFormat();

        EntityReference resolvedReference = resourceReferenceResolver.resolve(reference, EntityType.DOCUMENT);
        if (resolvedReference == null) {
            throw new IllegalArgumentException(String.valueOf(reference));
        }
        DocumentReference documentReference = new DocumentReference(resolvedReference);

        // Replace %w with the wiki name
        result = format.replace("%w", documentReference.getWikiReference().getName());

        // Replace %p with the page name
        result = result.replace("%p", documentReference.getName());

        // Replace %np with the page name if the name is not the default page name (e.g. "WebHome") or with the last
        // space name if it is.
        result = handleNestedPagesFormatting(NP_LOWER, result, documentReference);

        // Replace %s with the full space name (e.g. space1.space2)
        result = handleSpacesFormatting(result, documentReference);

        // Replace %ls with the last space name
        result = result.replace("%ls", documentReference.getLastSpaceReference().getName());

        // Replace %P with the page name in camel case + space
        result = handlePageCamelFormatting(result, documentReference);

        // Replace %NP with the nested page name in camel case + space
        result = handleNestedPageCamelFormatting(result, documentReference);

        // Replace %t with the document title and fall back to %p if the title is null or empty
        result = handleTitleFormatting(result, documentReference);

        return result;
    }

    private String handleNestedPageCamelFormatting(String result, DocumentReference documentReference)
    {
        String newResult = result;

        if (result.indexOf(NP_UPPER) > -1) {
            if (this.defaultEntityReferenceProvider.getDefaultReference(EntityType.DOCUMENT).getName().equals(
                documentReference.getName()))
            {
                newResult = replaceCamelFormatting(NP_UPPER, result,
                    documentReference.getLastSpaceReference().getName());
            } else {
                newResult = replaceCamelFormatting(NP_UPPER, result, documentReference.getName());
            }
        }

        return newResult;
    }

    private String handlePageCamelFormatting(String result, DocumentReference documentReference)
    {
        String newResult = result;

        if (result.indexOf(P_UPPER) > -1) {
            newResult = replaceCamelFormatting(P_UPPER, result, documentReference.getName());
        }

        return newResult;
    }

    private String replaceCamelFormatting(String token, String result, String value)
    {
        String normalizedPage = value.replaceAll("([a-z])([A-Z])", "$1 $2");
        return result.replace(token, normalizedPage);
    }

    private String handleSpacesFormatting(String result, DocumentReference documentReference)
    {
        return result.replace("%s", this.localReferenceSerializer.serialize(documentReference.getParent()));
    }

    private String handleNestedPagesFormatting(String token, String result, DocumentReference documentReference)
    {
        String newResult;

        if (this.defaultEntityReferenceProvider.getDefaultReference(EntityType.DOCUMENT).getName().equals(
            documentReference.getName()))
        {
            newResult = result.replace(token, documentReference.getLastSpaceReference().getName());
        } else {
            newResult = result.replace(token, documentReference.getName());
        }

        return newResult;
    }

    private String handleTitleFormatting(String result, DocumentReference documentReference)
    {
        String newResult = result;

        if (result.indexOf(T_LOWER) > -1) {
            try {
                DocumentModelBridge document = this.documentAccessBridge.getDocument(documentReference);
                if (StringUtils.isNotBlank(document.getTitle())) {
                    newResult = result.replace(T_LOWER, document.getTitle());
                } else {
                    // Title is empty, fall back to %np
                    newResult = handleNestedPagesFormatting(T_LOWER, result, documentReference);
                }
            } catch (Exception e) {
                // If there's an error (meaning the document cannot be retrieved from the database for some reason)
                // the fall back to displaying %np
                newResult = handleNestedPagesFormatting(T_LOWER, result, documentReference);
            }
        }

        return newResult;
    }
}
