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
package org.xwiki.rendering.macro.php;

import org.xwiki.rendering.macro.script.ScriptMockSetup;
import org.xwiki.rendering.scaffolding.RenderingTestSuite;
import org.xwiki.test.ComponentManagerTestSetup;

import junit.framework.Test;
import junit.framework.TestCase;

/**
 * Rendering tests for the php macro.
 * 
 * @version $Id$
 * @since 2.1M1
 */
public class RenderingTests extends TestCase
{
    /**
     * Creates a rendering test suit for testing the PHP macro.
     * 
     * @return rendering test suit for testing the PHP macro.
     * @throws Exception if an error occurs while setting up the test suite.
     */
    public static Test suite() throws Exception
    {
        RenderingTestSuite suite = new RenderingTestSuite("Test PHP Macro");
        ComponentManagerTestSetup testSetup = new ComponentManagerTestSetup(suite);
        new ScriptMockSetup(testSetup.getComponentManager());

        return testSetup;
    }
}
