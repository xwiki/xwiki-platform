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
package org.xwiki.security.requiredrights.test.ui;

import org.junit.jupiter.api.Order;
import org.junit.jupiter.params.ParameterizedTest;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.WikiReference;
import org.xwiki.test.docker.junit5.TestLocalReference;
import org.xwiki.test.docker.junit5.UITest;
import org.xwiki.test.docker.junit5.WikisSource;
import org.xwiki.test.ui.TestUtils;
import org.xwiki.test.ui.po.EditablePropertyPane;
import org.xwiki.test.ui.po.ViewPage;
import org.xwiki.test.ui.po.editor.ForceEditLockModal;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * UI integration tests verifying that the edit confirmation modal is shown when clicking an editable property while
 * the document has a required rights issue.
 *
 * @version $Id$
 */
@UITest
class EditablePropertyEditConfirmationIT
{
    /**
     * Verifies that clicking the edit pencil on an in-place editable property shows the required-rights warning modal
     * when the content author lacks the rights required by the page content. Confirming the modal proceeds to edit;
     * cancelling aborts it.
     */
    @ParameterizedTest
    @WikisSource(extensions = "org.xwiki.platform:xwiki-platform-security-requiredrights-default")
    @Order(1)
    void editablePropertyShowsRequiredRightsWarning(WikiReference wiki, TestLocalReference testLocalReference,
        TestUtils setup) throws Exception
    {
        DocumentReference testReference = new DocumentReference(testLocalReference, wiki);

        // Create a user without script right who will be the content author.
        String testUser = "EditPropConfirmUser";
        setup.createUserAndLogin(testUser, testUser);

        // Create the page as the low-privilege user using a textarea with wiki syntax (velocity macro). The velocity
        // macro requires script right, but the content author has none, so required-rights enforcement will produce a
        // warning when any editor tries to edit the page.
        setup.rest().delete(testReference);
        setup.rest().savePage(testReference, "{{velocity}}$xcontext.user{{/velocity}}", "");

        setup.loginAsSuperAdmin();
        ViewPage viewPage = setup.gotoPage(testReference);

        // Open the information pane to edit the hidden property.
        viewPage.openInformationDocExtraPane();

        EditablePropertyPane<Boolean> hiddenProperty = new EditablePropertyPane<>("hidden");

        // Click the edit pencil. The edit confirmation check in display.vm returns a 423 (warning), so the
        // ForceEditLockModal is shown instead of the editor.
        hiddenProperty.clickEditWithoutWaiting();
        ForceEditLockModal modal = new ForceEditLockModal();
        // The fade animation takes a while to complete, so we wait until the modal is actually displayed.
        setup.getDriver().waitUntilCondition(driver -> modal.isDisplayed());

        // Cancel: the editor must not open.
        modal.clickCancel();
        assertFalse(hiddenProperty.isEditing());

        // Click edit again; since we canceled, the confirmation was not stored in the session, so the modal reappears.
        hiddenProperty.clickEditWithoutWaiting();
        ForceEditLockModal modal2 = new ForceEditLockModal();
        assertTrue(modal2.isDisplayed());

        // Confirm: the editor should open after the modal closes.
        modal2.clickOk();
        hiddenProperty.waitUntilEditing();
        assertTrue(hiddenProperty.isEditing());

        // Clean up: cancel the edit without saving.
        hiddenProperty.clickCancel();

        // Third attempt: now that we've confirmed the warning, clicking on edit should work directly.
        hiddenProperty.clickEdit();
        assertTrue(hiddenProperty.isEditing());

        // Save the edit to make sure that the warning is not shown again.
        hiddenProperty.setValue(true).clickSave();
    }

    /**
     * Verifies that clicking the edit pencil on an in-place editable property opens the editor directly, without
     * showing the confirmation modal, when there are no required rights issues.
     */
    @ParameterizedTest
    @WikisSource(extensions = "org.xwiki.platform:xwiki-platform-security-requiredrights-default")
    @Order(2)
    void editablePropertyNoWarningWhenNoRequiredRightsIssue(WikiReference wiki, TestLocalReference testLocalReference,
        TestUtils setup) throws Exception
    {
        DocumentReference testReference = new DocumentReference(testLocalReference, wiki);

        // Create the page as superadmin with plain content (no velocity, no rights required).
        setup.loginAsSuperAdmin();
        setup.rest().delete(testReference);
        ViewPage viewPage = setup.createPage(testReference, "Plain content");
        viewPage.openInformationDocExtraPane();

        EditablePropertyPane<Boolean> hiddenProperty = new EditablePropertyPane<>("hidden");

        // Click edit: no required rights warning, so the editor opens immediately without any modal.
        hiddenProperty.clickEdit();
        assertTrue(hiddenProperty.isEditing());

        hiddenProperty.clickCancel();
    }
}
