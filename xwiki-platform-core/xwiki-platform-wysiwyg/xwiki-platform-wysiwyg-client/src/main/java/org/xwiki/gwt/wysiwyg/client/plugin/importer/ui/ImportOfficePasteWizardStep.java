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

import org.xwiki.gwt.user.client.FocusCommand;
import org.xwiki.gwt.user.client.ui.rta.RichTextArea;
import org.xwiki.gwt.user.client.ui.wizard.AbstractInteractiveWizardStep;
import org.xwiki.gwt.user.client.ui.wizard.NavigationListener.NavigationDirection;
import org.xwiki.gwt.wysiwyg.client.Strings;
import org.xwiki.gwt.wysiwyg.client.plugin.importer.ImportServiceAsync;
import org.xwiki.gwt.wysiwyg.client.plugin.importer.PasteFilter;

import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.event.dom.client.LoadEvent;
import com.google.gwt.event.dom.client.LoadHandler;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.CheckBox;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.InlineLabel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.Panel;

/**
 * Wizard step responsible for importing copy-pasted office content.
 * 
 * @version $Id$
 * @since 2.0.1
 */
public class ImportOfficePasteWizardStep extends AbstractInteractiveWizardStep implements LoadHandler
{
    /**
     * The text area where the user can paste his content.
     */
    private RichTextArea textArea;

    /**
     * Storage for the result of the import operation.
     */
    private Object result;

    /**
     * Checkbox allowing the user to select whether he wants to filter out office styles or not.
     */
    private CheckBox filterStylesCheckBox;

    /**
     * The component used to clean content copy&pasted from office documents.
     */
    private final ImportServiceAsync importService;

    /**
     * The object used to filter the pasted document before cleaning it on the server.
     */
    private final PasteFilter pasteFilter = GWT.create(PasteFilter.class);

    /**
     * Creates an instance of {@link ImportOfficePasteWizardStep}.
     * 
     * @param importService the component used to clean content copy and pasted from office documents
     */
    public ImportOfficePasteWizardStep(ImportServiceAsync importService)
    {
        this.importService = importService;

        setStepTitle(Strings.INSTANCE.importOfficePasteWizardStepTitle());
        setValidDirections(EnumSet.of(NavigationDirection.CANCEL, NavigationDirection.FINISH));
        setDirectionName(NavigationDirection.FINISH, Strings.INSTANCE.importWizardImportButtonCaption());

        // Info label.
        Panel infoLabel = new FlowPanel();
        infoLabel.setStyleName("xInfoLabel");
        infoLabel.add(new InlineLabel(Strings.INSTANCE.importOfficePasteInfoLabel()));
        InlineLabel mandatoryLabel = new InlineLabel(Strings.INSTANCE.mandatory());
        mandatoryLabel.addStyleName("xMandatory");
        infoLabel.add(mandatoryLabel);
        display().add(infoLabel);

        // Help label.
        Label helpLabel = new Label(Strings.INSTANCE.importOfficePasteHelpLabel());
        helpLabel.setStyleName("xHelpLabel");
        display().add(helpLabel);

        // Text area panel.
        textArea = new RichTextArea();
        textArea.addStyleName("xImportOfficeContentEditor");
        textArea.addLoadHandler(this);
        display().add(textArea);

        // Filter styles check box.
        this.filterStylesCheckBox = new CheckBox(Strings.INSTANCE.importOfficeContentFilterStylesCheckBoxLabel());
        // The filter styles check box should be checked by default.
        this.filterStylesCheckBox.setValue(true);
        display().add(filterStylesCheckBox);
    }

    @Override
    public void init(Object data, AsyncCallback< ? > cb)
    {
        textArea.setHTML("");
        cb.onSuccess(null);
    }

    @Override
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

    @Override
    public void onCancel()
    {
        textArea.setHTML("");
    }

    @Override
    public void onSubmit(final AsyncCallback<Boolean> async)
    {
        pasteFilter.filter(textArea.getDocument());
        String officeHTML = textArea.getHTML();
        if (officeHTML.trim().equals("")) {
            async.onSuccess(false);
        } else {
            importService.cleanOfficeHTML(officeHTML, "wysiwyg", getHTMLCleaningParams(), new AsyncCallback<String>()
            {
                @Override
                public void onSuccess(String result)
                {
                    setResult(result);
                    async.onSuccess(true);
                }

                @Override
                public void onFailure(Throwable thrown)
                {
                    async.onFailure(thrown);
                }
            });
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

    @Override
    public void onLoad(LoadEvent event)
    {
        if (event.getSource() == textArea) {
            // The rich text area where the content is pasted is reloaded each time this wizard step is displayed so we
            // use the load event to focus the text area. Note that we can't focus if the rich text area isn't loaded.
            // We use a deferred command in case the rich text area is loaded synchronously.
            Scheduler.get().scheduleDeferred(new FocusCommand(textArea));
        }
    }
}
