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

import org.xwiki.component.annotation.Component;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.model.reference.PageReferenceResolver;
import org.xwiki.rendering.listener.reference.ResourceReference;
import org.xwiki.localization.ContextualLocalizationManager;
import org.xwiki.rendering.listener.reference.ResourceType;
import org.xwiki.rendering.renderer.reference.link.WantedLinkTitleGenerator;

/**
 * Generates a wanted link title for a document resource reference.
 * This implementation uses translations to generate localized titles.
 *
 * @version $Id$
 * @since 16.3.0RC1
 */
@Component(hints = {"doc", "page"})
@Singleton
public class XWikiDocumentWantedLinkTitleGenerator implements WantedLinkTitleGenerator
{
    @Inject
    private ContextualLocalizationManager contextLocalization;

    /**
     * Used to extract the document name part in a document reference.
     */
    @Inject
    @Named("current")
    private DocumentReferenceResolver<String> currentDocumentReferenceResolver;

    /**
     * Used to extract the page name part in a page reference.
     */
    @Inject
    @Named("current")
    private PageReferenceResolver<String> currentPageReferenceResolver;

    @Override
    public String generateWantedLinkTitle(ResourceReference reference)
    {
        String documentTitleTranslationKey = "rendering.xwiki.wantedLink.document.label";
        String documentName;
        if (reference.isTyped() && reference.getType() == ResourceType.DOCUMENT) {
            documentName = this.currentDocumentReferenceResolver.resolve(reference.getReference()).getName();
        } else if (reference.isTyped() && reference.getType() == ResourceType.PAGE) {
            documentName = this.currentPageReferenceResolver.resolve(reference.getReference()).getName();
        } else {
            documentName = reference.getReference();
        }
        return this.contextLocalization.getTranslationPlain(documentTitleTranslationKey,
            documentName);
    }
}
