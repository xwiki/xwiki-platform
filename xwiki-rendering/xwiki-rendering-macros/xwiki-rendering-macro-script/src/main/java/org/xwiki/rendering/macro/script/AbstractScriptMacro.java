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
package org.xwiki.rendering.macro.script;

import java.io.StringReader;
import java.util.Collections;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.xwiki.bridge.DocumentAccessBridge;
import org.xwiki.classloader.ExtendedURLClassLoader;
import org.xwiki.component.annotation.Requirement;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.context.Execution;
import org.xwiki.rendering.block.Block;
import org.xwiki.rendering.block.MacroBlock;
import org.xwiki.rendering.block.XDOM;
import org.xwiki.rendering.internal.macro.script.AttachmentClassLoaderFactory;
import org.xwiki.rendering.internal.macro.script.ScriptMacroValidator;
import org.xwiki.rendering.macro.AbstractMacro;
import org.xwiki.rendering.macro.MacroExecutionException;
import org.xwiki.rendering.macro.descriptor.ContentDescriptor;
import org.xwiki.rendering.parser.ParseException;
import org.xwiki.rendering.parser.Parser;
import org.xwiki.rendering.transformation.MacroTransformationContext;
import org.xwiki.rendering.util.ParserUtils;

/**
 * Base Class for script evaluation macros.
 * 
 * @param <P> the type of macro parameters bean.
 * @version $Id$
 * @since 1.7M3
 */
