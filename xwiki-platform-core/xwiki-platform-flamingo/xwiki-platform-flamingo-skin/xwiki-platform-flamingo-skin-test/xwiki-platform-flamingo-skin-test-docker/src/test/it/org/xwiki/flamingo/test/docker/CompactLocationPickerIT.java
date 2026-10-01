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
package org.xwiki.flamingo.test.docker;

import java.util.List;

import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.xwiki.flamingo.skin.test.po.CompactLocationPickerElement;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.SpaceReference;
import org.xwiki.test.docker.junit5.TestReference;
import org.xwiki.test.docker.junit5.UITest;
import org.xwiki.test.ui.TestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Functional tests for the Compact Location Picker.
 *
 * @version $Id$
 * @since 18.9.0RC1
 */
@UITest
class CompactLocationPickerIT
{
    private static final String PICKER_TEMPLATE = "{{velocity}}{{html}}#template('locationPicker_macros.vm')"
        + "#compactLocationPicker({'name': 'locations', 'value': '%s', 'root': 'document:%s', 'multiple': %s})"
        + "{{/html}}{{/velocity}}";

    private static final String WEB_HOME = "WebHome";

    /**
     * With single selection, picking a location in the tree replaces the current one and closes the tree, since
     * there's nothing more to pick.
     */
    @Test
    @Order(1)
    void pickSingleLocationFromTree(TestUtils setup, TestReference reference) throws Exception
    {
        setup.loginAsSuperAdmin();
        SpaceReference alice = createSpace(setup, reference, "Alice");
        SpaceReference bob = createSpace(setup, reference, "Bob");

        CompactLocationPickerElement picker = displayPicker(setup, reference, alice, false);
        assertEquals("Browse for a location", picker.getBrowseLabel());

        picker.openBrowser();
        assertFalse(picker.hasCheckbox(getNodeId(setup, alice)));
        // The tree reflects the value of the picker.
        assertTrue(picker.isSelected(getNodeId(setup, alice)));

        picker.pick(getNodeId(setup, bob));
        assertFalse(picker.isBrowserOpen());
        assertEquals(List.of(setup.serializeLocalReference(bob)), picker.getSuggestInput().getValues());

        picker.openBrowser();
        assertTrue(picker.isSelected(getNodeId(setup, bob)));
        assertFalse(picker.isSelected(getNodeId(setup, alice)));
    }

    /**
     * With multiple selection, checking and unchecking locations in the tree adds them to and removes them from the
     * picker, and the tree stays open so that more locations can be picked.
     */
    @Test
    @Order(2)
    void checkMultipleLocationsInTree(TestUtils setup, TestReference reference) throws Exception
    {
        setup.loginAsSuperAdmin();
        SpaceReference alice = createSpace(setup, reference, "Alice");
        SpaceReference bob = createSpace(setup, reference, "Bob");

        CompactLocationPickerElement picker = displayPicker(setup, reference, alice, true);
        assertEquals("Browse for locations", picker.getBrowseLabel());

        picker.openBrowser();
        assertTrue(picker.hasCheckbox(getNodeId(setup, alice)));
        // The tree reflects the value of the picker.
        assertTrue(picker.isChecked(getNodeId(setup, alice)));

        picker.setChecked(getNodeId(setup, bob), true);
        assertTrue(picker.isBrowserOpen());
        assertEquals(List.of(setup.serializeLocalReference(alice), setup.serializeLocalReference(bob)),
            picker.getSuggestInput().getValues());

        picker.setChecked(getNodeId(setup, alice), false);
        assertEquals(List.of(setup.serializeLocalReference(bob)), picker.getSuggestInput().getValues());

        // The value can also be changed without the tree, which must then follow.
        picker.closeBrowser().getSuggestInput().clearSelectedSuggestions();
        picker.openBrowser();
        assertFalse(picker.isChecked(getNodeId(setup, bob)));
    }

    private SpaceReference createSpace(TestUtils setup, TestReference reference, String name) throws Exception
    {
        SpaceReference spaceReference = new SpaceReference(name, reference.getLastSpaceReference());
        setup.rest().savePage(new DocumentReference(WEB_HOME, spaceReference), "", name);
        return spaceReference;
    }

    private String getNodeId(TestUtils setup, SpaceReference spaceReference)
    {
        return "document:" + setup.serializeReference(new DocumentReference(WEB_HOME, spaceReference));
    }

    /**
     * Displays the picker on the test page, with the tree rooted at the test page so that the spaces created by the
     * test are the top level nodes.
     */
    private CompactLocationPickerElement displayPicker(TestUtils setup, TestReference reference,
        SpaceReference value, boolean multiple)
    {
        setup.createPage(reference, String.format(PICKER_TEMPLATE, setup.serializeLocalReference(value),
            setup.serializeReference(reference), multiple), reference.getLastSpaceReference().getName());
        return CompactLocationPickerElement.getFirstInPageContent();
    }
}
