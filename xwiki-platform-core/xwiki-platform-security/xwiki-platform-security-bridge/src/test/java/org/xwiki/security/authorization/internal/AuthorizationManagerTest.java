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

import junit.framework.Assert;

import org.junit.Before;
import org.junit.Test;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.security.authorization.AbstractWikiTestCase;
import org.xwiki.security.authorization.AuthorizationManager;
import org.xwiki.security.authorization.Right;
import org.xwiki.security.authorization.testwikibuilding.LegacyTestWiki;

import com.xpn.xwiki.XWikiContext;

public class AuthorizationManagerTest extends AbstractWikiTestCase
{
    private AuthorizationManager authorizationManager;

    @Override
    @Before
    public void setUp() throws Exception
    {
        super.setUp();

        this.authorizationManager = getComponentManager().getInstance(AuthorizationManager.class);
    }

    protected void assertAccessTrue(String message, Right right, DocumentReference userReference,
        DocumentReference documentReference, XWikiContext ctx) throws Exception
    {
        setContext(ctx);

        Assert.assertTrue(message, this.authorizationManager.hasAccess(right, userReference, documentReference));
    }

    protected void assertAccessFalse(String message, Right right, DocumentReference userReference,
        DocumentReference documentReference, XWikiContext ctx) throws Exception
    {
        setContext(ctx);

        Assert.assertFalse(message, this.authorizationManager.hasAccess(right, userReference, documentReference));
    }

    // Tests

    @Test
    public void testGlobalUserInEmptySubWiki() throws Exception
    {
        LegacyTestWiki testWiki = new LegacyTestWiki(getMockery(), getComponentManager(), "emptySubWiki.xml", false);

        XWikiContext ctx = testWiki.getXWikiContext();
        ctx.setDatabase("wiki2");

        assertAccessFalse("User from global wiki should have the same rights on empty subwiki", Right.VIEW,
            new DocumentReference("wiki", "XWiki", "user"), new DocumentReference("wiki2", "Space", "Page"), ctx);
        assertAccessFalse("User from global wiki should have the same rights on empty subwiki", Right.EDIT,
            new DocumentReference("wiki", "XWiki", "user"), new DocumentReference("wiki2", "Space", "Page"), ctx);
        assertAccessFalse("User from global wiki should have the same rights on empty subwiki", Right.COMMENT,
            new DocumentReference("wiki", "XWiki", "user"), new DocumentReference("wiki2", "Space", "Page"), ctx);
        assertAccessFalse("User from global wiki should have the same rights on empty subwiki", Right.DELETE,
            new DocumentReference("wiki", "XWiki", "user"), new DocumentReference("wiki2", "Space", "Page"), ctx);
        assertAccessFalse("User from global wiki should have the same rights on empty subwiki", Right.REGISTER,
            new DocumentReference("wiki", "XWiki", "user"), new DocumentReference("wiki2", "Space", "Page"), ctx);
        assertAccessFalse("User from global wiki should have the same rights on empty subwiki", Right.PROGRAM,
            new DocumentReference("wiki", "XWiki", "user"), new DocumentReference("wiki2", "Space", "Page"), ctx);
    }
}
