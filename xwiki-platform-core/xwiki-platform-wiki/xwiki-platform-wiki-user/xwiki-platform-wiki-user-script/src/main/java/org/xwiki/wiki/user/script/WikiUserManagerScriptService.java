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
package org.xwiki.wiki.user.script;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.script.service.ScriptService;
import org.xwiki.wiki.descriptor.WikiDescriptorManager;
import org.xwiki.wiki.manager.WikiManagerException;
import org.xwiki.wiki.user.MembershipType;
import org.xwiki.wiki.user.WikiUserManager;

@Component
@Named("wiki.user")
@Singleton
public class WikiUserManagerScriptService implements ScriptService
{
    @Inject
    private WikiUserManager wikiUserManager;

    @Inject
    private WikiDescriptorManager wikiDescriptorManager;

    /**
     * @return whether or not the current wiki has local users enabled
     */
    public Boolean hasLocalUsersEnabled()
    {
        return hasLocalUsersEnabled(wikiDescriptorManager.getMainWikiId());
    }

    /**
     * @param wikiId Id of the wiki to test
     * @return whether or not the specified wiki has local users enabled
     */
    public Boolean hasLocalUsersEnabled(String wikiId)
    {
        try {
            return wikiUserManager.hasLocalUsersEnabled(wikiId);
        } catch (WikiManagerException e) {
            return null;
        }
    }

    /**
     * @param wikiId Id of the wiki to change
     * @param enable enable/disable the local users support for the specified wiki
     * @return true if it succeed
     */
    public boolean enableLocalUsers(String wikiId, boolean enable)
    {
        boolean success = true;
        try {
            wikiUserManager.enableLocalUsers(wikiId, enable);
        } catch (WikiManagerException e) {
            success = false;
        }
        return success;
    }

    /**
     * @return the membership type of the current wiki
     */
    public MembershipType getMembershipType()
    {
        return getMembershipType(wikiDescriptorManager.getMainWikiId());
    }

    /**
     * @param wikiId Id of the wiki to test
     * @return the membership type of the specified wiki
     */
    public MembershipType getMembershipType(String wikiId)
    {
        try {
            return wikiUserManager.getMembershipType(wikiId);
        } catch (WikiManagerException e) {
            return null;
        }
    }

    /**
     * @param wikiId Id of the wiki to change
     * @param type the membership type to set
     * @return true if it succeed
     */
    public boolean setMembershypType(String wikiId, String type)
    {
        boolean success = true;
        try {
            wikiUserManager.setMembershypType(wikiId, MembershipType.valueOf(type.toUpperCase()));
        } catch (WikiManagerException e) {
            success = false;
        }
        return success;
    }
}
