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
import org.xwiki.livedata.test.po.LiveDataElement;
import org.xwiki.livedata.test.po.TableLayoutElement;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.SpaceReference;
import org.xwiki.model.reference.WikiReference;
import org.xwiki.test.docker.junit5.TestLocalReference;
import org.xwiki.test.docker.junit5.UITest;
import org.xwiki.test.docker.junit5.WikisSource;
import org.xwiki.test.ui.TestUtils;
import org.xwiki.test.ui.po.editor.ForceEditLockModal;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * UI integration test verifying that the edit confirmation modal is shown when editing an XObject property in-place in
 * a Live Data table while the edited document has a required rights issue.
 *
 * @version $Id$
 */
@UITest
class LiveDataXObjectEditConfirmationIT
{
    private static final String LIVE_DATA_ID = "test";

    private static final String NAME_COLUMN = "name";

    /**
     * Verifies that triggering the inline edit of an XObject property cell in a Live Data table shows the
     * required-rights warning modal when the edited document's content author lacks the rights required by its content.
     * Confirming the modal proceeds to edit; cancelling aborts it.
     */
    @ParameterizedTest
    @WikisSource(extensions = {
        "org.xwiki.platform:xwiki-platform-security-requiredrights-default",
        "org.xwiki.platform:xwiki-platform-livetable-ui"
    })
    @Order(1)
    void liveDataXObjectPropertyShowsRequiredRightsWarning(WikiReference wiki, TestLocalReference testLocalReference,
        TestUtils setup) throws Exception
    {
        // The live data page also holds the XClass whose objects are listed.
        DocumentReference liveDataReference = new DocumentReference(testLocalReference, wiki);
        SpaceReference space = liveDataReference.getLastSpaceReference();
        DocumentReference dataReference = new DocumentReference("EditConfirmationEntry", space);

        setup.loginAsSuperAdmin();
        setup.rest().delete(dataReference);
        setup.rest().delete(liveDataReference);

        // Define the class on the live data page.
        setup.addClassProperty(liveDataReference, NAME_COLUMN, "String");

        // Create a data document, authored by a user without script right, that contains an object of the class and a
        // velocity macro in its content. The velocity macro requires script right, but the content author has none, so
        // required-rights enforcement produces a warning when the page is edited.
        String testUser = "LiveDataEditConfirmUser";
        setup.createUserAndLogin(testUser, testUser);
        setup.rest().savePage(dataReference, "{{velocity}}$xcontext.user{{/velocity}}", "");
        setup.rest().addObject(dataReference, setup.serializeReference(liveDataReference.getLocalDocumentReference()),
            NAME_COLUMN, "Alice");

        // Build the live data page as superadmin, listing the objects of the class defined above.
        setup.loginAsSuperAdmin();
        createLiveDataPage(setup, liveDataReference);

        setup.gotoPage(liveDataReference);
        LiveDataElement liveData = new LiveDataElement(LIVE_DATA_ID);
        TableLayoutElement tableLayout = liveData.getTableLayout();
        tableLayout.waitUntilReady();
        // waitUntilReady() doesn't guarantee that rows are actually present (it considers an empty table ready), and
        // the entry can take a moment to become visible in the live data, so explicitly wait for the single expected
        // row before trying to edit it.
        tableLayout.waitUntilRowCountEqualsTo(1);

        // Trigger the inline edit of the XObject property. The edit confirmation check in display.vm returns a 423
        // (warning), so the ForceEditLockModal is shown instead of the editor.
        tableLayout.clickEditCell(NAME_COLUMN, 1);
        ForceEditLockModal modal = new ForceEditLockModal();
        // The fade animation takes a while to complete, so we wait until the modal is actually displayed.
        setup.getDriver().waitUntilCondition(driver -> modal.isDisplayed());

        // Cancel: the editor must not open.
        modal.clickCancel();
        assertFalse(tableLayout.isCellEditing(NAME_COLUMN, 1, NAME_COLUMN));

        // Trigger the edit again; since we canceled, the confirmation was not stored in the session, so the modal
        // reappears.
        tableLayout.clickEditCell(NAME_COLUMN, 1);
        ForceEditLockModal modal2 = new ForceEditLockModal();
        setup.getDriver().waitUntilCondition(driver -> modal2.isDisplayed());

        // Confirm: the editor should open after the modal closes.
        modal2.clickOk();
        setup.getDriver().waitUntilCondition(driver -> tableLayout.isCellEditing(NAME_COLUMN, 1, NAME_COLUMN));
        assertTrue(tableLayout.isCellEditing(NAME_COLUMN, 1, NAME_COLUMN));
    }

    private void createLiveDataPage(TestUtils setup, DocumentReference liveDataReference) throws Exception
    {
        String content = "{{liveData\n"
            + "  id=\"" + LIVE_DATA_ID + "\"\n"
            + "  properties=\"" + NAME_COLUMN + "\"\n"
            + "  source=\"liveTable\"\n"
            + "  sourceParameters=\"translationPrefix=&className=" + setup.serializeReference(
            liveDataReference.getLocalDocumentReference()) + "\"\n"
            + "}}{{/liveData}}";
        setup.rest().savePage(liveDataReference, content, "");
    }
}
