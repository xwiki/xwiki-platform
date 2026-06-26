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
package com.xpn.xwiki.stats.impl;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.test.annotation.AllComponents;
import org.xwiki.test.junit5.mockito.InjectComponentManager;
import org.xwiki.test.mockito.MockitoComponentManager;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.stats.impl.StatsUtil.PeriodType;
import com.xpn.xwiki.test.MockitoOldcore;
import com.xpn.xwiki.test.junit5.mockito.InjectMockitoOldcore;
import com.xpn.xwiki.test.junit5.mockito.OldcoreTest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.doReturn;

/**
 * Unit tests for the {@link StatsUtil} class.
 *
 * @version $Id$
 */
@OldcoreTest
@AllComponents
class StatsUtilTest
{
    @InjectMockitoOldcore
    private MockitoOldcore oldCore;

    @InjectComponentManager
    private MockitoComponentManager componentManager;


    /**
     * Test for the {@link StatsUtil#getPeriodAsInt(Date, PeriodType)}.
     */
    @Test
    void getPeriodAsInt()
    {
        Calendar cal = Calendar.getInstance();

        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMM");
        String a = sdf.format(cal.getTime());
        String b = StatsUtil.getPeriodAsInt(cal.getTime(), PeriodType.MONTH) + "";
        assertEquals(a, b, "Wrong month period format");

        sdf = new SimpleDateFormat("yyyyMMdd");
        a = sdf.format(cal.getTime());
        b = StatsUtil.getPeriodAsInt(cal.getTime(), PeriodType.DAY) + "";
        assertEquals(a, b, "Wrong day period format");
    }

    @Test
    void getFilteredUsers() throws XWikiException
    {
        XWiki wiki = this.oldCore.getSpyXWiki();
        doReturn(null).when(wiki).getXWikiPreference(any(), any(), any());
        doReturn(null).when(wiki).Param(any());

        XWikiContext context = this.oldCore.getXWikiContext();
        assertEquals(Set.of(), StatsUtil.getRequestFilteredUsers(context));

        doReturn("XWiki.XWikiGuest").when(wiki).Param(any());
        doReturn(new XWikiDocument(new DocumentReference("xwiki", "XWiki", "XWikiGuest")))
            .when(wiki)
            .getDocument(any(EntityReference.class), eq(context));

        assertEquals(
            Set.of(new DocumentReference("xwiki", "XWiki", "XWikiGuest")),
            StatsUtil.getRequestFilteredUsers(context));

        doReturn("XWiki.supeRadmin").when(wiki).Param(any());
        doReturn(new XWikiDocument(new DocumentReference("xwiki", "XWiki", "supeRadmin")))
            .when(wiki)
            .getDocument(any(EntityReference.class), eq(context));

        assertEquals(Set.of(new DocumentReference("xwiki", "XWiki", "supeRadmin")),
            StatsUtil.getRequestFilteredUsers(context));

        doReturn("invalid").when(wiki).Param(any());
        doReturn(new XWikiDocument(new DocumentReference("xwiki", "Space", "invalid")))
            .when(wiki)
            .getDocument(any(EntityReference.class), eq(context));

        assertEquals(Set.of(), StatsUtil.getRequestFilteredUsers(context));

        DocumentReference userReference = new DocumentReference("xwiki", "XWiki", "user");
        XWikiDocument userDoc = new XWikiDocument(userReference);
        userDoc.setNew(false);
        userDoc.addXObject(new DocumentReference("xwiki", "XWiki", "XWikiUsers"), new BaseObject());

        doReturn("XWiki.user").when(wiki).Param(any());
        doReturn(userDoc)
            .when(wiki)
            .getDocument(eq(userReference), any());

        assertEquals(Set.of(userReference), StatsUtil.getRequestFilteredUsers(context));

        DocumentReference user2Reference = new DocumentReference("xwiki", "XWiki", "user2");
        final XWikiDocument user2Doc = new XWikiDocument(user2Reference);
        user2Doc.setNew(false);
        user2Doc.addXObject(new DocumentReference("xwiki", "XWiki", "XWikiUsers"), new BaseObject());

        doReturn("XWiki.user,XWiki.user2").when(wiki).Param(any());
        doReturn(user2Doc)
            .when(wiki)
            .getDocument(eq(user2Reference), any());

        assertEquals(Set.of(userReference, user2Reference), StatsUtil.getRequestFilteredUsers(context));

        DocumentReference user3Reference = new DocumentReference("otherwiki", "XWiki", "user3");
        final XWikiDocument user3Doc = new XWikiDocument(user3Reference);
        user3Doc.setNew(false);
        user3Doc.addXObject(new DocumentReference("otherwiki", "XWiki", "XWikiUsers"), new BaseObject());

        DocumentReference groupReference = new DocumentReference("otherwiki", "XWiki", "group");
        final XWikiDocument groupDoc = new XWikiDocument(groupReference);
        groupDoc.setNew(false);
        BaseObject member = new BaseObject();
        member.setStringValue("member", "user3");
        groupDoc.addXObject(new DocumentReference("otherwiki", "XWiki", "XWikiGroups"), member);

        doReturn("XWiki.user,XWiki.user2,otherwiki:XWiki.group").when(wiki).Param(any());
        doReturn(groupDoc)
            .when(wiki)
            .getDocument(eq(groupReference), eq(context));
        doReturn(user3Doc)
            .when(wiki)
            .getDocument(eq(user3Reference), eq(context));

        assertEquals(Set.of(userReference, user2Reference, user3Reference), StatsUtil.getRequestFilteredUsers(context));
    }
}
