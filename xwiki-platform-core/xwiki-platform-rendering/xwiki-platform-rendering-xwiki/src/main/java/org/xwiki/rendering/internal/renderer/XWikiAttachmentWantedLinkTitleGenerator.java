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
import org.xwiki.localization.ContextualLocalizationManager;
import org.xwiki.model.reference.AttachmentReferenceResolver;
import org.xwiki.model.reference.PageAttachmentReferenceResolver;
import org.xwiki.rendering.listener.reference.ResourceReference;
import org.xwiki.rendering.listener.reference.ResourceType;
import org.xwiki.rendering.renderer.reference.link.WantedLinkTitleGenerator;

/**
 * Generates a wanted link title for an attachment resource reference.
 * This implementation uses translations to generate localized titles.
 *
 * @version $Id$
 * @since 16.3.0RC1
 */
@Component(hints = {"pageAttach", "attach"})
@Singleton
public class XWikiAttachmentWantedLinkTitleGenerator implements WantedLinkTitleGenerator
{
    @Inject
    private ContextualLocalizationManager contextLocalization;

    /**
     * Used to extract the attachment name part in an attachment reference.
     */
    @Inject
    @Named("current")
    private AttachmentReferenceResolver<String> currentAttachmentReferenceResolver;

    /**
     * Used to extract the page attachment name part in a page attachment reference.
     */
    @Inject
    @Named("current")
    private PageAttachmentReferenceResolver<String> currentPageAttachmentReferenceResolver;

    @Override
    public String generateWantedLinkTitle(ResourceReference reference)
    {
        String attachmentTitleTranslationKey = "rendering.xwiki.wantedLink.attachment.label";
        String attachmentName;
        if (reference.isTyped() && reference.getType() == ResourceType.ATTACHMENT) {
            attachmentName = this.currentAttachmentReferenceResolver.resolve(reference.getReference()).getName();
        } else if (reference.isTyped() && reference.getType() == ResourceType.PAGE_ATTACHMENT) {
            attachmentName = this.currentPageAttachmentReferenceResolver.resolve(reference.getReference()).getName();
        } else {
            attachmentName = reference.getReference();
        }
        return this.contextLocalization.getTranslationPlain(attachmentTitleTranslationKey,
            attachmentName);
    }
}
