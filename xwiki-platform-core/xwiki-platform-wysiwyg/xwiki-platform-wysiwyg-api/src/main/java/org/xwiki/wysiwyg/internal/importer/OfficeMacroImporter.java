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
package org.xwiki.wysiwyg.internal.importer;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.xwiki.bridge.DocumentAccessBridge;
import org.xwiki.bridge.DocumentModelBridge;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.model.reference.AttachmentReference;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.rendering.block.Block;
import org.xwiki.rendering.block.MacroBlock;
import org.xwiki.rendering.block.XDOM;
import org.xwiki.rendering.internal.transformation.MutableRenderingContext;
import org.xwiki.rendering.listener.MetaData;
import org.xwiki.rendering.renderer.BlockRenderer;
import org.xwiki.rendering.renderer.printer.DefaultWikiPrinter;
import org.xwiki.rendering.renderer.printer.WikiPrinter;
import org.xwiki.rendering.syntax.Syntax;
import org.xwiki.rendering.transformation.RenderingContext;
import org.xwiki.rendering.transformation.Transformation;
import org.xwiki.rendering.transformation.TransformationContext;

/**
 * Used to import an office attachment using the office macro (rather than converting and including the content of the
 * office attachment).
 * 
 * @version $Id$
 * @since 9.8
 */
@Component(roles = OfficeMacroImporter.class)
@Singleton
public class OfficeMacroImporter
{
    /**
     * Used to update the rendering context.
     */
    @Inject
    private RenderingContext renderingContext;

    /**
     * The component used to execute the XDOM macro transformations before rendering to XHTML.
     * <p>
     * NOTE: We execute only macro transformations because they are the only transformations protected by the WYSIWYG
     * editor. We should use the transformation manager once generic transformation markers are implemented in the
     * rendering module and the WYSIWYG editor supports them.
     * 
     * @see <a href="https://jira.xwiki.org/browse/XRENDERING-78">XWIKI-3260: Add markers to modified XDOM by
     *      Transformations/Macros</a>
     */
    @Inject
    @Named("macro")
    private Transformation macroTransformation;

    /**
     * Used to look up the renderer matching the requested output syntax.
     */
    @Inject
    @Named("context")
    private Provider<ComponentManager> contextComponentManagerProvider;

    /**
     * Used to serialize the reference of the document that has the office file attachment.
     */
    @Inject
    private EntityReferenceSerializer<String> entityReferenceSerializer;

    /**
     * Used to find out the Syntax of the document containing the attachment to render.
     */
    @Inject
    private DocumentAccessBridge documentAccessBridge;

    /**
     * Builds an XDOM with a single block, the office macro.
     * 
     * @param attachmentReference the office file to be viewed
     * @param filterStyles whether to filter text styles such as font, color, alignment, margins, etc.
     * @return the XDOM that can be rendered to view the office file
     */
    public XDOM buildXDOM(AttachmentReference attachmentReference, boolean filterStyles)
    {
        Map<String, String> macroParams = new HashMap<>();
        macroParams.put("attachment", attachmentReference.getName());
        if (!filterStyles) {
            macroParams.put("filterStyles", "false");
        }
        MacroBlock officeMacro = new MacroBlock("office", macroParams, false);

        XDOM xdom = new XDOM(Collections.<Block>singletonList(officeMacro));

        // Since we're generating an XDOM block we need to set up the required MetaData information

        // Set the BASE MetaData
        xdom.getMetaData().addMetaData(MetaData.BASE,
            entityReferenceSerializer.serialize(attachmentReference.getDocumentReference()));

        // Set the SYNTAX MetaData
        try {
            DocumentModelBridge document =
                documentAccessBridge.getTranslatedDocumentInstance(attachmentReference.getDocumentReference());
            xdom.getMetaData().addMetaData(MetaData.SYNTAX, document.getSyntax());
        } catch (Exception e) {
            throw new RuntimeException(String.format(
                "Failed to compute Syntax for the document containing attachment [%s]", attachmentReference), e);
        }

        return xdom;
    }

    /**
     * Renders the given XDOM to the specified syntax.
     * 
     * @param xdom the XDOM to be rendered
     * @param outputSyntax the syntax to render the given XDOM to
     * @return the result of rendering the given XDOM to the specified syntax
     * @throws Exception if rendering fails, in particular if no renderer exists for the specified syntax
     * @since 19.0.0RC1
     */
    public String render(XDOM xdom, Syntax outputSyntax) throws Exception
    {
        TransformationContext txContext = new TransformationContext();
        txContext.setXDOM(xdom);
        // A macro can render differently per target syntax, so the transformation needs the syntax too.
        txContext.setTargetSyntax(outputSyntax);
        ((MutableRenderingContext) renderingContext).transformInContext(macroTransformation, txContext, xdom);

        BlockRenderer renderer =
            this.contextComponentManagerProvider.get().getInstance(BlockRenderer.class, outputSyntax.toIdString());
        WikiPrinter printer = new DefaultWikiPrinter();
        renderer.render(xdom, printer);

        return printer.toString();
    }
}
