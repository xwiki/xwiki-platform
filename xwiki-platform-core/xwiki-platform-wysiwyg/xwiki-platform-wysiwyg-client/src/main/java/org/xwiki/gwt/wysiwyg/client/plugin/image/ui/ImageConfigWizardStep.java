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
package org.xwiki.gwt.wysiwyg.client.plugin.image.ui;

import java.util.ArrayList;
import java.util.List;

import org.xwiki.gwt.user.client.FocusCommand;
import org.xwiki.gwt.user.client.StringUtils;
import org.xwiki.gwt.user.client.ui.wizard.AbstractInteractiveWizardStep;
import org.xwiki.gwt.user.client.ui.wizard.NavigationListener;
import org.xwiki.gwt.user.client.ui.wizard.NavigationListener.NavigationDirection;
import org.xwiki.gwt.user.client.ui.wizard.NavigationListenerCollection;
import org.xwiki.gwt.user.client.ui.wizard.SourcesNavigationEvents;
import org.xwiki.gwt.wysiwyg.client.Strings;
import org.xwiki.gwt.wysiwyg.client.plugin.image.ImageConfig;
import org.xwiki.gwt.wysiwyg.client.wiki.AttachmentReference;
import org.xwiki.gwt.wysiwyg.client.wiki.EntityLink;
import org.xwiki.gwt.wysiwyg.client.wiki.URIReference;

import com.google.gwt.core.client.Scheduler;
import com.google.gwt.event.dom.client.KeyCodes;
import com.google.gwt.event.dom.client.KeyPressEvent;
import com.google.gwt.event.dom.client.KeyPressHandler;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.Panel;
import com.google.gwt.user.client.ui.RadioButton;
import com.google.gwt.user.client.ui.TextBox;

/**
 * Wizard step to configure the image parameters.
 * 
 * @version $Id$
 */
