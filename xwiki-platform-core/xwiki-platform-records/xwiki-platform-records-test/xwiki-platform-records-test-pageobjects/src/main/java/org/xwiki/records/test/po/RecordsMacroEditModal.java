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
package org.xwiki.records.test.po;

import java.util.List;
import java.util.function.Consumer;

import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.Select;
import org.xwiki.flamingo.skin.test.po.CompactLocationPickerElement;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.SpaceReference;
import org.xwiki.test.ui.po.SuggestInputElement;
import org.xwiki.tree.test.po.TreeElement;
import org.xwiki.wysiwyg.test.po.MacroDialogEditModal;

/**
 * The macro editor dialog of the Records macro, which spreads its optional parameters over tabs.
 *
 * @version $Id$
 * @since 18.10.0RC1
 */
public class RecordsMacroEditModal extends MacroDialogEditModal
{
    private static final String DATA_TYPE_PARAMETER = "class";

    private static final String COLUMNS_PARAMETER = "properties";

    private static final String FILTERS_PARAMETER = "filters";

    private static final String EDIT_MODE_PARAMETER = "allowEditMode";

    private static final String CREATION_STRATEGY_PARAMETER = "creationStrategy";

    private static final String LOCATION_PARAMETER = "newEntriesLocation";

    /**
     * Creates a page object whose {@link #clickSubmit()} lets the editor that opened the modal wait for the result of
     * the submission, which only that editor knows (e.g. CKEditor refreshes its content to render the macro).
     *
     * @param submitHandler receives the action that clicks the submit button, runs it and waits for its result
     */
    public RecordsMacroEditModal(Consumer<Runnable> submitHandler)
    {
        super(submitHandler);
    }

    /**
     * {@inheritDoc}
     * <p>
     * The dialog gives the focus to its first field, the object type picker, which opens its suggestions and covers
     * the tabs. Waits for that focus and hides the suggestions, so that the dialog can be used. Also waits for the
     * dialog script, which enhances the columns picker.
     */
    @Override
    public RecordsMacroEditModal waitUntilReady()
    {
        super.waitUntilReady();
        SuggestInputElement dataTypePicker = getMacroParameterSuggestInput(DATA_TYPE_PARAMETER);
        getDriver().waitUntilCondition(driver -> dataTypePicker.hasFocus());
        dataTypePicker.hideSuggestions();
        getMacroParameterSuggestInput(COLUMNS_PARAMETER);
        return this;
    }

    /**
     * Shows the parameters of one tab.
     *
     * @param groupId the identifier of the parameter group the tab shows
     * @return the current page object
     */
    public RecordsMacroEditModal openTab(String groupId)
    {
        String panelId = "macroParameterTreeNode-" + groupId;
        // We match *-editor-modal so the page object can be used both in Dashboard and CKEditor tests.
        getDriver().findElement(
            By.cssSelector("[class*=-editor-modal] .macro-tabs a[aria-controls='" + panelId + "']")).click();
        getDriver().waitUntilElementIsVisible(By.id(panelId));
        return this;
    }

    /**
     * Waits for the columns picker to be enhanced. The Columns tab must be the one shown.
     *
     * @return the columns picker
     * @since 18.10.0RC1
     */
    public SuggestInputElement getColumnsPicker()
    {
        return getMacroParameterSuggestInput(COLUMNS_PARAMETER);
    }

    /**
     * Waits for the filters picker to be enhanced. The Filter &amp; sort tab must be the one shown.
     *
     * @return the filters picker
     */
    public SuggestInputElement getFiltersPicker()
    {
        return new SuggestInputElement(getMacroParameterInput(FILTERS_PARAMETER));
    }

    /**
     * Allows or forbids the edit mode of the table. The Editing tab must be the one shown.
     *
     * @param allowed whether readers can switch the table to its edit mode
     * @return the current page object
     * @since 18.10.0RC1
     */
    public RecordsMacroEditModal setAllowEditMode(boolean allowed)
    {
        setMacroParameterCheckbox(EDIT_MODE_PARAMETER, allowed);
        return this;
    }

    /**
     * @return whether the creation strategy can be picked, which needs the edit mode
     * @since 18.10.0RC1
     */
    public boolean isCreationStrategyEnabled()
    {
        return getMacroParameterInput(CREATION_STRATEGY_PARAMETER).isEnabled();
    }

    /**
     * @param label the label of a strategy, as the dropdown shows it
     * @return whether that strategy can be picked, which needs the columns it names the new pages after
     * @since 18.10.0RC1
     */
    public boolean isCreationStrategyAvailable(String label)
    {
        return new Select(getMacroParameterInput(CREATION_STRATEGY_PARAMETER)).getOptions().stream()
            .filter(option -> label.equals(option.getText())).findFirst().orElseThrow().isEnabled();
    }

    /**
     * @return the warning shown below the creation strategy, saying why strategies cannot be picked, or an empty
     *     string when there is none
     * @since 18.10.0RC1
     */
    public String getCreationStrategyWarning()
    {
        return getDriver().findElementsWithoutWaiting(getMacroParameterInput(CREATION_STRATEGY_PARAMETER),
            By.xpath("following-sibling::*[1][contains(@class, 'warningmessage')]")).stream()
            .map(WebElement::getText).findFirst().orElse("");
    }

    /**
     * Picks how the pages of the entries created from the table are named. The Editing tab must be the one shown.
     *
     * @param label the label of the strategy, as the dropdown shows it
     * @return the current page object
     * @since 18.10.0RC1
     */
    public RecordsMacroEditModal setCreationStrategy(String label)
    {
        new Select(getMacroParameterInput(CREATION_STRATEGY_PARAMETER)).selectByVisibleText(label);
        return this;
    }

    /**
     * @return the value of the selected creation strategy
     * @since 18.10.0RC1
     */
    public String getCreationStrategy()
    {
        return getMacroParameter(CREATION_STRATEGY_PARAMETER);
    }

    /**
     * Picks the space the entries created from the table go to, from the tree of the location picker. The Editing tab
     * must be the one shown.
     *
     * @param location the space
     * @return the current page object
     * @since 18.10.0RC1
     */
    public RecordsMacroEditModal setNewEntriesLocation(SpaceReference location)
    {
        CompactLocationPickerElement picker = CompactLocationPickerElement.get(LOCATION_PARAMETER);
        TreeElement tree = picker.openDropDown();
        // Picking a node closes the tree, so its parents are opened one by one rather than with TreeElement#openTo.
        List<SpaceReference> parents = location.getReversedReferenceChain().stream()
            .filter(reference -> reference.getType() == EntityType.SPACE).map(SpaceReference::new).toList();
        parents.subList(0, parents.size() - 1).forEach(parent -> tree.getNode(getNodeId(parent)).open().waitForIt());
        picker.pick(getNodeId(location));
        return this;
    }

    private String getNodeId(SpaceReference space)
    {
        return "document:" + getUtil().serializeReference(new DocumentReference("WebHome", space));
    }

    /**
     * @return the value the dialog saves for the new entries location
     * @since 18.10.0RC1
     */
    public String getNewEntriesLocation()
    {
        return getMacroParameter(LOCATION_PARAMETER);
    }

    /**
     * @return the value the dialog saves for the filters parameter, as the picker builds it
     */
    public String getFilters()
    {
        return getMacroParameter(FILTERS_PARAMETER);
    }
}
