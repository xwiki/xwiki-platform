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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;
import org.openqa.selenium.support.ui.ExpectedCondition;
import org.xwiki.index.tree.test.po.DocumentTreeElement;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.test.ui.po.InlinePage;
import org.xwiki.test.ui.po.Select;
import org.xwiki.tree.test.po.TreeElement;
import org.xwiki.tree.test.po.TreeNodeElement;

/**
 * Represents a template provider page in inline mode
 *
 * @version $Id$
 * @since 4.2M1
 */
public class TemplateProviderInlinePage extends InlinePage
{
    public static final String ACTION_SAVEANDEDIT = "saveandedit";

    public static final String ACTION_EDITONLY = "edit";

    @FindBy(name = "XWiki.TemplateProviderClass_0_template")
    private WebElement templateInput;

    @FindBy(name = "XWiki.TemplateProviderClass_0_name")
    private WebElement templateNameInput;

    @FindBy(name = "XWiki.TemplateProviderClass_0_type")
    private WebElement templateTypeSelect;

    @FindBy(name = "XWiki.TemplateProviderClass_0_terminal")
    private WebElement terminalSelect;

    @FindBy(name = "XWiki.TemplateProviderClass_0_action")
    private WebElement templateActionSelect;

    @FindBy(name = "XWiki.TemplateProviderClass_0_creationRestrictions")
    private WebElement creationRestrictionsInput;

    @FindBy(name = "XWiki.TemplateProviderClass_0_creationRestrictionsAreSuggestions")
    private WebElement creationRestrictionsAreSuggestionsCheckbox;

    private DocumentTreeElement spacesTree;

    public String getTemplateName()
    {
        return this.templateNameInput.getAttribute("value");
    }

    public void setTemplateName(String value)
    {
        this.templateNameInput.clear();
        this.templateNameInput.sendKeys(value);
    }

    public String getTemplate()
    {
        return this.templateInput.getAttribute("value");
    }

    public void setTemplate(String value)
    {
        this.templateInput.clear();
        this.templateInput.sendKeys(value);
    }

    public boolean isPageTemplate()
    {
        return this.templateTypeSelect.findElement(By.xpath("//option[@value='page']")).isSelected();
    }

    public void setPageTemplate(boolean isPageTemplate)
    {
        Select select = new Select(this.templateTypeSelect);

        String value;
        if (isPageTemplate) {
            value = "page";
        } else {
            value = "space";
        }

        select.selectByValue(value);
    }

    public boolean isTerminal()
    {
        return this.terminalSelect.findElement(By.xpath("//option[@value='1']")).isSelected();
    }

    public void setTerminal(boolean isTerminal)
    {
        Select select = new Select(this.terminalSelect);

        String value;
        if (isTerminal) {
            value = "1";
        } else {
            value = "0";
        }

        select.selectByValue(value);
    }

    public TreeElement getSpacesTree()
    {
        if (this.spacesTree == null) {
            this.spacesTree =
                new DocumentTreeElement(this.getDriver().findElement(By.cssSelector(".templateProviderSheet .xtree")))
                    .waitForIt();
        }

        return this.spacesTree;
    }

    /**
     * @return the list of spaces
     * @since 8.2M3 (renamed from getSpaces)
     */
    public List<String> getVisibilityRestrictions()
    {
        List<String> spaces = new ArrayList<String>();

        for (String nodeID : getSpacesTree().getNodeIDs()) {
            String space = getSpaceFromNodeID(nodeID);
            spaces.add(space);
        }

        return spaces;
    }

    /**
     * @param spaces the list of spaces
     * @since 8.2M3 (renamed from setSpaces)
     */
    public void setVisibilityRestrictions(List<String> spaces)
    {
        // Clean any existing selection.
        List<String> selectedNodeIDs = getSpacesTree().getSelectedNodeIDs();
        for (String selectedSpaceID : selectedNodeIDs) {
            getSpacesTree().getNode(selectedSpaceID).deselect();
        }

        // Open to and select the given spaces.
        for (final String space : spaces) {
            String nodeId = getNodeIDFromSpace(space);

            getSpacesTree().openTo(nodeId);

            // Wait for the selection to get registered by the template provider UI javascript code.
            getDriver().waitUntilCondition(new ExpectedCondition<WebElement>()
            {
                @Override
                public WebElement apply(WebDriver input)
                {
                    return getDriver()
                        .findElementWithoutWaiting(
                            By.xpath("//input[@id='XWiki.TemplateProviderClass_0_visibilityRestrictions' and contains(@value, '"
                                + space + "')]"));
                }
            });
        }
    }

