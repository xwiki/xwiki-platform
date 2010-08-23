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

package org.xwiki.rendering.internal.macro.wikibridge;

import java.io.StringReader;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.xwiki.bridge.DocumentAccessBridge;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.context.Execution;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.rendering.block.Block;
import org.xwiki.rendering.block.MacroBlock;
import org.xwiki.rendering.block.XDOM;
import org.xwiki.rendering.macro.Macro;
import org.xwiki.rendering.macro.MacroExecutionException;
import org.xwiki.rendering.macro.descriptor.MacroDescriptor;
import org.xwiki.rendering.macro.descriptor.ParameterDescriptor;
import org.xwiki.rendering.macro.parameter.MacroParameterException;
import org.xwiki.rendering.macro.wikibridge.WikiMacro;
import org.xwiki.rendering.macro.wikibridge.WikiMacroParameters;
import org.xwiki.rendering.parser.ParseException;
import org.xwiki.rendering.parser.Parser;
import org.xwiki.rendering.syntax.SyntaxFactory;
import org.xwiki.rendering.transformation.MacroTransformationContext;
import org.xwiki.rendering.transformation.Transformation;
import org.xwiki.rendering.transformation.TransformationContext;
import org.xwiki.rendering.util.ParserUtils;

/**
 * Default implementation of {@link WikiMacro}.
 * 
 * @version $Id$
 * @since 2.0M1
 */
public class DefaultWikiMacro implements WikiMacro
{
    /**
     * The key under which macro context will be available in the XWikiContext for scripts.
     */
    private static final String MACRO_KEY = "macro";

    /**
     * Macro hint for {@link Transformation} component. Same as MACRO_KEY (Check style fix)
     */
    private static final String MACRO_HINT = MACRO_KEY;

    /**
     * The key under which macro body will be available inside macro context.
     */
    private static final String MACRO_CONTENT_KEY = "content";

    /**
     * The key under which macro parameters will be available inside macro context.
     */
    private static final String MACRO_PARAMS_KEY = "params";

    /**
     * The key under which macro transformation context will be available inside macro context.
     */
    private static final String MACRO_CONTEXT_KEY = "context";

    /**
     * The key under which macro can directly return the resulting {@link List} of {@link Block}.
     */
    private static final String MACRO_RESULT_KEY = "result";

    /**
     * They key used to access the current context document stored in XWikiContext.
     */
    private static final String CONTEXT_DOCUMENT_KEY = "doc";

    /**
     * The {@link MacroDescriptor} for this macro.
     */
    private MacroDescriptor descriptor;

    /**
     * Document which contains the definition of this macro.
     */
    private DocumentReference macroDocumentReference;

    /**
     * Whether this macro supports inline mode or not.
     */
    private boolean supportsInlineMode;

    /**
     * Macro content.
     */
    private String content;

    /**
     * Syntax id.
     */
    private String syntaxId;

    /**
     * The component manager used to lookup other components.
     */
    private ComponentManager componentManager;

    /**
     * Used to clean result of the parser syntax.
     */
    private ParserUtils parserUtils;

