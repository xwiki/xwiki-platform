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
package org.xwiki.gwt.wysiwyg.client.plugin.link.ui;

import org.xwiki.gwt.user.client.FocusCommand;
import org.xwiki.gwt.user.client.StringUtils;
import org.xwiki.gwt.user.client.ui.wizard.AbstractInteractiveWizardStep;
import org.xwiki.gwt.user.client.ui.wizard.NavigationListener;
import org.xwiki.gwt.user.client.ui.wizard.NavigationListener.NavigationDirection;
import org.xwiki.gwt.user.client.ui.wizard.NavigationListenerCollection;
import org.xwiki.gwt.user.client.ui.wizard.SourcesNavigationEvents;
import org.xwiki.gwt.wysiwyg.client.Strings;
import org.xwiki.gwt.wysiwyg.client.plugin.link.LinkConfig;
import org.xwiki.gwt.wysiwyg.client.wiki.AttachmentReference;
import org.xwiki.gwt.wysiwyg.client.wiki.EntityLink;
import org.xwiki.gwt.wysiwyg.client.wiki.ResourceReference;
import org.xwiki.gwt.wysiwyg.client.wiki.ResourceReference.ResourceType;
import org.xwiki.gwt.wysiwyg.client.wiki.WikiServiceAsync;

import com.google.gwt.core.client.Scheduler;
import com.google.gwt.event.dom.client.KeyCodes;
import com.google.gwt.event.dom.client.KeyPressEvent;
import com.google.gwt.event.dom.client.KeyPressHandler;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.CheckBox;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.InlineLabel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.Panel;
import com.google.gwt.user.client.ui.TextBox;

/**
 * Default implementation for the link configuration parameters, such as link labels, link tooltip, or opening the link
 * in a new window or not.
 * 
 * @version $Id$
 */
