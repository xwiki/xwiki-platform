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

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.LocalDocumentReference;
import org.xwiki.model.reference.WikiReference;
import org.xwiki.security.authorization.AuthorizationManager;
import org.xwiki.security.authorization.ContextualAuthorizationManager;
import org.xwiki.security.authorization.Right;
import org.xwiki.test.mockito.MockitoComponentMockingRule;

import com.xpn.xwiki.test.MockitoOldcoreRule;
import com.xpn.xwiki.test.reference.ReferenceComponentList;

import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.isNull;
import static org.mockito.Matchers.same;
import static org.mockito.Mockito.verify;

/**
 * Validate {@link DefaultContextualAuthorizationManager}.
 * 
 * @version $Id$
 */
@ReferenceComponentList
public class DefaultContextualAuthorizationManagerTest
{
    public MockitoComponentMockingRule<ContextualAuthorizationManager> mocker =
        new MockitoComponentMockingRule<ContextualAuthorizationManager>(DefaultContextualAuthorizationManager.class);

    @Rule
    public MockitoOldcoreRule oldcore = new MockitoOldcoreRule(mocker);

    private AuthorizationManager authorizationManager;

    private WikiReference currentWikiReference;

    @Before
    public void before() throws Exception
    {
        this.authorizationManager = this.mocker.getInstance(AuthorizationManager.class);

        this.currentWikiReference = new WikiReference("wiki");
        this.oldcore.getXWikiContext().setWikiId(this.currentWikiReference.getName());
    }

    // Tests

    @Test
    public void checkAccess() throws Exception
    {
        LocalDocumentReference localReference = new LocalDocumentReference("space", "page");

        this.mocker.getComponentUnderTest().checkAccess(Right.VIEW, localReference);

        verify(this.authorizationManager).checkAccess(same(Right.VIEW), isNull(DocumentReference.class),
            eq(new DocumentReference(localReference, this.currentWikiReference)));
    }

    @Test
    public void hasAccess() throws Exception
    {
        LocalDocumentReference localReference = new LocalDocumentReference("space", "page");

        this.mocker.getComponentUnderTest().hasAccess(Right.VIEW, localReference);

        verify(this.authorizationManager).hasAccess(same(Right.VIEW), isNull(DocumentReference.class),
            eq(new DocumentReference(localReference, this.currentWikiReference)));
    }
}
