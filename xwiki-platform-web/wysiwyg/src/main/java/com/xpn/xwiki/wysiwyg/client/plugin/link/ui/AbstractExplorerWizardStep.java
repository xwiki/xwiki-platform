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

import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.Panel;
import com.google.gwt.user.client.ui.Widget;
import com.xpn.xwiki.wysiwyg.client.widget.explorer.XWikiExplorer;
import com.xpn.xwiki.wysiwyg.client.widget.explorer.ds.WikiDataSource;

/**
 * Wizard step to provide an interface to selecting a wiki resource, using an {@link XWikiExplorer}.
 * 
 * @version $Id$
 */
public abstract class AbstractExplorerWizardStep extends AbstractSelectorWizardStep
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
        // strangely enough, this sets the style on the tree wrapper, which contains the input too, even if explorer is
        // a reference only to the tree
        explorer.addStyleName("xExplorerPanel");
        // we need to add the explorer in a wrapper, since the explorer creates its own wrapper around and adds the
        // input to that wrapper. We use this panel to have a reference to the _whole_ generated UI, since the explorer
        // reference would point only to the grid inside.
        explorerPanel.add(explorer);
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
     * @return the wiki explorer used by this selector
     */
    public XWikiExplorer getExplorer()
    {
        return explorer;
    }
}
