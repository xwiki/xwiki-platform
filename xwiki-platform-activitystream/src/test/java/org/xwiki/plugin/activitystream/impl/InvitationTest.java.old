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

/**
 * Unit tests for classes implementing {@link Invitation} interface
 */
public abstract class InvitationTest extends JoinRequestTest
{
    public Invitation getInvitation()
    {
        return (Invitation) joinRequest;
    }

    /**
     * test for {@link Invitation#getInvitee()}
     */
    public void testInvitee()
    {
        String invitee = "foo@bar.com";
        getInvitation().setInvitee(invitee);
        assertEquals(invitee, getInvitation().getInvitee());
    }

    /**
     * test for {@link Invitation#getInviter()}
     */
    public void testInviter()
    {
        String inviter = "foobar";
        getInvitation().setInviter(inviter);
        assertEquals(inviter, getInvitation().getInviter());
    }

    /**
     * test for {@link Invitation#getCode()}
     */
    public void testCode()
    {
        String code = "qwertyuiopasdfghjkl";
        getInvitation().setCode(code);
        assertEquals(code, getInvitation().getCode());
    }

    /**
     * test for {@link Invitation#isOpen()}
     */
    public void testOpen()
    {
        boolean open = true;
        getInvitation().setOpen(open);
        assertEquals(open, getInvitation().isOpen());
    }
}
