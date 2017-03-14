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
package org.xwiki.ckeditor.test.ui.internal;

import java.util.concurrent.TimeUnit;

import org.openqa.selenium.WebDriver;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.openqa.selenium.support.ui.ExpectedCondition;
import org.openqa.selenium.support.ui.Wait;
import org.openqa.selenium.support.ui.WebDriverWait;

/**
 * Overrides the {@link org.xwiki.test.ui.XWikiWebDriver} in order to make it work with the latest version of Selenium.
 * We should remove this class when the platform is upgraded to the latest Selenium.
 * 
 * @version $Id$
 */
public class XWikiWebDriver extends org.xwiki.test.ui.XWikiWebDriver
{
    public XWikiWebDriver(RemoteWebDriver wrappedDriver)
    {
        super(wrappedDriver);
    }

    /**
     * {@inheritDoc}
     * <p>
     * NOTE: We didn't change the code. The only reason for the override is that we have to recompile the code that uses
     * {@link Wait} because it was broken by the move to Java 8's {@link java.util.function.Function} . Binary
     * compatibility has been broken by
     * https://github.com/SeleniumHQ/selenium/commit/b2aa9fd534f7afbcba319231bb4bce85f825ef09#diff-626e39ef10a3e4aa187888381b34dca1
     * (on the Selenium side) and the move to Guava 21 which has moved to Java 8 too with
     * https://github.com/google/guava/commit/73e382fa877f80994817a136b0adcc4365ccd904#diff-96d37305d2e1f6725dd42a99d2138c32
     * .
     * 
     * @see org.xwiki.test.ui.XWikiWebDriver#waitUntilCondition(org.openqa.selenium.support.ui.ExpectedCondition)
     */
    @Override
    public <T> void waitUntilCondition(ExpectedCondition<T> condition)
    {
        // Temporarily remove the implicit wait on the driver since we're doing our own waits...
        manage().timeouts().implicitlyWait(0, TimeUnit.SECONDS);
        Wait<WebDriver> wait = new WebDriverWait(this, getTimeout());
        try {
            wait.until(condition);
        } finally {
            // Reset timeout
            setDriverImplicitWait();
        }
    }
}
