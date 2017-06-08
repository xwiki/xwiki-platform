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

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.junit.Assume;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.xwiki.test.escaping.framework.AbstractEscapingTest;
import org.xwiki.test.escaping.framework.AbstractVelocityEscapingTest;
import org.xwiki.test.escaping.framework.EscapingError;
import org.xwiki.test.escaping.framework.SingleXWikiExecutor;
import org.xwiki.test.escaping.framework.XMLEscapingValidator;
import org.xwiki.test.escaping.suite.ArchiveSuite;
import org.xwiki.test.escaping.suite.ArchiveSuite.AfterSuite;
import org.xwiki.test.escaping.suite.ArchiveSuite.ArchivePathGetter;
import org.xwiki.test.escaping.suite.ArchiveSuite.BeforeSuite;
import org.xwiki.validator.ValidationError;

/**
 * Runs automatically generated escaping tests for all velocity templates found in XWiki Enterprise WAR file.
 * <p>
 * The following configuration properties are supported (set in maven):
 * <ul>
 * <li>localRepository: Path to maven repository, where XWiki files can be found</li>
 * <li>pathToXWikiWar: Used to read all templates</li>
 * </ul></p>
 * 
 * @version $Id$
 * @since 2.5M1
 */
@RunWith(ArchiveSuite.class)
public class TemplateTest extends AbstractVelocityEscapingTest
{
    /**
     * Get the path to the archive from system properties defined in the maven build configuration.
     * 
     * @return local path to the WAR archive to use
     */
    @ArchivePathGetter
    public static String getArchivePath()
    {
        return System.getProperty("localRepository") + "/" + System.getProperty("pathToXWikiWar");
    }

    /**
     * Start XWiki server if needed and switch to multi-language mode.
     * 
     * @throws Exception on errors
     */
    @BeforeSuite
    public static void init() throws Exception
    {
        SingleXWikiExecutor.getExecutor().start();

        // for tests using "language" parameter
        AbstractEscapingTest.setMultiLanguageMode(true);
    }

    /**
     * Switch back to single language mode and stop XWiki server if no longer needed.
     * 
     * @throws Exception on errors
     */
    @AfterSuite
    public static void shutdown() throws Exception
    {
        // restore single language mode
        AbstractEscapingTest.setMultiLanguageMode(false);

        SingleXWikiExecutor.getExecutor().stop();
    }

    /**
     * Create new TemplateTest for all *.vm files.
     */
    public TemplateTest()
    {
        super(Pattern.compile(".*\\.vm"));
    }

    /**
     * Test escaping of the space name.
     */
    @Test
    public void testSpaceEscaping()
    {
        // space name
        String url = createUrl(XMLEscapingValidator.getTestString(), null, null, "");
        checkUnderEscaping(url, "space name");
    }

    /**
     * Test escaping of the page name.
     */
    @Test
    public void testPageEscaping()
    {
        // page name
        String url = createUrl("Main", XMLEscapingValidator.getTestString(), null, "");
        checkUnderEscaping(url, "page name");
    }

    /**
     * Test escaping of all found parameters.
     */
    @Test
    public void testParameterEscaping()
    {
        // skip the test if there are no parameters to test
        Assume.assumeTrue(!this.userInput.isEmpty());

        List<EscapingError> errors = new ArrayList<EscapingError>();
        for (String parameter : this.userInput) {
            String url = createUrl("Main", null, parameter, XMLEscapingValidator.getTestString());
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
     * Create a target URL from given parameters, adding the template name.
     * 
     * @param space space name to use, "Main" is used if null
     * @param page page name to use, "WebHome" is used if null
     * @param parameter parameter name to add, omitted if null or empty string
     * @param value parameter value, empty string is used if null
     * @return the resulting absolute URL
     * @see #createUrl(String, String, String, java.util.Map)
     */
    protected String createUrl(String space, String page, String parameter, String value)
    {
        String skin = "default";
        if (this.name.startsWith("skins")) {
            skin = this.name.replaceFirst("^\\w+/", "").replaceAll("/.+$", "");
        }

        final String TEMPLATES_DIR = "templates" + File.separator;
        final String SKINS_DIR = "skins" + File.separator;
        String template;
        if (this.name.startsWith(TEMPLATES_DIR)) {
            template = StringUtils.removeStart(this.name, TEMPLATES_DIR);
        } else if (this.name.startsWith(SKINS_DIR)) {
            template = StringUtils.removeStart(this.name, SKINS_DIR + skin + File.separator);
        } else {
            template = this.name.replaceAll("^(.+)/", "");
        }

        HashMap<String, String> parameters = new HashMap<String, String>();
        parameters.put("skin", skin);
        if ("xpart".equals(parameter)) {
            // xpart=something must be tested differently
            parameters.put("xpage", template).replaceAll("\\.\\w+$", "");
        } else {
            // this variant initializes some commonly used variables
            parameters.put("xpage", "xpart");
            parameters.put("vm", template);
        }
        if (parameter != null) {
            parameters.put(parameter, value);
        }
        return createUrl(null, space, page, parameters);
    }
}

