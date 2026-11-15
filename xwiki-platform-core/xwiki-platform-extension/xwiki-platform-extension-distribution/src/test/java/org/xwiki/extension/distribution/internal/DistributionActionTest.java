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
package org.xwiki.extension.distribution.internal;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.xwiki.icon.IconException;
import org.xwiki.icon.IconSet;
import org.xwiki.icon.IconSetManager;
import org.xwiki.icon.internal.context.IconSetContext;
import org.xwiki.test.LogLevel;
import org.xwiki.test.annotation.ComponentList;
import org.xwiki.test.junit5.LogCaptureExtension;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.test.MockitoOldcore;
import com.xpn.xwiki.test.junit5.mockito.InjectMockitoOldcore;
import com.xpn.xwiki.test.junit5.mockito.OldcoreTest;
import com.xpn.xwiki.web.XWikiRequest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Validate {@link DistributionAction}.
 *
 * @version $Id$
 */
@OldcoreTest
@ComponentList(IconSetContext.class)
class DistributionActionTest
{
    @InjectMockitoOldcore
    private MockitoOldcore oldcore;

    @MockComponent
    private DistributionManager distributionManager;

    @MockComponent
    private IconSetManager iconSetManager;

    @RegisterExtension
    private LogCaptureExtension logCapture = new LogCaptureExtension(LogLevel.WARN);

    private IconSetContext iconSetContext;

    private XWikiContext xcontext;

    @InjectMockComponents
    private DistributionAction action;

    @BeforeEach
    void beforeEach() throws Exception
    {
        this.iconSetContext = this.oldcore.getMocker().getInstance(IconSetContext.class);

        this.xcontext = this.oldcore.getXWikiContext();
        this.xcontext.setRequest(mock(XWikiRequest.class));

        when(this.distributionManager.canDisplayDistributionWizard()).thenReturn(true);
    }

    @Test
    void actionForcesDefaultSkinAndIconSet() throws Exception
    {
        IconSet defaultIconSet = new IconSet("default");
        when(this.iconSetManager.getDefaultIconSet()).thenReturn(defaultIconSet);

        assertTrue(this.action.action(this.xcontext));

        assertEquals(XWiki.DEFAULT_SKIN, this.xcontext.get(XWiki.CKEY_SKIN));
        assertSame(defaultIconSet, this.iconSetContext.getIconSet());
    }

    @Test
    void actionWhenDefaultIconSetFails() throws Exception
    {
        when(this.iconSetManager.getDefaultIconSet()).thenThrow(new IconException("error"));

        assertTrue(this.action.action(this.xcontext));

        assertNull(this.iconSetContext.getIconSet());
        assertEquals("Failed to force the default icon set in the Distribution Wizard: IconException: error",
            this.logCapture.getMessage(0));
    }
}
