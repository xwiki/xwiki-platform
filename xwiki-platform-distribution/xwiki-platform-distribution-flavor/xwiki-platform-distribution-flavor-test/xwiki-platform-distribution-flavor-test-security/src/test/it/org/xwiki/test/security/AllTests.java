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
package org.xwiki.test.security;

import java.util.List;

import org.junit.runner.RunWith;
import org.xwiki.test.integration.XWikiExecutor;
import org.xwiki.test.integration.XWikiExecutorSuite;
import org.xwiki.test.ui.PageObjectSuite;
import org.xwiki.test.ui.PersistentTestContext;
import org.xwiki.test.ui.TestUtils;

/**
 * Runs all functional tests found in the classpath.
 *
 * @version $Id$
 * @since 15.9RC1
 * @since 15.5.3
 */
@RunWith(PageObjectSuite.class)
public class AllTests
{
    @XWikiExecutorSuite.PreStart
    public void preStart(List<XWikiExecutor> executors) throws Exception
    {
        // We need to have access to the maven repositories to be able to installed the 
        // xwiki-platform-extension-security-ui extension at runtime (from the local repository, or from the remote
        // final and snapshot repositories).
        XWikiExecutor executor = executors.get(0);
        executor.loadXWikiPropertiesConfiguration().setProperty("extension.repositories",
            List.of("maven-local:maven:file://${sys:user.home}/.m2/repository"));
        executor.saveXWikiProperties();
    }

    @PageObjectSuite.PostStart
    public void postStart(PersistentTestContext context)
    {
        context.getUtil().setDefaultCredentials(TestUtils.ADMIN_CREDENTIALS);
    }
}
