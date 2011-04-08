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
package org.xwiki.gwt.wysiwyg.client.plugin.style;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.xwiki.gwt.user.client.Config;
import org.xwiki.gwt.user.client.ui.rta.RichTextArea;
import org.xwiki.gwt.user.client.ui.rta.cmd.Command;
import org.xwiki.gwt.wysiwyg.client.Strings;
import org.xwiki.gwt.wysiwyg.client.plugin.internal.AbstractStatefulPlugin;
import org.xwiki.gwt.wysiwyg.client.plugin.internal.FocusWidgetUIExtension;
import org.xwiki.gwt.wysiwyg.client.plugin.style.exec.BlockStyleNameExecutable;
import org.xwiki.gwt.wysiwyg.client.plugin.style.exec.InlineStyleNameExecutable;

import com.google.gwt.dom.client.NodeList;
import com.google.gwt.dom.client.OptGroupElement;
import com.google.gwt.dom.client.OptionElement;
import com.google.gwt.dom.client.SelectElement;
import com.google.gwt.dom.client.Style.Display;
import com.google.gwt.event.dom.client.ChangeEvent;
import com.google.gwt.event.dom.client.ChangeHandler;
import com.google.gwt.user.client.ui.ListBox;

/**
 * Enhances the editor with the ability to apply predefined styles to the current text selection. Installs a list box on
 * the tool bar with the available styles.
 * 
 * @version $Id$
 */
public class StylePlugin extends AbstractStatefulPlugin implements ChangeHandler
{
    /**
     * Command used to apply a given style name to each of the text nodes from the current text selection.
     */
    private static final Command INLINE_STYLE_NAME = new Command("inlineStyleName");

    /**
     * Command used to apply a given style name to each of the block nodes touched by the current text selection.
     */
    private static final Command BLOCK_STYLE_NAME = new Command("blockStyleName");

    /**
     * The CSS class name used to mark a style name as selected (i.e. applied on the current text selection).
     */
    private static final String SELECTED = "selected";

    /**
     * The name of the boolean property that specifies if a style name is in-line or not.
     */
    private static final String INLINE = "inline";

    /**
     * The widget used to pick a style name.
     */
    private ListBox styleNamePicker;

    /**
     * The list of in-line style names.
     */
    private final List<OptionElement> inlineStyles = new ArrayList<OptionElement>();

    /**
     * The list of block style names.
     */
    private final List<OptionElement> blockStyles = new ArrayList<OptionElement>();

    /**
     * Stores the previous value for each of the style commands to speed up the style picker update.
     * 
     * @see #update(List, Command)
     */
    private final Map<Command, String> previousValue = new HashMap<Command, String>();

    /**
     * User interface extension for the editor tool bar.
     */
    private final FocusWidgetUIExtension toolBarExtension = new FocusWidgetUIExtension("toolbar");

    /**
     * {@inheritDoc}
     * 
     * @see AbstractStatefulPlugin#init(RichTextArea, Config)
     */
    public void init(RichTextArea textArea, Config config)
    {
        super.init(textArea, config);

        // Register custom executables.
        getTextArea().getCommandManager().registerCommand(INLINE_STYLE_NAME, new InlineStyleNameExecutable(textArea));
        getTextArea().getCommandManager().registerCommand(BLOCK_STYLE_NAME, new BlockStyleNameExecutable(textArea));

        if (getTextArea().getCommandManager().isSupported(INLINE_STYLE_NAME)) {
            initStyleNamePicker();

            registerTextAreaHandlers();
            getUIExtensionList().add(toolBarExtension);
        }
    }

    /**
     * Initialize the style name picker.
     */
    private void initStyleNamePicker()
    {
        styleNamePicker = new ListBox();
        styleNamePicker.setTitle(Strings.INSTANCE.stylePickerTitle());
        styleNamePicker.addStyleName("xStyleNamePicker");
        styleNamePicker.addItem(Strings.INSTANCE.stylePickerLabel(), "");
        saveRegistration(styleNamePicker.addChangeHandler(this));

        StyleDescriptorJSONParser parser = new StyleDescriptorJSONParser();
        for (StyleDescriptor descriptor : parser.parse(getConfig().getParameter("styleNames", "[]"))) {
            styleNamePicker.addItem(descriptor.getLabel(), descriptor.getName());
            NodeList<OptionElement> options = SelectElement.as(styleNamePicker.getElement()).getOptions();
            OptionElement option = options.getItem(options.getLength() - 1);
            option.setPropertyBoolean(INLINE, descriptor.isInline());
            (descriptor.isInline() ? inlineStyles : blockStyles).add(option);
        }

        if (blockStyles.size() > 0 && inlineStyles.size() > 0) {
            groupStyleNames(Strings.INSTANCE.styleBlockGroupLabel(), blockStyles);
            groupStyleNames(Strings.INSTANCE.styleInlineGroupLabel(), inlineStyles);
        }
        styleNamePicker.setSelectedIndex(0);

        toolBarExtension.addFeature("stylename", styleNamePicker);
    }

