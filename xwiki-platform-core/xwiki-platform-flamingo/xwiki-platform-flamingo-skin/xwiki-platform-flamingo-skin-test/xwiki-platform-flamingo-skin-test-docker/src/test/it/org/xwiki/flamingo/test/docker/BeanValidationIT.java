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
package org.xwiki.flamingo.test.docker;

import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import org.xwiki.test.docker.junit5.TestReference;
import org.xwiki.test.docker.junit5.UITest;
import org.xwiki.test.ui.TestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Make sure Bean Validation on macros parameters works in all supported application servers.
 *
 * @version $Id$
 */
@UITest
class BeanValidationIT
{
    @BeforeAll
    public void setup(TestUtils setup)
    {
        setup.loginAsSuperAdmin();
    }

    @Test
    void checkBeanValidationInMacros(TestUtils setup, TestReference reference) throws Exception
    {
        setup.rest().savePage(reference, "{{test param=\"true\"/}} {{test param=\"false\"/}}", "");

        setup.gotoPage(reference, "get", "outputSyntax=plain");

        String content = setup.getDriver().findElement(By.tagName("body")).getText();

        assertEquals("testmacroOK", StringUtils.substringBefore(content, ' '));
        assertTrue(StringUtils.substringAfter(content, ' ').contains("Failed to validate bean: [must be true]"));
    }
}
