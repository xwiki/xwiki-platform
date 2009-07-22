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

package org.xwiki.rendering.macro.wikibridge;

import java.io.StringReader;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.xwiki.bridge.DocumentAccessBridge;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.context.Execution;
import org.xwiki.rendering.block.Block;
import org.xwiki.rendering.block.XDOM;
import org.xwiki.rendering.macro.Macro;
import org.xwiki.rendering.macro.MacroExecutionException;
import org.xwiki.rendering.macro.descriptor.MacroDescriptor;
import org.xwiki.rendering.macro.descriptor.ParameterDescriptor;
import org.xwiki.rendering.macro.parameter.MacroParameterException;
import org.xwiki.rendering.parser.ParseException;
import org.xwiki.rendering.parser.Parser;
import org.xwiki.rendering.parser.SyntaxFactory;
import org.xwiki.rendering.transformation.MacroTransformationContext;
import org.xwiki.rendering.transformation.Transformation;
import org.xwiki.rendering.util.ParserUtils;

/**
 * A generic macro that parses content in a given syntax. The actual content to be parsed is injected and the content
 * received when executing the macro is ignored This macro is meant to be registered dynamically against the component
 * manager, to allow its content definition outside java code itself (retrieved from the Wiki for example).
 * 
 * @version $Id$
 * @since 2.0M1
 */
public class WikiMacro implements Macro<WikiMacroParameters>
{
    /**
     * The key under which macro context will be available in the XwikiContext for scripts.
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
    private String macroDocument;

    /**
     * Name of this macro.
     */
    private String macroName;

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
     * Constructs a new {@link WikiMacro}.
     * 
     * @param macroDocument document which contains the definition of this macro.
     * @param macroName name of the macro.
     * @param descriptor the {@link MacroDescriptor} describing this macro.
     * @param macroContent macro content to be evaluated.
     * @param syntaxId syntax of the macroContent.
     * @param componentManager {@link ComponentManager} component used to look up for other components.
     */
    public WikiMacro(String macroDocument, String macroName, MacroDescriptor descriptor, String macroContent,
        String syntaxId, ComponentManager componentManager)
    {
        this.macroName = macroName;
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
        Map<String, ParameterDescriptor> parameterDescriptors = getDescriptor().getParameterDescriptorMap();
        for (String parameterName : parameterDescriptors.keySet()) {
            ParameterDescriptor parameterDescriptor = parameterDescriptors.get(parameterName);
            if (parameterDescriptor.isMandatory() && (null == parameters.get(parameterName))) {
                throw new MacroParameterException(String.format("Parameter [%s] is mandatory", parameterName));
            }
        }
        
        // Verify macro content against the content descriptor.
        if(getDescriptor().getContentDescriptor().isMandatory()) {
            if (StringUtils.isEmpty(macroContent)) {
                throw new MacroExecutionException("Missing macro content: this macro requires content (a body)");
            }
        }        

        // Parse the wiki macro content.
        XDOM xdom = null;
        try {
            Parser parser = componentManager.lookup(Parser.class, syntaxId);
            xdom = parser.parse(new StringReader(this.content));
        } catch (ComponentLookupException ex) {
            throw new MacroExecutionException("Could not find a parser for macro content", ex);
        } catch (ParseException ex) {
            throw new MacroExecutionException("Error while parsing macro content", ex);
        }

        // Prepare macro execution environment.
        Map<String, Object> macroContext = new HashMap<String, Object>();
        macroContext.put(MACRO_PARAMS_KEY, parameters);
        macroContext.put(MACRO_CONTENT_KEY, macroContent);
        macroContext.put(MACRO_CONTEXT_KEY, context);
        Map xwikiContext = null;
        Object contextDoc = null;
        try {
            Execution execution = componentManager.lookup(Execution.class);
            xwikiContext = (Map) execution.getContext().getProperty("xwikicontext");
            xwikiContext.put(MACRO_KEY, macroContext);

            // Save current context document.
            contextDoc = xwikiContext.get(CONTEXT_DOCUMENT_KEY);

            // Set the macro definition document as the context document, this is required to give the macro access to
            // it's parameters, otherwise macro will not be able to access it's parameters if the user executing the
            // macro does not have programming rights.
            DocumentAccessBridge docBridge = componentManager.lookup(DocumentAccessBridge.class);
            xwikiContext.put(CONTEXT_DOCUMENT_KEY, docBridge.getDocument(macroDocument));
        } catch (Exception ex) {
            throw new MacroExecutionException("Error while preparing macro execution environment", ex);
        }

        // Perform internal macro transformations.
        try {
            SyntaxFactory syntaxFactory = componentManager.lookup(SyntaxFactory.class);
            Transformation macroTransformation = componentManager.lookup(Transformation.class, MACRO_HINT);
            macroTransformation.transform(xdom, syntaxFactory.createSyntaxFromIdString(syntaxId));
        } catch (Exception ex) {
            throw new MacroExecutionException("Error while performing internal macro transformations", ex);
        }

        // Reset the context document.
        xwikiContext.put(CONTEXT_DOCUMENT_KEY, contextDoc);

        List<Block> result = xdom.getChildren();
        // If in inline mode remove any top level paragraph.
        if (context.isInline()) {
            this.parserUtils.removeTopLevelParagraph(result);
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
     * @return the name of this wiki macro.
     */
    public String getName()
    {
        return this.macroName;
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
        return true;
    }
}
