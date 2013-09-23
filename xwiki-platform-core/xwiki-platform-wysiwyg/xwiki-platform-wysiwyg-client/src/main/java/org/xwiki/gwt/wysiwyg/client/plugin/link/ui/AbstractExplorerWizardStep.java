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

import org.xwiki.gwt.user.client.StringUtils;
import org.xwiki.gwt.user.client.ui.VerticalResizePanel;
import org.xwiki.gwt.user.client.ui.wizard.NavigationListener;
import org.xwiki.gwt.user.client.ui.wizard.NavigationListener.NavigationDirection;
import org.xwiki.gwt.user.client.ui.wizard.NavigationListenerCollection;
import org.xwiki.gwt.user.client.ui.wizard.SourcesNavigationEvents;
import org.xwiki.gwt.wysiwyg.client.plugin.link.LinkConfig;
import org.xwiki.gwt.wysiwyg.client.widget.explorer.XWikiExplorer;
import org.xwiki.gwt.wysiwyg.client.widget.explorer.ds.WikiDataSource;
import org.xwiki.gwt.wysiwyg.client.widget.wizard.util.AbstractSelectorWizardStep;
import org.xwiki.gwt.wysiwyg.client.wiki.AttachmentReference;
import org.xwiki.gwt.wysiwyg.client.wiki.EntityLink;
import org.xwiki.gwt.wysiwyg.client.wiki.EntityReference;
import org.xwiki.gwt.wysiwyg.client.wiki.ResourceReference.ResourceType;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Label;
import com.smartgwt.client.widgets.events.KeyPressEvent;
import com.smartgwt.client.widgets.events.KeyPressHandler;
import com.smartgwt.client.widgets.grid.events.RecordDoubleClickEvent;
import com.smartgwt.client.widgets.grid.events.RecordDoubleClickHandler;

/**
 * Wizard step to provide an interface to selecting a wiki resource, using an {@link XWikiExplorer}.
 * 
 * @version $Id$
 */
public abstract class AbstractExplorerWizardStep extends AbstractSelectorWizardStep<EntityLink<LinkConfig>> implements
    SourcesNavigationEvents, RecordDoubleClickHandler, KeyPressHandler
{
    /**
     * The style of the fields under error.
     */
    protected static final String FIELD_ERROR_STYLE = "xErrorField";

    /**
     * The xwiki tree explorer, used to select the page or file to link to.
     */
    private final XWikiExplorer explorer = new XWikiExplorer();

    /**
     * The label to display the error on submission of the wizard step form.
     */
    private final Label errorLabel = new Label();

    /**
     * The help message displayed on the top of the explorer tree.
     */
    private final Label helpLabel = new Label();

    /**
     * The collection of listeners to launch navigation events to, when an item in the tree displayed by this step is
     * double clicked.
     */
    private final NavigationListenerCollection listeners = new NavigationListenerCollection();

    /**
     * Builds a {@link AbstractExplorerWizardStep} from the passed settings.
     * 
     * @param addPage specifies whether the wiki explorer should show the option to add a page
     * @param showAttachments specifies whether the wiki explorer should show the attached files for pages
     * @param addAttachments specifies whether the wiki explorer should show the option to add an attachment
     */
    public AbstractExplorerWizardStep(boolean addPage, boolean showAttachments, boolean addAttachments)
    {
        this(addPage, showAttachments, addAttachments, 455, 305);
    }

    /**
     * Builds a {@link AbstractExplorerWizardStep} from the passed settings, with parameters for size. <br />
     * FIXME: remove the size parameters when the explorer will be correctly sizable from CSS.
     * 
     * @param addPage specifies whether the wiki explorer should show the option to add a page
     * @param showAttachments specifies whether the wiki explorer should show the attached files for pages
     * @param addAttachments specifies whether the wiki explorer should show the option to add an attachment
     * @param width explorer width in pixels
     * @param height explorer height in pixels
     */
    protected AbstractExplorerWizardStep(boolean addPage, boolean showAttachments, boolean addAttachments, int width,
        int height)
    {
        super(new VerticalResizePanel());

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
        // Select nothing by default.
        explorer.setDefaultValue("");
        // FIXME: this is somewhat implementation specific, explorer.getElement returns the explorer wrapper while
        // explorer.addStyleName() actually sets the style on the inner tree. We need the style applied on the outer
        // wrapper.
        explorer.getElement().setClassName(explorer.getElement().getClassName() + " xExplorer");

        explorer.addRecordDoubleClickHandler(this);
        explorer.addKeyPressHandler(this);

        helpLabel.addStyleName("xHelpLabel");
        display().add(helpLabel);

        errorLabel.setVisible(false);
        errorLabel.addStyleName("xErrorMsg");
        display().add(errorLabel);

        display().addStyleName("xExplorerPanel");
        // we need to add the explorer in a wrapper, since the explorer creates its own wrapper around and adds the
        // input to that wrapper. We use this panel to have a reference to the _whole_ generated UI, since the explorer
        // reference would point only to the grid inside.
        display().add(explorer);
        display().setExpandingWidget(explorer, true);
    }

    /**
     * Sets the help message to be displayed on top of the explorer tree.
     * 
     * @param helpLabelText the new help message
     */
    public void setHelpLabelText(String helpLabelText)
    {
        helpLabel.setText(helpLabelText);
    }

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
    public VerticalResizePanel display()
    {
        return (VerticalResizePanel) super.display();
    }

    /**
     * @return the wiki explorer used by this selector
     */
    public XWikiExplorer getExplorer()
    {
        return explorer;
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
     * {@inheritDoc}
     * 
     * @see AbstractSelectorWizardStep#initializeSelection(AsyncCallback)
     */
    @Override
    protected void initializeSelection(AsyncCallback< ? > initCallback)
    {
        EntityReference targetEntityReference = getData().getDestination().getEntityReference();
        // If we don't have something selected or something was explicitly selected.
        if (StringUtils.isEmpty(getExplorer().getValue()) || !getData().getOrigin().equals(targetEntityReference)) {
            boolean isAttachment = getData().getDestination().getType() == ResourceType.ATTACHMENT;
            String fileName = new AttachmentReference(targetEntityReference).getFileName();
            String anchor = isAttachment && StringUtils.isEmpty(fileName) ? "Attachments" : null;

            getExplorer().selectEntity(targetEntityReference, anchor);
        }
        // Else, keep the previous selection.
        super.initializeSelection(initCallback);
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
        display().refreshHeights();
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
        display().refreshHeights();
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

    /**
     * Updates the link configuration object based on the selected entity.
     * 
     * @param selectedEntityReference a reference to the selected entity
     */
    protected void updateLinkConfig(EntityReference selectedEntityReference)
    {
        getData().getDestination().setEntityReference(selectedEntityReference.clone());
        // Reset the link configuration.
        getData().getData().setReference(null);
        getData().getData().setUrl(null);
    }

    /**
     * {@inheritDoc}
     * 
     * @see AbstractSelectorWizardStep#onCancel()
     */
    public void onCancel()
    {
        // Do nothing.
    }
}