// TODO: This class is on the edge of a fanout violation and needs to be refactored and split up.
public abstract class AbstractScriptMacro<P extends ScriptMacroParameters> extends AbstractMacro<P>
    implements ScriptMacro
{
    /**
     * The default description of the script macro content.
     */
    protected static final String CONTENT_DESCRIPTION = "the script to execute";

    /**
     * Key under which the class loader used by script executions is saved in the Execution Context, see
     * {@link #execution}.
     */
    private static final String EXECUTION_CONTEXT_CLASSLOADER_KEY = "scriptClassLoader";

    /**
     * Key under which the jar params used for the last macro execution are cached in the Execution Context.
     */
    private static final String EXECUTION_CONTEXT_JARPARAMS_KEY = "scriptJarParams";

    /**
     * Used to find if the current document's author has programming rights.
     */
    @Requirement
    protected DocumentAccessBridge documentAccessBridge;

    /**
     * Used to set the classLoader to be used by scripts across invocations. We save it in the Execution Context to be
     * sure it's the same classLoader used.
     */
    @Requirement
    protected Execution execution;

    /**
     * Used to get the current syntax parser.
     */
    @Requirement
    private ComponentManager componentManager;

    /**
     * Used to parse the result of the script execution into a XDOM object when the macro is configured by the user to
     * not interpret wiki syntax.
     */
    @Requirement("plain/1.0")
    private Parser plainTextParser;

    /**
     * Used to create a custom class loader that knows how to support JARs attached to wiki page.
     */
    @Requirement
    private AttachmentClassLoaderFactory attachmentClassLoaderFactory;

    /**
     * Used to clean result of the parser syntax.
     */
    private ParserUtils parserUtils = new ParserUtils();

    /**
     * Validators to make sure script macro is valid.
     */
    @Requirement(role = ScriptMacroValidator.class)
    private List<ScriptMacroValidator> validators;

    /**
     * @param macroName the name of the macro (eg "groovy")
     */
    public AbstractScriptMacro(String macroName)
    {
        super(macroName, null, ScriptMacroParameters.class);

        setDefaultCategory(DEFAULT_CATEGORY_DEVELOPMENT);
    }

    /**
     * @param macroName the name of the macro (eg "groovy")
     * @param macroDescription the text description of the macro.
     */
    public AbstractScriptMacro(String macroName, String macroDescription)
    {
        super(macroName, macroDescription, ScriptMacroParameters.class);

        setDefaultCategory(DEFAULT_CATEGORY_DEVELOPMENT);
    }

    /**
     * @param macroName the name of the macro (eg "groovy")
     * @param macroDescription the text description of the macro.
     * @param contentDescriptor the description of the macro content.
     */
    public AbstractScriptMacro(String macroName, String macroDescription, ContentDescriptor contentDescriptor)
    {
        super(macroName, macroDescription, contentDescriptor, ScriptMacroParameters.class);

        setDefaultCategory(DEFAULT_CATEGORY_DEVELOPMENT);
    }

    /**
     * @param macroName the name of the macro (eg "groovy")
     * @param macroDescription the text description of the macro.
     * @param parametersBeanClass class of the parameters bean for this macro.
     */
    public AbstractScriptMacro(String macroName, String macroDescription,
        Class< ? extends ScriptMacroParameters> parametersBeanClass)
    {
        super(macroName, macroDescription, parametersBeanClass);

        setDefaultCategory(DEFAULT_CATEGORY_DEVELOPMENT);
    }

    /**
     * @param macroName the name of the macro (eg "groovy")
     * @param macroDescription the text description of the macro.
     * @param contentDescriptor the description of the macro content.
     * @param parametersBeanClass class of the parameters bean for this macro.
     */
    public AbstractScriptMacro(String macroName, String macroDescription, ContentDescriptor contentDescriptor,
        Class< ? extends ScriptMacroParameters> parametersBeanClass)
    {
        super(macroName, macroDescription, contentDescriptor, parametersBeanClass);

        setDefaultCategory(DEFAULT_CATEGORY_DEVELOPMENT);
    }

    /**
     * @return the component manager
     * @since 2.0M1
     */
    protected ComponentManager getComponentManager()
    {
        return this.componentManager;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.macro.Macro#execute(Object, String, MacroTransformationContext)
     */
    public List<Block> execute(P parameters, String content, MacroTransformationContext context)
        throws MacroExecutionException
    {
        List<Block> result = Collections.emptyList();

        if (!StringUtils.isEmpty(content)) {

            // Set the context class loader to the script CL to ensure that any script engine using the context
            // classloader will work just fine.
            // Note: We must absolutely ensure that we always use the same context CL during the whole execution
            // request since JSR223 script engines (for example) that create internal class loaders need to
            // continue using these class loaders (where classes defined in scripts have been loaded for example).
            ClassLoader originalClassLoader = Thread.currentThread().getContextClassLoader();
            ClassLoader newClassLoader = getClassLoader(parameters.getJars(), originalClassLoader);
            try {
                Thread.currentThread().setContextClassLoader(newClassLoader);

                // 1) Abort execution if the script is nested
                for (ScriptMacroValidator validator : this.validators) {
                    validator.validate(parameters, content, context);
                }

                // 2) Run script engine on macro block content
                String scriptResult = evaluate(parameters, content, context);

                if (parameters.isOutput()) {
                    // 3) Run the wiki syntax parser on the script-rendered content
                    result = parseScriptResult(scriptResult, parameters, context);
                }
            } finally {
                // Restore original class loader
                Thread.currentThread().setContextClassLoader(originalClassLoader);
            }
        }

        return result;
    }

    /**
     * @param jarsParameterValue the value of the macro parameters used to pass extra URLs that should be in the
     *            execution class loader
     * @param parent the parent classloader for the classloader to create (if it doesn't already exist)
     * @return the class loader to use for executing the script
     * @throws MacroExecutionException in case of an error in building the class loader
     */
    protected ClassLoader getClassLoader(String jarsParameterValue, ClassLoader parent) throws MacroExecutionException
    {
        try {
            return findClassLoader(jarsParameterValue, parent);
        } catch (MacroExecutionException mee) {
            throw mee;
        } catch (Exception e) {
            throw new MacroExecutionException("Failed to add JAR URLs to the current class loader for ["
                + jarsParameterValue + "]", e);
        }
    }

    /**
     * @param jarsParameterValue the value of the macro parameters used to pass extra URLs that should be in the
     *            execution class loader
     * @param parent the parent classloader for the classloader to create (if it doesn't already exist)
     * @return the class loader to use for executing the script
     * @throws Exception in case of an error in building the class loader
     */
    private ClassLoader findClassLoader(String jarsParameterValue, ClassLoader parent) throws Exception
    {
        // We cache the Class Loader for improved performances and we check if the saved class loader had the same
        // jar parameters value as the current execution. If not, we compute a new class loader.
        ExtendedURLClassLoader cl =
                (ExtendedURLClassLoader) this.execution.getContext().getProperty(EXECUTION_CONTEXT_CLASSLOADER_KEY);

        if (cl == null) {
            if (!StringUtils.isEmpty(jarsParameterValue)) {
                cl = createOrExtendClassLoader(true, jarsParameterValue, parent);
            } else {
                cl = this.attachmentClassLoaderFactory.createAttachmentClassLoader("", parent);
            }
        } else {
            String cachedJarsParameterValue =
                    (String) this.execution.getContext().getProperty(EXECUTION_CONTEXT_JARPARAMS_KEY);
            if (cachedJarsParameterValue != jarsParameterValue) {
                cl = createOrExtendClassLoader(false, jarsParameterValue, cl);
            }
        }
        this.execution.getContext().setProperty(EXECUTION_CONTEXT_CLASSLOADER_KEY, cl);

        return cl;
    }

    /**
     * @param createNewClassLoader if true create a new classloader and if false extend an existing one with the passed
     *            additional jars
     * @param jarsParameterValue the value of the macro parameters used to pass extra URLs that should be in the
     *            execution class loader
     * @param classLoader the parent classloader for the classloader to create or the classloader to extend, depending
     *            on the value of the createNewClassLoader parameter
     * @return the new classloader or the extended one
     * @throws Exception in case of an error in building or extending the class loader
     */
    private ExtendedURLClassLoader createOrExtendClassLoader(boolean createNewClassLoader, String jarsParameterValue,
        ClassLoader classLoader) throws Exception
    {
        ExtendedURLClassLoader cl;
        if (canHaveJarsParameters()) {
            if (createNewClassLoader) {
                cl = this.attachmentClassLoaderFactory.createAttachmentClassLoader(jarsParameterValue, classLoader);
            } else {
                cl = (ExtendedURLClassLoader) classLoader;
                this.attachmentClassLoaderFactory.extendAttachmentClassLoader(jarsParameterValue, cl);
            }
            this.execution.getContext().setProperty(EXECUTION_CONTEXT_JARPARAMS_KEY, jarsParameterValue);
        } else {
            throw new MacroExecutionException(
                "You cannot pass additional jars since you don't have programming rights");
        }
        return cl;
    }

    /**
     * Convert script result as a {@link Block} list.
     * 
     * @param content the script result to parse.
     * @param parameters the macro parameters.
     * @param context the context of the macro transformation.
     * @return the {@link Block}s.
     * @throws MacroExecutionException Failed to find source parser.
     * @since 2.1M1
     */
    protected List<Block> parseScriptResult(String content, P parameters, MacroTransformationContext context)
        throws MacroExecutionException
    {
        List<Block> result;

        if (parameters.isWiki()) {
            XDOM parsedDom = parseSourceSyntax(content, context);

            // 3) If in inline mode remove any top level paragraph
            result = parsedDom.getChildren();
        } else {
            try {
                result = this.plainTextParser.parse(new StringReader(content)).getChildren();
            } catch (ParseException e) {
                // This shouldn't happen since the parser cannot throw an exception since the source is a memory
                // String.
                throw new MacroExecutionException("Failed to parse link label as plain text", e);
            }
        }

        if (context.isInline()) {
            // TODO: use inline parser instead
            this.parserUtils.removeTopLevelParagraph(result);

            // Make sure included macro is incline when script macro itself is inline
            // TODO: use inline parser instead
            if (!result.isEmpty() && result.get(0) instanceof MacroBlock && !((MacroBlock) result.get(0)).isInline()) {
                MacroBlock macro = (MacroBlock) result.get(0);
                result.set(0, new MacroBlock(macro.getId(), macro.getParameters(), macro.getContent(), true));
            }
        }

        return result;
    }

    /**
     * Execute provided script.
     * 
     * @param parameters the macro parameters.
     * @param content the script to execute.
     * @param context the context of the macro transformation.
     * @return the result of script execution.
     * @throws MacroExecutionException failed to evaluate provided content.
     */
    protected abstract String evaluate(P parameters, String content, MacroTransformationContext context)
        throws MacroExecutionException;

    /**
     * Get the parser of the current wiki syntax.
     * 
     * @param context the context of the macro transformation.
     * @return the parser of the current wiki syntax.
     * @throws MacroExecutionException Failed to find source parser.
     */
    protected Parser getSyntaxParser(MacroTransformationContext context) throws MacroExecutionException
    {
        try {
            return getComponentManager().lookup(Parser.class, context.getSyntax().toIdString());
        } catch (ComponentLookupException e) {
            throw new MacroExecutionException("Failed to find source parser", e);
        }
    }

    /**
     * Parse provided content with the parser of the current wiki syntax.
     * 
     * @param content the content to parse.
     * @param context the context of the macro transformation.
     * @return an XDOM containing the parser content.
     * @throws MacroExecutionException failed to parse content
     */
    protected XDOM parseSourceSyntax(String content, MacroTransformationContext context) throws MacroExecutionException
    {
        Parser parser = getSyntaxParser(context);

        try {
            return parser.parse(new StringReader(content));
        } catch (ParseException e) {
            throw new MacroExecutionException("Failed to parse content [" + content + "] with Syntax parser ["
                + parser.getSyntax() + "]", e);
        }
    }

    /**
     * Note that this method allows extending classes to override it to allow jars parameters to be used without
     * programming rights for example or to use some other conditions.
     * 
     * @return true if the user can use the macro parameter used to pass additional JARs to the classloader used to
     *         evaluate a script
     */
    protected boolean canHaveJarsParameters()
    {
        return this.documentAccessBridge.hasProgrammingRights();
    }
}
