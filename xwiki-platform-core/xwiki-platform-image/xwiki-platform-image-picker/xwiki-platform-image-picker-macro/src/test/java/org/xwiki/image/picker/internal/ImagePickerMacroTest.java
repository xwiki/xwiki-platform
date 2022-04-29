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
package org.xwiki.image.picker.internal;

import java.util.List;
import java.util.Map;

import javax.inject.Named;

import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.xwiki.image.picker.ImagePickerMacroParameters;
import org.xwiki.rendering.block.Block;
import org.xwiki.rendering.block.GroupBlock;
import org.xwiki.rendering.transformation.MacroTransformationContext;
import org.xwiki.skinx.SkinExtension;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;

import static java.util.Map.entry;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;

/**
 * Test of {@link ImagePickerMacro}.
 *
 * @version $Id$
 * @since 14.4RC1
 */
@ComponentTest
class ImagePickerMacroTest
{
    @InjectMockComponents
    private ImagePickerMacro imagePickerMacro;

    @MockComponent
    @Named("jsfx")
    private SkinExtension jsfx;

    @MockComponent
    @Named("ssfx")
    private SkinExtension ssfx;

    @Mock
    private MacroTransformationContext macroTransformationContext;

    @Test
    void execute() throws Exception
    {
        ImagePickerMacroParameters parameters = new ImagePickerMacroParameters();
        parameters.setId("my-id");
        List<Block> actual =
            this.imagePickerMacro.execute(parameters, null, this.macroTransformationContext);
        assertEquals(List.of(new GroupBlock(List.of(
            new GroupBlock(Map.of("class", "imagePickerSearch")),
            new GroupBlock(Map.of("class", "imagePickerResults"))
        ), Map.ofEntries(
            entry("id", parameters.getId()),
            entry("class", "imagePicker")
        ))), actual);
        verify(this.jsfx).use("uicomponents/widgets/imagePicker.js", Map.of("forceSkinAction", true));
        verify(this.ssfx).use("uicomponents/widgets/imagePicker.css");
    }
}
