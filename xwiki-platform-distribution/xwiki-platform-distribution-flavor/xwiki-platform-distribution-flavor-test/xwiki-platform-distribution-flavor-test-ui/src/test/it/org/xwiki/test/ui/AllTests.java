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
package org.xwiki.test.ui;

import java.util.List;

import org.apache.commons.configuration.PropertiesConfiguration;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xwiki.test.integration.XWikiExecutor;
import org.xwiki.test.integration.XWikiExecutorSuite;

/**
 * Runs all functional tests found in the classpath.
 * 
 * @version $Id$
 * @since 2.3M1
 */
@RunWith(PageObjectSuite.class)
public class AllTests
{
    private static final Logger LOGGER = LoggerFactory.getLogger(AllTests.class);

    @XWikiExecutorSuite.PreStart
    public void preStart(List<XWikiExecutor> executors) throws Exception
    {
        XWikiExecutor executor = executors.get(0);

        LOGGER.info("Put back the old WYSIWYG editor in xwiki.properties");

        PropertiesConfiguration properties = executor.loadXWikiPropertiesConfiguration();
        properties.setProperty("edit.defaultEditor.org.xwiki.rendering.syntax.SyntaxContent#wysiwyg", "gwt");
        executor.saveXWikiProperties(properties);
    }
}