    /**
     * Group the given style names under the specified group.
     * 
     * @param groupLabel the group label
     * @param styleNames the style names to group
     */
    private void groupStyleNames(String groupLabel, List<OptionElement> styleNames)
    {
        OptGroupElement group = styleNamePicker.getElement().getOwnerDocument().createOptGroupElement();
        group.setLabel(groupLabel);
        styleNamePicker.getElement().appendChild(group);
        for (OptionElement styleName : styleNames) {
            group.appendChild(styleName);
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see AbstractStatefulPlugin#destroy()
     */
    public void destroy()
    {
        if (styleNamePicker != null) {
            styleNamePicker.removeFromParent();
            styleNamePicker = null;
        }

        blockStyles.clear();
        inlineStyles.clear();
        toolBarExtension.clearFeatures();

        super.destroy();
    }

    /**
     * {@inheritDoc}
     * 
     * @see ChangeHandler#onChange(ChangeEvent)
     */
    public void onChange(ChangeEvent event)
    {
        if (styleNamePicker == event.getSource() && styleNamePicker.isEnabled()) {
            int selectedIndex = styleNamePicker.getSelectedIndex();
            styleNamePicker.setSelectedIndex(0);
            // Ignore the first option which is used as the list box label.
            if (selectedIndex > 0) {
                // The method that returns the options from a select element is broken in WebKit browsers.
                // See http://code.google.com/p/google-web-toolkit/issues/detail?id=4916 .
                if (selectedIndex <= blockStyles.size()) {
                    toggleStyle(blockStyles.get(selectedIndex - 1));
                } else {
                    toggleStyle(inlineStyles.get(selectedIndex - blockStyles.size() - 1));
                }
            }
        }
    }

    /**
     * Toggles the style name corresponding to the given style option.
     * 
     * @param styleOption one of the style option from the style name list box
     */
    private void toggleStyle(OptionElement styleOption)
    {
        String styleName = styleOption.getValue();
        Command command = styleOption.getPropertyBoolean(INLINE) ? INLINE_STYLE_NAME : BLOCK_STYLE_NAME;
        getTextArea().setFocus(true);
        getTextArea().getCommandManager().execute(command, styleName);
    }

    /**
     * {@inheritDoc}
     * 
     * @see AbstractStatefulPlugin#update()
     */
    public void update()
    {
        if (styleNamePicker.isEnabled()) {
            update(blockStyles, BLOCK_STYLE_NAME);
            update(inlineStyles, INLINE_STYLE_NAME);
        }
    }

    /**
     * Updates the selected state of all the options in the specified list.
     * 
     * @param styleNameOptions the list of style name options to update
     * @param styleNameCommand the command used to retrieve the list of applied style names
     */
    private void update(List<OptionElement> styleNameOptions, Command styleNameCommand)
    {
        // Ignore if the list of options is empty.
        if (styleNameOptions.size() == 0) {
            return;
        }

        // Check if the style name picker needs to be updated.
        String appliedStyleNames = getTextArea().getCommandManager().getStringValue(styleNameCommand);
        String previouslyAppliedStyleNames = previousValue.get(styleNameCommand);
        if (appliedStyleNames.equals(previouslyAppliedStyleNames)) {
            return;
        }
        previousValue.put(styleNameCommand, appliedStyleNames);

        // Update the style name picker.
        Set<String> styleNames = new HashSet<String>(Arrays.asList(appliedStyleNames.split("\\s+")));
        for (OptionElement option : styleNameOptions) {
            if (styleNames.contains(option.getValue())) {
                option.addClassName(SELECTED);
            } else {
                option.removeClassName(SELECTED);
            }
        }

        // Some browsers (e.g. Internet Explorer) don't update the selected options if we don't redisplay the select.
        styleNamePicker.getElement().getStyle().setDisplay(Display.NONE);
        styleNamePicker.getElement().getStyle().clearDisplay();
    }
}
