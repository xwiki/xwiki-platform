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
package org.xwiki.lesscss.internal.colortheme;

import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
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
 * Component to get the current color theme set by the request.
 *
 * @since 6.4M2
 * @version $Id$
 */
@Component(roles = CurrentColorThemeGetter.class)
@Singleton
public class CurrentColorThemeGetter
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

    @Inject
    private Logger logger;

    /**
     * @param fallbackValue value to return if the current color theme is invalid
     * @return the full name of the current color theme or fallbackValue if the current color theme is invalid
     */
    public String getCurrentColorTheme(String fallbackValue)
    {
        return getCurrentColorTheme(true, fallbackValue);
    }

    /**
     * @param checkRights check if the user has the right to see the color theme
     * @param fallbackValue value to return if the current color theme is invalid
     * @return the full name of the current color theme or fallbackValue if the current color theme is invalid
     * @since 7.0-M2
     */
    public String getCurrentColorTheme(boolean checkRights, String fallbackValue) 
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
        if (!exists(colorThemeReference, context) || (checkRights && !authorizationManager.hasAccess(Right.VIEW,
            context.getUserReference(), colorThemeReference))) {
            colorTheme = fallbackValue;
        }
        
        return colorTheme;
    }

    private boolean exists(DocumentReference colorThemeReference, XWikiContext context)
    {
        try {
            return context.getWiki().exists(colorThemeReference, context);
        } catch (Exception e) {
            this.logger.error("Failed to check the existence of the color theme with reference [{}]",
                colorThemeReference, e);
        }

        return false;
    }
}
