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
import java.io.Reader;
import java.text.MessageFormat;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import javax.inject.Inject;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.python.core.Py;
import org.python.core.PyObject;
import org.python.core.PyUnicode;
import org.python.util.PythonInterpreter;
import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.annotation.Requirement;
import org.xwiki.component.phase.Initializable;
import org.xwiki.component.phase.InitializationException;
import org.xwiki.rendering.block.Block;
import org.xwiki.rendering.block.NewLineBlock;
import org.xwiki.rendering.block.VerbatimBlock;
import org.xwiki.rendering.parser.AbstractHighlightParser;
import org.xwiki.rendering.parser.HighlightParser;
import org.xwiki.rendering.parser.ParseException;
import org.xwiki.rendering.syntax.Syntax;
import org.xwiki.rendering.syntax.SyntaxType;
import org.xwiki.rendering.parser.Parser;

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
public class PygmentsParser extends AbstractHighlightParser implements Initializable
{
    /**
     * The name of the lexer variable in Python code.
     */
    private static final String PY_LEXER_VARNAME = "lexer";

    /**
     * The name of the style variable in Python code.
     */
    private static final String PY_STYLE_VARNAME = "style";

    /**
     * The name of the formatter variable in Python code.
     */
    private static final String PY_FORMATTER_VARNAME = "formatter";

    /**
     * The name of the listener variable in Python code.
     */
    private static final String PY_LISTENER_VARNAME = "listener";

    /**
     * The name of the variable containing the source code to highlight in PPython code.
     */
    private static final String PY_CODE_VARNAME = "code";

    /**
     * Try part of the initialization.
     */
    private static final String PY_TRY = " = None\ntry:\n  ";

    /**
     * Try part of the lexer initialization.
     */
    private static final String PY_LEXER_TRY = PY_LEXER_VARNAME + PY_TRY + PY_LEXER_VARNAME;

    /**
     * Try part of the style initialization.
     */
    private static final String PY_STYLE_TRY = PY_STYLE_VARNAME + PY_TRY + PY_STYLE_VARNAME;

    /**
     * Catch part of the initialization.
     */
    private static final String PY_CATCH = "\nexcept ClassNotFound:\n  pass";

    /**
     * Python code to create the lexer.
     */
    private static final String PY_LEXER_CREATE = PY_LEXER_TRY + " = get_lexer_by_name(\"{0}\", stripnl=False)"
        + PY_CATCH;

    /**
     * Python code to create the style.
     */
    private static final String PY_STYLE_CREATE = PY_STYLE_TRY + " = get_style_by_name(\"{0}\")" + PY_CATCH;

    /**
     * Python code to find the lexer from source.
     */
    private static final String PY_LEXER_FIND = PY_LEXER_TRY + " = guess_lexer(code, stripnl=False)" + PY_CATCH;

    /**
     * The syntax identifier.
     */
    private Syntax syntax;

    /**
     * The Python interpreter used to execute Pygments.
     */
    private PythonInterpreter pythonInterpreter;

    /**
     * Used to parse Pygment token values into blocks.
     */
    @Requirement("plain/1.0")
    private Parser plainTextParser;

    /**
     * Pygments highligh parser configuration.
     */
    @Requirement
    private PygmentsParserConfiguration configuration;

