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
package org.xwiki.rendering.internal.macro.dashboard;

import java.io.StringReader;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;

import org.apache.velocity.VelocityContext;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.annotation.Requirement;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.context.Execution;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.rendering.block.Block;
import org.xwiki.rendering.block.MetaDataBlock;
import org.xwiki.rendering.block.XDOM;
import org.xwiki.rendering.listener.MetaData;
import org.xwiki.rendering.macro.dashboard.Gadget;
import org.xwiki.rendering.macro.dashboard.GadgetReader;
import org.xwiki.rendering.parser.Parser;
import org.xwiki.rendering.transformation.MacroTransformationContext;
import org.xwiki.rendering.util.ParserUtils;
import org.xwiki.velocity.VelocityEngine;
import org.xwiki.velocity.VelocityManager;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;

/**
 * Default gadget reader, reads the gadgets from XWiki Objects attached to the current document.
 * 
 * @version $Id$
 * @since 3.0M3
 */
@Component
public class DefaultGadgetReader implements GadgetReader
{
    /**
     * The reference to the gadgets class, relative to the current wiki.
     * TODO: to make sure that this class exists before trying to read objects of this type.
     */
    private static final EntityReference GADGET_CLASS =
        new EntityReference("GadgetClass", EntityType.DOCUMENT, new EntityReference("XWiki", EntityType.SPACE));

    /**
     * The execution context, to grab XWiki context and access to documents.
     */
    @Requirement
    protected Execution execution;

    /**
     * The current string reference resolver, to resolve the current document reference in the metadata of the block of
     * the current macro.
     */
    @Requirement("current")
    protected DocumentReferenceResolver<String> currentReferenceResolver;

    /**
     * The current entity reference resolver, to resolve the gadgets class reference.
     */
    @Requirement("current/reference")
    protected DocumentReferenceResolver<EntityReference> currentReferenceEntityResolver;

    /**
     * Used to get the Velocity Engine and Velocity Context to use to evaluate the titles of the gadgets.
     */
    @Requirement
    private VelocityManager velocityManager;

    /**
     * The component manager, to get the appropriate parser for the content of the gadget and the title. 
     */
    @Requirement
    private ComponentManager componentManager;

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.macro.dashboard.GadgetReader
     *      #getGadgets(org.xwiki.rendering.transformation.MacroTransformationContext)
     */
    public List<Gadget> getGadgets(MacroTransformationContext context) throws Exception
    {
        DocumentReference currentDocRef = getCurrentDocument(context);
        if (currentDocRef == null) {
            return new ArrayList<Gadget>();
        }

        // get the current document, read the objects and turn that into gadgets
        XWikiContext xContext = getXWikiContext();
        XWiki xWiki = xContext.getWiki();
        XWikiDocument currentDoc = xWiki.getDocument(currentDocRef, xContext);
        DocumentReference gadgetsClass = currentReferenceEntityResolver.resolve(GADGET_CLASS);
        List<BaseObject> gadgetObjects = currentDoc.getXObjects(gadgetsClass);

        if (gadgetObjects == null) {
            return new ArrayList<Gadget>();
        }

        return prepareGadgets(gadgetObjects, context);
    }

    /**
     * Prepares a list of gadgets from a list of XWiki objects.
     * 
     * @param objects the objects to read the gadgets from
     * @param context the macro transformation context, where the dashboard macro is being executed
     * @return the list of gadgets, as read from the xwiki objects
     * @throws Exception in case something happens while rendering the content in the objects
     */
    private List<Gadget> prepareGadgets(List<BaseObject> objects, MacroTransformationContext context) throws Exception
    {
        List<Gadget> gadgets = new ArrayList<Gadget>();

        // prepare velocity tools to render title
        VelocityContext velocityContext = velocityManager.getVelocityContext();
        // Use the Transformation id as the name passed to the Velocity Engine. This name is used internally
        // by Velocity as a cache index key for caching macros.
        String key = context.getTransformationContext().getId();
        if (key == null) {
            key = "unknown namespace";
        }
        VelocityEngine velocityEngine = velocityManager.getVelocityEngine();
        // prepare the parser to parse the title and content of the gadget into blocks
        Parser contentParser = (Parser) componentManager.lookup(Parser.class, context.getSyntax().toIdString());
        ParserUtils parserUtils = new ParserUtils();

        for (BaseObject xObject : objects) {
            if (xObject == null) {
                continue;
            }
            // get the data about the gadget from the object
            // TODO: filter for dashboard name when that field will be in
            String title = xObject.getStringValue("title");
            String content = xObject.getLargeStringValue("content");
            String position = xObject.getStringValue("position");

            // render title with velocity
            StringWriter writer = new StringWriter();
            // FIXME: the engine has an issue with $ and # as last character. To test and fix if it happens
            velocityEngine.evaluate(velocityContext, writer, key, title);
            String gadgetTitle = writer.toString();
            
            // parse both the title and content in the syntax of the transformation context
            XDOM titleXDom = contentParser.parse(new StringReader(gadgetTitle));
            List<Block> titleBlocks = titleXDom.getChildren();
            parserUtils.removeTopLevelParagraph(titleBlocks);
            XDOM contentXDom = contentParser.parse(new StringReader(content));
            List<Block> contentBlocks = contentXDom.getChildren();
            parserUtils.removeTopLevelParagraph(contentBlocks);

            // create a gadget will all these and add the gadget to the container of gadgets
            gadgets.add(new Gadget(titleBlocks, contentBlocks, position));
        }
        return gadgets;
    }

    /**
     * Gets the current document reference from the context.
     * 
     * @param context the macro transformation context
     * @return the document reference to the current document (the document containing the macro, if it's an include)
     */
    private DocumentReference getCurrentDocument(MacroTransformationContext context)
    {
        // go up recursively to the first metadata block that has a source metadata
        Block currentBlock = context.getCurrentMacroBlock();
        MetaDataBlock metadataBlock = currentBlock.getPreviousBlockByType(MetaDataBlock.class, true);
        String sourceMetadata = null;
        while (sourceMetadata == null && metadataBlock != null) {
            sourceMetadata = (String) metadataBlock.getMetaData().getMetaData(MetaData.SOURCE);
            metadataBlock = metadataBlock.getPreviousBlockByType(MetaDataBlock.class, true);
        }

        // if no such block was found, the reference is null
        if (sourceMetadata == null) {
            return null;
        }

        // resolve the source reference
        DocumentReference currentDocRef = currentReferenceResolver.resolve(sourceMetadata);
        return currentDocRef;
    }

    /**
     * Gets the xwiki context from the execution context.
     * 
     * @return the xwiki context
     */
    private XWikiContext getXWikiContext()
    {
        return (XWikiContext) execution.getContext().getProperty("xwikicontext");
    }
}
