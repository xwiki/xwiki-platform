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
package com.xpn.xwiki.wysiwyg.client.plugin.importer.ui;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.CheckBox;
import com.xpn.xwiki.wysiwyg.client.WysiwygService;
import com.xpn.xwiki.wysiwyg.client.editor.Strings;
import com.xpn.xwiki.wysiwyg.client.util.Attachment;
import com.xpn.xwiki.wysiwyg.client.util.Config;
import com.xpn.xwiki.wysiwyg.client.widget.wizard.NavigationListener.NavigationDirection;
import com.xpn.xwiki.wysiwyg.client.widget.wizard.util.AbstractFileUploadWizardStep;

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
     * Instantiates the office document import wizard step.
     * 
     * @param config wysiwyg configuration.
     */
    public ImportOfficeFileWizardStep(Config config)
    {
        this.config = config;
        
        // Add filter styles check box.
        this.filterStylesCheckBox = new CheckBox(Strings.INSTANCE.importOfficeContentFilterStylesCheckBoxLabel());
        getMainPanel().add(filterStylesCheckBox);
    }   

    /**
     * {@inheritDoc}
     */
    public String getPage()
    {
        return config.getParameter("page", "WebHome");
    }

    /**
     * {@inheritDoc}
     */
    public String getSpace()
    {
        return config.getParameter("space", "Main");
    }

    /**
     * {@inheritDoc}
     */
    public String getWiki()
    {
        return config.getParameter("wiki", "xwiki");
    }

    /**
     * {@inheritDoc}
     */
    protected String getFileHelpLabel()
    {
        return Strings.INSTANCE.importOfficeFileHelpLabel();
    }

    /**
     * {@inheritDoc}
     */
    protected void onAttachmentUploaded(Attachment attach, final AsyncCallback<Boolean> async)
    {

        String fullPageName = getSpace() + "." + getPage();
        WysiwygService.Singleton.getInstance().officeToXHTML(fullPageName, getHTMLCleaningParams(),
            new AsyncCallback<String>()
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
        return EnumSet.of(NavigationDirection.FINISH);
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
}
