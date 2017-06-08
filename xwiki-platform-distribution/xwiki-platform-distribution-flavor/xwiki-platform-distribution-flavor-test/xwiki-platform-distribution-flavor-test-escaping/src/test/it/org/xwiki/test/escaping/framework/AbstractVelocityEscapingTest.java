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
import java.io.Reader;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


/**
 * Abstract base class for automatic escaping tests that need to parse velocity. Implements simple
 * regular expression base parsing.
 * 
 * @version $Id$
 * @since 2.5M1
 */
public abstract class AbstractVelocityEscapingTest extends AbstractAutomaticTest
{
    /**
     * Create new AbstractVelocityEscapingTest.
     * 
     * @param fileNameMatcher file name pattern matcher
     */
    protected AbstractVelocityEscapingTest(Pattern fileNameMatcher)
    {
        super(fileNameMatcher);
    }

    /**
     * {@inheritDoc}
     * <p>
     * This implementation does some approximate regex matching to find used parameters and other
     * common user-controlled things like user name.</p>
     */
    @Override
    protected Set<String> parse(Reader reader)
    {
        // parameters in this set are known to produce false positives only
        Set<String> ignored = new HashSet<String>();
        // xpage is handled by actions (in xwiki-core) to render a velocity template
        // invalid template names produce "Unexpected empty response" warnings
        ignored.add("xpage");
        // form token is never (should not be) rendered, but is checked by CSRF protection
        ignored.add("form_token");
        // TODO match if user name, space name or action is used
        Set<String> input = new HashSet<String>();
        BufferedReader data = new BufferedReader(reader);
        Pattern pattern = Pattern.compile("\\$!?\\{?request\\.get\\((?:\"|')(\\w+)(?:\"|')\\)|"
                                        + "\\$!?\\{?request\\.getParameter\\((?:\"|')(\\w+)(?:\"|')\\)|"
                                        + "\\$!?\\{?request\\.(\\w+)[^(a-zA-Z_0-9]|"
                                        + "\\b(editor)\\b|"
                                        + "\\b(viewer)\\b|"
                                        + "\\b(section)\\b|"
                                        + "\\$!?\\{?(template)\\b|"
                                        + "\\$!?\\{?(revparams)\\b|"
                                        + "\\b(xredirect)\\b|"
                                        + "\\b(x-maximized)\\b|"
                                        + "\\b(xnotification)\\b|"
                                        + "\\b(classname)\\b|"
                                        + "\\b(comment)\\b|"
                                        + "\\b(rev1)\\b|"
                                        + "\\b(rev2)\\b|"
                                        + "\\b(sourcedoc)\\b|"
                                        + "\\b(targetdoc)\\b|"
                                        + "\\b(srid)\\b|"
                                        + "\\b(language)\\b");
        try {
            String line;
            while ((line = data.readLine()) != null) {
                Matcher match = pattern.matcher(line);
                while (match.find()) {
                    for (int i = 1; i <= match.groupCount(); i++) {
                        String parameter = match.group(i);
                        if (parameter != null && !parameter.matches("\\s*") && !ignored.contains(parameter)) {
                            input.add(parameter);
                        }
                    }
                }
            }
        } catch (IOException exception) {
            // ignore, use what was already found
        }
        return input;
    }
}
