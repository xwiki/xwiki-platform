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
package org.xwiki.rendering.internal.macro.context;

import java.util.Arrays;
import java.util.Collections;
import java.util.Map;

import org.jmock.Expectations;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.xwiki.bridge.DocumentAccessBridge;
import org.xwiki.component.descriptor.DefaultComponentDescriptor;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.properties.BeanManager;
import org.xwiki.rendering.block.Block;
import org.xwiki.rendering.block.LinkBlock;
import org.xwiki.rendering.block.MacroBlock;
import org.xwiki.rendering.block.MetaDataBlock;
import org.xwiki.rendering.block.ParagraphBlock;
import org.xwiki.rendering.block.XDOM;
import org.xwiki.rendering.listener.MetaData;
import org.xwiki.rendering.listener.reference.ResourceReference;
import org.xwiki.rendering.listener.reference.ResourceType;
import org.xwiki.rendering.macro.Macro;
import org.xwiki.rendering.macro.MacroContentParser;
import org.xwiki.rendering.macro.MacroExecutionException;
import org.xwiki.rendering.macro.context.ContextMacroParameters;
import org.xwiki.rendering.syntax.Syntax;
import org.xwiki.rendering.transformation.MacroTransformationContext;
import org.xwiki.rendering.wiki.WikiModel;
import org.xwiki.test.jmock.AbstractMockingComponentTestCase;
import org.xwiki.test.annotation.AllComponents;
import org.xwiki.test.jmock.annotation.MockingRequirement;

/**
 * Unit tests for {@link ContextMacro}.
 *
 * @version $Id$
 * @since 3.0M1
 */
@AllComponents
@MockingRequirement(value = ContextMacro.class, exceptions = { BeanManager.class, MacroContentParser.class })
public class ContextMacroTest extends AbstractMockingComponentTestCase
{
    private Macro macro;

    @Before
    public void configure() throws Exception
    {
        this.macro = getComponentManager().getInstance(Macro.class, "context");
    }

    @Test
    public void testExecuteWhenNoDocumentSpecified() throws Exception
    {
        ContextMacroParameters parameters = new ContextMacroParameters();

        try {
            this.macro.execute(parameters, "", new MacroTransformationContext());
            Assert.fail("Should have thrown an exception");
        } catch (MacroExecutionException expected) {
            Assert.assertEquals("You must specify a 'document' parameter pointing to the document to set in the "
                + "context as the current document.", expected.getMessage());
        }
    }

    @Test
    public void testExecuteWithReferencedDocumentHavingProgrammingRightsButNotCallingDocument() throws Exception
    {
        final MacroBlock macroBlock = new MacroBlock("context", Collections.<String, String>emptyMap(), false);
        final DocumentReferenceResolver<String> resolver =
            getComponentManager().getInstance(DocumentReferenceResolver.TYPE_STRING, "macro");
        final DocumentAccessBridge dab = getComponentManager().getInstance(DocumentAccessBridge.class);
        getMockery().checking(new Expectations() {{
            oneOf(resolver).resolve("wiki:space.page", macroBlock);
            will(returnValue(new DocumentReference("wiki", "space", "page")));
            // First call to hasProgrammingRights is for the current document
            oneOf(dab).hasProgrammingRights();
            will(returnValue(false));
            oneOf(dab).pushDocumentInContext(with(any(Map.class)), with(any(DocumentReference.class)));
            oneOf(dab).popDocumentFromContext(with(any(Map.class)));
            // Second call is for the passed document
            oneOf(dab).hasProgrammingRights();
            will(returnValue(true));
        }});

        ContextMacroParameters parameters = new ContextMacroParameters();
        parameters.setDocument("wiki:space.page");

        MacroTransformationContext macroContext = new MacroTransformationContext();
        macroContext.setCurrentMacroBlock(macroBlock);
        try {
            this.macro.execute(parameters, "", macroContext);
            Assert.fail("Should have thrown an exception");
        } catch (MacroExecutionException expected) {
            Assert.assertEquals("Current document must have programming rights since the context document provided ["
                + "wiki:space.page] has programming rights.", expected.getMessage());
        }
    }

