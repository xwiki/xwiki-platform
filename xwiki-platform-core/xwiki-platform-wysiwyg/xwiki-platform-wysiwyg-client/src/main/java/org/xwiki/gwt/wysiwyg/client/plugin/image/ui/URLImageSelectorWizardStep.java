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

import org.xwiki.gwt.user.client.FocusCommand;
import org.xwiki.gwt.user.client.StringUtils;
import org.xwiki.gwt.user.client.ui.wizard.AbstractInteractiveWizardStep;
import org.xwiki.gwt.user.client.ui.wizard.NavigationListener;
import org.xwiki.gwt.user.client.ui.wizard.NavigationListener.NavigationDirection;
import org.xwiki.gwt.user.client.ui.wizard.NavigationListenerCollection;
import org.xwiki.gwt.user.client.ui.wizard.SourcesNavigationEvents;
import org.xwiki.gwt.wysiwyg.client.Strings;
import org.xwiki.gwt.wysiwyg.client.plugin.image.ImageConfig;
import org.xwiki.gwt.wysiwyg.client.wiki.EntityLink;
import org.xwiki.gwt.wysiwyg.client.wiki.ResourceReference.ResourceType;
import org.xwiki.gwt.wysiwyg.client.wiki.URIReference;

import com.google.gwt.core.client.Scheduler;
import com.google.gwt.event.dom.client.KeyCodes;
import com.google.gwt.event.dom.client.KeyUpEvent;
import com.google.gwt.event.dom.client.KeyUpHandler;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.InlineLabel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.Panel;
import com.google.gwt.user.client.ui.TextBox;

/**
 * Allows the user to select an external image by specifying its URL.
 * 
 * @version $Id$
 */
public class URLImageSelectorWizardStep extends AbstractInteractiveWizardStep implements SourcesNavigationEvents,
    KeyUpHandler
{
    /**
     * The style of the fields under error.
     */
    private static final String FIELD_ERROR_STYLE = "xErrorField";

    /**
     * The character sequence that delimits the URL protocol from the URL body.
     */
    private static final String PROTOCOL_DELIMITER = "://";

    /**
     * The entity link managed by the wizard step. This wizard step updates image reference.
     */
    private EntityLink<ImageConfig> entityLink;

    /**
     * Collection of {@link NavigationListener}s, to be notified by navigation events from this step. Used to handle
     * default buttons in this wizard step, by firing next event whenever enter key is hit in the step form.
     */
    private final NavigationListenerCollection navigationListeners = new NavigationListenerCollection();

    /**
     * The text box used to input the image source in the form of an UR.
     */
    private final TextBox source = new TextBox();

    /**
     * The label used to display source validation errors.
     */
    private final Label sourceValidationError = new Label(Strings.INSTANCE.imageExternalLocationNotSpecifiedError());

    /**
     * Creates a new wizard step that allows the user to select an external image by specifying its URL.
     */
    public URLImageSelectorWizardStep()
    {
        setStepTitle(Strings.INSTANCE.imageSelectImageTitle());

        InlineLabel mandatoryLabel = new InlineLabel(Strings.INSTANCE.mandatory());
        mandatoryLabel.addStyleName("xMandatory");

        Panel sourceLabel = new FlowPanel();
        sourceLabel.setStyleName("xInfoLabel");
        sourceLabel.add(new InlineLabel(Strings.INSTANCE.imageExternalLocationLabel()));
        sourceLabel.add(mandatoryLabel);
        display().add(sourceLabel);

        Label sourceHelpLabel = new Label(Strings.INSTANCE.imageExternalLocationHelpLabel());
        sourceHelpLabel.setStyleName("xHelpLabel");
        display().add(sourceHelpLabel);

        sourceValidationError.addStyleName("xErrorMsg");
        sourceValidationError.setVisible(false);
        display().add(sourceValidationError);

        source.setTitle(Strings.INSTANCE.imageExternalLocationLabel());
        source.addKeyUpHandler(this);
        display().add(source);

        display().addStyleName("xExternalImage");
    }

    /**
     * {@inheritDoc}
     * 
     * @see AbstractInteractiveWizardStep#getResult()
     */
    public Object getResult()
    {
        return entityLink;
    }

    /**
     * {@inheritDoc}
     * 
     * @see AbstractInteractiveWizardStep#init(Object, AsyncCallback)
     */
    @SuppressWarnings("unchecked")
    public void init(Object data, AsyncCallback< ? > cb)
    {
        entityLink = (EntityLink<ImageConfig>) data;
        String imageURL = entityLink.getData().getUrl();
        if (!StringUtils.isEmpty(imageURL) && imageURL.contains(PROTOCOL_DELIMITER)) {
            // In case the image URL doesn't include the protocol (which can happen if the user selects an internal
            // image and then opts for an external image) we leave the source input blank to force the user to enter a
            // new image location.
            source.setText(imageURL);
        }
        Scheduler.get().scheduleDeferred(new FocusCommand(source));
        cb.onSuccess(null);
    }

    /**
     * {@inheritDoc}
     * 
     * @see AbstractInteractiveWizardStep#onCancel()
     */
    public void onCancel()
    {
        entityLink = null;
        hideValidationErrors();
    }

    /**
     * {@inheritDoc}
     * 
     * @see AbstractInteractiveWizardStep#onSubmit(AsyncCallback)
     */
    public void onSubmit(AsyncCallback<Boolean> async)
    {
        if (validate()) {
            updateResult();
            async.onSuccess(true);
        } else {
            async.onSuccess(false);
        }
    }

    /**
     * Validates all input fields from this wizard step and focuses the first field with an illegal value.
     * 
     * @return {@code true} if the current input values are valid, {@code false} otherwise
     */
    private boolean validate()
    {
        hideValidationErrors();
        if (source.getText().trim().length() == 0) {
            sourceValidationError.setVisible(true);
            source.addStyleName(FIELD_ERROR_STYLE);
            Scheduler.get().scheduleDeferred(new FocusCommand(source));
            return false;
        }
        return true;
    }

    /**
     * Updates the result object returned by this wizard step and notifies the call-back object.
     */
    private void updateResult()
    {
        String imageURL = source.getText().trim();
        if (!imageURL.contains(PROTOCOL_DELIMITER)) {
            imageURL = "http://" + imageURL;
        }
        entityLink.getDestination().setType(ResourceType.URL);
        entityLink.getDestination().setEntityReference(new URIReference(imageURL).getEntityReference());
        entityLink.getData().setReference(null);
        entityLink.getData().setUrl(imageURL);
    }

    /**
     * Hides the validation errors.
     */
    private void hideValidationErrors()
    {
        sourceValidationError.setVisible(false);
        source.removeStyleName(FIELD_ERROR_STYLE);
    }

    /**
     * {@inheritDoc}
     * 
     * @see SourcesNavigationEvents#addNavigationListener(NavigationListener)
     */
    public void addNavigationListener(NavigationListener listener)
    {
        navigationListeners.add(listener);
    }

    /**
     * {@inheritDoc}
     * 
     * @see SourcesNavigationEvents#removeNavigationListener(NavigationListener)
     */
    public void removeNavigationListener(NavigationListener listener)
    {
        navigationListeners.remove(listener);
    }

    /**
     * {@inheritDoc}
     * 
     * @see KeyUpHandler#onKeyUp(KeyUpEvent)
     */
    public void onKeyUp(KeyUpEvent event)
    {
        if (event.getNativeKeyCode() == KeyCodes.KEY_ENTER) {
            navigationListeners.fireNavigationEvent(NavigationDirection.NEXT);
        }
    }
}
