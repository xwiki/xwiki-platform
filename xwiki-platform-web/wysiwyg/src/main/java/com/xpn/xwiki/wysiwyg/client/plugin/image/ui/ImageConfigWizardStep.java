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
package com.xpn.xwiki.wysiwyg.client.plugin.image.ui;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.KeyboardListener;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.Panel;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.Widget;
import com.xpn.xwiki.wysiwyg.client.editor.Strings;
import com.xpn.xwiki.wysiwyg.client.plugin.image.ImageConfig;
import com.xpn.xwiki.wysiwyg.client.widget.RadioButton;
import com.xpn.xwiki.wysiwyg.client.widget.wizard.NavigationListener;
import com.xpn.xwiki.wysiwyg.client.widget.wizard.NavigationListenerCollection;
import com.xpn.xwiki.wysiwyg.client.widget.wizard.SourcesNavigationEvents;
import com.xpn.xwiki.wysiwyg.client.widget.wizard.WizardStep;
import com.xpn.xwiki.wysiwyg.client.widget.wizard.NavigationListener.NavigationDirection;

/**
 * Wizard step to configure the image parameters.
 * 
 * @version $Id$
 */
public class ImageConfigWizardStep implements WizardStep, KeyboardListener, SourcesNavigationEvents
{
    /**
     * The image data to be edited by this wizard step.
     */
    private ImageConfig imageData;

    /**
     * Collection of {@link NavigationListener}s, to be notified by navigation events from this step. Used to handle
     * default buttons in this wizard step, by firing next event whenever enter key is hit in the step form.
     */
    private final NavigationListenerCollection navigationListeners = new NavigationListenerCollection();

    /**
     * List of radio buttons for the alignment setting.
     */
    private List<RadioButton> alignmentOptions;

    /**
     * Image width textbox.
     */
    private TextBox widthBox;

    /**
     * Image height textbox.
     */
    private TextBox heightBox;

    /**
     * Alternative text textbox.
     */
    private TextBox altTextBox;

    /**
     * The panel holding the input for the label of the built link.
     */
    private final Panel mainPanel = new FlowPanel();

    /**
     * Default constructor.
     */
    public ImageConfigWizardStep()
    {
        mainPanel.addStyleName("xImageConfig");
        mainPanel.add(getSizePanel());
        mainPanel.add(getAltTextPanel());
        mainPanel.add(getAlignmentPanel());
    }

    /**
     * @return the panel holding the size settings for the image
     */
    private Panel getSizePanel()
    {
        widthBox = new TextBox();
        widthBox.addKeyboardListener(this);
        heightBox = new TextBox();
        heightBox.addKeyboardListener(this);
        FlowPanel sizePanel = new FlowPanel();
        sizePanel.addStyleName("xSizePanel");
        sizePanel.add(new Label(Strings.INSTANCE.imageWidthLabel()));
        sizePanel.add(widthBox);
        sizePanel.add(new Label(Strings.INSTANCE.imageHeightLabel()));
        sizePanel.add(heightBox);

        return sizePanel;
    }

    /**
     * @return the panel holding the alternative text settings for the image
     */
    private Panel getAltTextPanel()
    {
        altTextBox = new TextBox();
        altTextBox.addKeyboardListener(this);
        Label altTextLabel = new Label(Strings.INSTANCE.imageAltTextLabel());
        FlowPanel altTextPanel = new FlowPanel();
        altTextPanel.addStyleName("xAltPanel");
        altTextPanel.add(altTextLabel);
        altTextPanel.add(altTextBox);

        return altTextPanel;
    }

    /**
     * @return the panel holding the alignment settings for the image
     */
    private Panel getAlignmentPanel()
    {
        Panel alignmentPanel = new FlowPanel();
        String alignRadioGroup = "alignment";
        alignmentPanel.add(getHorizontalAlignmentPanel(alignRadioGroup));
        alignmentPanel.add(getVerticalAlignmentPanel(alignRadioGroup));
        return alignmentPanel;
    }

    /**
     * @param alignRadioGroup the name of the alignment radio group.
     * @return the panel holding the horizontal alignment settings for the image
     */
    private Panel getHorizontalAlignmentPanel(String alignRadioGroup)
    {
        FlowPanel hAlignPanel = new FlowPanel();
        Label hAlignLabel = new Label(Strings.INSTANCE.imageHorizontalAlignmentLabel());
        hAlignPanel.addStyleName("xHAlignPanel");
        hAlignPanel.add(hAlignLabel);
        alignmentOptions = new ArrayList<RadioButton>();
        RadioButton leftRadio = new RadioButton(alignRadioGroup, Strings.INSTANCE.imageAlignLeftLabel());
        leftRadio.setValue(ImageConfig.ImageAlignment.LEFT.toString());
        leftRadio.addKeyboardListener(this);
        RadioButton centerRadio = new RadioButton(alignRadioGroup, Strings.INSTANCE.imageAlignCenterLabel());
        centerRadio.setValue(ImageConfig.ImageAlignment.CENTER.toString());
        centerRadio.addKeyboardListener(this);
        RadioButton rightRadio = new RadioButton(alignRadioGroup, Strings.INSTANCE.imageAlignRightLabel());
        rightRadio.setValue(ImageConfig.ImageAlignment.RIGHT.toString());
        rightRadio.addKeyboardListener(this);
        alignmentOptions.add(leftRadio);
        alignmentOptions.add(centerRadio);
        alignmentOptions.add(rightRadio);
        hAlignPanel.add(leftRadio);
        hAlignPanel.add(centerRadio);
        hAlignPanel.add(rightRadio);

        return hAlignPanel;
    }

