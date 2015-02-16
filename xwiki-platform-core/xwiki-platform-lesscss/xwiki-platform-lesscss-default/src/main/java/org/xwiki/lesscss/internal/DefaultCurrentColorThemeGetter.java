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
package org.xwiki.lesscss.internal;

import javax.inject.Inject;
import javax.inject.Provider;

import org.apache.commons.lang3.StringUtils;
import org.xwiki.component.annotation.Component;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.model.reference.WikiReference;
import org.xwiki.security.authorization.AuthorizationManager;
import org.xwiki.security.authorization.Right;
import org.xwiki.wiki.descriptor.WikiDescriptorManager;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.web.XWikiRequest;

/**
 * Default implementation for {@link org.xwiki.lesscss.internal.CurrentColorThemeGetter}.
 *
 * @since 6.3M2
 * @version $Id$
 */
@Component
public class DefaultCurrentColorThemeGetter implements CurrentColorThemeGetter
{
    private static final String COLOR_THEME_FIELD = "colorTheme";

    @Inject
    private DocumentReferenceResolver<String> documentReferenceResolver;

    @Inject
    private EntityReferenceSerializer<String> entityReferenceSerializer;

    @Inject
    private Provider<XWikiContext> xcontextProvider;

    @Inject
    private WikiDescriptorManager wikiDescriptorManager;

    @Inject
    private AuthorizationManager authorizationManager;

    @Override
    public String getCurrentColorTheme(String fallbackValue)
    {
        // Get information about the context
        String wikiId = wikiDescriptorManager.getCurrentWikiId();
        XWikiContext context = xcontextProvider.get();
        XWiki xwiki = context.getWiki();
        XWikiRequest request = context.getRequest();

        // Get the current color theme
        String colorTheme = request.getParameter(COLOR_THEME_FIELD);
        if (StringUtils.isEmpty(colorTheme)) {
            // Get it from the preferences
            colorTheme = xwiki.getUserPreference(COLOR_THEME_FIELD, context);
        }

        // Getting the full name representation of colorTheme
        DocumentReference colorThemeReference =
                documentReferenceResolver.resolve(colorTheme, new WikiReference(wikiId));
        colorTheme = entityReferenceSerializer.serialize(colorThemeReference);

        // Check that the color theme exists, to avoid a DOS if some user tries to getResult a skin file
        // with random colorTheme names
        // Also check that the user has the right to see the color theme
        if (!xwiki.exists(colorThemeReference, context) || !authorizationManager.hasAccess(Right.VIEW,
                context.getUserReference(), colorThemeReference)) {
            colorTheme = fallbackValue;
        }

        return colorTheme;
    }

}
