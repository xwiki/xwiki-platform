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
package org.xwiki.rendering;

import java.util.ArrayList;
import java.util.List;

import junit.framework.Test;
import junit.framework.TestCase;

import org.xwiki.component.descriptor.ComponentDescriptor;
import org.xwiki.rendering.internal.MockDocumentAccessBridge;
import org.xwiki.rendering.internal.MockDocumentNameSerializer;
import org.xwiki.rendering.scaffolding.RenderingTestSuite;
import org.xwiki.test.ComponentManagerTestSetup;

/**
 * Rendering tests for the chart macro.
 *
 * @version $Id$
 * @since 2.0M1
 */
public class RenderingTests extends TestCase
{
    /**
     * Builds and returns a new {@link ComponentManagerTestSetup}.
     * 
     * @return a {@link ComponentManagerTestSetup}.
     * @throws Exception if an error occurs while building the test setup.
     */
    public static Test suite() throws Exception
    {
        RenderingTestSuite suite = new RenderingTestSuite("Test Chart Macro");

        suite.addTestsFromResource("macrochart1", true);        
        suite.addTestsFromResource("macrochart2", true);
        suite.addTestsFromResource("macrochart3", true);
        suite.addTestsFromResource("macrochart4", true);

        List<ComponentDescriptor< ? >> mocks = new ArrayList<ComponentDescriptor<?>>();
        mocks.add(MockDocumentAccessBridge.getComponentDescriptor());
        mocks.add(MockDocumentNameSerializer.getComponentDescriptor());
        
        return new ComponentManagerTestSetup(suite, mocks);
    }
}