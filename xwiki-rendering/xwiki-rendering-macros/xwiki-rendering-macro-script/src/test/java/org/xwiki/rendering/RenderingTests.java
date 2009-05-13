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
import org.xwiki.rendering.internal.macro.MockScriptContextManager;
import org.xwiki.rendering.scaffolding.RenderingTestCase;
import org.xwiki.rendering.scaffolding.RenderingTestSuite;
import org.xwiki.test.PlexusTestSetup;

/**
 * All Rendering integration tests defined in text files using a special format.
 * 
 * @version $Id$
 * @since 1.7M3
 */
public class RenderingTests extends TestCase
{
    public static Test suite() throws Exception
    {
        RenderingTestSuite suite = new RenderingTestSuite("Test all Parsers/Renderers");

        suite.addTestsFromResource("macroscript1", true);
        suite.addTestsFromResource("macroscript2", true);
        suite.addTestsFromResource("macroscript3", true);

        List<ComponentDescriptor> mocks = 
            new ArrayList<ComponentDescriptor>(RenderingTestCase.DEFAULT_MOCK_DESCRIPTORS);
        mocks.add(MockScriptContextManager.getComponentDescriptor());
        
        return new PlexusTestSetup(suite, mocks);
    }
}
