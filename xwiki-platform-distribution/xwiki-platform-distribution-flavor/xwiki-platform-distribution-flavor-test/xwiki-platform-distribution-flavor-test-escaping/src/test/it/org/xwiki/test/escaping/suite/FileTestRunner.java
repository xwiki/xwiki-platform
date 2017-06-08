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

package org.xwiki.test.escaping.suite;

import org.junit.runners.BlockJUnit4ClassRunner;
import org.junit.runners.model.FrameworkMethod;
import org.junit.runners.model.InitializationError;


/**
 * A custom runner that runs all tests methods found in the given {@link FileTest}. The most important
 * difference to the default JUnit4 test runner is that the tests are created and initialized by the parent
 * test suite.
 * 
 * @version $Id$
 * @since 2.5M1
 */
public class FileTestRunner extends BlockJUnit4ClassRunner
{
    /** The test to run. */
    private final FileTest test;

    /**
     * Create new FileTestRunner for the given file test.
     * 
     * @param fileTest the test to run
     * @throws InitializationError on errors
     */
    public FileTestRunner(FileTest fileTest) throws InitializationError
    {
        super(fileTest.getClass());
        this.test = fileTest;
    }

    @Override
    protected Object createTest() throws Exception
    {
        return this.test;
    }

    @Override
    protected String getName()
    {
        return this.test.toString();
    }

    @Override
    protected String testName(FrameworkMethod method)
    {
        return String.format("%-60s  %s", this.test.toString(), method.getName());
    }
}

