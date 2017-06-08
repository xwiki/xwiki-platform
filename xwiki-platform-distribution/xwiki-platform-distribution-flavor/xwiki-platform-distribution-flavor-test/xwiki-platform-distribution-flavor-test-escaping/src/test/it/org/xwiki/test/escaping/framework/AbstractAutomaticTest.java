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

import java.util.regex.Pattern;

/**
 * Abstract base class for all automatic tests. Adds support for "patternExcludeFiles" properties.
 * 
 * @version $Id$
 * @since 2.5M1
 */
public abstract class AbstractAutomaticTest extends AbstractEscapingTest
{
    /**
     * Create new {@link AbstractAutomaticTest}.
     * 
     * @param fileNameMatcher regex pattern used to filter files by name
     */
    public AbstractAutomaticTest(Pattern fileNameMatcher)
    {
        super(fileNameMatcher);
    }

    /**
     * {@inheritDoc}
     * <p>
     * The default implementation for automatic tests checks the "patternExcludeFiles" property (set in
     * maven build configuration).
     * 
     * @param fileName file name to check
     * @return true if the file should be excluded, false otherwise
     * @see AbstractEscapingTest#isExcludedFile(java.lang.String)
     */
    @Override
    protected boolean isExcludedFile(String fileName)
    {
        for (String pattern : System.getProperty("patternExcludeFiles", "").split("\\s+")) {
            Pattern exclude = Pattern.compile(pattern);
            if (exclude.matcher(fileName).matches()) {
                return true;
            }
        }
        return false;
    }
}