    @Test
    public void testExecuteWithReferencedDocumentHavingProgrammingRightsAndCallingDocumentToo() throws Exception
    {
        final MacroBlock macroBlock = new MacroBlock("context", Collections.<String, String>emptyMap(), false);
        final MacroTransformationContext macroContext = new MacroTransformationContext();
        macroContext.setSyntax(Syntax.XWIKI_2_0);
        macroContext.setCurrentMacroBlock(macroBlock);

        final DocumentReferenceResolver<String> drr =
            getComponentManager().getInstance(DocumentReferenceResolver.TYPE_STRING, "macro");
        final DocumentAccessBridge dab = getComponentManager().getInstance(DocumentAccessBridge.class);
        getMockery().checking(new Expectations() {{
            oneOf(drr).resolve("wiki:space.page", macroBlock);
            will(returnValue(new DocumentReference("wiki", "space", "page")));
            // First call to hasProgrammingRights is for the current document
            oneOf(dab).hasProgrammingRights();
            will(returnValue(true));
            oneOf(dab).pushDocumentInContext(with(any(Map.class)), with(any(DocumentReference.class)));
            oneOf(dab).popDocumentFromContext(with(any(Map.class)));
            // Second call is for the passed document
            oneOf(dab).hasProgrammingRights();
            will(returnValue(true));
        }});

        ContextMacroParameters parameters = new ContextMacroParameters();
        parameters.setDocument("wiki:space.page");

        this.macro.execute(parameters, "", macroContext);
    }

    @Test
    public void testExecute() throws Exception
    {
        final MacroBlock macroBlock = new MacroBlock("context", Collections.<String, String>emptyMap(), false);
        final MacroTransformationContext macroContext = new MacroTransformationContext();
        macroContext.setSyntax(Syntax.XWIKI_2_0);
        macroContext.setCurrentMacroBlock(macroBlock);

        // Register a mock implementation of WikiModel so that we're in wiki mode and so that link references are
        // resolved as document references and not URLs.
        WikiModel wikiModel = getMockery().mock(WikiModel.class);
        DefaultComponentDescriptor<WikiModel> descriptorWM = new DefaultComponentDescriptor<WikiModel>();
        descriptorWM.setRole(WikiModel.class);
        getComponentManager().registerComponent(descriptorWM, wikiModel);

        // The XDOM returned by the Macro Content Parser
        XDOM xdom = new XDOM(Arrays.asList((Block) new ParagraphBlock(Arrays.asList((Block) new LinkBlock(
            Collections.<Block>emptyList(), new ResourceReference("", ResourceType.DOCUMENT), false)))));

        final DocumentReference docReference = new DocumentReference("wiki", "space", "page");

        final DocumentReferenceResolver<String> drr =
            getComponentManager().getInstance(DocumentReferenceResolver.TYPE_STRING, "macro");
        final DocumentAccessBridge dab = getComponentManager().getInstance(DocumentAccessBridge.class);
        getMockery().checking(new Expectations() {{
            oneOf(drr).resolve("wiki:space.page", macroBlock);
            will(returnValue(docReference));
            // First call to hasProgrammingRights is for the current document
            oneOf(dab).hasProgrammingRights();
            will(returnValue(false));
            oneOf(dab).pushDocumentInContext(with(any(Map.class)), with(any(DocumentReference.class)));
            oneOf(dab).popDocumentFromContext(with(any(Map.class)));
            // Second call is for the passed document
            oneOf(dab).hasProgrammingRights();
            will(returnValue(false));
        }});

        ContextMacroParameters parameters = new ContextMacroParameters();
        parameters.setDocument("wiki:space.page");

        // Note: we're not testing the returned value since we don't need to and is tested by the unit tests for both
        // the Macro Content Parser and the XDOM Resource Reference Resolver.
        this.macro.execute(parameters, "", macroContext);
    }

    @Test
    public void executeWithRelativeDocumentReferenceParameter() throws Exception
    {
        final MacroBlock macroBlock = new MacroBlock("context", Collections.<String, String>emptyMap(), false);
        MetaData metaData = new MetaData(
            Collections.<String, Object> singletonMap(MetaData.BASE, "basewiki:basespace.basepage"));
        new XDOM(Arrays.<Block>asList(new MetaDataBlock(Arrays.<Block>asList(macroBlock), metaData)));

        final MacroTransformationContext macroContext = new MacroTransformationContext();
        macroContext.setSyntax(Syntax.XWIKI_2_0);
        macroContext.setCurrentMacroBlock(macroBlock);

        final DocumentReferenceResolver<String> drr =
            getComponentManager().getInstance(DocumentReferenceResolver.TYPE_STRING, "macro");
        final DocumentReference docReference = new DocumentReference("basewiki", "basespace", "page");
        final DocumentAccessBridge dab = getComponentManager().getInstance(DocumentAccessBridge.class);
        getMockery().checking(new Expectations() {{
            oneOf(drr).resolve("page", macroBlock);
            will(returnValue(docReference));
            // First call to hasProgrammingRights is for the current document
            oneOf(dab).hasProgrammingRights();
            will(returnValue(false));
            oneOf(dab).pushDocumentInContext(with(any(Map.class)), with(any(DocumentReference.class)));
            oneOf(dab).popDocumentFromContext(with(any(Map.class)));
            // Second call is for the passed document
            oneOf(dab).hasProgrammingRights();
            will(returnValue(false));
        }});

        ContextMacroParameters parameters = new ContextMacroParameters();
        parameters.setDocument("page");

        this.macro.execute(parameters, "", macroContext);
    }
}
