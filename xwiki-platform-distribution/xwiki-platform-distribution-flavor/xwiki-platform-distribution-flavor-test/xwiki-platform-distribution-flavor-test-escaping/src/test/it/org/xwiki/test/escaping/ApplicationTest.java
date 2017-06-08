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
package org.xwiki.test.escaping;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.regex.Pattern;

import org.junit.Assume;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.xwiki.test.escaping.framework.AbstractEscapingTest;
import org.xwiki.test.escaping.framework.AbstractVelocityEscapingTest;
import org.xwiki.test.escaping.framework.EscapingError;
import org.xwiki.test.escaping.framework.SingleXWikiExecutor;
import org.xwiki.test.escaping.framework.XMLEscapingValidator;
import org.xwiki.test.escaping.suite.ArchiveSuite;
import org.xwiki.validator.ValidationError;


/**
 * Runs automatically generated escaping tests for all XWiki documents found in XWiki Enterprise XAR file.
 * <p>
 * The following configuration properties are supported (set in maven):
 * <ul>
 * <li>localRepository: Path to maven repository, where XWiki files can be found</li>
 * <li>pathToXWikiXar: Used to read all documents</li>
 * </ul></p>
 * 
 * @version $Id$
 * @since 2.5M1
 */
@RunWith(ArchiveSuite.class)
public class ApplicationTest extends AbstractVelocityEscapingTest
{
    /**
     * Get the path to the archive from system properties defined in the maven build configuration.
     * 
     * @return local path to the XAR archive to use
     */
    @ArchiveSuite.ArchivePathGetter
    public static String getArchivePath()
    {
        return System.getProperty("pathToDocuments");
    }

    /**
     * Initialize the test.
     * 
     * @throws Exception on errors
     */
    @ArchiveSuite.BeforeSuite
    public static void init() throws Exception
    {
        SingleXWikiExecutor.getExecutor().start();

        // for tests using "language" parameter
        AbstractEscapingTest.setMultiLanguageMode(true);
    }

    /**
     * Clean up.
     * 
     * @throws Exception on errors
     */
    @ArchiveSuite.AfterSuite
    public static void shutdown() throws Exception
    {
        // restore single language mode
        AbstractEscapingTest.setMultiLanguageMode(false);

        SingleXWikiExecutor.getExecutor().stop();
    }

    /**
     * Create new ApplicationTest for all *.xml files in subdirectories.
     */
    public ApplicationTest()
    {
        super(Pattern.compile(".+/.+\\.xml"));
    }

    /**
     * Test escaping of all found parameters for flamingo.
     */
    @Test
    public void testParametersInFlamingo()
    {
        // NOTE: results in other skins seem to be the same
        testParameterEscaping("flamingo");
    }

    /**
     * Run the tests for all parameters for given skin.
     * 
     * @param skin skin to use
     */
    private void testParameterEscaping(String skin)
    {
        // skip the test if there are no parameters to test
        Assume.assumeTrue(!this.userInput.isEmpty());

        String space = this.name.replaceAll("/.+$", "");
        String page = this.name.replaceAll("^.+/", "").replaceAll("\\..+$", "");

        // all found parameters
        // TODO need to also consider parameters from page header templates bound to variables
        List<EscapingError> errors = new ArrayList<EscapingError>();
        for (String parameter : this.userInput) {
            String url = createUrl(space, page, parameter, XMLEscapingValidator.getTestString(), skin);
            List<ValidationError> val_errors = getUnderEscapingErrors(url);
            if (!val_errors.isEmpty()) {
                errors.add(new EscapingError("* Parameter: \"" + parameter + "\"", this.name, url, val_errors));
            }
        }
        if (!errors.isEmpty()) {
            throw new EscapingError("Escaping test failed.", errors);
        }
    }

    /**
     * Convenience method to create the target URL with one parameter.
     * 
     * @param space space name to use
     * @param page page name to use
     * @param parameter parameter name to add, omitted if null or empty string
     * @param value parameter value, empty string is used if null
     * @param skin skin name to use
     * @return the resulting absolute URL
     * @see #createUrl(String, String, String, java.util.Map)
     */
    protected String createUrl(String space, String page, String parameter, String value, String skin)
    {
        HashMap<String, String> parameters = new HashMap<String, String>();
        if (skin != null) {
            parameters.put("skin", skin);
        }
        if (parameter != null) {
            parameters.put(parameter, value);
        }
        return createUrl(null, space, page, parameters);
    }
}

