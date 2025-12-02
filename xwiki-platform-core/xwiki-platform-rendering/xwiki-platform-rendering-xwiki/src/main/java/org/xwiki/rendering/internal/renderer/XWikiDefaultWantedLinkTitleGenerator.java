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
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.localization.ContextualLocalizationManager;
import org.xwiki.rendering.listener.reference.ResourceReference;
import org.xwiki.rendering.renderer.reference.link.WantedLinkTitleGenerator;

/**
 * Fallback to generate a wanted link title for a resource which type doesn't have a specific implementation yet.
 * This implementation uses translations to generate localized titles.
 * This implementation uses the reference itself, it should be overridden by type specific implementations using a
 * human-readable name instead. E.g. {@link XWikiDocumentWantedLinkTitleGenerator}
 *
 * @version $Id$
 * @since 16.3.0RC1
 */
@Component
@Singleton
public class XWikiDefaultWantedLinkTitleGenerator implements WantedLinkTitleGenerator
{
    @Inject
    private ContextualLocalizationManager contextLocalization;

    @Override
    public String generateWantedLinkTitle(ResourceReference reference)
    {
        return this.contextLocalization.getTranslationPlain("rendering.xwiki.wantedLink.default.label",
            reference.getReference());
    }
}
