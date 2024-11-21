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
package org.xwiki.index.test.ui.docker;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.Keys;
import org.xwiki.index.tree.test.po.BreadcrumbTree;
import org.xwiki.index.tree.test.po.DocumentTreeElement;
import org.xwiki.index.tree.test.po.PinnedPagesAdministrationSectionPage;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.SpaceReference;
import org.xwiki.test.docker.junit5.TestReference;
import org.xwiki.test.docker.junit5.UITest;
import org.xwiki.test.ui.TestUtils;
import org.xwiki.test.ui.po.RenamePage;
import org.xwiki.test.ui.po.SuggestInputElement;
import org.xwiki.tree.test.po.TreeNodeElement;

/**
 * Functional tests for the pinned pages feature.
 *
 * @version $Id$
 */
@UITest
class PinnedPagesIT
{
    @Test
    @Order(1)
    void refactorPinnedPages(TestUtils setup, TestReference testReference)
    {
        setup.loginAsSuperAdmin();

        // Cleanup.
        setup.deletePage(testReference, true);

        // Create the pages to be pinned.
        DocumentReference levelOne =
            new DocumentReference("WebHome", new SpaceReference("LevelOne", testReference.getLastSpaceReference()));
        DocumentReference levelTwo =
            new DocumentReference("WebHome", new SpaceReference("LevelTwo", levelOne.getLastSpaceReference()));
        DocumentReference zero = new DocumentReference("000", levelTwo.getLastSpaceReference());
        DocumentReference alice = new DocumentReference("Alice", levelTwo.getLastSpaceReference());
        DocumentReference bob =
            new DocumentReference("WebHome", new SpaceReference("Bob", levelTwo.getLastSpaceReference()));
        DocumentReference carol = new DocumentReference("Carol", levelTwo.getLastSpaceReference());
        DocumentReference denis =
            new DocumentReference("WebHome", new SpaceReference("Denis", levelTwo.getLastSpaceReference()));

        setup.createPage(testReference, "", "");
        setup.createPage(levelOne, "", "");
        setup.createPage(levelTwo, "", "");
        setup.createPage(zero, "", "");
        setup.createPage(alice, "", "");
        setup.createPage(bob, "", "");
        setup.createPage(carol, "", "");
        setup.createPage(denis, "", "");

        // Pin the pages.
        PinnedPagesAdministrationSectionPage pinnedPagesAdminSection =
            PinnedPagesAdministrationSectionPage.gotoPage(levelTwo.getLastSpaceReference());
        SuggestInputElement pinnedPagesPicker = pinnedPagesAdminSection.getPinnedPagesPicker();
        pinnedPagesPicker.sendKeys("Carol").waitForSuggestions().selectByIndex(0);
        pinnedPagesPicker.sendKeys("Bob").waitForSuggestions().selectByIndex(0);
        pinnedPagesPicker.sendKeys("Denis").waitForSuggestions().selectByIndex(0);
        pinnedPagesPicker.sendKeys("Alice").waitForSuggestions().selectByIndex(0);
        // Close the suggestions dropdown because it may hide the save button.
        pinnedPagesPicker.sendKeys(Keys.ESCAPE);
        pinnedPagesAdminSection.clickSave();

        // Verify the result.
        setup.gotoPage(levelTwo);
        DocumentTreeElement tree = BreadcrumbTree.open("LevelTwo");
        List<String> children =
            tree.getNode(levelTwo).getChildren().stream().map(TreeNodeElement::getLabel).collect(Collectors.toList());
        assertEquals(List.of("Carol", "Bob", "Denis", "Alice", "000", "Page Administration"), children);

        // Refactor the pinned pages.
        setup.deletePage(bob);
        renamePage(setup, carol, new DocumentReference("Charlie", carol.getLastSpaceReference()));
        movePage(setup, alice, levelOne);
        DocumentReference levelOneRenamed = new DocumentReference("WebHome",
            new SpaceReference("LevelOneRenamed", testReference.getLastSpaceReference()));
        renamePage(setup, levelOne, levelOneRenamed);

        // Verify the result.
        DocumentReference levelTwoRenamed =
            new DocumentReference("WebHome", new SpaceReference("LevelTwo", levelOneRenamed.getLastSpaceReference()));
        pinnedPagesAdminSection =
            PinnedPagesAdministrationSectionPage.gotoPage(levelTwoRenamed.getLastSpaceReference());
        pinnedPagesPicker = pinnedPagesAdminSection.getPinnedPagesPicker();
        assertEquals(List.of("Charlie", "Denis/"), pinnedPagesPicker.getValues());

        setup.gotoPage(levelTwoRenamed);
        tree = BreadcrumbTree.open("LevelTwo");
        children = tree.getNode(levelTwoRenamed).getChildren().stream().map(TreeNodeElement::getLabel)
            .collect(Collectors.toList());
        assertEquals(List.of("Charlie", "Denis", "000", "Page Administration"), children);
    }

    private void movePage(TestUtils setup, DocumentReference source, DocumentReference target)
    {
        if ("WebHome".equals(source.getName())) {
            renamePage(setup, source, new DocumentReference("WebHome",
                new SpaceReference(source.getLastSpaceReference().getName(), target.getLastSpaceReference())));
        } else {
            renamePage(setup, source, new DocumentReference(source.getName(), target.getLastSpaceReference()));
        }
    }

    private void renamePage(TestUtils setup, DocumentReference source, DocumentReference target)
    {
        EntityReference actualTarget = target;
        if ("WebHome".equals(actualTarget.getName())) {
            actualTarget = actualTarget.getParent();
        }

        RenamePage renamePage = setup.gotoPage(source).rename();
        renamePage.getDocumentPicker().setTitle(actualTarget.getName())
            .setParent(setup.serializeLocalReference(actualTarget.getParent()));
        renamePage.clickRenameButton().waitUntilFinished();
    }
}
