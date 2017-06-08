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
package org.xwiki.test.selenium;

import java.util.List;

import org.apache.commons.configuration.PropertiesConfiguration;
import org.junit.runner.RunWith;
import org.xwiki.test.integration.XWikiExecutor;
import org.xwiki.test.integration.XWikiExecutorSuite;
import org.xwiki.test.ui.PageObjectSuite;
import org.xwiki.test.ui.PersistentTestContext;

/**
 * Runs all functional tests found in the classpath.
 * 
 * @version $Id$
 */
@RunWith(PageObjectSuite.class)
public class AllTests
{
    @XWikiExecutorSuite.PreStart
    public void preStart(List<XWikiExecutor> executors) throws Exception
    {
        // Put back the old WYSIWYG editor in xwiki.properties
        XWikiExecutor executor = executors.get(0);
        PropertiesConfiguration properties = executor.loadXWikiPropertiesConfiguration();
        properties.setProperty("edit.defaultEditor.org.xwiki.rendering.syntax.SyntaxContent#wysiwyg", "gwt");
        executor.saveXWikiProperties(properties);
    }

    @PageObjectSuite.PostStart
    public void postStart(PersistentTestContext context) throws Exception
    {
        // Disable the tour because it pops-up on the home page and many tests access the home page and they want to
        // skip the tour. We don't plan to test the tour here anyway.
        context.getUtil().loginAsAdmin();
        context.getUtil().gotoPage("TourCode", "TourJS", "save",
            "XWiki.JavaScriptExtension_0_use=onDemand&xredirect=" + context.getUtil().getURL("Main", "WebHome"));
    }
}