public class LinkConfigWizardStep extends AbstractInteractiveWizardStep implements SourcesNavigationEvents,
    KeyPressHandler
{
    /**
     * The default style of the link configuration dialog.
     */
    public static final String DEFAULT_STYLE_NAME = "xLinkConfig";

    /**
     * The style of the information labels in this form.
     */
    public static final String INFO_LABEL_STYLE = "xInfoLabel";

    /**
     * The style of the description labels in this form.
     */
    public static final String HELP_LABEL_STYLE = "xHelpLabel";

    /**
     * The style of the error labels in this form.
     */
    public static final String ERROR_LABEL_STYLE = "xErrorMsg";

    /**
     * The style of the fields under error.
     */
    protected static final String FIELD_ERROR_STYLE = "xErrorField";

    /**
     * The entity link managed by the wizard step. This wizard step updates the configuration object attached to the
     * entity link.
     */
    private EntityLink<LinkConfig> entityLink;

    /**
     * Collection of {@link NavigationListener}s, to be notified by navigation events from this step. Used to handle
     * default buttons in this wizard step, by firing next event whenever enter key is hit in the step form.
     */
    private final NavigationListenerCollection navigationListeners = new NavigationListenerCollection();

    /**
     * The text box where the user will insert the text of the link to create.
     */
    private final TextBox labelTextBox = new TextBox();

    /**
     * The label to signal the error on the label field of this form.
     */
    private final Label labelErrorLabel = new Label();

    /**
     * The text box to get the link tooltip.
     */
    private final TextBox tooltipTextBox = new TextBox();

    /**
     * The checkbox to query about whether the link should be opened in a new window or not.
     */
    private CheckBox newWindowCheckBox;

    /**
     * The service used to parse the image reference when the link label is an image.
     */
    private final WikiServiceAsync wikiService;

    /**
     * Creates a new link configuration wizard step.
     * 
     * @param wikiService the service to be used for parsing the image reference in case the link label is an image
     */
    public LinkConfigWizardStep(WikiServiceAsync wikiService)
    {
        this.wikiService = wikiService;
        setStepTitle(Strings.INSTANCE.linkConfigTitle());

        display().addStyleName(DEFAULT_STYLE_NAME);
        setUpLabelField();
        Label tooltipLabel = new Label(Strings.INSTANCE.linkTooltipLabel());
        tooltipLabel.setStyleName(INFO_LABEL_STYLE);
        Label helpTooltipLabel = new Label(getTooltipTextBoxTooltip());
        helpTooltipLabel.setStyleName(HELP_LABEL_STYLE);
        // on enter in the textbox, submit the form
        tooltipTextBox.addKeyPressHandler(this);
        tooltipTextBox.setTitle(getTooltipTextBoxTooltip());
        display().add(tooltipLabel);
        display().add(helpTooltipLabel);
        display().add(tooltipTextBox);
        newWindowCheckBox = new CheckBox(Strings.INSTANCE.linkOpenInNewWindowLabel());
        // just add the style, because we need to be able to still detect this is a checkbox
        newWindowCheckBox.addStyleName(INFO_LABEL_STYLE);
        Label helpNewWindowLabel = new Label(Strings.INSTANCE.linkOpenInNewWindowHelpLabel());
        helpNewWindowLabel.setStyleName(HELP_LABEL_STYLE);
        display().add(newWindowCheckBox);
        display().add(helpNewWindowLabel);
    }

    /**
     * Helper function to setup the label field in this link form.
     */
    private void setUpLabelField()
    {
        Panel labelLabel = new FlowPanel();
        labelLabel.setStyleName(INFO_LABEL_STYLE);
        labelLabel.add(new InlineLabel(Strings.INSTANCE.linkLabelLabel()));
        InlineLabel mandatoryLabel = new InlineLabel(Strings.INSTANCE.mandatory());
        mandatoryLabel.addStyleName("xMandatory");
        labelLabel.add(mandatoryLabel);
        Label helpLabelLabel = new Label(getLabelTextBoxTooltip());
        helpLabelLabel.setStyleName(HELP_LABEL_STYLE);

        labelErrorLabel.addStyleName(ERROR_LABEL_STYLE);
        labelErrorLabel.setVisible(false);
        // on enter in the textbox, submit the form
        labelTextBox.addKeyPressHandler(this);
        labelTextBox.setTitle(getLabelTextBoxTooltip());
        display().add(labelLabel);
        display().add(helpLabelLabel);
        display().add(labelErrorLabel);
        display().add(labelTextBox);
    }

    /**
     * {@inheritDoc}
     */
    @SuppressWarnings("unchecked")
    public void init(Object data, final AsyncCallback< ? > callback)
    {
        entityLink = (EntityLink<LinkConfig>) data;
        LinkConfig linkConfig = entityLink.getData();
        if (linkConfig.isReadOnlyLabel()) {
            wikiService.parseLinkReference(linkConfig.getLabelText(), entityLink.getOrigin(),
                new AsyncCallback<ResourceReference>()
                {
                    public void onFailure(Throwable caught)
                    {
                        callback.onFailure(caught);
                    }

                    public void onSuccess(ResourceReference result)
                    {
                        init(result, callback);
                    }
                });
        } else {
            init(entityLink.getDestination(), callback);
        }
    }

    /**
     * Initializes the wizard step based on the underlying link configuration object. If the link label is an image the
     * UI is adjusted accordingly (the image name is displayed as the link label and the link label is read-only). If
     * the link target is an attachment we use the attachment name as the link label.
     * 
     * @param labelResourceReference a reference to the resource specified by the link label
     * @param callback the object to be notified after the wizard step has been initialized
     */
    private void init(ResourceReference labelResourceReference, AsyncCallback< ? > callback)
    {
        LinkConfig linkConfig = entityLink.getData();
        boolean useFileName =
            labelResourceReference.getType() == ResourceType.ATTACHMENT
                && (linkConfig.isReadOnlyLabel() || StringUtils.isEmpty(linkConfig.getLabel()));
        labelTextBox.setText(useFileName ? new AttachmentReference(labelResourceReference.getEntityReference())
            .getFileName() : linkConfig.getLabelText());
        labelTextBox.setEnabled(!linkConfig.isReadOnlyLabel());
        tooltipTextBox.setText(linkConfig.getTooltip() == null ? "" : linkConfig.getTooltip());
        newWindowCheckBox.setValue(linkConfig.isOpenInNewWindow());

        hideErrors();
        callback.onSuccess(null);
        setFocus();
    }

    /**
     * Sets the default focus in this wizard step.
     */
    protected void setFocus()
    {
        Scheduler.get().scheduleDeferred(new FocusCommand(labelTextBox.isEnabled() ? labelTextBox : tooltipTextBox));
    }

    /**
     * @return the labelTextBox
     */
    protected TextBox getLabelTextBox()
    {
        return labelTextBox;
    }

    /**
     * @return the tooltip for label text box
     */
    protected String getLabelTextBoxTooltip()
    {
        return Strings.INSTANCE.linkConfigLabelTextBoxTooltip();
    }

    /**
     * @return the tooltip for the tooltip text box
     */
    protected String getTooltipTextBoxTooltip()
    {
        return Strings.INSTANCE.linkConfigTooltipTextBoxTooltip();
    }

    /**
     * @return the tooltipTextBox
     */
    public TextBox getTooltipTextBox()
    {
        return tooltipTextBox;
    }

    /**
     * @return the newWindowCheckBox
     */
    public CheckBox getNewWindowCheckBox()
    {
        return newWindowCheckBox;
    }

    /**
     * {@inheritDoc}
     */
    public void onSubmit(AsyncCallback<Boolean> async)
    {
        // first reset all error labels, consider everything's fine
        hideErrors();
        // validate and save if everything's fine
        if (!validateForm()) {
            async.onSuccess(false);
            // and set the focus
            setFocus();
        } else {
            saveForm(async);
        }
    }

    /**
     * Validates this step's form and displays errors if needed.
     * 
     * @return {@code true} if the form is valid and data can be saved, {@code false} otherwise.
     */
    protected boolean validateForm()
    {
        if (labelTextBox.getText().trim().length() == 0) {
            displayLabelError(Strings.INSTANCE.linkNoLabelError());
            return false;
        }
        return true;
    }

    /**
     * Saves the form values in this step's data, to be called only when {@link #validateForm()} returns {@code true}.
     * 
     * @param callback the object to be notified after the form is saved
     */
    protected void saveForm(AsyncCallback<Boolean> callback)
    {
        LinkConfig linkConfig = entityLink.getData();
        if (!linkConfig.isReadOnlyLabel() && !labelTextBox.getText().trim().equals(linkConfig.getLabelText().trim())) {
            linkConfig.setLabel(labelTextBox.getText().trim());
            linkConfig.setLabelText(labelTextBox.getText().trim());
        }
        linkConfig.setTooltip(getTooltipTextBox().getText());
        linkConfig.setOpenInNewWindow(getNewWindowCheckBox().getValue());
        callback.onSuccess(true);
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
        // Always return the modified entity link as result of this wizard step.
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

    /**
     * Display the label error message and markers.
     * 
     * @param errorMessage the error message to display.
     */
    protected void displayLabelError(String errorMessage)
    {
        labelErrorLabel.setText(errorMessage);
        labelErrorLabel.setVisible(true);
        labelTextBox.addStyleName(FIELD_ERROR_STYLE);
    }

    /**
     * Hides the error message and markers for this dialog.
     */
    protected void hideErrors()
    {
        labelErrorLabel.setVisible(false);
        labelTextBox.removeStyleName(FIELD_ERROR_STYLE);
    }

    /**
     * @return the labelErrorLabel
     */
    protected Label getLabelErrorLabel()
    {
        return labelErrorLabel;
    }

    /**
     * @return the data configured by this wizard step
     */
    protected EntityLink<LinkConfig> getData()
    {
        return entityLink;
    }

    /**
     * @return the service used to parse and serialize entity/resource references
     */
    protected WikiServiceAsync getWikiService()
    {
        return wikiService;
    }
}
