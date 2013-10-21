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
package org.xwiki.wiki.user.internal;

import java.util.Collection;

import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.wiki.user.MemberCandidacy;
import org.xwiki.wiki.user.WikiUserManager;
import org.xwiki.wiki.user.WikiUserManagerException;

@Component
@Singleton
public class DefaultWikiUserManager implements WikiUserManager
{
    @Override
    public Collection<String> getLocalUsers(String wikiId) throws WikiUserManagerException
    {
        return null;
    }

    @Override
    public Collection<String> getMembers(String wikiId) throws WikiUserManagerException
    {
        return null;
    }

    @Override
    public void addMember(String userId, String wikiId) throws WikiUserManagerException
    {

    }

    @Override
    public void removeMember(String userId, String wikiId) throws WikiUserManagerException
    {

    }

    @Override
    public MemberCandidacy askToJoin(String userId, String wikiId, String message)
            throws WikiUserManagerException
    {
        return null;
    }

    @Override
    public void acceptRequest(MemberCandidacy request, String message, String privateComment)
            throws WikiUserManagerException
    {
    }

    @Override
    public void refuseRequest(MemberCandidacy request, String message, String privateComment)
            throws WikiUserManagerException
    {
    }

    @Override
    public MemberCandidacy invite(String userId, String wikiId, String message)
            throws WikiUserManagerException
    {
        return null;
    }

    @Override
    public void acceptInvitation(MemberCandidacy invitation, String message) throws WikiUserManagerException
    {
    }

    @Override
    public void refuseInvitation(MemberCandidacy invitation, String message) throws WikiUserManagerException
    {
    }
}
