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

import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.Panel;
import com.google.gwt.user.client.ui.Widget;
import com.xpn.xwiki.wysiwyg.client.WysiwygService;
import com.xpn.xwiki.wysiwyg.client.editor.Strings;
import com.xpn.xwiki.wysiwyg.client.plugin.link.LinkConfig;
import com.xpn.xwiki.wysiwyg.client.plugin.link.LinkConfig.LinkType;
import com.xpn.xwiki.wysiwyg.client.util.StringUtils;
import com.xpn.xwiki.wysiwyg.client.widget.explorer.XWikiExplorer;
import com.xpn.xwiki.wysiwyg.client.widget.explorer.ds.WikiDataSource;
import com.xpn.xwiki.wysiwyg.client.widget.wizard.WizardStep;
import com.xpn.xwiki.wysiwyg.client.widget.wizard.NavigationListener.NavigationDirection;

/**
 * Wizard step to provide an interface to selecting a wiki object.
 * 
 * @version $Id$
 */
public class SelectorWizardStep implements WizardStep
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
     * Default constructor.
     */
    public SelectorWizardStep()
    {
        explorer = new XWikiExplorer();
        explorer.setDisplayLinks(false);
        // display the new page option
        explorer.setDisplayAddPage(true);
        explorer.setDisplayAddPageOnTop(false);
        // no attachments here
        explorer.setDisplayAttachments(false);
        explorer.setDisplayAddAttachment(false);
        explorerPanel.setWidth("459px");
        explorerPanel.setHeight("329px");

        WikiDataSource ds = new WikiDataSource();
        explorer.setDataSource(ds);
        explorerPanel.setStyleName("xExplorerPanel");
        explorer.setHtmlElement(explorerPanel.getElement());
        explorer.draw();
    }

    /**
     * {@inheritDoc}
     */
    public void init(Object data, AsyncCallback< ? > cb)
    {
        linkData = (LinkConfig) data;
        // TODO: set the value of the explorer to the selected value
        cb.onSuccess(null);
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
    public String getNextStep()
    {
        return "wikipageconfig";
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
    public String getStepTitle()
    {
        return Strings.INSTANCE.selectPage();
    }

    /**
     * {@inheritDoc}
     */
    public EnumSet<NavigationDirection> getValidDirections()
    {
        return EnumSet.of(NavigationDirection.NEXT, NavigationDirection.PREVIOUS, NavigationDirection.CANCEL);
    }

    /**
     * {@inheritDoc}
     */
    public void onCancel(AsyncCallback<Boolean> async)
    {
        async.onSuccess(true);
    }

    /**
     * {@inheritDoc}
     */
    public void onSubmit(final AsyncCallback<Boolean> async)
    {
        // should check that the selection is ok according to the desired type and to "commit" it in the link config
        String selectedValue = explorer.getValue();
        // selected resource should not be empty
        if (StringUtils.isEmpty(selectedValue)) {
            Window.alert(Strings.INSTANCE.noPageSelectedError());
            async.onSuccess(false);
        } else {
            // commit the changes in the config
            // create a link to a space if there is no page selected and there is a space selected
            // TODO: remove this when the reference is set correctly from the explorer to an existing space
            if (!StringUtils.isEmpty(explorer.getSelectedSpace()) && StringUtils.isEmpty(explorer.getSelectedPage())
                && !explorer.isNewPage()) {
                linkData.setReference(selectedValue + ".WebHome");
            } else {
                linkData.setReference(selectedValue);
            }
            // if indeed a new page is selected, make sure we commit the link type in the config
            if (explorer.isNewPage()) {
                linkData.setType(LinkType.NEW_WIKIPAGE);
            }
            // build the page url from the parameters in the explorer tree
            // TODO: remove this when the explorer will return the selected resource URL
            WysiwygService.Singleton.getInstance().getPageLink(explorer.getSelectedWiki(), explorer.getSelectedSpace(),
                explorer.getSelectedPage(), null, null, new AsyncCallback<LinkConfig>()
                {
                    public void onSuccess(LinkConfig result)
                    {
                        linkData.setUrl(result.getUrl());
                        async.onSuccess(true);
                    }

                    public void onFailure(Throwable caught)
                    {
                        async.onSuccess(false);
                    }
                });
        }
    }
}
