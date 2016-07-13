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
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashSet;

import org.jmock.Mock;
import org.xwiki.model.reference.DocumentReference;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.stats.impl.StatsUtil.PeriodType;
import com.xpn.xwiki.test.AbstractBridgedXWikiComponentTestCase;

/**
 * Unit tests for the {@link StatsUtil} class.
 * 
 * @version $Id$
 */
public class StatsUtilTest extends AbstractBridgedXWikiComponentTestCase
{
    private Mock mockXWiki;

    @Override
    protected void setUp() throws Exception
    {
        super.setUp();

        this.mockXWiki = mock(XWiki.class);

        getContext().setWiki((XWiki) this.mockXWiki.proxy());
    }

    /**
     * Test for the {@link StatsUtil#getPeriodAsInt(java.util.Date, PeriodType)}.
     */
    public void testGetPeriodAsInt()
    {
        Calendar cal = Calendar.getInstance();

        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMM");
        String a = sdf.format(cal.getTime());
        String b = StatsUtil.getPeriodAsInt(cal.getTime(), PeriodType.MONTH) + "";
        assertEquals("Wrong month period format", a, b);

        sdf = new SimpleDateFormat("yyyyMMdd");
        a = sdf.format(cal.getTime());
        b = StatsUtil.getPeriodAsInt(cal.getTime(), PeriodType.DAY) + "";
        assertEquals("Wrong day period format", a, b);
    }

    public void testGetFilteredUsers() throws XWikiException
    {
        this.mockXWiki.stubs().method("getXWikiPreference").will(returnValue(null));
        this.mockXWiki.stubs().method("Param").will(returnValue(null));

        assertEquals(new HashSet<DocumentReference>(), StatsUtil.getRequestFilteredUsers(getContext()));

        this.mockXWiki.stubs().method("Param").will(returnValue("XWiki.XWikiGuest"));
        this.mockXWiki.stubs().method("getDocument")
            .will(returnValue(new XWikiDocument(new DocumentReference("xwiki", "XWiki", "XWikiGuest"))));

        assertEquals(
            new HashSet<>(Collections.singletonList(new DocumentReference("xwiki", "XWiki", "XWikiGuest"))),
            StatsUtil.getRequestFilteredUsers(getContext()));

        this.mockXWiki.stubs().method("Param").will(returnValue("XWiki.supeRadmin"));
        this.mockXWiki.stubs().method("getDocument")
            .will(returnValue(new XWikiDocument(new DocumentReference("xwiki", "XWiki", "supeRadmin"))));

        assertEquals(new HashSet<>(Collections.singletonList(new DocumentReference("xwiki", "XWiki", "supeRadmin"))),
            StatsUtil.getRequestFilteredUsers(getContext()));

        this.mockXWiki.stubs().method("Param").will(returnValue("invalid"));
        this.mockXWiki.stubs().method("getDocument").will(returnValue(
            new XWikiDocument(new DocumentReference("xwiki", "Space", "invalid"))));

        assertEquals(new HashSet<DocumentReference>(), StatsUtil.getRequestFilteredUsers(getContext()));

        DocumentReference userReference = new DocumentReference("xwiki", "XWiki", "user");
        final XWikiDocument userDoc = new XWikiDocument(userReference);
        userDoc.setNew(false);
        userDoc.addXObject(new DocumentReference("xwiki", "XWiki", "XWikiUsers"), new BaseObject());

        this.mockXWiki.stubs().method("Param").will(returnValue("XWiki.user"));
        this.mockXWiki.stubs().method("getDocument").with(eq(userReference), ANYTHING).will(returnValue(userDoc));

        assertEquals(new HashSet<>(Collections.singletonList(userReference)),
            StatsUtil.getRequestFilteredUsers(getContext()));

        DocumentReference user2Reference = new DocumentReference("xwiki", "XWiki", "user2");
        final XWikiDocument user2Doc = new XWikiDocument(user2Reference);
        user2Doc.setNew(false);
        user2Doc.addXObject(new DocumentReference("xwiki", "XWiki", "XWikiUsers"), new BaseObject());

        this.mockXWiki.stubs().method("Param").will(returnValue("XWiki.user,XWiki.user2"));
        this.mockXWiki.stubs().method("getDocument").with(eq(user2Reference), ANYTHING).will(returnValue(user2Doc));

        assertEquals(new HashSet<>(Arrays.asList(userReference, user2Reference)),
            StatsUtil.getRequestFilteredUsers(getContext()));

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

        this.mockXWiki.stubs().method("Param").will(returnValue("XWiki.user,XWiki.user2,otherwiki:XWiki.group"));
        this.mockXWiki.stubs().method("getDocument").with(eq(groupReference), ANYTHING).will(returnValue(groupDoc));
        this.mockXWiki.stubs().method("getDocument").with(eq(user3Reference), ANYTHING).will(returnValue(user3Doc));

        assertEquals(new HashSet<>(Arrays.asList(userReference, user2Reference, user3Reference)),
            StatsUtil.getRequestFilteredUsers(getContext()));
    }
}
