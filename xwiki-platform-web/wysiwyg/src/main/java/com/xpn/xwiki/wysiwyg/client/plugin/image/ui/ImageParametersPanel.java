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
import java.util.List;

import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.Panel;
import com.google.gwt.user.client.ui.TextBox;
import com.xpn.xwiki.wysiwyg.client.editor.Strings;
import com.xpn.xwiki.wysiwyg.client.plugin.image.ImageConfig;
import com.xpn.xwiki.wysiwyg.client.widget.RadioButton;

/**
 * Builds the panel to set image parameters such as alignment, size and alternative text.
 * 
 * @version $Id$
 */
public class ImageParametersPanel extends Composite
{
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
     * Default constructor.
     */
    public ImageParametersPanel()
    {
        Panel parametersPanel = new FlowPanel();
        parametersPanel.addStyleName("xSettingsPanel");
        Label parametersLabel = new Label(Strings.INSTANCE.imageSettingsLabel());
        parametersPanel.add(parametersLabel);
        parametersLabel.addStyleName("xSettingsLabel");
        parametersPanel.add(getSizePanel());
        parametersPanel.add(getAltTextPanel());
        parametersPanel.add(getAlignmentPanel());

        initWidget(parametersPanel);
    }

    /**
     * @return the panel holding the size settings for the image
     */
    private Panel getSizePanel()
    {
        widthBox = new TextBox();
        heightBox = new TextBox();
        Label sizeLabel = new Label(Strings.INSTANCE.imageSizeLabel());
        FlowPanel sizePanel = new FlowPanel();
        sizePanel.addStyleName("xSizePanel");
        sizePanel.add(sizeLabel);
        sizePanel.add(widthBox);
        sizePanel.add(new Label("X"));
        sizePanel.add(heightBox);

        return sizePanel;
    }

    /**
     * @return the panel holding the alternative settings for the image
     */
    private Panel getAltTextPanel()
    {
        altTextBox = new TextBox();
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
        Label alignmentLabel = new Label(Strings.INSTANCE.imageAlignmentLabel());
        alignmentLabel.addStyleName("xAlignLabel");
        String alignRadioGroup = "alignment";
        alignmentPanel.add(alignmentLabel);
        // radio buttons for alignments
        alignmentPanel.add(getHorizontalAlignmentPanel(alignRadioGroup));
        alignmentPanel.add(getVerticalAlignmentPanel(alignRadioGroup));
        return alignmentPanel;
    }

    /**
     * @return the panel holding the horizontal alignment settings for the image
     * @param alignRadioGroup the name of the alignment radio group.
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
        RadioButton centerRadio = new RadioButton(alignRadioGroup, Strings.INSTANCE.imageAlignCenterLabel());
        centerRadio.setValue(ImageConfig.ImageAlignment.CENTER.toString());
        RadioButton rightRadio = new RadioButton(alignRadioGroup, Strings.INSTANCE.imageAlignRightLabel());
        rightRadio.setValue(ImageConfig.ImageAlignment.RIGHT.toString());
        alignmentOptions.add(leftRadio);
        alignmentOptions.add(centerRadio);
        alignmentOptions.add(rightRadio);
        hAlignPanel.add(leftRadio);
        hAlignPanel.add(centerRadio);
        hAlignPanel.add(rightRadio);

        return hAlignPanel;
    }

    /**
     * @return the panel holding the vertical alignment settings for the image
     * @param alignRadioGroup the name of the alignment radio group.
     */
    private Panel getVerticalAlignmentPanel(String alignRadioGroup)
    {
        FlowPanel vAlignPanel = new FlowPanel();
        Label vAlignLabel = new Label(Strings.INSTANCE.imageVerticalAlignmentLabel());
        vAlignPanel.addStyleName("xVAlignPanel");
        vAlignPanel.add(vAlignLabel);
        RadioButton topRadio = new RadioButton(alignRadioGroup, Strings.INSTANCE.imageAlignTopLabel());
        topRadio.setValue(ImageConfig.ImageAlignment.TOP.toString());
        RadioButton middleRadio = new RadioButton(alignRadioGroup, Strings.INSTANCE.imageAlignMiddleLabel());
        middleRadio.setValue(ImageConfig.ImageAlignment.MIDDLE.toString());
        RadioButton bottomRadio = new RadioButton(alignRadioGroup, Strings.INSTANCE.imageAlignBottomLabel());
        bottomRadio.setValue(ImageConfig.ImageAlignment.BOTTOM.toString());
        alignmentOptions.add(topRadio);
        alignmentOptions.add(middleRadio);
        alignmentOptions.add(bottomRadio);
        vAlignPanel.add(topRadio);
        vAlignPanel.add(middleRadio);
        vAlignPanel.add(bottomRadio);

        return vAlignPanel;
    }

    /**
     * Sets the default alternative text for this image settings.
     * 
     * @param altText the string to be used as the default alternative text for the edited image.
     */
    public void setDefaultAltText(String altText)
    {
        if (altTextBox != null) {
            altTextBox.setText(altText);
        }
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
     * @return the user set width for this image
     */
    public String getWidth()
    {
        return getValueOf(this.widthBox);
    }

    /**
     * @return the user set height for this image
     */
    public String getHeight()
    {
        return getValueOf(this.heightBox);
    }

    /**
     * @return the specified alternative text
     */
    public String getAltText()
    {
        return getValueOf(altTextBox);
    }

    /**
     * @param textbox the textbox to get value from
     * @return the value of the specified text box, or null if no value was specified.
     */
    private String getValueOf(TextBox textbox)
    {
        String value = textbox.getText().trim();
        if (value.length() == 0) {
            return null;
        }
        return value;
    }

    /**
     * Handles showing the parameters panel. Currently we need to set the focus in the size input when this panel is
     * shown.
     */
    public void onShow()
    {
        widthBox.setFocus(true);
    }
}
