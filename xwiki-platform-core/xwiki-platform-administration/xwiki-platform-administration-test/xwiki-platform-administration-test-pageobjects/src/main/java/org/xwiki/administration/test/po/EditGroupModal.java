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
package org.xwiki.administration.test.po;

import org.openqa.selenium.By;
import org.xwiki.test.ui.po.BaseModal;
import org.xwiki.test.ui.po.LiveTableElement;

/**
 * Represents the edit group modal.
 * 
 * @version $Id$
 * @since 10.9
 */
public class EditGroupModal extends BaseModal
{
    private static class ModalContent extends GroupEditPage
    {
        @Override
        public void waitUntilPageJSIsLoaded()
        {
            // There's no need to wait for any JavaScript here because the group edit form is loaded with AJAX inside
            // the modal body, after the page has been loaded.
        }
    }

    private ModalContent groupEditPage = new ModalContent();

    public EditGroupModal()
    {
        super(By.id("editGroupModal"));
    }

    public EditGroupModal waitUntilReady()
    {
        // Wait until the modal content is loaded.
        getDriver().waitUntilElementIsVisible(By.cssSelector("#editGroupModal #groupusers"));
        // Wait until the members live table is loaded.
        getMembersTable().waitUntilReady();
        return this;
    }

    public LiveTableElement getMembersTable()
    {
        return this.groupEditPage.getMembersTable();
    }

    public EditGroupModal addMember(String member, boolean isUser)
    {
        this.groupEditPage.addMemberToGroup(member, isUser);
        return this;
    }

    public LiveTableElement filterMembers(String member)
    {
        this.groupEditPage.filterMembers(member);
        return getMembersTable();
    }
}
