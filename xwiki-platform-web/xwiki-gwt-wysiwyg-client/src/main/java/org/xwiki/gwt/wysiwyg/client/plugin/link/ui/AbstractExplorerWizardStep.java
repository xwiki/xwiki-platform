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

import org.xwiki.gwt.user.client.ui.VerticalResizePanel;
import org.xwiki.gwt.user.client.ui.wizard.NavigationListener;
import org.xwiki.gwt.user.client.ui.wizard.NavigationListenerCollection;
import org.xwiki.gwt.user.client.ui.wizard.SourcesNavigationEvents;
import org.xwiki.gwt.user.client.ui.wizard.NavigationListener.NavigationDirection;
import org.xwiki.gwt.wysiwyg.client.plugin.link.LinkConfig;
import org.xwiki.gwt.wysiwyg.client.widget.explorer.XWikiExplorer;
import org.xwiki.gwt.wysiwyg.client.widget.explorer.ds.WikiDataSource;
import org.xwiki.gwt.wysiwyg.client.widget.wizard.util.AbstractSelectorWizardStep;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.Widget;
import com.smartgwt.client.widgets.events.KeyPressEvent;
import com.smartgwt.client.widgets.events.KeyPressHandler;
import com.smartgwt.client.widgets.grid.events.RecordDoubleClickEvent;
import com.smartgwt.client.widgets.grid.events.RecordDoubleClickHandler;

/**
 * Wizard step to provide an interface to selecting a wiki resource, using an {@link XWikiExplorer}.
 * 
 * @version $Id$
 */