public class ImageConfigWizardStep extends AbstractInteractiveWizardStep implements KeyPressHandler,
    SourcesNavigationEvents
{
    /**
     * The style for the information labels.
     */
    private static final String INFO_LABEL_STYLE = "xInfoLabel";

    /**
     * The style for the help labels.
     */
    private static final String HELP_LABEL_STYLE = "xHelpLabel";

    /**
     * The entity link managed by the wizard step. This wizard step updates the configuration object attached to the
     * entity link.
     */
    private EntityLink<ImageConfig> entityLink;

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
     * Default constructor.
     */
    public ImageConfigWizardStep()
    {
        setStepTitle(Strings.INSTANCE.imageConfigTitle());

        display().addStyleName("xImageConfig");
        display().add(getSizePanel());
        display().add(getAltTextPanel());
        display().add(getAlignmentPanel());
    }

    /**
     * @return the panel holding the size settings for the image
     */
    private Panel getSizePanel()
    {
        widthBox = new TextBox();
        widthBox.addKeyPressHandler(this);
        heightBox = new TextBox();
        heightBox.addKeyPressHandler(this);
        FlowPanel sizePanel = new FlowPanel();
        sizePanel.addStyleName("xSizePanel");
        Label widthLabel = new Label(Strings.INSTANCE.imageWidthLabel());
        widthLabel.setStyleName(INFO_LABEL_STYLE);
        Label widthHelpLabel = new Label(Strings.INSTANCE.imageWidthHelpLabel());
        widthHelpLabel.setStyleName(HELP_LABEL_STYLE);
        sizePanel.add(widthLabel);
        sizePanel.add(widthHelpLabel);
        sizePanel.add(widthBox);
        Label heightLabel = new Label(Strings.INSTANCE.imageHeightLabel());
        heightLabel.setStyleName(INFO_LABEL_STYLE);
        Label heightHelpLabel = new Label(Strings.INSTANCE.imageHeightHelpLabel());
        heightHelpLabel.setStyleName(HELP_LABEL_STYLE);
        sizePanel.add(heightLabel);
        sizePanel.add(heightHelpLabel);
        sizePanel.add(heightBox);

        return sizePanel;
    }

    /**
     * @return the panel holding the alternative text settings for the image
     */
    private Panel getAltTextPanel()
    {
        altTextBox = new TextBox();
        altTextBox.addKeyPressHandler(this);
        Label altTextLabel = new Label(Strings.INSTANCE.imageAltTextLabel());
        altTextLabel.setStyleName(INFO_LABEL_STYLE);
        Label altTextHelpLabel = new Label(Strings.INSTANCE.imageAltTextHelpLabel());
        altTextHelpLabel.setStyleName(HELP_LABEL_STYLE);
        FlowPanel altTextPanel = new FlowPanel();
        altTextPanel.addStyleName("xAltPanel");
        altTextPanel.add(altTextLabel);
        altTextPanel.add(altTextHelpLabel);
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
        hAlignLabel.setStyleName(INFO_LABEL_STYLE);
        Label hAlignHelpLabel = new Label(Strings.INSTANCE.imageHorizontalAlignmentHelpLabel());
        hAlignHelpLabel.setStyleName(HELP_LABEL_STYLE);
        hAlignPanel.addStyleName("xHAlignPanel");
        hAlignPanel.add(hAlignLabel);
        hAlignPanel.add(hAlignHelpLabel);
        alignmentOptions = new ArrayList<RadioButton>();
        RadioButton leftRadio = new RadioButton(alignRadioGroup, Strings.INSTANCE.imageAlignLeftLabel());
        leftRadio.setFormValue(ImageConfig.ImageAlignment.LEFT.toString());
        leftRadio.addKeyPressHandler(this);
        RadioButton centerRadio = new RadioButton(alignRadioGroup, Strings.INSTANCE.imageAlignCenterLabel());
        centerRadio.setFormValue(ImageConfig.ImageAlignment.CENTER.toString());
        centerRadio.addKeyPressHandler(this);
        RadioButton rightRadio = new RadioButton(alignRadioGroup, Strings.INSTANCE.imageAlignRightLabel());
        rightRadio.setFormValue(ImageConfig.ImageAlignment.RIGHT.toString());
        rightRadio.addKeyPressHandler(this);
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
        vAlignLabel.setStyleName(INFO_LABEL_STYLE);
        Label vAlignHelpLabel = new Label(Strings.INSTANCE.imageVerticalAlignmentHelpLabel());
        vAlignHelpLabel.setStyleName(HELP_LABEL_STYLE);
        vAlignPanel.addStyleName("xVAlignPanel");
        vAlignPanel.add(vAlignLabel);
        vAlignPanel.add(vAlignHelpLabel);
        RadioButton topRadio = new RadioButton(alignRadioGroup, Strings.INSTANCE.imageAlignTopLabel());
        topRadio.setFormValue(ImageConfig.ImageAlignment.TOP.toString());
        topRadio.addKeyPressHandler(this);
        RadioButton middleRadio = new RadioButton(alignRadioGroup, Strings.INSTANCE.imageAlignMiddleLabel());
        middleRadio.setFormValue(ImageConfig.ImageAlignment.MIDDLE.toString());
        middleRadio.addKeyPressHandler(this);
        RadioButton bottomRadio = new RadioButton(alignRadioGroup, Strings.INSTANCE.imageAlignBottomLabel());
        bottomRadio.setFormValue(ImageConfig.ImageAlignment.BOTTOM.toString());
        bottomRadio.addKeyPressHandler(this);
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
            if (rb.getValue()) {
                return ImageConfig.ImageAlignment.valueOf(rb.getFormValue());
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
            if (rb.getFormValue().equals(alignValue)) {
                rb.setValue(true);
            } else {
                rb.setValue(false);
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @SuppressWarnings("unchecked")
    public void init(Object data, AsyncCallback< ? > cb)
    {
        // Store the data received as parameter.
        entityLink = (EntityLink<ImageConfig>) data;
        // Set the step configuration according to the received data.
        widthBox.setText(entityLink.getData().getWidth());
        heightBox.setText(entityLink.getData().getHeight());
        setImageAlignment(entityLink.getData().getAlignment());

        String altText = entityLink.getData().getAltText();
        if (StringUtils.isEmpty(altText)) {
            switch (entityLink.getDestination().getEntityReference().getType()) {
                case ATTACHMENT:
                    altText = new AttachmentReference(entityLink.getDestination().getEntityReference()).getFileName();
                    break;
                case EXTERNAL:
                    altText = new URIReference(entityLink.getDestination().getEntityReference()).getURI();
                    break;
                default:
                    altText = "";
                    break;
            }
        }
        altTextBox.setText(altText);

        cb.onSuccess(null);
        Scheduler.get().scheduleDeferred(new FocusCommand(widthBox));
    }

    /**
     * {@inheritDoc}
     */
    public void onSubmit(AsyncCallback<Boolean> async)
    {
        String altText = altTextBox.getText().trim();
        AttachmentReference imageReference = new AttachmentReference(entityLink.getDestination().getEntityReference());
        entityLink.getData().setAltText(altText.length() > 0 ? altText : imageReference.getFileName());
        entityLink.getData().setWidth(widthBox.getText().trim());
        entityLink.getData().setHeight(heightBox.getText().trim());
        ImageConfig.ImageAlignment alignment = getSelectedAlignment();
        entityLink.getData().setAlignment(alignment);
        async.onSuccess(true);
    }

    /**
     * {@inheritDoc}
     */
    public void onCancel()
    {
    }

    /**
     * {@inheritDoc}
     */
    public Object getResult()
    {
        return entityLink;
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
     * 
     * @see KeyPressHandler#onKeyPress(KeyPressEvent)
     */
    public void onKeyPress(KeyPressEvent event)
    {
        if (event.getNativeEvent().getKeyCode() == KeyCodes.KEY_ENTER) {
            // fire the event for the default direction
            navigationListeners.fireNavigationEvent(getDefaultDirection());
        }
    }
}
