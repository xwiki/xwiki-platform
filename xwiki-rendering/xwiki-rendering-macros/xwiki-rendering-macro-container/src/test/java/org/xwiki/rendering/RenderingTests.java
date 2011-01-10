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

import java.util.Map;

import junit.framework.Test;
import junit.framework.TestCase;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.integration.junit4.JUnit4Mockery;
import org.xwiki.component.descriptor.DefaultComponentDescriptor;
import org.xwiki.rendering.scaffolding.RenderingTestSuite;
import org.xwiki.skinx.SkinExtension;
import org.xwiki.test.ComponentManagerTestSetup;

/**
 * All Rendering integration tests defined in text files using a special format.
 * 
 * @version $Id$
 * @since 2.5M2
 */
public class RenderingTests extends TestCase
{
    /**
     * The mockery to create the skinx mocks.
     */
    private static Mockery mockery = new JUnit4Mockery();

    /**
     * Builds the test suite.
     * 
     * @return the test suite for the container tests.
     * @throws Exception if an exception occurs while preparing the tests from the test files.
     */
    public static Test suite() throws Exception
    {
        RenderingTestSuite suite = new RenderingTestSuite("Test Container Macro");

        ComponentManagerTestSetup testSetup = new ComponentManagerTestSetup(suite);
        setUpSkinExtensionStubs(testSetup);

        return testSetup;
    }

    /**
     * Sets up the stubs for the skin extensions, allowing use methods to be called on them.
     * 
     * @param testSetup the test setup to register the skinx stubs in
     * @throws Exception in case anything goes wrong
     */
    private static void setUpSkinExtensionStubs(ComponentManagerTestSetup testSetup) throws Exception
    {
        final SkinExtension ssfxMock = mockery.mock(SkinExtension.class, "ssfxMock");

        mockery.checking(new Expectations()
        {
            {
                String cssPath = "uicomponents/container/columns.css";
                allowing(ssfxMock).use(with(cssPath));
                allowing(ssfxMock).use(with(cssPath), with(any(Map.class)));
            }
        });

        // and inject these in the test setup
        DefaultComponentDescriptor<SkinExtension> ssfxDesc = new DefaultComponentDescriptor<SkinExtension>();
        ssfxDesc.setRole(SkinExtension.class);
        ssfxDesc.setRoleHint("ssfx");

        testSetup.getComponentManager().registerComponent(ssfxDesc, ssfxMock);
    }
}
