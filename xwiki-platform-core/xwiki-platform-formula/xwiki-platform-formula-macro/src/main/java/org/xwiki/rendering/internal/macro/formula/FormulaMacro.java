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
package org.xwiki.rendering.internal.macro.formula;

import java.util.Collections;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.xwiki.bridge.DocumentAccessBridge;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.formula.FormulaRenderer;
import org.xwiki.formula.FormulaRenderer.FontSize;
import org.xwiki.formula.FormulaRenderer.Type;
import org.xwiki.model.reference.AttachmentReference;
import org.xwiki.rendering.block.Block;
import org.xwiki.rendering.block.ImageBlock;
import org.xwiki.rendering.block.ParagraphBlock;
import org.xwiki.rendering.block.WordBlock;
import org.xwiki.rendering.listener.reference.ResourceReference;
import org.xwiki.rendering.listener.reference.ResourceType;
import org.xwiki.rendering.macro.AbstractMacro;
import org.xwiki.rendering.macro.MacroExecutionException;
import org.xwiki.rendering.macro.descriptor.DefaultContentDescriptor;
import org.xwiki.rendering.macro.formula.FormulaMacroConfiguration;
import org.xwiki.rendering.macro.formula.FormulaMacroParameters;
import org.xwiki.rendering.transformation.MacroTransformationContext;

/**
 * Displays a formula, in LaTeX syntax, as an image.
 * 
 * @version $Id$
 * @since 2.0M3
 */
@Component
@Named("formula")
@Singleton
public class FormulaMacro extends AbstractMacro<FormulaMacroParameters>
{
    /** Predefined error message: empty formula. */
    public static final String CONTENT_MISSING_ERROR = "The mandatory formula text is missing.";

    /** Predefined error message: invalid formula. */
    public static final String WRONG_CONTENT_ERROR = "The formula text is not valid, please correct it.";

    /** The description of the macro. */
    private static final String DESCRIPTION = "Displays a mathematical formula.";

    /** The description of the macro content. */
    private static final String CONTENT_DESCRIPTION = "The mathematical formula, in LaTeX syntax";

    /** Component manager, needed for retrieving the selected formula renderer. */
    @Inject
    private ComponentManager manager;

    /** Defines from where to read the rendering configuration data. */
    @Inject
    private FormulaMacroConfiguration configuration;

    /** Needed for computing the URL for accessing the rendered image. */
    @Inject
    private DocumentAccessBridge dab;

    /**
     * The logger to log.
     */
    @Inject
    private Logger logger;

    /**
     * Create and initialize the descriptor of the macro.
     */
    public FormulaMacro()
    {
        super("Formula", DESCRIPTION, new DefaultContentDescriptor(CONTENT_DESCRIPTION), FormulaMacroParameters.class);
        setDefaultCategory(DEFAULT_CATEGORY_CONTENT);
    }

    @Override
    public List<Block> execute(FormulaMacroParameters parameters, String content, MacroTransformationContext context)
        throws MacroExecutionException
    {
        if (StringUtils.isEmpty(content)) {
            throw new MacroExecutionException(CONTENT_MISSING_ERROR);
        }

        String rendererHint = this.configuration.getRenderer();
        FontSize size = parameters.getFontSize();
        Type type = parameters.getImageType();
        Block result;
        try {
            result = render(content, context.isInline(), size, type, rendererHint);
        } catch (MacroExecutionException ex) {
            this.logger.debug("Failed to render content with the [{}] renderer. Falling back to the safe renderer.",
                rendererHint, ex);
            try {
                result = render(content, context.isInline(), size, type, this.configuration.getSafeRenderer());
            } catch (IllegalArgumentException ex2) {
                throw new MacroExecutionException(WRONG_CONTENT_ERROR);
            }
        } catch (IllegalArgumentException ex) {
            throw new MacroExecutionException(WRONG_CONTENT_ERROR);
        }

        // If no image was generated, just return the original text
        if (result == null) {
            result = new WordBlock(content);
        }
        // Block level formulae should be wrapped in a paragraph element
        if (!context.isInline()) {
            result = new ParagraphBlock(Collections.<Block> singletonList(result));
        }
        return Collections.singletonList(result);
    }

    /**
     * Renders the formula using the specified renderer.
     * 
     * @param formula the formula text
     * @param inline is the formula supposed to be used inline or as a block-level element
     * @param fontSize the specified font size
     * @param imageType the specified resulting image type
     * @param rendererHint the hint for the renderer to use
     * @return the resulting block holding the generated image, or {@code null} in case of an error.
     * @throws MacroExecutionException if no renderer exists for the passed hint or if that rendered failed to render
     *         the formula
     * @throws IllegalArgumentException if the formula is not valid, according to the LaTeX syntax
     */
    private Block render(String formula, boolean inline, FontSize fontSize, Type imageType, String rendererHint)
        throws MacroExecutionException, IllegalArgumentException
    {
        try {
            FormulaRenderer renderer = this.manager.getInstance(FormulaRenderer.class, rendererHint);
            String imageName = renderer.process(formula, inline, fontSize, imageType);
            // TODO: HACK!!
            // We're going through the getAttachmentURL() API so that when the PdfURLFactory is used, the generated
            // image is saved and then embedded in the exported PDF thanks to PDFURIResolver. In the future we need
            // to remove this hack by introduce a proper Resource for generated image (say TemporaryResource),
            // implement a TemporaryResourceSerializer<URL> and introduce a ResourceLoader interface and have it
            // implemented for TemporaryResource...
            AttachmentReference attachmentReference =
                new AttachmentReference(imageName, this.dab.getCurrentDocumentReference());
            String url = this.dab.getAttachmentURL(attachmentReference, false);
            // Note that we have to replace the download action by the tex action since the getAttachmentURL() API
            // will use the "download" action but when the generated URL is called by the browser it needs to point to
            // the TexAction...
            url = url.replace("/download/", "/tex/");
            // TODO: end HACK!!
            ResourceReference imageReference = new ResourceReference(url, ResourceType.URL);
            ImageBlock result = new ImageBlock(imageReference, false);
            // Set the alternative text for the image to be the original formula
            result.setParameter("alt", formula);
            return result;
        } catch (Exception e) {
            throw new MacroExecutionException(
                String.format("Failed to render formula using the [%s] renderer", rendererHint), e);
        }
    }

    @Override
    public boolean supportsInlineMode()
    {
        return true;
    }
}
