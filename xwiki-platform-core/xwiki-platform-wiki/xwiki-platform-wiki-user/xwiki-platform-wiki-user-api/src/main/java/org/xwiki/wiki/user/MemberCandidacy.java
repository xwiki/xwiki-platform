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
package org.xwiki.wiki.user;

/**
 * Represents the invitation or the request to join a wiki.
 * @since 5.3M2
 * @version $Id$
 */
public class MemberCandidacy
{
    private enum CandidateType
    {
        /**
         * An admin has invited the user to join the wiki.
         */
        INVITATION,
        /**
         * The user has requested to join the wiki.
         */
        REQUEST
    }

    private String id;

    private String wikiId;

    private String userId;

    private String userComment;

    private String adminId;

    private String adminComment;

    private String adminPrivateComment;

    private CandidateType type;

    /**
     * @return the id of the candidacy
     */
    public String getId()
    {
        return id;
    }

    /**
     * @return id of the wiki to join
     */
    public String getWikiId()
    {
        return wikiId;
    }

    /**
     * @param wikiId id of the wiki to join
     */
    public void setWikiId(String wikiId)
    {
        this.wikiId = wikiId;
    }

    /**
     * The user is the person who ask or has been invited to join the wiki.
     * @return id of the user
     */
    public String getUserId()
    {
        return userId;
    }

    /**
     * @param userId id of the user
     */
    public void setUserId(String userId)
    {
        this.userId = userId;
    }

    /**
     * The admin is the person who has sent the invitation or who review the join request.
     * @return the id of the Admin
     */
    public String getAdminId()
    {
        return adminId;
    }

    /**
     * @param adminId if of the admin
     */
    public void setAdminId(String adminId)
    {
        this.adminId = adminId;
    }

    /**
     * Message of the user which motivates his request or message about his acceptance/refusal of the invitation.
     * @return the user comment
     */
    public String getUserComment()
    {
        return userComment;
    }

    /**
     * @param userComment the user comment
     */
    public void setUserComment(String userComment)
    {
        this.userComment = userComment;
    }

    /**
     * Message of the admin that goes along the invitation or about his acceptance/refusal of the request.
     * @return the admin comment
     */
    public String getAdminComment()
    {
        return adminComment;
    }

    /**
     * @param adminComment the admin comment
     */
    public void setAdminComment(String adminComment)
    {
        this.adminComment = adminComment;
    }

    /**
     * Private message about the candidacy that only the admin can see.
     * @return the private message
     */
    public String getAdminPrivateComment()
    {
        return adminPrivateComment;
    }

    /**
     * @param adminPrivateComment the private message
     */
    public void setAdminPrivateComment(String adminPrivateComment)
    {
        this.adminPrivateComment = adminPrivateComment;
    }

    /**
     * @return the type of candidacy
     */
    public CandidateType getType()
    {
        return type;
    }

}
