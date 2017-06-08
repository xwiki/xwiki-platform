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

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.extensions.cpsuite.ClasspathSuite;
import org.junit.runner.RunWith;
import org.xwiki.test.escaping.framework.SingleXWikiExecutor;

/**
 * Parent test suite that runs all functional escaping tests. Starts XWiki server before other tests
 * and stops it afterwards.
 * 
 * TODO
 * - check that the fixed templates are fixed
 * - add over-escaping test
 * - test for escaping of action
 * - need to test comments*.vm on a page with comments
 * - create the space and page named {@link org.xwiki.test.escaping.framework.XMLEscapingValidator#getTestString()}
 *   before running space/page tests
 * - sometimes, templates need the document to be in syntax 1.0
 * 
 * @version $Id$
 * @since 2.5M1
 */
@RunWith(ClasspathSuite.class)
public class AllTests
{
    /**
     * Start XWiki server.
     * 
     * @throws Exception on errors
     */
    @BeforeClass
    public static void init() throws Exception
    {
        SingleXWikiExecutor.getExecutor().start();
    }

    /**
     * Stop XWiki server.
     * 
     * @throws Exception on errors
     */
    @AfterClass
    public static void shutdown() throws Exception
    {
        SingleXWikiExecutor.getExecutor().stop();
    }
}
