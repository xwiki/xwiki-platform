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
package org.xwiki.gwt.wysiwyg.client.plugin.importer.ui;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;

import org.xwiki.gwt.user.client.Config;
import org.xwiki.gwt.user.client.ui.wizard.NavigationListener.NavigationDirection;
import org.xwiki.gwt.wysiwyg.client.Strings;
import org.xwiki.gwt.wysiwyg.client.plugin.importer.ImportServiceAsync;
import org.xwiki.gwt.wysiwyg.client.widget.wizard.util.AbstractFileUploadWizardStep;
import org.xwiki.gwt.wysiwyg.client.wiki.Attachment;
import org.xwiki.gwt.wysiwyg.client.wiki.WikiPageReference;
import org.xwiki.gwt.wysiwyg.client.wiki.WikiServiceAsync;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.CheckBox;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.Panel;
import com.google.gwt.user.client.ui.Widget;

/**
 * Wizard step responsible for importing the content of an office document into the wysiwyg editor.
 * 
 * @version $Id$
 * @since 2.0.1
 */
public class ImportOfficeFileWizardStep extends AbstractFileUploadWizardStep
{
    /**
     * Wysiwyg configuration object.
     */
    private Config config;

    /**
     * Result of the import operation.
     */
    private Object result;

    /**
     * Checkbox allowing the user to select whether he wants to filter out office styles or not.
     */
    private CheckBox filterStylesCheckBox;

    /**
     * Flag indicating whether an active openoffice server is available or not.
     */
    private boolean isOpenOfficeServerConnected;

    /**
     * Panel to be used as the UI of this wizard step in case we cannot locate an active openoffice server.
     */
    private Panel errorMessagePanel;

    /**
     * The component used to import office documents.
     */
    private final ImportServiceAsync importService;

    /**
     * Instantiates the office document import wizard step.
     * 
     * @param config the object used to configure this wizard step
     * @param wikiService the component used to upload office documents
     * @param importService the component used to import office documents
     */
    public ImportOfficeFileWizardStep(Config config, WikiServiceAsync wikiService, ImportServiceAsync importService)
    {
        super(wikiService);

        this.config = config;
        this.importService = importService;

        setFileHelpLabel(Strings.INSTANCE.importOfficeFileHelpLabel());

        // Add filter styles check box.
        this.filterStylesCheckBox = new CheckBox(Strings.INSTANCE.importOfficeContentFilterStylesCheckBoxLabel());
        // The filter styles check box should be checked by default.
        this.filterStylesCheckBox.setValue(true);
        getMainPanel().add(filterStylesCheckBox);

        // Read wysiwyg configuration for openoffice server availability.
        isOpenOfficeServerConnected = config.getParameter("openofficeServerConnected", "false").equals("true");
    }

    /**
     * {@inheritDoc}
     */
    public Widget display()
    {
        if (isOpenOfficeServerConnected) {
            return super.display();
        } else {
            return getErrorMessagePanel();
        }
    }

    /**
     * {@inheritDoc}
     */
    protected void onAttachmentUploaded(Attachment attach, final AsyncCallback<Boolean> async)
    {
        importService.officeToXHTML(attach, getHTMLCleaningParams(), new AsyncCallback<String>()
        {
            public void onSuccess(String result)
            {
                setResult(result);

                // Resume the wizard step submit operation.
                async.onSuccess(true);
            }

            public void onFailure(Throwable thrown)
            {
                setResult(null);

                // Display the error and avoid submit operation from continuing.
                displayError(thrown.getMessage());
                async.onSuccess(false);
            }
        });
    }

    /**
     * {@inheritDoc}
     */
    public Object getResult()
    {
        return this.result;
    }

    /**
     * Sets the result of this wizard step.
     * 
     * @param result the result.
     */
    private void setResult(Object result)
    {
        this.result = result;
    }

    /**
     * {@inheritDoc}
     */
    public String getNextStep()
    {
        return null;
    }

    /**
     * {@inheritDoc}
     */
    public String getDirectionName(NavigationDirection direction)
    {
        if (direction == NavigationDirection.FINISH) {
            return Strings.INSTANCE.importWizardImportButtonCaption();
        }
        return null;
    }

    /**
     * {@inheritDoc}
     */
    public String getStepTitle()
    {
        return Strings.INSTANCE.importOfficeFileWizardStepTitle();
    }

    /**
     * {@inheritDoc}
     */
    public EnumSet<NavigationDirection> getValidDirections()
    {
        if (isOpenOfficeServerConnected) {
            return EnumSet.of(NavigationDirection.CANCEL, NavigationDirection.FINISH);
        } else {
            return EnumSet.of(NavigationDirection.CANCEL);
        }
    }

    /**
     * Prepares the cleaning parameters map.
     * 
     * @return a {@link Map} with cleaning parameters for office importer.
     */
    protected Map<String, String> getHTMLCleaningParams()
    {
        Map<String, String> params = new HashMap<String, String>();
        if (filterStylesCheckBox.getValue()) {
            params.put("filterStyles", "strict");
        }
        // For Office2007: Office2007 generates an xhtml document (when copied) which has attributes and tags of
        // several namespaces. But the document itself doesn't contain the namespace definitions, which causes
        // the HTMLCleaner (the DomSerializer) to fail while performing it's operations. As a workaround we
        // force HTMLCleaner to avoid parsing of namespace information.
        params.put("namespacesAware", Boolean.toString(false));
        return params;
    }

    /**
     * Returns a UI widget with an error message explaining the unavailability of this import feature.
     * 
     * @return a UI panel with the given error message placed on it.
     */
    private Panel getErrorMessagePanel()
    {
        if (null == errorMessagePanel) {
            errorMessagePanel = new FlowPanel();
            Label errorMessageLabel = new Label(Strings.INSTANCE.importOfficeFileFeatureNotAvailable());
            errorMessageLabel.addStyleName("xErrorMsg");
            errorMessagePanel.add(errorMessageLabel);
        }
        return errorMessagePanel;
    }

    /**
     * {@inheritDoc}
     * 
     * @see AbstractFileUploadWizardStep#getTargetPageReference()
     */
    @Override
    protected WikiPageReference getTargetPageReference()
    {
        WikiPageReference targetPageReference = new WikiPageReference();
        targetPageReference.setWikiName(config.getParameter("wiki", "xwiki"));
        targetPageReference.setSpaceName(config.getParameter("space", "Main"));
        targetPageReference.setPageName(config.getParameter("page", "WebHome"));
        return targetPageReference;
    }
}
