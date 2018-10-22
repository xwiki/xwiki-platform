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
package org.xwiki.test.docker.junit5;

import org.openqa.selenium.remote.DesiredCapabilities;

/**
 * The browser to use for the UI tests.
 *
 * @version $Id$
 * @since 10.6RC1
 */
public enum Browser
{
    /**
     * The browser is selected based on a system property value.
     * @see TestConfiguration
     */
    SYSTEM,

    /**
     * the Firefox Browser.
     */
    FIREFOX(DesiredCapabilities.firefox()),

    /**
     * the Chrome Browser.
     */
    CHROME(DesiredCapabilities.chrome());

    private DesiredCapabilities capabilities;

    Browser()
    {
        // Capability will be resolved at runtime
    }

    Browser(DesiredCapabilities capabilities)
    {
        this.capabilities = capabilities;
    }

    DesiredCapabilities getCapabilities()
    {
        return this.capabilities;
    }
}
