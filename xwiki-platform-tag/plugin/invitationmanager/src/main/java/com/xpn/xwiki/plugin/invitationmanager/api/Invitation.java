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
package com.xpn.xwiki.plugin.invitationmanager.api;

/**
 * An invitation made by a member of a space to an unregistered non-member to join the space. It is
 * stored in InvitationManager.InvitationClass
 * 
 * @version $Id: $
 */
public interface Invitation extends JoinRequest
{

    /**
     * @return The one who is invited to join the space
     */
    String getInvitee();

    /**
     * @param invitee The wikiname or the e-mail address of the invitee
     * @see #getInvitee()
     */
    void setInvitee(String invitee);

    /**
     * @return The one who makes the invitation (must be a member of the space)
     */
    String getInviter();

    /**
     * @param inviter The wikiname of the inviter
     * @see #getInviter()
     */
    void setInviter(String inviter);

    /**
     * @return The code of the invitation
     */
    String getCode();

    /**
     * @see #getCode()
     */
    void setCode(String code);

    /**
     * @return <code>true</code> if the invitation does not have just one invitee but many. This
     *         may be the case when the invitation is sent to a mailing list. A closed invitation
     *         can be accepted just once, while an opened one can be accepted many times, by anyone
     *         knowing its code.
     */
    boolean isOpen();

    /**
     * @see #isOpen()
     */
    void setOpen(boolean open);
}