    /**
     * Constructs a new {@link DefaultWikiMacro}.
     * 
     * @param macroDocumentReference the name of the document which contains the definition of this macro
     * @param supportsInlineMode says if macro support inline mode or not
     * @param descriptor the {@link MacroDescriptor} describing this macro.
     * @param macroContent macro content to be evaluated.
     * @param syntaxId syntax of the macroContent.
     * @param componentManager {@link ComponentManager} component used to look up for other components.
     * @since 2.3M1
     */
    public DefaultWikiMacro(DocumentReference macroDocumentReference, boolean supportsInlineMode,
        MacroDescriptor descriptor, String macroContent, String syntaxId, ComponentManager componentManager)
    {
        this.macroDocumentReference = macroDocumentReference;
        this.supportsInlineMode = supportsInlineMode;
        this.descriptor = descriptor;
        this.content = macroContent;
        this.syntaxId = syntaxId;
        this.parserUtils = new ParserUtils();
        this.componentManager = componentManager;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.macro.Macro#execute(Object, String, MacroTransformationContext)
     */
    @SuppressWarnings("unchecked")
    public List<Block> execute(WikiMacroParameters parameters, String macroContent, MacroTransformationContext context)
        throws MacroExecutionException
    {
        // First verify that all mandatory parameters are provided.
        // Note that we currently verify automatically mandatory parameters in Macro Transformation but for the moment
        // this is only checked for Java-based macros. Hence why we need to check here too.
        Map<String, ParameterDescriptor> parameterDescriptors = getDescriptor().getParameterDescriptorMap();
        for (String parameterName : parameterDescriptors.keySet()) {
            ParameterDescriptor parameterDescriptor = parameterDescriptors.get(parameterName);
            Object parameterValue = parameters.get(parameterName);
            if (parameterDescriptor.isMandatory() && (null == parameterValue)) {
                throw new MacroParameterException(String.format("Parameter [%s] is mandatory", parameterName));
            }

            // Set default parameter value if applicable.
            Object parameterDefaultValue = parameterDescriptor.getDefaultValue();
            if (parameterValue == null && parameterDefaultValue != null) {
                parameters.set(parameterName, parameterDefaultValue);
            }
        }

        // Verify the a macro content is not empty if it was declared mandatory.
        if (getDescriptor().getContentDescriptor() != null && getDescriptor().getContentDescriptor().isMandatory()) {
            if (StringUtils.isEmpty(macroContent)) {
                throw new MacroExecutionException("Missing macro content: this macro requires content (a body)");
            }
        }

        // Parse the wiki macro content.
        XDOM xdom;
        try {
            Parser parser = componentManager.lookup(Parser.class, syntaxId);
            xdom = parser.parse(new StringReader(this.content));
        } catch (ComponentLookupException ex) {
            throw new MacroExecutionException("Could not find a parser for macro content", ex);
        } catch (ParseException ex) {
            throw new MacroExecutionException("Error while parsing macro content", ex);
        }

        // Macro code segment is always parsed into a separate xdom document. Now if this code segment starts with
        // another macro block, it will always be interpreted as a block macro regardless of the current wiki macro's
        // context (because as far as the nested macro is concerned, it starts on a new line). This will introduce
        // unnecessary paragraph elements when the wiki macro is used inline, so we need to force such opening macro
        // blocks to behave as inline macros if the wiki macro is used inline.
        if (context.isInline()) {
            List<Block> children = xdom.getChildren();
            if (children.size() > 0 && children.get(0) instanceof MacroBlock) {
                MacroBlock old = (MacroBlock) children.get(0);
                MacroBlock replacement = new MacroBlock(old.getId(), old.getParameters(), old.getContent(), true);
                xdom.replaceChild(replacement, old);
            }
        }

        // Prepare macro context.
        Map<String, Object> macroContext = new HashMap<String, Object>();
        macroContext.put(MACRO_PARAMS_KEY, parameters);
        macroContext.put(MACRO_CONTENT_KEY, macroContent);
        macroContext.put(MACRO_CONTEXT_KEY, context);
        macroContext.put(MACRO_RESULT_KEY, context);

        Map xwikiContext = null;
        Object contextDoc = null;
        try {
            Execution execution = componentManager.lookup(Execution.class);
            DocumentAccessBridge docBridge = componentManager.lookup(DocumentAccessBridge.class);
            SyntaxFactory syntaxFactory = componentManager.lookup(SyntaxFactory.class);
            Transformation macroTransformation = componentManager.lookup(Transformation.class, MACRO_HINT);

            // Place macro context inside xwiki context ($context.macro).
            xwikiContext = (Map) execution.getContext().getProperty("xwikicontext");
            xwikiContext.put(MACRO_KEY, macroContext);

            // Save current context document.
            contextDoc = xwikiContext.get(CONTEXT_DOCUMENT_KEY);

            // Set the macro definition document as the context document, this is required to give the macro access to
            // it's context ($context.macro) which holds macro parameters, macro content and other important structures.
            // This workaround ensures that macro code is evaluated with programming rights, which in turn ensures that
            // $context.macro is accessible within the macro code.
            xwikiContext.put(CONTEXT_DOCUMENT_KEY, docBridge.getDocument(getDocumentReference()));

            // Perform internal macro transformations.
            TransformationContext txContext = new TransformationContext(xdom,
                syntaxFactory.createSyntaxFromIdString(syntaxId));
            macroTransformation.transform(xdom, txContext);
        } catch (Exception ex) {
            throw new MacroExecutionException("Error while performing internal macro transformations", ex);
        } finally {
            if (null != xwikiContext) {
                // Remove macro context from xwiki context.
                xwikiContext.remove(MACRO_KEY);
                if (null != contextDoc) {
                    // Reset the context document.
                    xwikiContext.put(CONTEXT_DOCUMENT_KEY, contextDoc);
                }
            }
        }

        Object resultObject = macroContext.get(MACRO_RESULT_KEY);

        List<Block> result;
        if (resultObject != null && resultObject instanceof List) {
            result = (List<Block>) macroContext.get(MACRO_RESULT_KEY);
        } else {
            result = xdom.getChildren();
            // If in inline mode remove any top level paragraph.
            if (context.isInline()) {
                this.parserUtils.removeTopLevelParagraph(result);
            }
        }

        return result;
    }

    /**
     * {@inheritDoc}
     */
    public MacroDescriptor getDescriptor()
    {
        return this.descriptor;
    }

    /**
     * {@inheritDoc}
     */
    public int getPriority()
    {
        return 1000;
    }

    /**
     * {@inheritDoc}
     */
    public String getId()
    {
        return this.descriptor.getId().getId();
    }

    /**
     * {@inheritDoc}
     */
    public DocumentReference getDocumentReference()
    {
        return this.macroDocumentReference;
    }

    /**
     * {@inheritDoc}
     */
    public int compareTo(Macro< ? > macro)
    {
        if (getPriority() != macro.getPriority()) {
            return getPriority() - macro.getPriority();
        }
        return this.getClass().getSimpleName().compareTo(macro.getClass().getSimpleName());
    }

    /**
     * {@inheritDoc}
     */
    public boolean supportsInlineMode()
    {
        return this.supportsInlineMode;
    }
}