    /**
     * @param alignRadioGroup the name of the alignment radio group.
     * @return the panel holding the vertical alignment settings for the image
     */
    private Panel getVerticalAlignmentPanel(String alignRadioGroup)
    {
        FlowPanel vAlignPanel = new FlowPanel();
        Label vAlignLabel = new Label(Strings.INSTANCE.imageVerticalAlignmentLabel());
        vAlignPanel.addStyleName("xVAlignPanel");
        vAlignPanel.add(vAlignLabel);
        RadioButton topRadio = new RadioButton(alignRadioGroup, Strings.INSTANCE.imageAlignTopLabel());
        topRadio.setValue(ImageConfig.ImageAlignment.TOP.toString());
        topRadio.addKeyboardListener(this);
        RadioButton middleRadio = new RadioButton(alignRadioGroup, Strings.INSTANCE.imageAlignMiddleLabel());
        middleRadio.setValue(ImageConfig.ImageAlignment.MIDDLE.toString());
        middleRadio.addKeyboardListener(this);
        RadioButton bottomRadio = new RadioButton(alignRadioGroup, Strings.INSTANCE.imageAlignBottomLabel());
        bottomRadio.setValue(ImageConfig.ImageAlignment.BOTTOM.toString());
        bottomRadio.addKeyboardListener(this);
        alignmentOptions.add(topRadio);
        alignmentOptions.add(middleRadio);
        alignmentOptions.add(bottomRadio);
        vAlignPanel.add(topRadio);
        vAlignPanel.add(middleRadio);
        vAlignPanel.add(bottomRadio);

        return vAlignPanel;
    }

    /**
     * @return the selected image alignment
     */
    public ImageConfig.ImageAlignment getSelectedAlignment()
    {
        for (RadioButton rb : alignmentOptions) {
            if (rb.isChecked()) {
                return ImageConfig.ImageAlignment.valueOf(rb.getValue());
            }
        }
        return null;
    }

    /**
     * Sets the passed alignment in the image alignment radio set.
     * 
     * @param alignment the alignment to set
     */
    protected void setImageAlignment(ImageConfig.ImageAlignment alignment)
    {
        String alignValue = alignment != null ? alignment.toString() : "";
        for (RadioButton rb : alignmentOptions) {
            if (rb.getValue().equals(alignValue)) {
                rb.setChecked(true);
            } else {
                rb.setChecked(false);
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    public void init(Object data, AsyncCallback< ? > cb)
    {
        // store the data received as parameter
        imageData = (ImageConfig) data;
        // set the step configuration according to the received config data
        widthBox.setText(imageData.getWidth());
        heightBox.setText(imageData.getHeight());
        setImageAlignment(imageData.getAlignment());
        altTextBox.setText(imageData.getAltText());
        cb.onSuccess(null);
    }

    /**
     * {@inheritDoc}
     */
    public Widget display()
    {
        return mainPanel;
    }

    /**
     * {@inheritDoc}
     */
    public void onSubmit(AsyncCallback<Boolean> async)
    {
        imageData.setAltText(altTextBox.getText().trim());
        imageData.setWidth(widthBox.getText().trim());
        imageData.setHeight(heightBox.getText().trim());
        ImageConfig.ImageAlignment alignment = getSelectedAlignment();
        imageData.setAlignment(alignment);
        async.onSuccess(true);
    }

    /**
     * {@inheritDoc}
     */
    public void onCancel(AsyncCallback<Boolean> async)
    {
        async.onSuccess(true);
    }

    /**
     * {@inheritDoc}
     */
    public Object getResult()
    {
        return imageData;
    }

    /**
     * {@inheritDoc}
     */
    public String getNextStep()
    {
        // this is the last step in the wizard.
        return null;
    }

    /**
     * {@inheritDoc}
     */
    public String getStepTitle()
    {
        return Strings.INSTANCE.imageConfigTitle();
    }

    /**
     * {@inheritDoc}. Configure this as the last wizard step, by default, allowing to finish, cancel or go to previous
     * step if the navigation stack is not empty at this point.
     */
    public EnumSet<NavigationDirection> getValidDirections()
    {
        return EnumSet.of(NavigationDirection.FINISH, NavigationDirection.CANCEL, NavigationDirection.PREVIOUS);
    }

    /**
     * {@inheritDoc}
     */
    public String getDirectionName(NavigationDirection direction)
    {
        switch (direction) {
            case FINISH:
                return Strings.INSTANCE.imageCreateImageButton();
            default:
                return null;
        }
    }

    /**
     * @return the default navigation direction, to be fired automatically when enter is hit in an input in the form of
     *         this configuration wizard step. To be overridden by subclasses to provide the specific direction to be
     *         followed.
     */
    public NavigationDirection getDefaultDirection()
    {
        return NavigationDirection.FINISH;
    }

    /**
     * {@inheritDoc}
     */
    public void addNavigationListener(NavigationListener listener)
    {
        navigationListeners.add(listener);
    }

    /**
     * {@inheritDoc}
     */
    public void removeNavigationListener(NavigationListener listener)
    {
        navigationListeners.remove(listener);
    }

    /**
     * {@inheritDoc}
     */
    public void onKeyPress(Widget sender, char keyCode, int modifiers)
    {
        if (keyCode == KEY_ENTER) {
            // fire the event for the default direction
            navigationListeners.fireNavigationEvent(getDefaultDirection());
        }
    }

    /**
     * {@inheritDoc}
     */
    public void onKeyUp(Widget sender, char keyCode, int modifiers)
    {
        // nothing
    }

    /**
     * {@inheritDoc}
     */
    public void onKeyDown(Widget sender, char keyCode, int modifiers)
    {
        // nothing
    }
}
