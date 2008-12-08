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
package org.xwiki.rendering.internal.macro.include;

import java.io.StringReader;
import java.util.List;

import org.xwiki.bridge.DocumentAccessBridge;
import org.xwiki.context.Execution;
import org.xwiki.context.ExecutionContext;
import org.xwiki.context.ExecutionContextInitializerException;
import org.xwiki.context.ExecutionContextInitializerManager;
import org.xwiki.rendering.block.Block;
import org.xwiki.rendering.block.XDOM;
import org.xwiki.rendering.internal.transformation.MacroTransformation;
import org.xwiki.rendering.internal.util.EnumConverter;
import org.xwiki.rendering.macro.AbstractMacro;
import org.xwiki.rendering.macro.MacroExecutionException;
import org.xwiki.rendering.macro.descriptor.DefaultMacroDescriptor;
import org.xwiki.rendering.macro.include.IncludeMacroParameters.Context;
import org.xwiki.rendering.macro.include.IncludeMacroParameters;
import org.xwiki.rendering.parser.Parser;
import org.xwiki.rendering.parser.Syntax;
import org.xwiki.rendering.parser.SyntaxType;
import org.xwiki.rendering.transformation.MacroTransformationContext;

/**
 * @version $Id$
 * @since 1.5M2
 */
public class IncludeMacro extends AbstractMacro<IncludeMacroParameters>
{
    /**
     * The description of the macro.
     */
    private static final String DESCRIPTION = "Include other pages into the current page.";

    /**
     * The XWiki 2.0 syntax identifier.
     */
    private static final Syntax SYNTAX = new Syntax(SyntaxType.XWIKI, "2.0");

    /**
     * Injected by the Component Manager.
     */
    private Parser parser;

    /**
     * Injected by the Component Manager.
     */
    private Execution execution;

    /**
     * Injected by the Component Manager.
     */
    private ExecutionContextInitializerManager executionContextInitializerManager;

    /**
     * USed to acces document content and check view acces right.
     */
    private DocumentAccessBridge documentAccessBridge;

    /**
     * Default constructor.
     */
    public IncludeMacro()
    {
        super(new DefaultMacroDescriptor(DESCRIPTION, IncludeMacroParameters.class));

        registerConverter(new EnumConverter(Context.class), Context.class);
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

    /**
     * Allows overriding the Document Access Bridge used (useful for unit tests).
     * 
     * @param documentAccessBridge the new Document Access Bridge to use
     */
    public void setDocumentAccessBridge(DocumentAccessBridge documentAccessBridge)
    {
        this.documentAccessBridge = documentAccessBridge;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.macro.Macro#execute(Object, String, MacroTransformationContext)
     */
    public List<Block> execute(IncludeMacroParameters parameters, String content, MacroTransformationContext context)
        throws MacroExecutionException
    {
        String documentName = parameters.getDocument();
        Context actualContext = parameters.getContext();

        // Retrieve the included document's content
        String includedContent = null;
        try {
            if (this.documentAccessBridge.isDocumentViewable(documentName)) {
                includedContent = this.documentAccessBridge.getDocumentContent(documentName);
            } else {
                throw new MacroExecutionException("Current user doesn't have view rights on document [" + documentName
                    + "]");
            }
        } catch (Exception e) {
            throw new MacroExecutionException("Failed to get content for Document [" + documentName + "]", e);
        }

        List<Block> result;

        // Check the value of the "context" parameter.
        //
        // If CONTEXT_NEW then get the included
        // page content, parse it, apply Transformations to it and return the resulting AST.
        // Note that we need to push a new Container Request before doing this so that the
        // Velocity, Groovy and any other scripting languages have a clean execution context.
        //
        // if CONTEXT_CURRENT, then simply get the included page's content, parse it and return
        // the resulting AST (i.e. don't apply any transformations since we don't want any Macro
        // to be executed at this stage since they should be executed by the currently running
        // Macro Transformation.
        if (actualContext == Context.NEW) {
            result = executeWithNewContext(documentName, includedContent, context.getMacroTransformation());
        } else {
            result = executeWithCurrentContext(documentName, includedContent);
        }

        return result;
    }

    /**
     * Parse and execute target document content in a new context.
     * 
     * @param includedDocumentName the name of the document to include.
     * @param includedContent the content of the document to include.
     * @param macroTransformation the macro transformation.
     * @return the result of parsing and transformation of the document to include.
     * @throws MacroExecutionException error when parsing content.
     */
    private List<Block> executeWithNewContext(String includedDocumentName, String includedContent,
        MacroTransformation macroTransformation) throws MacroExecutionException
    {
        List<Block> result;

        // Push new Execution Context to isolate the contexts (Velocity, Groovy, etc).
        // TODO: Instead of creating a new Execution Context we should somehow clone the Execution Context
        // at the moment the rendering process starts. For example imagine a URL having
        // &skin=someskin. Since this affects the global velocimacros used the included page
        // should be processed with the same URL, which is not the case right now.
        try {
            ExecutionContext ec = new ExecutionContext();
            this.executionContextInitializerManager.initialize(ec);
            this.execution.pushContext(ec);
            // TODO: Need to set the current document, space and wiki. This is required for wiki syntax acting on
            // documents. For example if a link says "WebHome" it should point to the webhome of the current space.
            result = generateIncludedPageDOM(includedDocumentName, includedContent, macroTransformation);
        } catch (ExecutionContextInitializerException e) {
            throw new MacroExecutionException("Failed to create new Execution Context for included page ["
                + includedDocumentName + "]", e);
        } finally {
            // Reset the Execution Context as before
            this.execution.popContext();
        }

        return result;
    }

    /**
     * Parse and execute target document content in a the current context.
     * 
     * @param includedDocumentName the name of the document to include.
     * @param includedContent the content of the document to include.
     * @return the result of parsing and transformation of the document to include.
     * @throws MacroExecutionException error when parsing content.
     */
    private List<Block> executeWithCurrentContext(String includedDocumentName, String includedContent)
        throws MacroExecutionException
    {
        return generateIncludedPageDOM(includedDocumentName, includedContent, null);
    }

    /**
     * Parse and execute target document content.
     * 
     * @param includedDocumentName the name of the document to include.
     * @param includedContent the content of the document to include.
     * @param macroTransformation the macro transformation.
     * @return the result of parsing and transformation of the document to include.
     * @throws MacroExecutionException error when parsing content.
     */
    private List<Block> generateIncludedPageDOM(String includedDocumentName, String includedContent,
        MacroTransformation macroTransformation) throws MacroExecutionException
    {
        XDOM includedDom;
        try {
            includedDom = this.parser.parse(new StringReader(includedContent));

            // Only run Macro transformation when the context is a new one as otherwise we need the macros in the
            // included page to be added to the list of macros on the including page so that they're all sorted
            // and executed in the right order. Note that this works only because the Include macro has the highest
            // execution priority and is thus executed first.
            if (macroTransformation != null) {
                macroTransformation.transform(includedDom, SYNTAX);
            }
        } catch (Exception e) {
            throw new MacroExecutionException("Failed to parse included page [" + includedDocumentName + "]", e);
        }

        return includedDom.getChildren();
    }
}
