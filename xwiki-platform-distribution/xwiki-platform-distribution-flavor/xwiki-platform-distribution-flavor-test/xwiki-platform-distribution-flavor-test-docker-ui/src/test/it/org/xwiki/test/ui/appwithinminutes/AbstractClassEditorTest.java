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
package org.xwiki.test.ui.appwithinminutes;

import org.junit.jupiter.api.BeforeEach;
import org.xwiki.appwithinminutes.test.po.ApplicationClassEditPage;
import org.xwiki.test.docker.junit5.TestReference;
import org.xwiki.test.ui.TestUtils;

/**
 * Base class for testing the application class editor.
 *
 * @version $Id$
 * @since 12.8RC1
 */
public abstract class AbstractClassEditorTest
{
    // @Rule
    // public AdminAuthenticationRule adminAuthenticationRule = new AdminAuthenticationRule(true, testUtils);

    /**
     * The page being tested.
     */
    protected ApplicationClassEditPage editor;

    @BeforeEach
    void setUp(TestUtils testUtils, TestReference testReference)
    {
        testUtils.deleteSpace(testReference.getLastSpaceReference().getName());

        goToEditor(testUtils, testReference);
    }

    protected void goToEditor(TestUtils testUtils, TestReference testReference)
    {
        testUtils.gotoPage(testReference, "edit",
            "editor=inline&template=AppWithinMinutes.ClassTemplate&title=" + testReference.getName() + " Class");
        this.editor = new ApplicationClassEditPage();
    }
}
