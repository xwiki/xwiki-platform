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

import com.xpn.xwiki.plugin.invitationmanager.api.JoinRequestStatus;
import com.xpn.xwiki.plugin.invitationmanager.api.MembershipRequest;
import com.xpn.xwiki.plugin.invitationmanager.impl.InvitationManagerImpl;
import com.xpn.xwiki.plugin.invitationmanager.impl.MembershipRequestImpl;

/**
 * Unit tests for {@link MembershipRequestImpl} class
 */
public class MembershipRequestImplTest extends MembershipRequestTest
{
    private InvitationManagerImpl manager;

    protected void setUp() throws Exception
    {
        super.setUp();

        manager = new InvitationManagerImpl();
        manager.setMailNotification(false);
        joinRequest = new MembershipRequestImpl(null, null, true, manager, getContext());
    }

    public void testSave()
    {
        try {
            String requester = "testRequester";
            String space = "testSpace";

            MembershipRequest expected =
                new MembershipRequestImpl(requester, space, true, manager, getContext());
            expected.setResponder("testResponder");

            Map map = new HashMap();
            map.put("allowMailNotifications", "false");
            map.put("notifyChanges", "true");
            expected.setMap(map);

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

            MembershipRequest actual =
                new MembershipRequestImpl(requester, space, false, manager, getContext());

            assertEquals(expected.getRequester(), actual.getRequester());
            assertEquals(expected.getResponder(), actual.getResponder());
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
