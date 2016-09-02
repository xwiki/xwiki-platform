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
package org.xwiki.test.page;

import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.xwiki.localization.script.LocalizationScriptService;
import org.xwiki.script.service.ScriptService;
import org.xwiki.test.TestComponentManager;

import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Set up Localization Services for Page Tests.
 *
 * @version $Id$
 * @since 8.3M2
 */
public final class LocalizationSetup
{
    private LocalizationSetup()
    {
        // Utility class and thus no public constructor.
    }

    /**
     * Sets up localization so that all translation return their key as translation values.
     *
     * @param tcm the stubbed Component Manager for the test
     * @throws Exception when a setup error occurs
     */
    public static void setUp(TestComponentManager tcm) throws Exception
    {
        LocalizationScriptService lss = mock(LocalizationScriptService.class);
        tcm.registerComponent(ScriptService.class, "localization", lss);
        when(lss.render(anyString())).thenAnswer(
            new Answer() {
                @Override
                public Object answer(InvocationOnMock invocationOnMock) throws Throwable
                {
                    // Return the translation key as the value
                    return invocationOnMock.getArgumentAt(0, String.class);
                }
            }
        );
    }
}
