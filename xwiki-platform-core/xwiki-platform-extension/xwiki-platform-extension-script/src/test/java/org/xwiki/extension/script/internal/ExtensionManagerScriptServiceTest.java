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
package org.xwiki.extension.script.internal;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jmock.Expectations;
import org.junit.Before;
import org.junit.Test;
import org.xwiki.extension.InstallException;
import org.xwiki.extension.UninstallException;
import org.xwiki.extension.test.RepositoryUtils;
import org.xwiki.job.Job;
import org.xwiki.logging.LogLevel;
import org.xwiki.logging.event.LogEvent;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.script.service.ScriptService;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.objects.classes.BaseClass;
import com.xpn.xwiki.test.AbstractBridgedComponentTestCase;
import com.xpn.xwiki.user.api.XWikiRightService;
import com.xpn.xwiki.util.XWikiStubContextProvider;

public class ExtensionManagerScriptServiceTest extends AbstractBridgedComponentTestCase
{
    private XWiki mockXWiki;

    private XWikiRightService mockRightService;

    private RepositoryUtils repositoryUtil;

    private Map<String, BaseClass> classes = new HashMap<String, BaseClass>();

    private DocumentReference contextUser;

    private ExtensionManagerScriptService scriptService;

    @Override
    @Before
    public void setUp() throws Exception
    {
        super.setUp();

        this.repositoryUtil = new RepositoryUtils(getComponentManager(), getMockery());
        this.repositoryUtil.setup();

        // mock

        // TODO: replace with a real mock when moving to JMock 2.6 (http://www.jmock.org/threading-synchroniser.html)
        this.mockXWiki = new XWiki()
        {
            @Override
            public XWikiRightService getRightService()
            {
                return mockRightService;
            }

            @Override
            public void prepareResources(XWikiContext context)
            {
                // Do nothing
            }
        };

        getContext().setWiki(this.mockXWiki);
        getContext().setDatabase("xwiki");
        this.contextUser = new DocumentReference(getContext().getDatabase(), "XWiki", "ExtensionUser");

        this.mockRightService = getMockery().mock(XWikiRightService.class);

        // classes

        BaseClass styleSheetClass = new BaseClass();
        this.classes.put("StyleSheetExtension", styleSheetClass);

        // checking

        getContext().setUserReference(this.contextUser);

        ((XWikiStubContextProvider) getComponentManager().getInstance(XWikiStubContextProvider.class))
            .initialize(getContext());

        // lookup

        this.scriptService = getComponentManager().getInstance(ScriptService.class, "extension");
    }

    // tools

    private Job install(String id, String version, String namespace) throws Throwable
    {
        Job job = this.scriptService.install(id, version, namespace);
        if (job == null) {
            throw this.scriptService.getLastError();
        }

        job.join();

        List<LogEvent> errors = job.getStatus().getLog().getLogsFrom(LogLevel.WARN);
        if (!errors.isEmpty()) {
            throw errors.get(0).getThrowable();
        }

        return job;
    }

    private Job uninstall(String id, String namespace) throws Throwable
    {
        Job job = this.scriptService.uninstall(id, namespace);
        if (job == null) {
            throw this.scriptService.getLastError();
        }

        job.join();

        List<LogEvent> errors = job.getStatus().getLog().getLogsFrom(LogLevel.WARN);
        if (!errors.isEmpty()) {
            throw errors.get(0).getThrowable();
        }

        return job;
    }

    // Tests

    // install

    @Test
    public void testInstallOnRoot() throws Throwable
    {
        getMockery().checking(new Expectations()
        {
            {
                oneOf(mockRightService).hasProgrammingRights(with(any(XWikiContext.class)));
                will(returnValue(true));
                oneOf(mockRightService).hasAccessLevel(with(equal("programming")),
                    with(equal("xwiki:XWiki.ExtensionUser")), with(equal("XWiki.XWikiPreferences")),
                    with(any(XWikiContext.class)));
                will(returnValue(true));
            }
        });

        install("extension", "version", null);
    }

