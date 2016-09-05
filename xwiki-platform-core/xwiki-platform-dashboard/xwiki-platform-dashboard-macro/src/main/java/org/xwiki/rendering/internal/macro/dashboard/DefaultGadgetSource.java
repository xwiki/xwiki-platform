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

import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.commons.lang3.StringUtils;
import org.apache.velocity.VelocityContext;
import org.xwiki.component.annotation.Component;
import org.xwiki.context.Execution;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.rendering.block.Block;
import org.xwiki.rendering.block.GroupBlock;
import org.xwiki.rendering.block.LinkBlock;
import org.xwiki.rendering.block.WordBlock;
import org.xwiki.rendering.block.XDOM;
import org.xwiki.rendering.listener.reference.ResourceReference;
import org.xwiki.rendering.listener.reference.ResourceType;
import org.xwiki.rendering.macro.dashboard.Gadget;
import org.xwiki.rendering.macro.dashboard.GadgetSource;
import org.xwiki.rendering.parser.ContentParser;
import org.xwiki.rendering.syntax.Syntax;
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
@Singleton
public class DefaultGadgetSource implements GadgetSource
{
    /**
     * The reference to the gadgets class, relative to the current wiki. <br>
     * TODO: to make sure that this class exists before trying to read objects of this type.
     */
    private static final EntityReference GADGET_CLASS =
        new EntityReference("GadgetClass", EntityType.DOCUMENT, new EntityReference("XWiki", EntityType.SPACE));

    /**
     * The execution context, to grab XWiki context and access to documents.
     */
    @Inject
    protected Execution execution;

    /**
     * The current string reference resolver, to resolve the current document reference in the metadata of the block of
     * the current macro.
     */
    @Inject
    @Named("current")
    protected DocumentReferenceResolver<String> currentReferenceResolver;

    /**
     * The current entity reference resolver, to resolve the gadgets class reference.
     */
    @Inject
    @Named("current")
    protected DocumentReferenceResolver<EntityReference> currentReferenceEntityResolver;

    @Inject
    @Named("local")
    private EntityReferenceSerializer<String> localReferenceSerializer;

    /**
     * Used to get the Velocity Engine and Velocity Context to use to evaluate the titles of the gadgets.
     */
    @Inject
    private VelocityManager velocityManager;

    /**
     * The parser, to parse the content of the gadget and the title.
     */
    @Inject
    private ContentParser contentParser;

    @Override
    public List<Gadget> getGadgets(String source, MacroTransformationContext context) throws Exception
    {
        // use the passed source as a document reference
        DocumentReference sourceDocRef = getSourceDocumentReference(source);
        if (sourceDocRef == null) {
            return new ArrayList<>();
        }

        // get the current document, read the objects and turn that into gadgets
        XWikiContext xContext = getXWikiContext();
        XWiki xWiki = xContext.getWiki();
        XWikiDocument sourceDoc = xWiki.getDocument(sourceDocRef, xContext);
        DocumentReference gadgetsClass = currentReferenceEntityResolver.resolve(GADGET_CLASS);
        List<BaseObject> gadgetObjects = sourceDoc.getXObjects(gadgetsClass);

        if (gadgetObjects == null) {
            return new ArrayList<>();
        }

        return prepareGadgets(gadgetObjects, sourceDoc.getSyntax(), context);
    }

