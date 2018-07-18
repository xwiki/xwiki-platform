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
package org.xwiki.test.escaping.framework;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import org.xwiki.validator.ValidationError;
import org.xwiki.validator.ValidationError.Type;
import org.xwiki.validator.Validator;

/**
 * A validator that checks for proper XML escaping. The document must be constructed using the special test input string
 * (see {@link #getTestString()}).
 * 
 * @version $Id$
 * @since 2.5M1
 */
public class XMLEscapingValidator implements Validator
{
    /** Unescaped test string containing XML significant characters. */
    private static final String INPUT_STRING = "aaa\"bbb'ccc>ddd<eee";

    /** Test for unescaped apostrophe. */
    private static final String TEST_APOS = "bbb'ccc";

    /** Test for unescaped quote. */
    private static final String TEST_QUOT = "aaa\"bbb";

    private static final String TEST_QUOT_ESCAPED = "aaa&quot;bbb";

    /** Test for unescaped tag start. */
    private static final String TEST_LT = "ddd<eee";

    /** Test for unescaped tag end. */
    private static final String TEST_GT = "ccc>ddd";

    /** JavaScript-escaped TEST_APOS. */
    private static final String TEST_JS_APOS = "bbb\\'ccc";

    /** JavaScript-escaped TEST_QUOT. */
    private static final String TEST_JS_QUOT = "aaa\\\"bbb";

    /** Source of the XML document to validate. */
    private List<String> document = new ArrayList<String>();

    /** List of validation errors. */
    private List<ValidationError> errors = new ArrayList<ValidationError>();

    /**
     * Get the input string containing XML significant characters that should be used.
     * 
     * @return test string to use
     */
    public static String getTestString()
    {
        return INPUT_STRING;
    }

    /**
     * {@inheritDoc}
     * <p>
     * Clears previous list of validation errors.
     * </p>
     * 
     * @see org.xwiki.validator.Validator#setDocument(java.io.InputStream)
     */
    @Override
    public void setDocument(InputStream document)
    {
        BufferedReader reader = new BufferedReader(new InputStreamReader(document));
        String line;
        this.document = new ArrayList<String>();
        try {
            while ((line = reader.readLine()) != null) {
                this.document.add(line);
            }
        } catch (IOException exception) {
            throw new RuntimeException("Could not read document: ", exception);
        }
        clear();
    }

    /**
     * {@inheritDoc}
     * <p>
     * Throws {@link EscapingError} on errors.
     * </p>
     * 
     * @see org.xwiki.validator.Validator#validate()
     */
    @Override
    public List<ValidationError> validate()
    {
        clear();

        int lineNr = 1;
        for (String line : this.document) {
            checkStringDelimiters(line, lineNr);
            checkTagDelimiter(line, lineNr, TEST_LT, "Unescaped < character");
            checkTagDelimiter(line, lineNr, TEST_GT, "Unescaped > character");

            int idx;
            if ((idx = line.indexOf("Error while parsing velocity page")) >= 0) {
                this.errors.add(new ValidationError(Type.WARNING, lineNr, idx,
                    "Parse error in the response. The template was not evaluated correctly."));
            }
            if ((idx = line.indexOf("org.xwiki.rendering.macro.MacroExecutionException")) >= 0) {
                this.errors.add(new ValidationError(Type.WARNING, lineNr, idx,
                    "Macro execution exception in the response."));
            }
            if ((idx = line.indexOf("Wrapped Exception: unexpected char:")) >= 0) {
                this.errors.add(new ValidationError(Type.WARNING, lineNr, idx, "Possible SQL error trace."));
            }
            // TODO also check \ for JavaScript
            // TODO check for overescaping
            lineNr++;
        }
        return this.errors;
    }

