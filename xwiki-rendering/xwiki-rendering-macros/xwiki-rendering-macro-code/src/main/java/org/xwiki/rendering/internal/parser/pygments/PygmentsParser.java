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
import java.net.URL;
import java.net.URLDecoder;
import java.text.MessageFormat;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.python.core.Py;
import org.python.core.PyObject;
import org.python.core.PyUnicode;
import org.python.util.PythonInterpreter;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.phase.Initializable;
import org.xwiki.component.phase.InitializationException;
import org.xwiki.rendering.block.Block;
import org.xwiki.rendering.block.VerbatimBlock;
import org.xwiki.rendering.parser.AbstractHighlightParser;
import org.xwiki.rendering.parser.HighlightParser;
import org.xwiki.rendering.parser.ParseException;
import org.xwiki.rendering.parser.Syntax;
import org.xwiki.rendering.parser.SyntaxType;

/**
 * Highlight provided source using Pygments.
 * 
 * @version $Id$
 * @since 1.7RC1
 */
// Note that we force the Component annotation so that this component is only registered as a Highlight Parser
// and not a Parser too since we don't want this parser to be visible to users as a valid standard input parser
// component.
@Component(roles = { HighlightParser.class })
public class PygmentsParser extends AbstractHighlightParser implements Initializable
{
    /**
     * A Pygments .py file to search for the location of the jar.
     */
    private static final String LEXER_PY = "Lib/pygments/lexer.py";

    /**
     * A Pygments .py file to search for the location of the jar.
     */
    private static final String XDOMFORMATTER_PY = "Lib/pygments/formatters/xdom.py";

    /**
     * The name of the lexer variable in PPython code.
     */
    private static final String PY_LEXER_VARNAME = "lexer";

    /**
     * The name of the formatter variable in PPython code.
     */
    private static final String PY_FORMATTER_VARNAME = "formatter";

    /**
     * The name of the listener variable in PPython code.
     */
    private static final String PY_LISTENER_VARNAME = "listener";

    /**
     * The name of the variable containing the source code to highlight in PPython code.
     */
    private static final String PY_CODE_VARNAME = "code";

    /**
     * Python code to create the lexer.
     */
    private static final String PY_LEXER_CREATE =
        PY_LEXER_VARNAME + " = pygments.lexers.get_lexer_by_name(\"{0}\", stripall=True)";

    /**
     * Python code to find the lexer from source.
     */
    private static final String PY_LEXER_FIND =
        PY_LEXER_VARNAME + " = None\n" + "try:\n" + "  " + PY_LEXER_VARNAME + " = guess_lexer(code, stripall=True)\n"
            + "except ClassNotFound:\n" + "  pass";

    /**
     * Java jar URL special characters.
     */
    private static final String JAR_URL_PREFIX = "jar:file:";

    /**
     * Jar path separator.
     */
    private static final String JAR_SEPARATOR = "!";

    /**
     * The character use to separate URL parts.
     */
    private static final String URL_SEPARATOR = "/";

    /**
     * The syntax identifier.
     */
    private Syntax syntax;

    /**
     * The Python interpreter used to execute Pygments.
     */
    private PythonInterpreter pythonInterpreter;

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.component.phase.Initializable#initialize()
     */
    public void initialize() throws InitializationException
    {
        this.syntax = new Syntax(SyntaxType.getSyntaxType(getSyntaxId() + "-highlight"), "1.0");

        System.setProperty("python.home", findPygmentsPath());

        this.pythonInterpreter = new PythonInterpreter();

        // imports Pygments
        this.pythonInterpreter.exec("import pygments");

        this.pythonInterpreter.execfile(getClass().getClassLoader().getResourceAsStream(XDOMFORMATTER_PY));

        this.pythonInterpreter.exec("from pygments.lexers import guess_lexer");
        this.pythonInterpreter.exec("from pygments.util import ClassNotFound");
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
        PythonInterpreter interpreter = getPythonInterpreter();
        BlocksGeneratorPygmentsListener listener = new BlocksGeneratorPygmentsListener();

        String code;
        try {
            code = IOUtils.toString(source);
        } catch (IOException e) {
            throw new ParseException("Failed to read source", e);
        }

        interpreter.set(PY_LISTENER_VARNAME, listener);
        interpreter.set(PY_CODE_VARNAME, new PyUnicode(code));

        if (!StringUtils.isEmpty(syntaxId)) {
            interpreter.exec(MessageFormat.format(PY_LEXER_CREATE, syntaxId));
        } else {
            interpreter.exec(PY_LEXER_FIND);
        }

        PyObject lexer = interpreter.get(PY_LEXER_VARNAME);
        if (lexer == null || lexer == Py.None) {
            // No lexer found
            if (getLogger().isDebugEnabled()) {
                getLogger().debug("no lexer found");
            }

            return Collections.<Block> singletonList(new VerbatimBlock(code, true));
        }

        interpreter.exec(MessageFormat.format("{0} = XDOMFormatter({1})", PY_FORMATTER_VARNAME, PY_LISTENER_VARNAME));
        interpreter.exec(MessageFormat.format("pygments.highlight({0}, {1}, {2})", PY_CODE_VARNAME, PY_LEXER_VARNAME,
            PY_FORMATTER_VARNAME));

        List<String> vars = Arrays.asList(PY_LISTENER_VARNAME, PY_CODE_VARNAME, PY_LEXER_VARNAME, PY_FORMATTER_VARNAME);
        for (String var : vars) {
            interpreter.exec("del " + var);
        }

        return listener.getBlocks();
    }

    /**
     * @return the python interpreter.
     */
    protected PythonInterpreter getPythonInterpreter()
    {
        return this.pythonInterpreter;
    }

    /**
     * Get the full URL root path of provided Python file.
     * 
     * @param fileToFind the Python file to find in the classpath.
     * @return the root URL path.
     */
    private String findPath(String fileToFind)
    {
        URL url = getClass().getResource(URL_SEPARATOR + fileToFind);
        String urlString = URLDecoder.decode(url.toString());

        // we expect an URL like
        // jar:file:/jar_dir/jython-lib.jar!/Lib/pygments/lexer.py
        int jarSeparatorIndex = urlString.indexOf(JAR_SEPARATOR);
        if (urlString.startsWith(JAR_URL_PREFIX) && jarSeparatorIndex > 0) {
            urlString = urlString.substring(JAR_URL_PREFIX.length(), jarSeparatorIndex);
        } else {
            // Just in case we don't get a jar URL
            int begin = urlString.indexOf(URL_SEPARATOR);
            int lexerPyIndex = urlString.lastIndexOf(fileToFind);
            urlString = urlString.substring(begin, lexerPyIndex);
            if (urlString.endsWith(URL_SEPARATOR)) {
                urlString = urlString.substring(0, urlString.length() - 1);
            }
            if (urlString.endsWith(JAR_SEPARATOR)) {
                urlString = urlString.substring(0, urlString.length() - 1);
            }
        }

        return urlString;
    }

    /**
     * Determine and register the home of the Pygments Pyton files.
     * 
     * @return the root path of Pygments Pyton files.
     */
    private String findPygmentsPath()
    {
        return findPath(LEXER_PY);
    }
}
