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
package org.xwiki.rendering.internal.macro.gallery;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.commons.lang3.StringUtils;
import org.xwiki.component.annotation.Component;
import org.xwiki.rendering.block.Block;
import org.xwiki.rendering.block.GroupBlock;
import org.xwiki.rendering.block.MacroBlock;
import org.xwiki.rendering.macro.AbstractMacro;
import org.xwiki.rendering.macro.MacroContentParser;
import org.xwiki.rendering.macro.MacroExecutionException;
import org.xwiki.rendering.macro.MacroPreparationException;
import org.xwiki.rendering.macro.descriptor.DefaultContentDescriptor;
import org.xwiki.rendering.macro.gallery.GalleryMacroParameters;
import org.xwiki.rendering.transformation.MacroTransformationContext;
import org.xwiki.skinx.SkinExtension;

/**
 * Displays the images found in the provided content using a slide-show view.
 * 
 * @version $Id$
 * @since 3.0M3
 */
@Component
@Named("gallery")
@Singleton
public class GalleryMacro extends AbstractMacro<GalleryMacroParameters>
{
    /**
     * The description of the macro.
     */
    private static final String DESCRIPTION =
        "Displays the images found in the provided content using a slide-show view.";

    /**
     * The description of the macro content.
     */
    private static final String CONTENT_DESCRIPTION =
        "The images to be displayed in the gallery. All the images found in the provided wiki content are included. "
            + "Images should be specified using the syntax of the current document. "
            + "Example, for XWiki 2.0 syntax: image:Space.Page@alice.png image:http://www.example.com/path/to/bob.jpg";

    /**
     * The parser used to parse gallery content.
     */
    @Inject
    private MacroContentParser contentParser;

    /**
     * The component used to import JavaScript file extensions.
     */
    @Inject
    @Named("jsfx")
    private SkinExtension jsfx;

    /**
     * The component used to import style-sheet file extensions.
     */
    @Inject
    @Named("ssfx")
    private SkinExtension ssfx;

    /**
     * Create and initialize the descriptor of the macro.
     */
    public GalleryMacro()
    {
        super("Gallery", DESCRIPTION, new DefaultContentDescriptor(CONTENT_DESCRIPTION), GalleryMacroParameters.class);
        setDefaultCategories(Set.of(DEFAULT_CATEGORY_LAYOUT));
    }

    @Override
    public List<Block> execute(GalleryMacroParameters parameters, String content, MacroTransformationContext context)
        throws MacroExecutionException
    {
        if (context != null) {
            Map<String, Object> skinExtensionParameters = Collections.singletonMap("forceSkinAction", (Object) true);
            this.jsfx.use("uicomponents/widgets/gallery/gallery.js", skinExtensionParameters);
            this.ssfx.use("uicomponents/widgets/gallery/gallery.css");

            StringBuilder inlineStyle = new StringBuilder();
            if (!StringUtils.isEmpty(parameters.getWidth())) {
                // Non-empty width value. The empty value means "no explicit width".
                inlineStyle.append("width: ").append(parameters.getWidth()).append(';');
            }
            if (!StringUtils.isEmpty(parameters.getHeight())) {
                // Non-empty height value. The empty value means "no explicit height".
                inlineStyle.append("height: ").append(parameters.getHeight()).append(';');
            }

            Map<String, String> groupParameters = new HashMap<>();
            groupParameters.put("class", ("gallery " + StringUtils.defaultString(parameters.getClassNames())).trim());
            // Disable lightbox for the gallery macro since the two features are very similar and it produces confusion.
            groupParameters.put("data-xwiki-lightbox", "false");
            if (inlineStyle.length() > 0) {
                groupParameters.put("style", inlineStyle.toString());
            }

            Block galleryBlock = new GroupBlock(groupParameters);
            // Don't execute transformations explicitly. They'll be executed on the generated content later on.
            galleryBlock.addChildren(this.contentParser.parse(content, context, false, false).getChildren());
            return Collections.singletonList(galleryBlock);
        } else {
            return Collections.emptyList();
        }
    }

    @Override
    public void prepare(MacroBlock macroBlock) throws MacroPreparationException
    {
        this.contentParser.prepareContentWiki(macroBlock);
    }

    @Override
    public boolean supportsInlineMode()
    {
        return false;
    }
}
