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

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.image.picker.ImagePickerMacroParameters;
import org.xwiki.rendering.block.Block;
import org.xwiki.rendering.block.GroupBlock;
import org.xwiki.rendering.block.MacroBlock;
import org.xwiki.rendering.block.WordBlock;
import org.xwiki.rendering.macro.AbstractMacro;
import org.xwiki.rendering.macro.MacroExecutionException;
import org.xwiki.rendering.transformation.MacroTransformationContext;
import org.xwiki.skinx.SkinExtension;

import static java.util.Map.entry;

/**
 * TODO: document me. TODO: translations (macro title, description, properties...)
 *
 * @version $Id$
 * @since 14.4RC1
 */
@Component
@Named("imagePicker")
@Singleton
public class ImagePickerMacro extends AbstractMacro<ImagePickerMacroParameters>
{
    /**
     * id parameter for the {@link Block}s returned by
     * {@link #execute(ImagePickerMacroParameters, String, MacroTransformationContext)}.
     */
    private static final String BLOCK_PARAM_ID = "id";

    /**
     * class parameter for the {@link Block}s returned by
     * {@link #execute(ImagePickerMacroParameters, String, MacroTransformationContext)}.
     */
    private static final String BLOCK_PARAM_CLASS = "class";

    private static final String IMAGE_PICKER_CLASSES = "imagePicker";

    /**
     * CSS file skin extension, to include the image picker css.
     */
    @Inject
    @Named("ssfx")
    private SkinExtension ssfx;

    /**
     * JS file skin extension, to include the image picker javascript. A single javascript file provided by the war
     * module is required. This file has some velocity to define the other webjar dependencies dynamically, all the rest
     * is provided by the webjar module and its dependencies.
     */
    @Inject
    @Named("jsfx")
    private SkinExtension jsfx;

    /**
     * Default constructor.
     */
    public ImagePickerMacro()
    {
        super("Image Picker", "Grid based image picker.", ImagePickerMacroParameters.class);
    }

    @Override
    public boolean supportsInlineMode()
    {
        return false;
    }

    @Override
    public List<Block> execute(ImagePickerMacroParameters parameters, String content,
        MacroTransformationContext context) throws MacroExecutionException
    {
        // TODO: add data-* attributes to pass the other configurations (or a single json serialized data)?
        // TODO: verify that the minified versions are actually loaded in the browser. 
        this.jsfx.use("uicomponents/widgets/imagePicker.js", Map.of("forceSkinAction", true));
        this.ssfx.use("uicomponents/widgets/imagePicker.css");
        return List.of(new GroupBlock(List.of(
            // Search block.
            new GroupBlock(List.of(), Map.of(BLOCK_PARAM_CLASS, "imagePickerSearch")),
            // Results block.
            new GroupBlock(Map.of(BLOCK_PARAM_CLASS, "imagePickerResults")),
            // No results block.
            // TODO: localization
            new GroupBlock(List.of(new WordBlock("No results.")),
                Map.of(BLOCK_PARAM_CLASS, "imagePickerNoResults hidden box warningmessage")),
            // Images carousel.
            // TODO: add an explicit runtime dependency to the lighbox-ui module. 
            new MacroBlock("include", Map.of("reference", "XWiki.Lightbox.Code.BlueImpScripts"), false),
            // TODO: check XSS.
            new MacroBlock("velocity", Map.of(),
                "{{html}}#lightboxHTMLTemplate('" + parameters.getId() + "-gallery'){{/html}}", false)
        ), Map.ofEntries(
            entry(BLOCK_PARAM_ID, parameters.getId()),
            entry(BLOCK_PARAM_CLASS, IMAGE_PICKER_CLASSES),
            entry("data-xwiki-lightbox", "false")
        )));
    }
}
