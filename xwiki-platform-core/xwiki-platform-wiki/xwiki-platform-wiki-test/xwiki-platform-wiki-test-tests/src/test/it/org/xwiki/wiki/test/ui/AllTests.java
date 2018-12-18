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
package org.xwiki.wiki.test.ui;

import java.util.List;

import org.apache.commons.configuration2.PropertiesConfiguration;
import org.junit.runner.RunWith;
import org.xwiki.test.integration.XWikiExecutor;
import org.xwiki.test.integration.XWikiExecutorSuite;
import org.xwiki.test.ui.PageObjectSuite;

/**
 * Runs all functional tests found in the classpath.
 *
 * @version $Id$
 * @since 5.4RC1
 */
@RunWith(PageObjectSuite.class)
public class AllTests
{
    @XWikiExecutorSuite.PreStart
    public void preStart(List<XWikiExecutor> executors) throws Exception
    {
        XWikiExecutor executor = executors.get(0);

        PropertiesConfiguration properties = executor.loadXWikiPropertiesConfiguration();

        // Put local Maven as extensions repository to speed up resolution
        properties.setProperty("extension.repositories",
            "localmaven:maven:file://" + System.getProperty("user.home") + "/.m2/repository");
        // Local Maven repository does not maintain any checksum and we don't want false positive warning in the install
        // log
        properties.setProperty("extension.repositories.localmaven.checksumPolicy", "ignore");

        executor.saveXWikiProperties();
    }
}