    /**
     * Prepares a list of gadgets from a list of XWiki objects.
     * 
     * @param objects the objects to read the gadgets from
     * @param sourceSyntax the syntax of the source of the gadget objects
     * @param context the macro transformation context, where the dashboard macro is being executed
     * @return the list of gadgets, as read from the xwiki objects
     * @throws Exception in case something happens while rendering the content in the objects
     */
    private List<Gadget> prepareGadgets(List<BaseObject> objects, Syntax sourceSyntax,
        MacroTransformationContext context) throws Exception
    {
        List<Gadget> gadgets = new ArrayList<>();

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
            String id = xObject.getNumber() + "";

            // render title with velocity
            StringWriter writer = new StringWriter();
            // FIXME: the engine has an issue with $ and # as last character. To test and fix if it happens
            velocityEngine.evaluate(velocityContext, writer, key, title);
            String gadgetTitle = writer.toString();

            // parse both the title and content in the syntax of the transformation context
            XDOM titleXDom = contentParser.parse(gadgetTitle, sourceSyntax, xObject.getDocumentReference());
            List<Block> titleBlocks = titleXDom.getChildren();
            parserUtils.removeTopLevelParagraph(titleBlocks);
            XDOM contentXDom = contentParser.parse(content, sourceSyntax, xObject.getDocumentReference());
            List<Block> contentBlocks = contentXDom.getChildren();
            parserUtils.removeTopLevelParagraph(contentBlocks);

            // create a gadget will all these and add the gadget to the container of gadgets
            Gadget gadget = new Gadget(id, titleBlocks, contentBlocks, position);
            gadget.setTitleSource(title);
            gadgets.add(gadget);
        }
        return gadgets;
    }

    /**
     * Resolves the source of the dashboard, based on the source parameter passed to this reader, handling the default
     * behaviour when the source is missing.
     * 
     * @param source the serialized reference of the document to read gadgets from
     * @return the document reference to the current document (the document containing the macro, if it's an include)
     */
    private DocumentReference getSourceDocumentReference(String source)
    {
        // if the source is empty or null, use current document
        if (StringUtils.isEmpty(source)) {
            return getXWikiContext().getDoc().getDocumentReference();
        }

        // resolve the source as document reference, relative to current context
        return currentReferenceResolver.resolve(source);
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

    @Override
    public List<Block> getDashboardSourceMetadata(String source, MacroTransformationContext context)
    {
        DocumentReference sourceDoc = getSourceDocumentReference(source);
        String classParameterName = "class";
        GroupBlock metadataContainer = new GroupBlock();
        metadataContainer.setParameter(classParameterName, DashboardMacro.METADATA);

        // generate anchors for the urls
        XWikiContext xContext = getXWikiContext();
        String editURL = xContext.getWiki().getURL(sourceDoc, "save", "", "", xContext);
        LinkBlock editURLBlock =
            new LinkBlock(Collections.<Block> emptyList(), new ResourceReference(editURL, ResourceType.URL), false);
        editURLBlock.setParameter(classParameterName, DashboardMacro.EDIT_URL);
        metadataContainer.addChild(editURLBlock);
        String removeURL = xContext.getWiki().getURL(sourceDoc, "objectremove", "", "", xContext);
        LinkBlock removeURLBlock =
            new LinkBlock(Collections.<Block> emptyList(), new ResourceReference(removeURL, ResourceType.URL), false);
        removeURLBlock.setParameter(classParameterName, DashboardMacro.REMOVE_URL);
        metadataContainer.addChild(removeURLBlock);
        String addURL = xContext.getWiki().getURL(sourceDoc, "objectadd", "", "", xContext);
        LinkBlock addURLBlock =
            new LinkBlock(Collections.<Block> emptyList(), new ResourceReference(addURL, ResourceType.URL), false);
        addURLBlock.setParameter(classParameterName, DashboardMacro.ADD_URL);
        metadataContainer.addChild(addURLBlock);

        // and create divs for the source metadata
        GroupBlock sourcePageBlock = new GroupBlock();
        sourcePageBlock.addChild(new WordBlock(sourceDoc.getName()));
        sourcePageBlock.setParameter(classParameterName, DashboardMacro.SOURCE_PAGE);
        metadataContainer.addChild(sourcePageBlock);
        GroupBlock sourceSpaceBlock = new GroupBlock();
        // Extract the full Space Reference (in order to support Nested Spaces) and set it in the XDOM
        sourceSpaceBlock.addChild(new WordBlock(
            this.localReferenceSerializer.serialize(sourceDoc.getLastSpaceReference())));
        sourceSpaceBlock.setParameter(classParameterName, DashboardMacro.SOURCE_SPACE);
        metadataContainer.addChild(sourceSpaceBlock);
        GroupBlock sourceWikiBlock = new GroupBlock();
        sourceWikiBlock.addChild(new WordBlock(sourceDoc.getWikiReference().getName()));
        sourceWikiBlock.setParameter(classParameterName, DashboardMacro.SOURCE_WIKI);
        metadataContainer.addChild(sourceWikiBlock);
        String sourceURL = xContext.getWiki().getURL(sourceDoc, "view", "", "", xContext);
        LinkBlock sourceURLBlock =
            new LinkBlock(Collections.<Block> emptyList(), new ResourceReference(sourceURL, ResourceType.URL), false);
        sourceURLBlock.setParameter(classParameterName, DashboardMacro.SOURCE_URL);
        metadataContainer.addChild(sourceURLBlock);

        return Collections.<Block> singletonList(metadataContainer);
    }

    @Override
    public boolean isEditing()
    {
        // get the XWiki context and look at the action. if it's "inline" or "edit", it's edit mode
        XWikiContext context = getXWikiContext();
        return "inline".equals(context.getAction()) || "edit".equals(context.getAction());
    }
}