    /**
     * Check whether < and > are properly escaped. Attempts to avoid false positives caused by JavaScript escaping.
     * Found problems are added to the internal list of escaping errors.
     * 
     * @param line the line to check
     * @param lineNr line number reported on failures
     * @param testMatch the test string to search for, e.g. TEST_LT
     * @param errorMessage error message to use on failures
     */
    private void checkTagDelimiter(String line, int lineNr, String testMatch, String errorMessage)
    {
        // NOTE this method produces false NEGATIVES if JavaScript escaping is used where XML/URL escaping is needed
        int idx = 0;
        while ((idx = line.indexOf(testMatch, idx)) >= 0) {
            // avoid false positives caused by JavaScript escaping
            if (!isJavascriptEscaped(line, testMatch, idx)) {
                this.errors.add(new ValidationError(Type.ERROR, lineNr, idx, errorMessage));
            }
            idx++;
        }
    }

    /**
     * Check whether double quotes and singe quotes are properly escaped. The rule we apply is:
     * <ul>
     *   <li>Double quotes must always be escaped inside an attribute that has double quotes around the value</li>
     *   <li>Single quotes must always be escaped inside an attribute that has single quotes around the value</li>
     *   <li>Single and double quotes don't need to be escaped into element text</li>
     * </ul>
     * 
     * @param line the line to check
     * @param lineNr line number reported on failures
     */
    protected void checkStringDelimiters(String line, int lineNr)
    {
        // Check for unescaped single quotes that must be escaped
        int idx = -1;
        while ((idx = line.indexOf(TEST_APOS, idx + 1)) >= 0) {
            char c = isInAttribute(line, idx);
            // If we're in an attribute and the attribute delimiter is a single quote then the single quote should
            // have been escaped
            if (c == '\'') {
                this.errors.add(new ValidationError(Type.WARNING, lineNr, idx, "Unescaped ' character"));
            }
        }

        // Check for unescaped double quotes that must be escaped
        idx = -1;
        while ((idx = line.indexOf(TEST_QUOT, idx + 1)) >= 0) {
            char c = isInAttribute(line, idx);
            // If we're in an attribute and the attribute delimiter is a double quote then the double quote should
            // have been escaped
            if (c == '\"') {
                this.errors.add(new ValidationError(Type.WARNING, lineNr, idx, "Unescaped \" character"));
            }
        }
    }

    private char isInAttribute(String line, int startPosition)
    {
        int position = startPosition;
        for (; position > -1; position--) {
            char c = line.charAt(position);
            if (c == '<' || c == '>') {
                return 0;
            }
            if (c == '\"' || c == '\'') {
                // TODO: Improve to handle spaces/tabs between "=" and quotes
                if (position > 0 && line.charAt(position - 1) == '=') {
                    return c;
                }
            }
        }
        return 0;
    }

    /**
     * Check if the matched test string appears to be JavaScript-escaped. Checks whether both ' and " appearing in the
     * test string right before index are JavaScript-escaped. Used to avoid false positives in {@link #validate()}.
     * 
     * @param line the line where the string was matched
     * @param match substring of the test string that was matched, e.g. TEST_APOS
     * @param index position where the match was found in line, as reported by {@link String#indexOf(String, int)}
     * @return true if the found input string is JavaScript-escaped, false otherwise
     */
    private boolean isJavascriptEscaped(String line, String match, int index)
    {
        int offset = INPUT_STRING.indexOf(match);
        if (index < 0 || offset < 0) {
            return false;
        }
        // JavaScript-escaping adds 2 characters
        offset += 2;
        int pos_apos = line.indexOf(TEST_JS_APOS, index - offset);
        int pos_quot = line.indexOf(TEST_JS_QUOT, index - offset);
        return (pos_apos >= 0 && pos_apos < index && pos_quot >= 0 && pos_quot < index);
    }

    @Override
    public List<ValidationError> getErrors()
    {
        return this.errors;
    }

    @Override
    public void clear()
    {
        this.errors = new ArrayList<ValidationError>();
    }

    @Override
    public String getName()
    {
        return "XML ESCAPING";
    }
}
