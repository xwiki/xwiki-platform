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
package org.xwiki.ckeditor.test.ui;

import org.apache.commons.lang3.reflect.FieldUtils;
import org.junit.BeforeClass;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.xwiki.ckeditor.test.ui.internal.XWikiWebDriver;
import org.xwiki.test.ui.AbstractTest;

/**
 * Base class for CKEditor functional tests.
 * 
 * @version $Id$
 * @since 1.13
 */
public class AbstractCKEditorTest extends AbstractTest
{
    /**
     * Replace the default {@link org.xwiki.test.ui.XWikiWebDriver} with something that works with the latest version of
     * Selenium.
     * 
     * @throws Exception if something goes wrong
     */
    @BeforeClass
    public static void replaceXWikiWebDriver() throws Exception
    {
        if (context != null) {
            RemoteWebDriver wrappedDriver =
                (RemoteWebDriver) FieldUtils.readField(context.getDriver(), "wrappedDriver", true);
            FieldUtils.writeField(context, "driver", new XWikiWebDriver(wrappedDriver), true);
        }
    }
}
