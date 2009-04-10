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
package com.xpn.xwiki.plugin.invitationmanager.impl;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.RandomStringUtils;

import com.xpn.xwiki.plugin.invitationmanager.api.Invitation;
import com.xpn.xwiki.plugin.invitationmanager.api.JoinRequestStatus;
import com.xpn.xwiki.plugin.invitationmanager.impl.InvitationImpl;
import com.xpn.xwiki.plugin.invitationmanager.impl.InvitationManagerImpl;

/**
 * Unit tests for {@link InvitationImpl} class
 */
public class InvitationImplTest extends InvitationTest
{
    private InvitationManagerImpl manager;

    protected void setUp() throws Exception
    {
        super.setUp();

        manager = new InvitationManagerImpl();
        manager.setMailNotification(false);
        joinRequest = new InvitationImpl(null, null, true, manager, getContext());
    }

    public void testSave()
    {
        try {
            String invitee = "testInvitee";
            String space = "testSpace";

            Invitation expected = new InvitationImpl(invitee, space, true, manager, getContext());
            expected.setCode(RandomStringUtils.random(8));
            expected.setInviter("testInviter");

            Map map = new HashMap();
            map.put("allowMailNotifications", "false");
            map.put("notifyChanges", "true");
            expected.setMap(map);

            expected.setOpen(false);

            SimpleDateFormat sdf = new SimpleDateFormat("yyyy.MM.dd");
            expected.setRequestDate(sdf.parse("2006.09.25"));
            expected.setResponseDate(new Date());

            List roles = new ArrayList();
            roles.add("developer");
            roles.add("tester");
            expected.setRoles(roles);

            expected.setStatus(JoinRequestStatus.REFUSED);
            expected.setText("testText");

            expected.save();

            Invitation actual = new InvitationImpl(invitee, space, false, manager, getContext());

            assertEquals(expected.isOpen(), actual.isOpen());
            assertEquals(expected.getCode(), actual.getCode());
            assertEquals(expected.getInvitee(), actual.getInvitee());
            assertEquals(expected.getInviter(), actual.getInviter());
            assertEquals(expected.getMap(), actual.getMap());
            assertEquals(expected.getRequestDate(), actual.getRequestDate());
            assertEquals(expected.getResponseDate(), actual.getResponseDate());
            assertEquals(expected.getRoles(), actual.getRoles());
            assertEquals(expected.getSpace(), actual.getSpace());
            assertEquals(expected.getStatus(), actual.getStatus());
            assertEquals(expected.getText(), actual.getText());
        } catch (Exception e) {
            assertTrue(false);
        }
    }
}
