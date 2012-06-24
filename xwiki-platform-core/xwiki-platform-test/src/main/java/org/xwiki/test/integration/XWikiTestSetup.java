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
package org.xwiki.test.integration;

import junit.extensions.TestSetup;
import junit.framework.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

/**
 * JUnit TestSetup extension that starts/stops XWiki using a script passed using System Properties. These properties are
 * meant to be passed by the underlying build system. This class is meant to wrap a JUnit TestSuite. For example:
 * 
 * <pre>
 * &lt;code&gt;
 * public static Test suite()
 * {
 *     // Create some TestSuite object here
 *     return new XWikiTestSetup(suite);
 * }
 * &lt;/code&gt;
 * </pre>
 * <p>
 * Note: We could start XWiki using Java directly but we're using a script so that we can test the exact same script
 * used by XWiki users who download the standalone distribution.
 * </p>
 * 
 * @version $Id$
 * @deprecated use {@link XWikiExecutorSuite} instead
 */
@Deprecated
public class XWikiTestSetup extends TestSetup
{
    private List<XWikiExecutor> executors = new ArrayList<XWikiExecutor>();

    public XWikiTestSetup(Test test) throws Exception
    {
        this(test, 1);
    }

    public XWikiTestSetup(Test test, int nb) throws Exception
    {
        super(test);

        for (int i = 0; i < nb; ++i) {
            XWikiExecutor executor = new XWikiExecutor(i);

            // Don't set any extension repository since we don't need it and we don't want to require internet
            // connection for the tests.
            Properties properties = executor.loadXWikiProperties();
            properties.setProperty("extension.repositories", "");
            executor.saveXWikiProperties(properties);

            this.executors.add(executor);
        }
    }

    @Override
    protected void setUp() throws Exception
    {
        for (XWikiExecutor executor : this.executors) {
            executor.start();
        }
    }

    @Override
    protected void tearDown() throws Exception
    {
        for (XWikiExecutor executor : this.executors) {
            executor.stop();
        }
    }

    protected XWikiExecutor getXWikiExecutor()
    {
        return getXWikiExecutor(0);
    }

    protected XWikiExecutor getXWikiExecutor(int index)
    {
        return this.executors.get(index);
    }
}
