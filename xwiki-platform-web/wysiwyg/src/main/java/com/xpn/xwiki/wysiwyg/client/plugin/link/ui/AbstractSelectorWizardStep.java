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
package com.xpn.xwiki.wysiwyg.client.plugin.link.ui;

import java.util.EnumSet;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.Panel;
import com.google.gwt.user.client.ui.Widget;
import com.xpn.xwiki.wysiwyg.client.editor.Strings;
import com.xpn.xwiki.wysiwyg.client.plugin.link.LinkConfig;
import com.xpn.xwiki.wysiwyg.client.util.StringUtils;
import com.xpn.xwiki.wysiwyg.client.widget.explorer.XWikiExplorer;
import com.xpn.xwiki.wysiwyg.client.widget.explorer.ds.WikiDataSource;
import com.xpn.xwiki.wysiwyg.client.widget.wizard.WizardStep;
import com.xpn.xwiki.wysiwyg.client.widget.wizard.NavigationListener.NavigationDirection;

/**
 * Wizard step to provide an interface to selecting a wiki resource, using an {@link XWikiExplorer}. Implementing
 * classes will have to implement the {@link #onSubmit(AsyncCallback)} and {@link #onCancel(AsyncCallback)} to handle
 * selection validation and submit or cancel.
 * 
 * @version $Id$
 */
public abstract class AbstractSelectorWizardStep implements WizardStep
{
    /**
     * The xwiki tree explorer, used to select the page or file to link to.
     */
    private XWikiExplorer explorer;

    /**
     * The panel to hold the xwiki explorer.
     */
    private final Panel explorerPanel = new FlowPanel();

    /**
     * The link config edited by this dialog.
     */
    private LinkConfig linkData;

    /**
     * Builds a {@link AbstractSelectorWizardStep} from the passed settings.
     * 
     * @param addPage specifies whether the wiki explorer should show the option to add a page
     * @param showAttachments specifies whether the wiki explorer should show the attached files for pages
     * @param addAttachments specifies whether the wiki explorer should show the option to add an attachment
     * @param defaultSelection the default selection of the wiki explorer displayed by this step
     */
    public AbstractSelectorWizardStep(boolean addPage, boolean showAttachments, boolean addAttachments,
        String defaultSelection)
    {
        explorer = new XWikiExplorer();
        explorer.setDisplayLinks(false);
        // display the new page option
        explorer.setDisplayAddPage(addPage);
        explorer.setDisplayAddPageOnTop(false);
        // no attachments here
        explorer.setDisplayAttachments(showAttachments);
        explorer.setDisplayAddAttachment(showAttachments && addAttachments);
        explorer.setDisplayAttachmentsWhenEmpty(showAttachments && addAttachments);
        explorer.setWidth("455px");
        explorer.setHeight("305px");
        WikiDataSource ds = new WikiDataSource();
        explorer.setDataSource(ds);
        explorer.setDefaultValue(defaultSelection);
        // strangely enough, this sets the style on the tree wrapper, which contains the input too, even if explorer is
        // a reference only to the tree
        explorer.addStyleName("xExplorerPanel");
        // we need to add the explorer in a wrapper, since the explorer creates its own wrapper around and adds the
        // input to that wrapper. We use this panel to have a reference to the _whole_ generated UI, since the explorer
        // reference would point only to the grid inside.
        explorerPanel.add(explorer);
    }

    /**
     * {@inheritDoc}
     */
    public void init(Object data, AsyncCallback< ? > cb)
    {
        linkData = (LinkConfig) data;
        initializeExplorerSelection();
        cb.onSuccess(null);
    }

    /**
     * Sets the initial explorer selection on {@link #init(Object, AsyncCallback)} time, to be overwritten by subclasses
     * to perform specific actions. By default, the reference of the passed {@link LinkConfig}, if any, is set in the
     * explorer.
     */
    protected void initializeExplorerSelection()
    {
        if (!StringUtils.isEmpty(linkData.getReference())) {
            explorer.setValue(linkData.getReference());
        }
        // else leave the tree where the last selection was
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
    public Widget display()
    {
        return explorerPanel;
    }

    /**
     * {@inheritDoc}
     */
    public String getDirectionName(NavigationDirection direction)
    {
        return Strings.INSTANCE.select();
    }

    /**
     * {@inheritDoc}
     */
    public Object getResult()
    {
        return linkData;
    }

    /**
     * {@inheritDoc}
     */
    public EnumSet<NavigationDirection> getValidDirections()
    {
        return EnumSet.of(NavigationDirection.NEXT, NavigationDirection.PREVIOUS, NavigationDirection.CANCEL);
    }

    /**
     * @return the wiki explorer used by this selector
     */
    public XWikiExplorer getExplorer()
    {
        return explorer;
    }

    /**
     * @return the {@link LinkConfig} configured by this {@link WizardStep}
     */
    public LinkConfig getLinkData()
    {
        return linkData;
    }
}