public abstract class AbstractExplorerWizardStep extends AbstractSelectorWizardStep<LinkConfig> implements
    SourcesNavigationEvents, RecordDoubleClickHandler, KeyPressHandler
{
    /**
     * The style of the fields under error.
     */
    protected static final String FIELD_ERROR_STYLE = "xErrorField";

    /**
     * The xwiki tree explorer, used to select the page or file to link to.
     */
    private XWikiExplorer explorer;

    /**
     * The panel to hold the xwiki explorer.
     */
    private final VerticalResizePanel mainPanel = new VerticalResizePanel();

    /**
     * The label to display the error on submission of the wizard step form.
     */
    private final Label errorLabel = new Label();

    /**
     * The collection of listeners to launch navigation events to, when an item in the tree displayed by this step is
     * double clicked.
     */
    private NavigationListenerCollection listeners = new NavigationListenerCollection();

    /**
     * Builds a {@link AbstractExplorerWizardStep} from the passed settings.
     * 
     * @param addPage specifies whether the wiki explorer should show the option to add a page
     * @param showAttachments specifies whether the wiki explorer should show the attached files for pages
     * @param addAttachments specifies whether the wiki explorer should show the option to add an attachment
     * @param defaultSelection the default selection of the wiki explorer displayed by this step
     */
    public AbstractExplorerWizardStep(boolean addPage, boolean showAttachments, boolean addAttachments,
        String defaultSelection)
    {
        this(addPage, showAttachments, addAttachments, defaultSelection, 455, 305);
    }

    /**
     * Builds a {@link AbstractExplorerWizardStep} from the passed settings, with parameters for size. <br />
     * FIXME: remove the size parameters when the explorer will be correctly sizable from CSS.
     * 
     * @param addPage specifies whether the wiki explorer should show the option to add a page
     * @param showAttachments specifies whether the wiki explorer should show the attached files for pages
     * @param addAttachments specifies whether the wiki explorer should show the option to add an attachment
     * @param defaultSelection the default selection of the wiki explorer displayed by this step
     * @param width explorer width in pixels
     * @param height explorer height in pixels
     */
    protected AbstractExplorerWizardStep(boolean addPage, boolean showAttachments, boolean addAttachments,
        String defaultSelection, int width, int height)
    {
        explorer = new XWikiExplorer();
        explorer.setDisplayLinks(false);
        // display the new page option
        explorer.setDisplayAddPage(addPage);
        explorer.setDisplayAddPageOnTop(true);
        // no attachments here
        explorer.setDisplayAttachments(showAttachments);
        explorer.setDisplayAddAttachment(showAttachments && addAttachments);
        explorer.setDisplayAddAttachmentOnTop(true);
        explorer.setDisplayAttachmentsWhenEmpty(showAttachments && addAttachments);
        String sizeUnit = "px";
        explorer.setWidth(width + sizeUnit);
        explorer.setHeight(height + sizeUnit);
        WikiDataSource ds = new WikiDataSource();
        explorer.setDataSource(ds);
        explorer.setDefaultValue(defaultSelection);
        // FIXME: this is somewhat implementation specific, explorer.getElement returns the explorer wrapper while
        // explorer.addStyleName() actually sets the style on the inner tree. We need the style applied on the outer
        // wrapper.
        explorer.getElement().setClassName(explorer.getElement().getClassName() + " xExplorer");

        explorer.addRecordDoubleClickHandler(this);
        explorer.addKeyPressHandler(this);

        // create a label with the help for this step
        Label helpLabel = new Label();
        helpLabel.addStyleName("xHelpLabel");
        helpLabel.setText(getHelpLabelText());
        mainPanel.add(helpLabel);

        errorLabel.setText(getDefaultErrorText());
        errorLabel.setVisible(false);
        errorLabel.addStyleName("xErrorMsg");
        mainPanel.add(errorLabel);

        mainPanel.addStyleName("xExplorerPanel");
        // we need to add the explorer in a wrapper, since the explorer creates its own wrapper around and adds the
        // input to that wrapper. We use this panel to have a reference to the _whole_ generated UI, since the explorer
        // reference would point only to the grid inside.
        mainPanel.add(explorer);
        mainPanel.setExpandingWidget(explorer, true);
    }

    /**
     * @return the help message for this explorer step, to be displayed on top of the explorer tree
     */
    protected abstract String getHelpLabelText();

    /**
     * @return the default error message for this wizard step form
     */
    protected abstract String getDefaultErrorText();

    /**
     * Invalidates the cache on the explorer, so that it will be reloaded on next display. To be used to request an
     * update of the tree when new data is added to it.
     */
    protected void invalidateExplorerData()
    {
        // let's be silently safe about it, no calling function should fail because of this, at least for the moment
        try {
            explorer.invalidateCache();
        } catch (Exception e) {
            // nothing
        }
    }

    /**
     * {@inheritDoc}
     */
    public Widget display()
    {
        return mainPanel;
    }

    /**
     * @return the wiki explorer used by this selector
     */
    public XWikiExplorer getExplorer()
    {
        return explorer;
    }

    /**
     * @return the error label
     */
    public Label getErrorLabel()
    {
        return errorLabel;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void init(Object data, AsyncCallback< ? > cb)
    {
        hideError();
        super.init(data, cb);
    }

    /**
     * Displays the passed error message and markers in this wizard step.
     * 
     * @param errorMessage the error message to print
     */
    protected void displayError(String errorMessage)
    {
        errorLabel.setText(errorMessage);
        errorLabel.setVisible(true);
        // set the class at the wrapper level, the element of this explorer
        if (!explorer.getElement().getClassName().contains(FIELD_ERROR_STYLE)) {
            explorer.getElement().setClassName(explorer.getElement().getClassName() + " " + FIELD_ERROR_STYLE);
        }
        mainPanel.refreshHeights();
    }

    /**
     * Hides the error messages and markers in this wizard step.
     */
    protected void hideError()
    {
        errorLabel.setVisible(false);
        // remove the class from the wrapper level, the element of this explorer
        String boundary = "\\b";
        explorer.getElement().setClassName(
            explorer.getElement().getClassName().replaceAll(boundary + FIELD_ERROR_STYLE + boundary, ""));
        mainPanel.refreshHeights();
    }

    /**
     * {@inheritDoc}
     */
    public void addNavigationListener(NavigationListener listener)
    {
        listeners.add(listener);
    }

    /**
     * {@inheritDoc}
     */
    public void removeNavigationListener(NavigationListener listener)
    {
        listeners.remove(listener);
    }

    /**
     * {@inheritDoc}
     */
    public void onRecordDoubleClick(RecordDoubleClickEvent event)
    {
        listeners.fireNavigationEvent(NavigationDirection.NEXT);
    }

    /**
     * {@inheritDoc}
     */
    public void onKeyPress(KeyPressEvent event)
    {
        // :)
        if ("Enter".equals(event.getKeyName())) {
            listeners.fireNavigationEvent(NavigationDirection.NEXT);
        }
    }
}