    /**
     * The logger to log.
     */
    @Inject
    private Logger logger;

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.component.phase.Initializable#initialize()
     */
    public void initialize() throws InitializationException
    {
        String highlightSyntaxId = getSyntaxId() + "-highlight";
        this.syntax = new Syntax(new SyntaxType(highlightSyntaxId, highlightSyntaxId), "1.0");

        this.pythonInterpreter = new PythonInterpreter();

        // imports Pygments
        this.pythonInterpreter.exec("import pygments"
            + "\nfrom pygments.lexers import guess_lexer"
            + "\nfrom pygments.lexers import get_lexer_by_name"
            + "\nfrom pygments.styles import get_style_by_name"
            + "\nfrom pygments.util import ClassNotFound"
            + "\nfrom pygments.formatters.xdom import XDOMFormatter");
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.parser.Parser#getSyntax()
     */
    public Syntax getSyntax()
    {
        return this.syntax;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.parser.HighlightParser#highlight(java.lang.String, java.io.Reader)
     */
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

        List<Block> blocks = highlight(syntaxId, code);

        // TODO: there is a bug in Pygments that makes it always put a newline at the end of the content, should be
        // fixed in Pygments 1.3.
        if (code.charAt(code.length() - 1) != '\n' && !blocks.isEmpty()
            && blocks.get(blocks.size() - 1) instanceof NewLineBlock) {
            blocks.remove(blocks.size() - 1);
        }

        return blocks;
    }

    /**
     * Return a highlighted version of the provided content.
     * <p>
     * This method is synchronized because we reuse the same Jython interpreter (because recreating one eaach time would
     * be costly) and an interpreter is not thread safe.
     * 
     * @param syntaxId the identifier of the source syntax.
     * @param code the content to highlight.
     * @return the highlighted version of the provided source.
     * @throws ParseException the highlighting failed.
     */
    private synchronized List<Block> highlight(String syntaxId, String code) throws ParseException
    {
        PythonInterpreter interpreter = getPythonInterpreter();
        BlocksGeneratorPygmentsListener listener = new BlocksGeneratorPygmentsListener(this.plainTextParser);

        interpreter.set(PY_LISTENER_VARNAME, listener);
        interpreter.set(PY_CODE_VARNAME, new PyUnicode(code));

        // Resolve lexer
        PyObject lexer = getLexer(syntaxId);
        if (lexer == null || lexer == Py.None) {
            // No lexer found
            this.logger.debug("no lexer found");

            return Collections.<Block> singletonList(new VerbatimBlock(code, true));
        }

        // Resolve style
        PyObject style = getStyle();

        if (style == null || style == Py.None) {
            interpreter.exec(MessageFormat
                .format("{0} = XDOMFormatter({1})", PY_FORMATTER_VARNAME, PY_LISTENER_VARNAME));
        } else {
            interpreter.exec(MessageFormat.format("{0} = XDOMFormatter({1}, style={2})", PY_FORMATTER_VARNAME,
                PY_LISTENER_VARNAME, PY_STYLE_VARNAME));
        }

        interpreter.exec(MessageFormat.format("pygments.highlight({0}, {1}, {2})", PY_CODE_VARNAME, PY_LEXER_VARNAME,
            PY_FORMATTER_VARNAME));

        List<String> vars = Arrays.asList(PY_LISTENER_VARNAME, PY_CODE_VARNAME, PY_LEXER_VARNAME, PY_FORMATTER_VARNAME);
        for (String var : vars) {
            interpreter.exec("del " + var);
        }

        return listener.getBlocks();
    }

    /**
     * Resolve lexer from provided language identifier.
     * 
     * @param language the source language
     * @return the lexer, null or Py.None if none can be found
     */
    private PyObject getLexer(String language)
    {
        PythonInterpreter interpreter = getPythonInterpreter();

        if (!StringUtils.isEmpty(language)) {
            interpreter.exec(MessageFormat.format(PY_LEXER_CREATE, language));
        } else {
            interpreter.exec(PY_LEXER_FIND);
        }

        return interpreter.get(PY_LEXER_VARNAME);
    }

    /**
     * Resolve style to use to highlight the source.
     * 
     * @return the style object
     */
    private PyObject getStyle()
    {
        PythonInterpreter interpreter = getPythonInterpreter();

        String style = this.configuration.getStyle();

        if (style != null) {
            interpreter.exec(MessageFormat.format(PY_STYLE_CREATE, style));

            return interpreter.get(PY_STYLE_VARNAME);
        } else {
            return null;
        }
    }

    /**
     * @return the python interpreter.
     */
    protected PythonInterpreter getPythonInterpreter()
    {
        return this.pythonInterpreter;
    }
}
