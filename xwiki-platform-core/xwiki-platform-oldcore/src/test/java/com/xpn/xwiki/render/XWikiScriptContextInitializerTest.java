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
package com.xpn.xwiki.render;

import javax.script.ScriptContext;
import javax.script.SimpleScriptContext;
import javax.servlet.ServletContext;

import org.junit.jupiter.api.Test;
import org.xwiki.security.authorization.ContextualAuthorizationManager;
import org.xwiki.security.authorization.Right;
import org.xwiki.stability.Unstable;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;

import com.xpn.xwiki.test.MockitoOldcore;
import com.xpn.xwiki.test.junit5.mockito.InjectMockitoOldcore;
import com.xpn.xwiki.test.junit5.mockito.OldcoreTest;
import com.xpn.xwiki.web.XWikiRequest;
import com.xpn.xwiki.web.XWikiServletRequestStub;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Validate {@link XWikiScriptContextInitializer}.
 * 
 * @version $Id$
 */
@OldcoreTest
public class XWikiScriptContextInitializerTest
{
    @InjectMockitoOldcore
    MockitoOldcore oldcore;

    @MockComponent
    ContextualAuthorizationManager authorization;

    @InjectMockComponents
    XWikiScriptContextInitializer initializer;

    ScriptContext scriptContext = new SimpleScriptContext();

    @Test
    public void getServletContext()
    {
        this.oldcore.getXWikiContext().setRequest(new XWikiServletRequestStub()
        {
            @Override
            public ServletContext getServletContext()
            {
                return mock(ServletContext.class);
            }
        });

        this.initializer.initialize(this.scriptContext);

        XWikiRequest request = (XWikiRequest) this.scriptContext.getAttribute("request");

        when(authorization.hasAccess(Right.PROGRAM)).thenReturn(true);

        assertNotNull(request.getServletContext());

        when(authorization.hasAccess(Right.PROGRAM)).thenReturn(false);

        assertNull(request.getServletContext());
    }
}
