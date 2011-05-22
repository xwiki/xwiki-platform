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
import com.google.gwt.user.client.ui.Label;

/**
 * Wizard step responsible for importing the content of an office document into the wysiwyg editor.
 * 
 * @version $Id$
 * @since 2.0.1
 */
public class ImportOfficeFileWizardStep extends AbstractFileUploadWizardStep
{
    /**
     * Result of the import operation.
     */
    private Object result;

    /**
     * Checkbox allowing the user to select whether he wants to filter out office styles or not.
     */
    private CheckBox filterStylesCheckBox;

    /**
     * The component used to import office documents.
     */
    private final ImportServiceAsync importService;

    /**
     * The page where the office document or text will be imported.
     */
    private final WikiPageReference targetPageReference;

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

        this.importService = importService;

        // Read WYSIWYG configuration for OpenOffice server availability.
        boolean isOpenOfficeServerConnected =
            Boolean.valueOf(config.getParameter("openofficeServerConnected", "false"));

        setStepTitle(Strings.INSTANCE.importOfficeFileWizardStepTitle());
        setDirectionName(NavigationDirection.FINISH, Strings.INSTANCE.importWizardImportButtonCaption());
        setValidDirections(isOpenOfficeServerConnected ? EnumSet.of(NavigationDirection.CANCEL,
            NavigationDirection.FINISH) : EnumSet.of(NavigationDirection.CANCEL));

        targetPageReference = new WikiPageReference();
        targetPageReference.setWikiName(config.getParameter("wiki", "xwiki"));
        targetPageReference.setSpaceName(config.getParameter("space", "Main"));
        targetPageReference.setPageName(config.getParameter("page", "WebHome"));

        if (isOpenOfficeServerConnected) {
            setFileHelpLabel(Strings.INSTANCE.importOfficeFileHelpLabel());
            // Add filter styles check box.
            this.filterStylesCheckBox = new CheckBox(Strings.INSTANCE.importOfficeContentFilterStylesCheckBoxLabel());
            // The filter styles check box should be checked by default.
            this.filterStylesCheckBox.setValue(true);
            display().add(filterStylesCheckBox);
        } else {
            Label errorMessageLabel = new Label(Strings.INSTANCE.importOfficeFileFeatureNotAvailable());
            errorMessageLabel.addStyleName("xErrorMsg");
            // Display only the error message.
            display().clear();
            display().add(errorMessageLabel);
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
                ImportOfficeFileWizardStep.this.result = result;
                // Resume the wizard step submit operation.
                async.onSuccess(true);
            }

            public void onFailure(Throwable thrown)
            {
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
     * {@inheritDoc}
     * 
     * @see AbstractFileUploadWizardStep#getTargetPageReference()
     */
    @Override
    protected WikiPageReference getTargetPageReference()
    {
        return targetPageReference;
    }
}