    /**
     * @param spaces the list of spaces to set
     * @since 8.3M2
     */
    public void setCreationRestrictions(List<String> spaces)
    {
        String value = StringUtils.join(spaces.toArray());

        this.creationRestrictionsInput.clear();
        this.creationRestrictionsInput.sendKeys(value);
    }

    /**
     * @return the list of spaces
     * @since 8.2M2
     */
    public List<String> getCreationRestrictions()
    {
        String value = this.creationRestrictionsInput.getAttribute("value");
        List<String> values = Arrays.asList(value.split(","));

        return values;
    }

    /**
     * @return the list of _actually_ checked spaces. If none is checked it actually means that the template is
     *         available in all spaces
     */
    public List<String> getSelectedSpaces()
    {
        List<String> selectedSpaces = new ArrayList<String>();

        List<String> selectedNodeIDs = getSpacesTree().getSelectedNodeIDs();
        for (String selectedNodeID : selectedNodeIDs) {
            String spaceLocalSerializedReference = getSpaceFromNodeID(selectedNodeID);
            selectedSpaces.add(spaceLocalSerializedReference);
        }

        return selectedSpaces;
    }

    private String getNodeIDFromSpace(String space)
    {
        EntityReference spaceReference = getUtil().resolveDocumentReference(String.format("%s.WebHome", space));
        String nodeId = getUtil().serializeReference(spaceReference);
        nodeId = String.format("document:%s", nodeId);
        return nodeId;
    }

    private String getSpaceFromNodeID(String selectedNodeId)
    {
        String selectedNodeIdReferenceString = selectedNodeId.substring("document:".length());
        EntityReference nodeReference = getUtil().resolveDocumentReference(selectedNodeIdReferenceString);
        nodeReference = nodeReference.removeParent(nodeReference.extractReference(EntityType.WIKI));
        nodeReference = nodeReference.extractReference(EntityType.SPACE);

        String spaceLocalSerializedReference = getUtil().serializeReference(nodeReference);
        return spaceLocalSerializedReference;
    }

    /**
     * Sets all spaces besides the ones passed in the list.
     *
     * @param spaces the spaces to exclude
     */
    public void excludeSpaces(List<String> spaces)
    {
        List<String> selectedSpaces = getSelectedSpaces();

        // Go through each (loaded) node in the tree.
        List<String> nodeIDs = getSpacesTree().getNodeIDs();
        for (String nodeId : nodeIDs) {
            TreeNodeElement node = getSpacesTree().getNode(nodeId);

            if (spaces.contains(getSpaceFromNodeID(nodeId))) {
                // If its in the list, deselect it.
                node.deselect();
            } else if (selectedSpaces.size() == 0 || spaces.containsAll(selectedSpaces)) {
                // If its not in the list, but if there would be no selections left in the tree after the exclusion
                // operation, select it, for the exclusion to still make sense.
                node.select();
            }
        }
    }

    /**
     * The action to execute when the create button is pushed, you can configure here whether the new document is saved
     * before it is opened for edition or not.
     *
     * @param actionName the behavior to have on create; valid values are "saveandedit" and "edit". See
     *            {@link #ACTION_EDITONLY} and {@link #ACTION_SAVEANDEDIT}
     * @since 7.2M2
     */
    public void setActionOnCreate(String actionName)
    {
        this.templateActionSelect.findElement(By.xpath("//option[@value='" + actionName + "']")).click();
    }

    /**
     * @return true if the creationRestrictions are suggestions, false otherwise
     * @since 8.3M2
     */
    public boolean isCreationRestrictionsSuggestions()
    {
        return this.creationRestrictionsAreSuggestionsCheckbox.isSelected();
    }

    /**
     * @param selected true if the creationRestrictions should be suggestions, false otherwise
     * @since 8.3M2
     */
    public void setCreationRestrictionsSuggestions(boolean selected)
    {
        if (this.creationRestrictionsAreSuggestionsCheckbox.isSelected() != selected) {
            this.creationRestrictionsAreSuggestionsCheckbox.click();
        }
    }
}
