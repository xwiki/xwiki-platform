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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.xwiki.bridge.DocumentAccessBridge;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.annotation.Requirement;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.component.phase.Composable;
import org.xwiki.context.Execution;
import org.xwiki.context.ExecutionContext;
import org.xwiki.context.ExecutionContextManager;
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
import org.xwiki.rendering.transformation.MacroTransformationContext;

/**
 * @version $Id$
 * @since 1.5M2
 */
@Component("include")
public class IncludeMacro extends AbstractMacro<IncludeMacroParameters> implements Composable
{
    /**
     * The description of the macro.
     */
    private static final String DESCRIPTION = "Include other pages into the current page.";

    /**
     * Used to find the parser from syntax identifier.
     */
    private ComponentManager componentManager;

    /**
     * Used to get the current context that we clone if the users asks to execute the included page in its
     * own context.
     */
    @Requirement
    private Execution execution;

    /**
     * Used in order to clone the execution context when the user asks to execute the included page in its
     * own context.
     */
    @Requirement
    private ExecutionContextManager executionContextManager;

    /**
     * Used to access document content and check view access right.
     */
    @Requirement
    private DocumentAccessBridge documentAccessBridge;

    /**
     * Default constructor.
     */
    public IncludeMacro()
    {
        super(new DefaultMacroDescriptor(DESCRIPTION, null, IncludeMacroParameters.class));

        registerConverter(new EnumConverter(Context.class), Context.class);
        
        // The include macro must execute first since if it runs with the current context it needs to bring
        // all the macros from the included page before the other macros are executed.
        setPriority(10);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.component.phase.Composable#compose(org.xwiki.component.manager.ComponentManager)
     */
    public void compose(ComponentManager componentManager)
    {
        this.componentManager = componentManager;
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
        if (documentName == null) {
            throw new MacroExecutionException(
                "You must specify a 'document' parameter pointing to the document to include.");
        }
        
        Context actualContext = parameters.getContext();

        // Retrieve the included document's content
        String includedContent = null;
        String includedSyntax = null;
        try {
            if (this.documentAccessBridge.isDocumentViewable(documentName)) {
                includedContent = this.documentAccessBridge.getDocumentContent(documentName);
                includedSyntax = this.documentAccessBridge.getDocumentSyntaxId(documentName);
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
            result =
                executeWithNewContext(documentName, includedContent, includedSyntax, context.getMacroTransformation());
        } else {
            result = executeWithCurrentContext(documentName, includedContent, includedSyntax);
        }

        return result;
    }

    /**
     * Parse and execute target document content in a new context.
     * 
     * @param includedDocumentName the name of the document to include.
     * @param includedContent the content of the document to include.
     * @param includedSyntax the syntax identifier of the provided content.
     * @param macroTransformation the macro transformation.
     * @return the result of parsing and transformation of the document to include.
     * @throws MacroExecutionException error when parsing content.
     */
    private List<Block> executeWithNewContext(String includedDocumentName, String includedContent,
        String includedSyntax, MacroTransformation macroTransformation) throws MacroExecutionException
    {
        List<Block> result;

        try {
            // Push new Execution Context to isolate the contexts (Velocity, Groovy, etc).
            ExecutionContext clonedEc = this.executionContextManager.clone(this.execution.getContext());
            
            this.execution.pushContext(clonedEc);

            // TODO: Need to set the current document, space and wiki. This is required for wiki syntax acting on
            // documents. For example if a link says "WebHome" it should point to the webhome of the current space.
            Map<String, Object> backupObjects = new HashMap<String, Object>();
            try {
                this.documentAccessBridge.pushDocumentInContext(backupObjects, includedDocumentName);
                result = generateIncludedPageDOM(includedDocumentName, includedContent, includedSyntax, 
                    macroTransformation);
            } finally {
                this.documentAccessBridge.popDocumentFromContext(backupObjects);
            }

        } catch (Exception e) {
            throw new MacroExecutionException("Failed to render page [" + includedDocumentName 
                + "] in new context", e);
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
     * @param includedSyntax the syntax identifier of the provided content.
     * @return the result of parsing and transformation of the document to include.
     * @throws MacroExecutionException error when parsing content.
     */
    private List<Block> executeWithCurrentContext(String includedDocumentName, String includedContent,
        String includedSyntax) throws MacroExecutionException
    {
        return generateIncludedPageDOM(includedDocumentName, includedContent, includedSyntax, null);
    }

    /**
     * Parse and execute target document content.
     * 
     * @param includedDocumentName the name of the document to include.
     * @param includedContent the content of the document to include.
     * @param includedSyntax the syntax identifier of the provided content.
     * @param macroTransformation the macro transformation.
     * @return the result of parsing and transformation of the document to include.
     * @throws MacroExecutionException error when parsing content.
     */
    private List<Block> generateIncludedPageDOM(String includedDocumentName, String includedContent,
        String includedSyntax, MacroTransformation macroTransformation) throws MacroExecutionException
    {
        XDOM includedDom;
        try {
            Parser parser = (Parser) this.componentManager.lookup(Parser.ROLE, includedSyntax);
            includedDom = parser.parse(new StringReader(includedContent));

            // Only run Macro transformation when the context is a new one as otherwise we need the macros in the
            // included page to be added to the list of macros on the including page so that they're all sorted
            // and executed in the right order. Note that this works only because the Include macro has the highest
            // execution priority and is thus executed first.
            if (macroTransformation != null) {
                macroTransformation.transform(includedDom, parser.getSyntax());
            }
        } catch (Exception e) {
            throw new MacroExecutionException("Failed to parse included page [" + includedDocumentName + "]", e);
        }

        return includedDom.getChildren();
    }
}
