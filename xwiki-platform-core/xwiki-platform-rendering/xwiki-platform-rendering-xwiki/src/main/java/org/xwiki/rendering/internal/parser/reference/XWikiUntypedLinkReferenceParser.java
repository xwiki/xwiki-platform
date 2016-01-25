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
import org.xwiki.model.reference.EntityReferenceProvider;
import org.xwiki.rendering.listener.reference.ResourceReference;

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
}
