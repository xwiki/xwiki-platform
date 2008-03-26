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
package org.xwiki.plugin.invitationmanager.impl;

import org.xwiki.plugin.invitationmanager.api.Invitation;
import org.xwiki.plugin.invitationmanager.api.MembershipRequest;

import com.xpn.xwiki.XWikiException;

/**
 * Unit tests for {@link InvitationManagerImpl} class
 */
public class InvitationManagerImplTest extends InvitationManagerTest
{
    protected void setUp() throws Exception
    {
        super.setUp();
        invitationManager = new InvitationManagerImpl();
        ((InvitationManagerImpl) invitationManager).setMailNotification(false);
    }

    protected Invitation createInvitation(String invitee, String space) throws XWikiException
    {
        return new InvitationImpl(invitee,
            space,
            true,
            (InvitationManagerImpl) invitationManager,
            context);
    }

    protected MembershipRequest createMembershipRequest(String requester, String space)
        throws XWikiException
    {
        return new MembershipRequestImpl(requester,
            space,
            true,
            (InvitationManagerImpl) invitationManager,
            context);
    }

    protected Invitation getInvitation(String invitee, String space) throws XWikiException
    {
        return new InvitationImpl(invitee,
            space,
            false,
            (InvitationManagerImpl) invitationManager,
            context);
    }

    protected MembershipRequest getMembershipRequest(String requester, String space)
        throws XWikiException
    {
        return new MembershipRequestImpl(requester,
            space,
            false,
            (InvitationManagerImpl) invitationManager,
            context);
    }
}
