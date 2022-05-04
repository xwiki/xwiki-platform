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

import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.xwiki.attachment.picker.AttachmentPickerMacroParameters;
import org.xwiki.rendering.block.Block;
import org.xwiki.rendering.block.GroupBlock;
import org.xwiki.rendering.block.WordBlock;
import org.xwiki.rendering.transformation.MacroTransformationContext;
import org.xwiki.skinx.SkinExtension;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;

import static java.util.Map.entry;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;

/**
 * Test of {@link AttachmentPickerMacro}.
 *
 * @version $Id$
 * @since 14.4RC1
 */
@ComponentTest
class AttachmentPickerMacroTest
{
    @InjectMockComponents
    private AttachmentPickerMacro attachmentPickerMacro;

    @MockComponent
    @Named("jsfx")
    private SkinExtension jsfx;

    @MockComponent
    @Named("ssfx")
    private SkinExtension ssfx;

    @Mock
    private MacroTransformationContext macroTransformationContext;

    @Test
    void execute()
    {
        AttachmentPickerMacroParameters params = new AttachmentPickerMacroParameters();
        params.setId("my-id");
        List<Block> actual =
            this.attachmentPickerMacro.execute(params, null, this.macroTransformationContext);
        assertEquals(List.of(new GroupBlock(List.of(
            new GroupBlock(List.of(), Map.of("class", "attachmentPickerSearch")),
            new GroupBlock(Map.of("class", "attachmentPickerResults")),
            new GroupBlock(List.of(new WordBlock("No results.")),
                Map.of("class", "attachmentPickerNoResults hidden box warningmessage"))
        ), Map.ofEntries(
            entry("id", "my-id"),
            entry("class", "attachmentPicker"),
            entry("data-xwiki-lightbox", "false"),
            entry("data-xwiki-attachment-picker-types", "")
        ))), actual);
        verify(this.jsfx).use("uicomponents/widgets/attachmentPicker.js", Map.of("forceSkinAction", true));
        verify(this.ssfx).use("uicomponents/widgets/attachmentPicker.css");
    }

    @Test
    void executeWithTypes()
    {
        AttachmentPickerMacroParameters params = new AttachmentPickerMacroParameters();
        params.setId("my-id");
        params.setTypes(List.of("image/png", "image/jpeg"));
        List<Block> actual =
            this.attachmentPickerMacro.execute(params, null, this.macroTransformationContext);
        assertEquals("image/png,image/jpeg", actual.get(0).getParameter("data-xwiki-attachment-picker-types"));
    }
}
