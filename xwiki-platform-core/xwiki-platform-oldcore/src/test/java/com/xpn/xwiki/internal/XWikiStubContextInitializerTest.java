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
package com.xpn.xwiki.internal;

import org.junit.jupiter.api.Test;
import org.xwiki.context.ExecutionContext;
import org.xwiki.context.internal.DefaultExecution;
import org.xwiki.context.internal.DefaultExecutionContextManager;
import org.xwiki.test.annotation.ComponentList;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.test.junit5.mockito.OldcoreTest;
import com.xpn.xwiki.util.XWikiStubContextProvider;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Validate XWikiStubContextInitializer.
 * 
 * @version $Id$
 */
@OldcoreTest
public class XWikiStubContextInitializerTest
{
    @MockComponent
    private XWikiStubContextProvider stubContextProvider;

    @InjectMockComponents
    private XWikiStubContextInitializer initializer;

    @Test
    public void testWithAndWithoutXWikiContext() throws Exception
    {
        XWikiContext stubContext = new XWikiContext();

        when(this.stubContextProvider.createStubContext()).thenReturn(stubContext);

        final ExecutionContext daemonContext = new ExecutionContext();

        this.initializer.initialize(daemonContext);

        XWikiContext daemonXcontext = (XWikiContext) daemonContext.getProperty("xwikicontext");
        assertNotNull(daemonXcontext);
    }
}
