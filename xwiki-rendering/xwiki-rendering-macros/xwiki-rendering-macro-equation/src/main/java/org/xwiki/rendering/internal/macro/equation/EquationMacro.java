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
package org.xwiki.rendering.internal.macro.equation;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.xwiki.bridge.DocumentAccessBridge;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.annotation.Requirement;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.equation.EquationRenderer;
import org.xwiki.equation.EquationRenderer.FontSize;
import org.xwiki.equation.EquationRenderer.Type;
import org.xwiki.rendering.block.Block;
import org.xwiki.rendering.block.ImageBlock;
import org.xwiki.rendering.block.ParagraphBlock;
import org.xwiki.rendering.block.WordBlock;
import org.xwiki.rendering.listener.Image;
import org.xwiki.rendering.listener.URLImage;
import org.xwiki.rendering.macro.AbstractMacro;
import org.xwiki.rendering.macro.MacroExecutionException;
import org.xwiki.rendering.macro.descriptor.DefaultContentDescriptor;
import org.xwiki.rendering.macro.equation.EquationMacroConfiguration;
import org.xwiki.rendering.macro.equation.EquationMacroParameters;
import org.xwiki.rendering.transformation.MacroTransformationContext;

/**
 * Displays an equation, in LaTeX syntax, as an image.
 * 
 * @version $Id$
 * @since 2.0M3
 */
@Component("equation")
public class EquationMacro extends AbstractMacro<EquationMacroParameters>
{
    /** Predefined error message: empty equation. */
    public static final String CONTENT_MISSING_ERROR = "The mandatory equation text is missing.";

    /** Predefined error message: invalid equation. */
    public static final String WRONG_CONTENT_ERROR = "The equation text is not valid, please correct it.";

    /** Logging helper object. */
    private static final Log LOG = LogFactory.getLog(EquationMacro.class);

    /** The description of the macro. */
    private static final String DESCRIPTION = "Displays a mathematical equation.";

    /** The description of the macro content. */
    private static final String CONTENT_DESCRIPTION = "The mathematical equation, in LaTeX syntax";

    /** Component manager, needed for retrieving the selected equation renderer. */
    @Requirement
    private ComponentManager manager;

    /** Defines from where to read the rendering configuration data. */
    @Requirement
    private EquationMacroConfiguration configuration;

    /** Needed for computing the URL for accessing the rendered image. */
    @Requirement
    private DocumentAccessBridge dab;

    /**
     * Create and initialize the descriptor of the macro.
     */
    public EquationMacro()
    {
        super(DESCRIPTION, new DefaultContentDescriptor(CONTENT_DESCRIPTION), EquationMacroParameters.class);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.macro.Macro#execute(Object, String, MacroTransformationContext)
     */
    public List<Block> execute(EquationMacroParameters parameters, String content, MacroTransformationContext context)
        throws MacroExecutionException
    {
        if (StringUtils.isEmpty(content)) {
            throw new MacroExecutionException(CONTENT_MISSING_ERROR);
        }

        String rendererHint = this.configuration.getRenderer();
        FontSize size = parameters.getFontSize();
        Type type = parameters.getImageType();
        Block result = null;
        try {
            result = render(content, context.isInline(), size, type, rendererHint);
        } catch (ComponentLookupException ex) {
            LOG.error("Invalid renderer: [" + rendererHint + "]. Falling back to the safe renderer.", ex);
            try {
                result = render(content, context.isInline(), size, type, this.configuration.getSafeRenderer());
            } catch (ComponentLookupException ex2) {
                LOG.error("Safe renderer not found. No image generated. Returning plain text.", ex);
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
        // Block level equations should be wrapped in a paragraph element
        if (!context.isInline()) {
            result = new ParagraphBlock(Collections.<Block> singletonList(result));
        }
        return Collections.singletonList(result);
    }

    /**
     * Renders the equation using the specified renderer.
     * 
     * @param equation the equation text
     * @param inline is the equation supposed to be used inline or as a block-level element
     * @param fontSize the specified font size
     * @param imageType the specified resulting image type
     * @param rendererHint the hint for the renderer to use
     * @return the resulting block holding the generated image, or {@code null} in case of an error.
     * @throws ComponentLookupException if no component with the specified hint can be retrieved
     * @throws IllegalArgumentException if the equation is not valid, according to the LaTeX syntax
     */
    private Block render(String equation, boolean inline, FontSize fontSize, Type imageType, String rendererHint)
        throws ComponentLookupException, IllegalArgumentException
    {
        try {
            EquationRenderer renderer = this.manager.lookup(EquationRenderer.class, rendererHint);
            String imageName = renderer.process(equation, inline, fontSize, imageType);
            String url = this.dab.getURL(null, "tex", null, null) + "/" + imageName;
            Image image = new URLImage(url);
            ImageBlock result = new ImageBlock(image, false);
            // Set the alternative text for the image to be the original equation
            result.setParameter("alt", equation);
            return result;
        } catch (IOException ex) {
            throw new ComponentLookupException("Failed to render equation using [" + rendererHint + "] renderer");
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.macro.Macro#supportsInlineMode()
     */
    public boolean supportsInlineMode()
    {
        return true;
    }
}