    @Test
    public void testInstallOnNamespace() throws Throwable
    {
        getMockery().checking(new Expectations()
        {
            {
                oneOf(mockRightService).hasProgrammingRights(with(any(XWikiContext.class)));
                will(returnValue(true));
                oneOf(mockRightService).hasAccessLevel(with(equal("programming")),
                    with(equal("xwiki:XWiki.ExtensionUser")), with(equal("XWiki.XWikiPreferences")),
                    with(any(XWikiContext.class)));
                will(returnValue(true));
            }
        });

        install("extension", "version", "namespace");
    }

    @Test(expected = InstallException.class)
    public void testInstallOnRootWithoutProgrammingRigths() throws Throwable
    {
        getMockery().checking(new Expectations()
        {
            {
                oneOf(mockRightService).hasProgrammingRights(with(any(XWikiContext.class)));
                will(returnValue(true));
                oneOf(mockRightService).hasAccessLevel(with(equal("programming")),
                    with(equal("xwiki:XWiki.ExtensionUser")), with(equal("XWiki.XWikiPreferences")),
                    with(any(XWikiContext.class)));
                will(returnValue(false));
            }
        });

        install("extension", "version", null);
    }

    @Test(expected = InstallException.class)
    public void testInstallOnNamespaceWithoutProgrammingRigths() throws Throwable
    {
        getMockery().checking(new Expectations()
        {
            {
                oneOf(mockRightService).hasProgrammingRights(with(any(XWikiContext.class)));
                will(returnValue(true));
                oneOf(mockRightService).hasAccessLevel(with(equal("programming")),
                    with(equal("xwiki:XWiki.ExtensionUser")), with(equal("XWiki.XWikiPreferences")),
                    with(any(XWikiContext.class)));
                will(returnValue(false));
            }
        });

        install("extension", "version", "namespace");
    }

    // uninstall

    @Test
    public void testUninstallFromRoot() throws Throwable
    {
        getMockery().checking(new Expectations()
        {
            {
                oneOf(mockRightService).hasAccessLevel(with(equal("programming")),
                    with(equal("xwiki:XWiki.ExtensionUser")), with(equal("XWiki.XWikiPreferences")),
                    with(any(XWikiContext.class)));
                will(returnValue(true));
            }
        });

        uninstall("installedonroot", null);
    }

    @Test
    public void testUninstallOnNamespace() throws Throwable
    {
        getMockery().checking(new Expectations()
        {
            {
                oneOf(mockRightService).hasAccessLevel(with(equal("programming")),
                    with(equal("xwiki:XWiki.ExtensionUser")), with(equal("XWiki.XWikiPreferences")),
                    with(any(XWikiContext.class)));
                will(returnValue(true));
            }
        });

        uninstall("installedonnamespace", "namespace");
    }

    @Test(expected = UninstallException.class)
    public void testUninstallOnRootWithoutProgrammingRigths() throws Throwable
    {
        getMockery().checking(new Expectations()
        {
            {
                oneOf(mockRightService).hasAccessLevel(with(equal("programming")),
                    with(equal("xwiki:XWiki.ExtensionUser")), with(equal("XWiki.XWikiPreferences")),
                    with(any(XWikiContext.class)));
                will(returnValue(false));
            }
        });

        uninstall("installedonroot", null);
    }

    @Test(expected = UninstallException.class)
    public void testUninstallOnNamespaceWithoutProgrammingRigths() throws Throwable
    {
        getMockery().checking(new Expectations()
        {
            {
                oneOf(mockRightService).hasAccessLevel(with(equal("programming")),
                    with(equal("xwiki:XWiki.ExtensionUser")), with(equal("XWiki.XWikiPreferences")),
                    with(any(XWikiContext.class)));
                will(returnValue(false));
            }
        });

        uninstall("installedonnamespace", "namespace");
    }
}
