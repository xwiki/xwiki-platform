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

import javax.inject.Named;

import org.junit.jupiter.api.Test;
import org.xwiki.localization.ContextualLocalizationManager;
import org.xwiki.model.reference.SpaceReference;
import org.xwiki.model.reference.SpaceReferenceResolver;
import org.xwiki.rendering.listener.reference.SpaceResourceReference;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link XWikiSpaceWantedLinkTitleGenerator}.
 *
 * @version $Id$
 */
@ComponentTest
class XWikiSpaceWantedLinkTitleGeneratorTest
{
    @InjectMockComponents
    private XWikiSpaceWantedLinkTitleGenerator generator;

    @MockComponent
    private ContextualLocalizationManager contextLocalization;

    @MockComponent
    @Named("current")
    private SpaceReferenceResolver<String> currentSpaceReferenceResolver;

    @Test
    void generateWantedLinkTitleUsesTheSpaceName()
    {
        when(this.currentSpaceReferenceResolver.resolve("Fa.Fi"))
            .thenReturn(new SpaceReference("wiki", "Fa", "Fi"));
        when(this.contextLocalization.getTranslationPlain("rendering.xwiki.wantedLink.space.label", "Fi"))
            .thenReturn("Create space: Fi");

        assertEquals("Create space: Fi",
            this.generator.generateWantedLinkTitle(new SpaceResourceReference("Fa.Fi")));
    }
}
