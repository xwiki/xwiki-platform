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
package org.xwiki.rendering.internal.util;

import javax.inject.Inject;
import javax.inject.Singleton;
import org.xwiki.component.annotation.Component;
import org.xwiki.icon.IconException;
import org.xwiki.icon.IconRenderer;
import org.xwiki.icon.IconSet;
import org.xwiki.icon.IconSetManager;
import org.xwiki.localization.ContextualLocalizationManager;
import org.xwiki.rendering.block.Block;
import org.xwiki.rendering.block.CompositeBlock;
import org.xwiki.rendering.block.FormatBlock;
import org.xwiki.rendering.block.RawBlock;
import org.xwiki.rendering.block.WordBlock;
import org.xwiki.rendering.listener.Format;
import org.xwiki.rendering.syntax.Syntax;

import java.util.List;

/**
 * Component to use the icon theme to provide a proper block for displaying an icon.
 *
 * @version $Id$
 * @since 15.10.9
 * @since 16.3.0RC1
 */
@Component
@Singleton
public class XWikiIconProvider extends DefaultIconProvider
{
    @Inject
    private IconSetManager iconSetManager;
    @Inject
    private IconRenderer iconRenderer;
    @Inject
    private ContextualLocalizationManager l10n;

    /**
     * Uses the icon theme to provide the right block for displaying an icon.
     * @param iconName the name of the icon to display
     * @return the block containing an icon.
     */
    @Override
    public Block get(String iconName)
    {
        try {
            IconSet iconSet = getIconSet(iconName);
            String iconContent = this.iconRenderer.renderHTML(iconName, iconSet);
            Block iconAlternative;
            try {
                // Try to retrieve a translation for the icon.
                iconAlternative = new FormatBlock(List.of(l10n.getTranslation(
                    String.format("rendering.icon.provider.icon.alternative.%s", iconName)).render()), Format.NONE);
            } catch (Exception e) {
                // As a fallback, we just get the english name of the icon as an alternative.
                iconAlternative = new FormatBlock(List.of(new WordBlock(iconName)), Format.NONE);
            }
            iconAlternative.setParameter("class", "sr-only");
            Block iconRaw = new RawBlock(iconContent, Syntax.HTML_5_0);
            // We return a combination of the icon itself and its text alternative.
            return new CompositeBlock(List.of(iconRaw, iconAlternative));
        } catch (IconException e) {
            return super.get(iconName);
        }
    }

    private IconSet getIconSet(String iconName) throws IconException
    {
        IconSet iconSet = this.iconSetManager.getCurrentIconSet();
        if (iconSet == null || !iconSet.hasIcon(iconName)) {
            iconSet = this.iconSetManager.getDefaultIconSet();
        }
        return iconSet;
    }
}
