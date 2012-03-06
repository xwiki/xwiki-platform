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
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.component.annotation.Component;
import org.xwiki.model.reference.DocumentReferenceResolver;
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
    @Inject
    private RenderingConfiguration renderingConfiguration;

    @Inject
    private DocumentAccessBridge documentAccessBridge;

    @Inject
    @Named("current")
    private DocumentReferenceResolver<String> currentDocumentReferenceResolver;

    /**
     * {@inheritDoc}
     * @since 2.5RC1
     */
    @Override
    public String generate(ResourceReference reference)
    {
        String result;

        String format = this.renderingConfiguration.getLinkLabelFormat();
        DocumentReference documentReference = this.currentDocumentReferenceResolver.resolve(reference.getReference());

        // Replace %w with the wiki name
        result = format.replace("%w", documentReference.getWikiReference().getName());

        // Replace %p with the page name
        result = result.replace("%p", documentReference.getName());

        // Replace %s with the space name
        result = result.replace("%s", documentReference.getLastSpaceReference().getName());

        // Replace %P with the page name in camel case + space
        if (result.indexOf("%P") > -1) {
            String normalizedPage = documentReference.getName().replaceAll("([a-z])([A-Z])", "$1 $2");
            result = result.replace("%P", normalizedPage);
        }

        // Replace %t with the document title and fall back to %p if the title is null or empty
        if (result.indexOf("%t") > -1) {
            try {
                DocumentModelBridge document = this.documentAccessBridge.getDocument(documentReference);
                if (StringUtils.isNotBlank(document.getTitle())) {
                    result = result.replace("%t", document.getTitle());
                } else {
                    result = documentReference.getName();
                }
            } catch (Exception e) {
                // If there's an error (meaning the document cannot be retrieved from the database for some reason)
                // the fall back to displaying %p
                result = documentReference.getName();
            }
        }

        return result;
    }
}
