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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.LocalDocumentReference;
import org.xwiki.model.reference.WikiReference;
import org.xwiki.security.authorization.AuthorizationManager;
import org.xwiki.security.authorization.Right;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;

import com.xpn.xwiki.test.MockitoOldcore;
import com.xpn.xwiki.test.junit5.mockito.InjectMockitoOldcore;
import com.xpn.xwiki.test.junit5.mockito.OldcoreTest;
import com.xpn.xwiki.test.reference.ReferenceComponentList;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.verify;

/**
 * Validate {@link DefaultContextualAuthorizationManager}.
 * 
 * @version $Id$
 */
@ReferenceComponentList
@OldcoreTest
class DefaultContextualAuthorizationManagerTest
{
    @MockComponent
    private AuthorizationManager authorizationManager;

    @InjectMockComponents
    private DefaultContextualAuthorizationManager contextualAuthorizationManager;

    @InjectMockitoOldcore
    private MockitoOldcore oldcore;

    private WikiReference currentWikiReference;

    @BeforeEach
    public void before() throws Exception
    {
        this.currentWikiReference = new WikiReference("wiki");
        this.oldcore.getXWikiContext().setWikiId(this.currentWikiReference.getName());
    }

    // Tests

    @Test
    void checkAccess() throws Exception
    {
        LocalDocumentReference localReference = new LocalDocumentReference("space", "page");

        this.contextualAuthorizationManager.checkAccess(Right.VIEW, localReference);

        verify(this.authorizationManager).checkAccess(same(Right.VIEW), isNull(),
            eq(new DocumentReference(localReference, this.currentWikiReference)));
    }

    @Test
    void hasAccess()
    {
        LocalDocumentReference localReference = new LocalDocumentReference("space", "page");

        this.contextualAuthorizationManager.hasAccess(Right.VIEW, localReference);

        verify(this.authorizationManager).hasAccess(same(Right.VIEW), isNull(),
            eq(new DocumentReference(localReference, this.currentWikiReference)));
    }
}
