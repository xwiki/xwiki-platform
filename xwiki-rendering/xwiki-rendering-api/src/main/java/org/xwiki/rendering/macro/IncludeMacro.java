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
package org.xwiki.rendering.macro;

import org.xwiki.rendering.block.Block;
import org.xwiki.rendering.block.XDOM;
import org.xwiki.rendering.parser.Parser;
import org.xwiki.rendering.parser.Syntax;
import org.xwiki.rendering.parser.SyntaxType;
import org.xwiki.rendering.transformation.Transformation;
import org.xwiki.rendering.DocumentManager;
import org.xwiki.context.ExecutionContextInitializerManager;
import org.xwiki.context.ExecutionContext;
import org.xwiki.context.ExecutionContextInitializerException;
import org.xwiki.context.Execution;

import java.util.List;
import java.util.Map;
import java.io.StringReader;

/**
 * @version $Id$
 * @since 1.5M2
 */
public class IncludeMacro extends AbstractMacro
{
    private static final String CONTEXT_NEW = "new";

    private static final String CONTEXT_CURRENT = "current";

    private static final Syntax SYNTAX = new Syntax(SyntaxType.XWIKI, "2.0");
    
    /**
     * Injected by the Component Manager.
     */
    private Parser parser;

    /**
     * Injected by the Component Manager.
     */
    private Transformation macroTransformation;

    /**
     * Injected by the Component Manager.
     */
    private Execution execution;

    /**
     * Injected by the Component Manager.
     */
    private ExecutionContextInitializerManager executionContextInitializerManager;

    private DocumentManager documentManager;

    /**
     * Allows overriding the Document Manager used (useful for unit tests).
     *
     * @param documentManager the new Document Manager to use
     */
    public void setDocumentManager(DocumentManager documentManager)
    {
        this.documentManager = documentManager;        
    }

    /**
     * {@inheritDoc}
     * @see Macro#execute(Map, String, org.xwiki.rendering.block.XDOM)
     */
    public List<Block> execute(Map<String, String> parameters, String content,
        XDOM dom) throws MacroExecutionException
    {
        List<Block> result;

        String documentName = parameters.get("document");

        // A document parameter must always be specified.
        if (documentName == null) {
            throw new MacroExecutionException("A \"document\" parameter pointing to a Document "
                + "must be specified. For example: {include:document=Space.Page/}");
        }

        // If no context parameter is passed then assume it's CONTEXT_CURRENT.
        String actualContext;
        if (!parameters.containsKey("context")) {
            actualContext = CONTEXT_NEW;
        } else {
            actualContext = parameters.get("context");
        }

        // If the context value is invalid return an error
        if (!actualContext.equalsIgnoreCase(CONTEXT_CURRENT) && !actualContext.equalsIgnoreCase(CONTEXT_NEW)) {
            throw new MacroExecutionException("Invalid value [" + actualContext + "] for "
                + "parameter \"context\". valid values are \"" + CONTEXT_CURRENT + "\" or \""
                + CONTEXT_NEW + "\".");
        }

        // Retrieve the included document's content
        String includedContent = null;
        try {
            includedContent = this.documentManager.getDocumentContent(documentName);
        } catch (Exception e) {
            throw new MacroExecutionException("Failed to get content for Document ["
                + documentName + "]", e);
        }

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
        if (actualContext.equalsIgnoreCase(CONTEXT_NEW)) {
            result = executeWithNewContext(documentName, includedContent);
        } else {
            result = executeWithCurrentContext(documentName, includedContent);
        }

        return result;
    }

    private List<Block> executeWithNewContext(String includedDocumentName, String includedContent)
        throws MacroExecutionException
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
            result = generateIncludedPageDOM(includedDocumentName, includedContent, true);
        } catch (ExecutionContextInitializerException e) {
            throw new MacroExecutionException("Failed to create new Execution Context for included page ["
                + includedDocumentName + "]", e);
        } finally {
            // Reset the Execution Context as before
            this.execution.popContext();
        }

        return result;
    }

    private List<Block> executeWithCurrentContext(String includedDocumentName, String includedContent)
        throws MacroExecutionException
    {
        return generateIncludedPageDOM(includedDocumentName, includedContent, false);
    }

    private List<Block> generateIncludedPageDOM(String includedDocumentName, String includedContent,
        boolean runMacroTransformation) throws MacroExecutionException
    {
        XDOM includedDom;
        try {
            includedDom = this.parser.parse(new StringReader(includedContent));

            // Only run Macro transformation when the context is a new one as otherwise we need the macros in the
            // included page to be added to the list of macros on the including page so that they're all sorted
            // and executed in the right order. Note that this works only because the Include macro has the highest
            // execution priority and is thus executed first.
            if (runMacroTransformation) {
                this.macroTransformation.transform(includedDom, SYNTAX);
            }
        } catch (Exception e) {
            throw new MacroExecutionException("Failed to parse included page ["
                + includedDocumentName + "]", e);
        }

        return includedDom.getChildren();
    }

}
