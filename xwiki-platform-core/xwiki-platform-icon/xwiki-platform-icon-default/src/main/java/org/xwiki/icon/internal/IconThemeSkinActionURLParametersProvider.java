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
package org.xwiki.icon.internal;

import java.util.Map;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.icon.IconException;
import org.xwiki.icon.IconSet;
import org.xwiki.icon.IconSetManager;
import org.xwiki.skin.SkinActionURLParametersProvider;

/**
 * Adds the current icon theme to the URL of the skin resources evaluated by the skin action, since they can render
 * icons (e.g. with {@code $services.icon.renderHTML()}), so that the browser doesn't keep using the icons of the
 * previous icon theme after the icon theme is changed.
 *
 * @version $Id$
 * @since 18.10.0RC1
 */
@Component
@Named(IconThemeSkinActionURLParametersProvider.PARAMETER_NAME)
@Singleton
public class IconThemeSkinActionURLParametersProvider implements SkinActionURLParametersProvider
{
    /**
     * The name of the URL query parameter holding the name of the current icon theme.
     */
    public static final String PARAMETER_NAME = "iconTheme";

    @Inject
    private IconSetManager iconSetManager;

    @Override
    public Map<String, Object> getParameters()
    {
        IconSet iconSet;
        try {
            iconSet = this.iconSetManager.getCurrentIconSet();
        } catch (IconException e) {
            throw new IllegalStateException("Failed to get the current icon set", e);
        }

        // No current icon set means that the default icon set is used, so there's no need to differentiate the URL.
        if (iconSet == null || iconSet.getName() == null) {
            return Map.of();
        }

        return Map.of(PARAMETER_NAME, iconSet.getName());
    }
}
