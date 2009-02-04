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
package com.xpn.xwiki.wysiwyg.client.plugin.link;

import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.PushButton;
import com.google.gwt.user.client.ui.Widget;
import com.xpn.xwiki.wysiwyg.client.Wysiwyg;
import com.xpn.xwiki.wysiwyg.client.editor.Images;
import com.xpn.xwiki.wysiwyg.client.editor.Strings;
import com.xpn.xwiki.wysiwyg.client.plugin.image.ImageConfig;
import com.xpn.xwiki.wysiwyg.client.plugin.internal.AbstractPlugin;
import com.xpn.xwiki.wysiwyg.client.plugin.internal.FocusWidgetUIExtension;
import com.xpn.xwiki.wysiwyg.client.plugin.link.ui.LinkDialog;
import com.xpn.xwiki.wysiwyg.client.util.Config;
import com.xpn.xwiki.wysiwyg.client.widget.PopupListener;
import com.xpn.xwiki.wysiwyg.client.widget.SourcesPopupEvents;
import com.xpn.xwiki.wysiwyg.client.widget.rta.RichTextArea;
import com.xpn.xwiki.wysiwyg.client.widget.rta.SelectionPreserver;
import com.xpn.xwiki.wysiwyg.client.widget.rta.cmd.Command;

/**
 * Rich text editor plug-in for inserting links, using a dialog to get link settings from the user. It installs two
 * buttons in the toolbar, for its two actions: link and unlink.
 * 
 * @version $Id$
 */
public class LinkPlugin extends AbstractPlugin implements ClickListener, PopupListener
{
    /**
     * The button from the toolbar for create a link.
     */
    private PushButton link;

    /**
     * The button from the toolbar for destroy a link.
     */
    private PushButton unlink;

    /**
     * The dialog for create a link.
     */
    private LinkDialog linkDialog;

    /**
     * Selection preserver to store the selection before and after the dialog showing.
     */
    private SelectionPreserver selectionPreserver;

    /**
     * The toolbar extension used to add the link buttons to the toolbar.
     */
    private final FocusWidgetUIExtension toolBarExtension = new FocusWidgetUIExtension("toolbar");
    
    /**
     * The link metadata extractor, to handle the link metadata.
     */
    private LinkMetaDataExtractor metaDataExtractor;

    /**
     * {@inheritDoc}
     * 
     * @see AbstractPlugin#init(Wysiwyg, RichTextArea, Config)
     */
    public void init(Wysiwyg wysiwyg, RichTextArea textArea, Config config)
    {
        super.init(wysiwyg, textArea, config);

        if (getTextArea().getCommandManager().isSupported(Command.CREATE_LINK)) {
            link = new PushButton(Images.INSTANCE.link().createImage(), this);
            link.setTitle(Strings.INSTANCE.link());
            toolBarExtension.addFeature("link", link);
        }

        if (getTextArea().getCommandManager().isSupported(Command.UNLINK)) {
            unlink = new PushButton(Images.INSTANCE.unlink().createImage(), this);
            unlink.setTitle(Strings.INSTANCE.unlink());
            toolBarExtension.addFeature("unlink", unlink);
        }

        if (toolBarExtension.getFeatures().length > 0) {
            getTextArea().addClickListener(this);
            getUIExtensionList().add(toolBarExtension);
            selectionPreserver = new SelectionPreserver(textArea);
            // Initialize the metadata extractor, to handle link metadatas
            metaDataExtractor = new LinkMetaDataExtractor();
            // do the initial extracting on the loaded document
            metaDataExtractor.onInnerHTMLChange(getTextArea().getDocument().getDocumentElement());
            getTextArea().getDocument().addInnerHTMLListener(metaDataExtractor);
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see AbstractPlugin#destroy()
     */
    public void destroy()
    {
        if (link != null) {
            link.removeFromParent();
            link.removeClickListener(this);
            link = null;

            if (linkDialog != null) {
                linkDialog.hide();
                linkDialog.removeFromParent();
                linkDialog.removePopupListener(this);
                linkDialog = null;
            }
        }

        if (unlink != null) {
            unlink.removeFromParent();
            unlink.removeClickListener(this);
            unlink = null;
        }

        if (toolBarExtension.getFeatures().length > 0) {
            getTextArea().removeClickListener(this);
            toolBarExtension.clearFeatures();
            // if a link metadata extractor has been created and setup, remove it
            if (metaDataExtractor != null) {
                getTextArea().getDocument().removeInnerHTMLListener(metaDataExtractor);
                metaDataExtractor = null;
            }
        }
        super.destroy();
    }

    /**
     * {@inheritDoc}
     * 
     * @see ClickListener#onClick(Widget)
     */
    public void onClick(Widget sender)
    {
        if (sender == link) {
            onLink(true);
        } else if (sender == unlink) {
            onUnlink();
        }
    }

    /**
     * Depending on the passed parameter, the link dialog is displayed for the user to create a link or the created link
     * is inserted in the document.
     * 
     * @param show true if the link dialog must be shown, false otherwise
     */
    public void onLink(boolean show)
    {
        if (show) {
            // save the selection
            selectionPreserver.saveSelection();
            // setup the dialog data
            // use only the first range in the user selection
            // Check the special case when the selection is an image and add a link on an image
            String imageParam =
                (getTextArea().getCommandManager().getExecutable(Command.INSERT_IMAGE)).getParameter(getTextArea());
            if (imageParam != null) {
                // it's an image selection, set the label readonly and put the image filename in the label text
                ImageConfig imgConfig = new ImageConfig();
                imgConfig.fromJSON(imageParam);
                getLinkDialog().setLabel(getTextArea().getDocument().getSelection().getRangeAt(0).toHTML(),
                    imgConfig.getImageFileName(), true);
            } else {
                getLinkDialog().setLabel(getTextArea().getDocument().getSelection().getRangeAt(0).toHTML(),
                    getTextArea().getDocument().getSelection().getRangeAt(0).toString(), false);
            }
            // show the dialog
            getLinkDialog().center();
        } else {
            // restore selection to be sure to execute the command on the right selection.
            selectionPreserver.restoreSelection();
            String url = getLinkDialog().getLink();
            if (url != null) {
                getTextArea().getCommandManager().execute(Command.CREATE_LINK, url);
            } else {
                // We get here if the link dialog has been closed by clicking the close button.
                // In this case we return the focus to the text area.
                getTextArea().setFocus(true);
            }
        }
    }

    /**
     * Executed when the unlink button is clicked.
     */
    public void onUnlink()
    {
        getTextArea().getCommandManager().execute(Command.UNLINK);
    }

    /**
     * {@inheritDoc}
     * 
     * @see PopupListener#onPopupClosed(SourcesPopupEvents, boolean)
     */
    public void onPopupClosed(SourcesPopupEvents sender, boolean autoHide)
    {
        if (sender == getLinkDialog() && !autoHide) {
            onLink(false);
        }
    }

    /**
     * Lazy creation of the link dialog, to optimize editor loading time.
     * 
     * @return the link dialog to be used for inserting the link.
     */
    private LinkDialog getLinkDialog()
    {
        if (linkDialog == null) {
            linkDialog =
                new LinkDialog(getConfig().getParameter("wiki", "xwiki"), getConfig().getParameter("space", "Main"),
                    getConfig().getParameter("page", "WebHome"));
            linkDialog.addPopupListener(this);
        }
        return linkDialog;
    }
}
