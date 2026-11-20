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
package org.xwiki.wysiwyg.internal.converter;

import java.io.StringReader;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Provider;
import jakarta.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.rendering.block.XDOM;
import org.xwiki.rendering.internal.transformation.MutableRenderingContext;
import org.xwiki.rendering.listener.MetaData;
import org.xwiki.rendering.parser.Parser;
import org.xwiki.rendering.renderer.BlockRenderer;
import org.xwiki.rendering.renderer.printer.DefaultWikiPrinter;
import org.xwiki.rendering.renderer.printer.WikiPrinter;
import org.xwiki.rendering.syntax.Syntax;
import org.xwiki.rendering.transformation.RenderingContext;
import org.xwiki.rendering.transformation.Transformation;
import org.xwiki.rendering.transformation.TransformationContext;

/**
 * Refreshes the content edited with a WYSIWYG editor whose content syntax is not HTML (for example BlockNote):
 * executes the macro calls of the edited content, so that the editor can show the output of the macro calls that were
 * inserted or modified since the content was loaded. The edited content is handled like when it's saved and then
 * viewed: the content and the parameters of the macro calls that are edited in-place are serialized in the syntax used
 * to save the content, and the macros are executed for that syntax.
 *
 * @version $Id$
 * @since 18.10.0RC1
 */
@Component(roles = EditedContentRefresher.class)
@Singleton
public class EditedContentRefresher
{
    // Executing the macros of the edited content with the same transformation id lets them share their execution
    // context (for example the variables defined by a script macro are visible to the following script macros).
    private static final String TRANSFORMATION_ID = "wysiwygRefresh";

    @Inject
    private RenderingContext renderingContext;

    @Inject
    @Named("macro")
    private Transformation macroTransformation;

    @Inject
    @Named("context")
    private Provider<ComponentManager> contextComponentManagerProvider;

    @Inject
    private EntityReferenceSerializer<String> serializer;

    /**
     * Parses the given edited content, executes its macro calls and renders the result in the syntax of the editor.
     *
     * @param content the edited content, in the syntax of the editor
     * @param editorSyntax the syntax of the editor, used to parse the given content and to render the result
     * @param syntax the syntax used to save the edited content, which is the default syntax of the macro content
     * @param sourceReference the document where the edited content is saved, used to resolve the references found in
     *            the content
     * @param restricted {@code true} to execute the macros in a restricted context
     * @return the refreshed content, in the syntax of the editor
     * @throws Exception if the parser or the renderer of the editor syntax is missing, or if the given content can't be
     *             parsed or if the macros can't be executed
     */
    public String refresh(String content, Syntax editorSyntax, Syntax syntax, DocumentReference sourceReference,
        boolean restricted) throws Exception
    {
        ComponentManager componentManager = this.contextComponentManagerProvider.get();
        Parser parser = componentManager.getInstance(Parser.class, editorSyntax.toIdString());
        BlockRenderer renderer = componentManager.getInstance(BlockRenderer.class, editorSyntax.toIdString());

        XDOM xdom = parse(parser, content, syntax);
        xdom.getMetaData().addMetaData(MetaData.SOURCE, this.serializer.serialize(sourceReference));
        // The syntax meta data is the default syntax of the macro content.
        xdom.getMetaData().addMetaData(MetaData.SYNTAX, syntax);

        TransformationContext transformationContext = new TransformationContext(xdom, syntax, restricted);
        transformationContext.setId(TRANSFORMATION_ID);
        transformationContext.setTargetSyntax(editorSyntax);
        ((MutableRenderingContext) this.renderingContext).transformInContext(this.macroTransformation,
            transformationContext, xdom);

        WikiPrinter printer = new DefaultWikiPrinter();
        renderer.render(xdom, printer);
        return printer.toString();
    }

    private XDOM parse(Parser parser, String content, Syntax syntax) throws Exception
    {
        // The content and the parameters of the macro calls that are edited in-place are serialized, while parsing, in
        // the target syntax of the rendering context, which has to be the syntax used to save the content.
        MutableRenderingContext mutableRenderingContext = (MutableRenderingContext) this.renderingContext;
        mutableRenderingContext.push(this.renderingContext.getTransformation(), this.renderingContext.getXDOM(),
            syntax, this.renderingContext.getTransformationId(), this.renderingContext.isRestricted(), syntax);
        try {
            return parser.parse(new StringReader(content));
        } finally {
            mutableRenderingContext.pop();
        }
    }
}
