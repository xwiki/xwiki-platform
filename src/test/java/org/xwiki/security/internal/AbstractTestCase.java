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
 *
 */
package org.xwiki.security.internal;

import org.xwiki.test.AbstractComponentTestCase;

import com.xpn.xwiki.web.Utils;

import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReference;

import org.junit.Before;
import org.junit.runner.RunWith;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.States;
import org.jmock.integration.junit4.JMock;
import org.jmock.integration.junit4.JUnit4Mockery;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.user.api.XWikiGroupService;

import org.xwiki.context.Execution;
import org.xwiki.context.ExecutionContext;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.xwiki.security.*;
import static org.xwiki.security.Right.*;
import static org.xwiki.security.RightState.*;

@RunWith(JMock.class)
public abstract class AbstractTestCase extends AbstractComponentTestCase
{
    /**
     * The logging tool.
     */
    protected final Log LOG = LogFactory.getLog(this.getClass());

    protected RightResolver resolver;

    protected DocumentReferenceResolver<String> docRefResolver;

    protected DocumentReferenceResolver<String> uResolver;

    protected MockXWiki wiki;

    protected Mockery mockery;

    protected XWikiGroupService mockGroupService;

    protected XWikiContext xwikiContext;

    protected RightCacheInvalidator invalidator;

    @Before
    public void initializeTests() throws Exception
    {
        try {
            Utils.setComponentManager(getComponentManager());
            resolver = getComponentManager().lookup(RightResolver.class, "default");
            docRefResolver = getComponentManager().lookup(DocumentReferenceResolver.class);
            uResolver = getComponentManager().lookup(DocumentReferenceResolver.class, "user");
            Execution execution = getComponentManager().lookup(Execution.class);
            xwikiContext = new XWikiContext();
            xwikiContext.setMainXWiki("xwiki");
            System.out.println("Setting main wiki: " + xwikiContext.getMainXWiki());
            wiki = new MockXWiki("xwiki", null);
            xwikiContext.setWiki(wiki);
            mockery = new JUnit4Mockery();
            mockGroupService = mockery.mock(XWikiGroupService.class);
            wiki.setGroupService(mockGroupService);
            execution.getContext().setProperty(XWikiContext.EXECUTIONCONTEXT_KEY, xwikiContext);
            invalidator = getComponentManager().lookup(RightCacheInvalidator.class);
        } catch (Exception e) {
            LOG.error("Caught exception", e);
            throw e;
        }
    }
}