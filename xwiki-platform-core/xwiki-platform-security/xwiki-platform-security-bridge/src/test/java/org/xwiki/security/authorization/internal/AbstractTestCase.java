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
package org.xwiki.security.authorization.internal;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jmock.integration.junit4.JMock;
import org.junit.After;
import org.junit.Before;
import org.junit.runner.RunWith;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.context.Execution;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.security.SecurityReferenceFactory;
import org.xwiki.security.authorization.AuthorizationSettler;
import org.xwiki.security.authorization.cache.SecurityCacheRulesInvalidator;
import org.xwiki.test.AbstractComponentTestCase;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.user.api.XWikiGroupService;
import com.xpn.xwiki.web.Utils;

@RunWith(JMock.class)
public abstract class AbstractTestCase extends AbstractComponentTestCase
{
    /**
     * The logging tool.
     */
    protected final Log LOG = LogFactory.getLog(this.getClass());

    protected AuthorizationSettler settler;

    protected DocumentReferenceResolver<String> docRefResolver;

    protected DocumentReferenceResolver<String> uResolver;

    protected MockXWiki wiki;

    protected XWikiGroupService mockGroupService;

    protected XWikiContext xwikiContext;

    protected SecurityCacheRulesInvalidator rulesInvalidator;

    protected SecurityReferenceFactory referenceFactory;

    @Before
    public void initializeTests() throws Exception
    {
        super.setUp();

        // Statically store the component manager in {@link Utils} to be able to access it without
        // the context.
        Utils.setComponentManager(getComponentManager());

        this.xwikiContext = new XWikiContext();

        this.xwikiContext.setDatabase("xwiki");
        this.xwikiContext.setMainXWiki("xwiki");
        wiki = new MockXWiki("xwiki", null);
        xwikiContext.setWiki(wiki);
        mockGroupService = getMockery().mock(XWikiGroupService.class);
        wiki.setGroupService(mockGroupService);

        // We need to initialize the Component Manager so that the components can be looked up
        this.xwikiContext.put(ComponentManager.class.getName(), getComponentManager());

        // Bridge with old XWiki Context, required for old code.
        Execution execution = getComponentManager().getInstance(Execution.class);
        execution.getContext().setProperty(XWikiContext.EXECUTIONCONTEXT_KEY, this.xwikiContext);

        settler = getComponentManager().getInstance(AuthorizationSettler.class);

        docRefResolver = getComponentManager().getInstance(DocumentReferenceResolver.TYPE_STRING);
        uResolver = getComponentManager().getInstance(DocumentReferenceResolver.TYPE_STRING, "user");
        rulesInvalidator = getComponentManager().getInstance(SecurityCacheRulesInvalidator.class);
        referenceFactory = getComponentManager().getInstance(SecurityReferenceFactory.class);
    }

    @After
    public void tearDown() throws Exception
    {
        Utils.setComponentManager(null);
        super.tearDown();
    }
}
