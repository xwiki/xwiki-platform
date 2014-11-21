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
package org.xwiki.rendering.internal.parser.pygments;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.io.StringReader;
import java.util.Collections;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import javax.script.ScriptContext;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import javax.script.SimpleScriptContext;

import org.apache.commons.io.IOUtils;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.phase.Initializable;
import org.xwiki.component.phase.InitializationException;
import org.xwiki.rendering.block.Block;
import org.xwiki.rendering.block.NewLineBlock;
import org.xwiki.rendering.parser.AbstractHighlightParser;
import org.xwiki.rendering.parser.HighlightParser;
import org.xwiki.rendering.parser.ParseException;
import org.xwiki.rendering.parser.Parser;
import org.xwiki.rendering.syntax.Syntax;
import org.xwiki.rendering.syntax.SyntaxType;

/**
 * Highlight provided source using Pygments.
 * 
 * @version $Id$
 * @since 1.7RC1
 */
// Note that we force the Component annotation so that this component is only registered as a Highlight Parser
// and not a Parser too since we don't want this parser to be visible to users as a valid standard input parser
// component.
@Component(roles = {HighlightParser.class })
@Singleton
public class PygmentsParser extends AbstractHighlightParser implements Initializable
{
    /**
     * The name of the style variable in Python code.
     */
    private static final String PY_STYLE_VARNAME = "style";

    /**
     * The name of the listener variable in Python code.
     */
    private static final String PY_LISTENER_VARNAME = "listener";

    /**
     * The name of the variable containing the source code to highlight in Python code.
     */
    private static final String PY_CODE_VARNAME = "code";

    /**
     * The name of the variable containing the language of the source.
     */
    private static final String PY_LANGUAGE_VARNAME = "language";

    /**
     * The name of the lexer variable in Python code.
     */
    private static final String PY_LEXER_VARNAME = "pygmentLexer";

    /**
     * The identifier of the Java Scripting engine to use.
     */
    private static final String ENGINE_ID = "python";

    /**
     * The syntax identifier.
     */
    private Syntax syntax;

    /**
     * Used to parse Pygment token values into blocks.
     */
    @Inject
    @Named("plain/1.0")
    private Parser plainTextParser;

    /**
     * Pygments highligh parser configuration.
     */
    @Inject
    private PygmentsParserConfiguration configuration;

    /**
     * The JSR223 Script Engine we use to evaluate Python scripts.
     */
    private ScriptEngine engine;

    /**
     * The Python script used to manipulate Pygments.
     */
    private String script;

    @Override
    public void initialize() throws InitializationException
    {
        ScriptEngineManager scriptEngineManager = new ScriptEngineManager();

        // Get the script
        InputStream is = getClass().getResourceAsStream("/pygments/code.py");
        if (is != null) {
            try {
                this.script = IOUtils.toString(is, "UTF8");
            } catch (Exception e) {
                throw new InitializationException("Failed to read resource /pygments/code.py resource", e);
            } finally {
                IOUtils.closeQuietly(is);
            }
        } else {
            throw new InitializationException("Failed to find resource /pygments/code.py resource");
        }

        // Get the Python engine
        this.engine = scriptEngineManager.getEngineByName(ENGINE_ID);

        if (this.engine == null) {
            throw new InitializationException("Failed to find engine for Python script language");
        }

        String highlightSyntaxId = getSyntaxId() + "-highlight";
        this.syntax = new Syntax(new SyntaxType(highlightSyntaxId, highlightSyntaxId), "1.0");
    }

    @Override
    public Syntax getSyntax()
    {
        return this.syntax;
    }

    @Override
    public List<Block> highlight(String syntaxId, Reader source) throws ParseException
    {
        String code;
        try {
            code = IOUtils.toString(source);
        } catch (IOException e) {
            throw new ParseException("Failed to read source", e);
        }

        if (code.length() == 0) {
            return Collections.emptyList();
        }

        List<Block> blocks;
        try {
            blocks = highlight(syntaxId, code);
        } catch (ScriptException e) {
            throw new ParseException("Failed to highlight code", e);
        }

        // TODO: there is a bug in Pygments that makes it always put a newline at the end of the content
        if (code.charAt(code.length() - 1) != '\n' && !blocks.isEmpty()
            && blocks.get(blocks.size() - 1) instanceof NewLineBlock) {
            blocks.remove(blocks.size() - 1);
        }

        return blocks;
    }

    /**
     * Return a highlighted version of the provided content.
     * 
     * @param syntaxId the identifier of the source syntax.
     * @param code the content to highlight.
     * @return the highlighted version of the provided source.
     * @throws ScriptException when failed to execute the script
     * @throws ParseException when failed to parse the content as plain text
     */
    private List<Block> highlight(String syntaxId, String code) throws ScriptException, ParseException
    {
        BlocksGeneratorPygmentsListener listener = new BlocksGeneratorPygmentsListener(this.plainTextParser);

        ScriptContext scriptContext = new SimpleScriptContext();

        scriptContext.setAttribute(PY_LANGUAGE_VARNAME, syntaxId, ScriptContext.ENGINE_SCOPE);
        scriptContext.setAttribute(PY_CODE_VARNAME, code, ScriptContext.ENGINE_SCOPE);
        scriptContext.setAttribute(PY_STYLE_VARNAME, this.configuration.getStyle(), ScriptContext.ENGINE_SCOPE);
        scriptContext.setAttribute(PY_LISTENER_VARNAME, listener, ScriptContext.ENGINE_SCOPE);

        this.engine.eval(this.script, scriptContext);

        List<Block> blocks;
        if (scriptContext.getAttribute(PY_LEXER_VARNAME) != null) {
            blocks = listener.getBlocks();
        } else {
            blocks = this.plainTextParser.parse(new StringReader(code)).getChildren().get(0).getChildren();
        }

        return blocks;
    }
}
