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

import org.junit.Before;
import org.junit.Rule;
import org.xwiki.appwithinminutes.test.po.ApplicationClassEditPage;
import org.xwiki.test.ui.AbstractTest;
import org.xwiki.test.ui.AdminAuthenticationRule;

/**
 * Base class for testing the application class editor.
 * 
 * @version $Id$
 * @since 3.5
 */
public abstract class AbstractClassEditorTest extends AbstractTest
{
    @Rule
    public AdminAuthenticationRule adminAuthenticationRule = new AdminAuthenticationRule(true, getUtil());

    /**
     * The page being tested.
     */
    protected ApplicationClassEditPage editor;

    @Before
    public void setUp() throws Exception
    {
        getUtil().deleteSpace(getTestClassName());

        goToEditor();
    }

    /**
     * @since 11.9RC1
     */
    protected void goToEditor()
    {
        getUtil().gotoPage(getTestClassName(), getTestMethodName(), "edit",
            "editor=inline&template=AppWithinMinutes.ClassTemplate&title=" + getTestMethodName() + " Class");
        editor = new ApplicationClassEditPage();
    }
}
