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
package org.xwiki.test.ui;

import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Runs all functional tests found in the classpath.
 * 
 * @version $Id$
 * @since 12.8RC1
 */
@RunWith(PageObjectSuite.class)
public class AllTests
{
    // TODO: migrate
    // private static final Logger LOGGER = LoggerFactory.getLogger(AllTests.class);
    //
    // @PageObjectSuite.PostStart
    // void postStart(PersistentTestContext context) throws Exception
    // {
    //     context.testUtils.setDefaultCredentials(TestUtils.ADMIN_CREDENTIALS);
    //
    //     // Use the text editor by default to speed up and simplify the tests. We test the WYSIWYG editor separately.
    //     LOGGER.info("Use the text editor for the tests");
    //     context.testUtils.setWikiPreference("editor", "Text");
    //
    //     // We need to disable syntax highlighting so that tests using the wiki editor can set and get content.
    //     LOGGER.info("Disable Syntax Highlighting for the tests");
    //     context.testUtils.disableSyntaxHighlighting();
    // }
}
