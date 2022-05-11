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
package org.xwiki.attachment.picker.internal;

import java.util.List;
import java.util.Map;

import javax.inject.Named;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.xwiki.attachment.picker.AttachmentGalleryPickerMacroParameters;
import org.xwiki.localization.ContextualLocalizationManager;
import org.xwiki.localization.Translation;
import org.xwiki.rendering.block.Block;
import org.xwiki.rendering.block.GroupBlock;
import org.xwiki.rendering.transformation.MacroTransformationContext;
import org.xwiki.skinx.SkinExtension;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;

import static java.util.Map.entry;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Test of {@link AttachmentGalleryPickerMacro}.
 *
 * @version $Id$
 * @since 14.4RC1
 */
@ComponentTest
class AttachmentGalleryPickerMacroTest
{
    @InjectMockComponents
    private AttachmentGalleryPickerMacro attachmentGalleryPickerMacro;

    @MockComponent
    @Named("ssx")
    private SkinExtension ssx;

    @MockComponent
    @Named("jsx")
    private SkinExtension jsx;

    @MockComponent
    private ContextualLocalizationManager l10n;

    @Mock
    private MacroTransformationContext macroTransformationContext;

    @Mock
    private Block translationRenderBlock;

    @BeforeEach
    void setUp()
    {
        Translation translation = mock(Translation.class);
        when(this.l10n.getTranslation("attachment.picker.macro.notResult.message")).thenReturn(translation);
        when(translation.render()).thenReturn(this.translationRenderBlock);
    }

    @Test
    void execute()
    {
        AttachmentGalleryPickerMacroParameters params = new AttachmentGalleryPickerMacroParameters();
        params.setId("my-id");
        List<Block> actual = this.attachmentGalleryPickerMacro.execute(params, null, this.macroTransformationContext);
        assertEquals(List.of(new GroupBlock(List.of(
            new GroupBlock(List.of(), Map.of("class", "attachmentPickerSearch")),
            new GroupBlock(Map.of("class", "attachmentPickerResults")),
            new GroupBlock(List.of(this.translationRenderBlock),
                Map.of("class", "attachmentPickerNoResults hidden box warningmessage"))
        ), Map.ofEntries(
            entry("id", "my-id"),
            entry("class", "attachmentGalleryPicker"),
            entry("data-xwiki-lightbox", "false"),
            entry("data-xwiki-attachment-picker-filter", ""),
            entry("data-xwiki-attachment-picker-limit", "20")
        ))), actual);
        verify(this.jsx).use("Attachment.Picker.Code.AttachmentGalleryPicker");
        verify(this.ssx).use("Attachment.Picker.Code.AttachmentGalleryPicker");
    }

    @Test
    void executeWithLimit()
    {
        AttachmentGalleryPickerMacroParameters params = new AttachmentGalleryPickerMacroParameters();
        params.setId("my-id");
        params.setLimit(10);
        List<Block> actual = this.attachmentGalleryPickerMacro.execute(params, null, this.macroTransformationContext);
        assertEquals("10", actual.get(0).getParameter("data-xwiki-attachment-picker-limit"));
    }

    @Test
    void executeWithFilter()
    {
        AttachmentGalleryPickerMacroParameters params = new AttachmentGalleryPickerMacroParameters();
        params.setId("my-id");
        params.setFilter(List.of("image/*", "image/jpeg"));
        List<Block> actual =
            this.attachmentGalleryPickerMacro.execute(params, null, this.macroTransformationContext);
        assertEquals("image/*,image/jpeg", actual.get(0).getParameter("data-xwiki-attachment-picker-filter"));
    }
}
