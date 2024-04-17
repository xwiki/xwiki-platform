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

import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.xwiki.icon.Icon;
import org.xwiki.icon.IconRenderer;
import org.xwiki.icon.IconSet;
import org.xwiki.icon.IconSetManager;
import org.xwiki.localization.ContextualLocalizationManager;
import org.xwiki.localization.LocalizationException;
import org.xwiki.localization.Translation;
import org.xwiki.localization.internal.DefaultTranslation;
import org.xwiki.rendering.block.*;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link XWikiIconProvider}.
 *
 * @version $Id
 */
@ComponentTest
class XWikiIconProviderTest
{
    @InjectMockComponents
    private XWikiIconProvider iconProvider;

    @MockComponent
    private IconSetManager iconSetManager;
    @MockComponent
    private IconRenderer iconRenderer;
    @MockComponent
    private ContextualLocalizationManager l10n;

    @Mock
    private Translation translationResult;

    @Test
    public void get() throws Exception {
        // Setting up the mocks
        // Mock behaviour for the iconsetManager
        IconSet currentIconSet = new IconSet("fontawesome");
        IconSet defaultIconSet = new IconSet("silk");
        currentIconSet.addIcon("test", new Icon("hello"));
        when(iconSetManager.getCurrentIconSet()).thenReturn(currentIconSet);
        when(iconSetManager.getDefaultIconSet()).thenReturn(defaultIconSet);

        // Mock behaviour for the iconRenderer
        String testIconFA = "<span class=\"fa fa-test\"aria-hidden=\"true\"></span>";
        String testIconSilk = "<img src=\"$xwiki.getSkinFile(\"icons/silk/test.png\")\" alt=\"\" " +
         "data-xwiki-lightbox=\"false\" />";
        when(this.iconRenderer.renderHTML("test", currentIconSet)).thenReturn(testIconFA);
        when(this.iconRenderer.renderHTML("test", defaultIconSet)).thenReturn(testIconSilk);
        // Mock behaviour for the LocalizationManager
        Block translationRendered = new CompositeBlock();
        translationRendered.addChild(new WordBlock("Test translation"));
        when(l10n.getTranslation("rendering.icon.provider.icon.alternative.test")).thenReturn(translationResult);
        when(translationResult.render()).thenReturn(translationRendered);

        // Test
        Block result = iconProvider.get("test");
        // We want to make sure that the returned result is a composite block
        assertEquals(result.getClass(), CompositeBlock.class);
        // We make sure we have two blocks inside the result
        assertEquals(result.getChildren().size(), 2);
        // We check the icon itself
        assertEquals(RawBlock.class, result.getChildren().get(0).getClass());
        assertEquals(testIconFA, ((RawBlock)result.getChildren().get(0)).getRawContent());
        // We check that the second block is the icon alternative
        assertEquals(FormatBlock.class, result.getChildren().get(1).getClass());
        assertEquals("sr-only", result.getChildren().get(1).getParameter("class"));
        assertEquals(translationRendered, result.getChildren().get(1).getChildren().get(0));
    }

    @Test
    public void getNoTranslation() throws Exception {
        // Setting up the mocks
        // Mock behaviour for the iconsetManager
        IconSet currentIconSet = new IconSet("fontawesome");
        IconSet defaultIconSet = new IconSet("silk");
        currentIconSet.addIcon("test", new Icon("hello"));
        when(iconSetManager.getCurrentIconSet()).thenReturn(currentIconSet);
        when(iconSetManager.getDefaultIconSet()).thenReturn(defaultIconSet);

        // Mock behaviour for the iconRenderer
        String testIconFA = "<span class=\"fa fa-test\"aria-hidden=\"true\"></span>";
        String testIconSilk = "<img src=\"$xwiki.getSkinFile(\"icons/silk/test.png\")\" alt=\"\" " +
                "data-xwiki-lightbox=\"false\" />";
        when(this.iconRenderer.renderHTML("test", currentIconSet)).thenReturn(testIconFA);
        when(this.iconRenderer.renderHTML("test", defaultIconSet)).thenReturn(testIconSilk);
        // Mock behaviour for the LocalizationManager
        when(l10n.getTranslation("rendering.icon.provider.icon.alternative.test")).thenReturn(null);

        // Test
        Block result = iconProvider.get("test");
        // We check that an icon alternative got generated, despite the lack of translation
        assertEquals(WordBlock.class, result.getChildren().get(1).getChildren().get(0).getClass());
        assertEquals("test", ((WordBlock)result.getChildren().get(1).getChildren().get(0)).getWord());
    }

    @Test
    public void getDefaultIconsetFallback() throws Exception {
        // Setting up the mocks
        // Mock behaviour for the iconsetManager
        IconSet currentIconSet = new IconSet("fontawesome");
        IconSet defaultIconSet = new IconSet("silk");
        defaultIconSet.addIcon("test", new Icon("hello"));
        when(iconSetManager.getCurrentIconSet()).thenReturn(currentIconSet);
        when(iconSetManager.getDefaultIconSet()).thenReturn(defaultIconSet);

        // Mock behaviour for the iconRenderer
        String testIconFA = "<span class=\"fa fa-test\"aria-hidden=\"true\"></span>";
        String testIconSilk = "<img src=\"$xwiki.getSkinFile(\"icons/silk/test.png\")\" alt=\"\" " +
                "data-xwiki-lightbox=\"false\" />";
        when(this.iconRenderer.renderHTML("test", currentIconSet)).thenReturn(testIconFA);
        when(this.iconRenderer.renderHTML("test", defaultIconSet)).thenReturn(testIconSilk);
        // Mock behaviour for the LocalizationManager
        Block translationRendered = new CompositeBlock();
        translationRendered.addChild(new WordBlock("Test translation"));
        when(l10n.getTranslation("rendering.icon.provider.icon.alternative.test")).thenReturn(translationResult);
        when(translationResult.render()).thenReturn(translationRendered);

        // Test
        Block result = iconProvider.get("test");
        // We check that the icon provider fell back on the default icon theme when needed
        assertEquals(testIconSilk, ((RawBlock)result.getChildren().get(0)).getRawContent());
    }
}
