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
import org.xwiki.extension.test.RepositoryUtil;
import org.xwiki.job.Job;
import org.xwiki.logging.LogLevel;
import org.xwiki.logging.event.LogEvent;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.script.service.ScriptService;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.classes.BaseClass;
import com.xpn.xwiki.store.XWikiStoreInterface;
import com.xpn.xwiki.test.AbstractBridgedComponentTestCase;
import com.xpn.xwiki.user.api.XWikiRightService;
import com.xpn.xwiki.util.XWikiStubContextProvider;

public class ExtensionManagerScriptServiceTest extends AbstractBridgedComponentTestCase
{
    private XWiki mockXWiki;

    private XWikiStoreInterface mockStore;

    private XWikiRightService mockRightService;

    private Map<DocumentReference, Map<String, XWikiDocument>> documents =
        new HashMap<DocumentReference, Map<String, XWikiDocument>>();

    private RepositoryUtil repositoryUtil;

    private Map<String, BaseClass> classes = new HashMap<String, BaseClass>();

    private DocumentReference contextUser;

    private ExtensionManagerScriptService scriptService;

    @Override
    @Before
    public void setUp() throws Exception
    {
        super.setUp();

        this.repositoryUtil = new RepositoryUtil(getComponentManager(), getMockery());
        this.repositoryUtil.setup();

        // mock

        this.mockXWiki = getMockery().mock(XWiki.class);
        getContext().setWiki(this.mockXWiki);
        getContext().setDatabase("xwiki");
        this.contextUser = new DocumentReference(getContext().getDatabase(), "XWiki", "ExtensionUser");

        this.mockStore = getMockery().mock(XWikiStoreInterface.class);

        this.mockRightService = getMockery().mock(XWikiRightService.class);

        // classes

        BaseClass styleSheetClass = new BaseClass();
        this.classes.put("StyleSheetExtension", styleSheetClass);

        // checking

        getMockery().checking(new Expectations()
        {
            {

                allowing(mockXWiki).isVirtualMode();
                will(returnValue(true));

                allowing(mockXWiki).getRightService();
                will(returnValue(mockRightService));

                allowing(mockXWiki).prepareResources(with(any(XWikiContext.class)));
            }
        });

        getContext().setUserReference(this.contextUser);

        ((XWikiStubContextProvider) getComponentManager().getInstance(XWikiStubContextProvider.class))
            .initialize(getContext());

        // lookup

        this.scriptService = getComponentManager().getInstance(ScriptService.class, "extension");
    }

    private Job install(String id, String version, String namespace) throws Throwable
    {
        Job job = this.scriptService.install("extension", "version", null);
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

    @Test
    public void testInstallOnRoot() throws Throwable
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

        // install

        install("extension", "version", null);
    }

    @Test
    public void testInstallOnNamespace() throws Throwable
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

        // install

        install("extension", "version", "namespace");
    }
}
