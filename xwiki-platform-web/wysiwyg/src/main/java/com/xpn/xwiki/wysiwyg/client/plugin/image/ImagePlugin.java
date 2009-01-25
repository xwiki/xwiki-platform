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
package com.xpn.xwiki.wysiwyg.client.plugin.image;

import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.PushButton;
import com.google.gwt.user.client.ui.Widget;
import com.xpn.xwiki.wysiwyg.client.Wysiwyg;
import com.xpn.xwiki.wysiwyg.client.editor.Images;
import com.xpn.xwiki.wysiwyg.client.editor.Strings;
import com.xpn.xwiki.wysiwyg.client.plugin.image.exec.InsertImageExecutable;
import com.xpn.xwiki.wysiwyg.client.plugin.image.ui.ImageDialog;
import com.xpn.xwiki.wysiwyg.client.plugin.internal.AbstractPlugin;
import com.xpn.xwiki.wysiwyg.client.plugin.internal.FocusWidgetUIExtension;
import com.xpn.xwiki.wysiwyg.client.util.Config;
import com.xpn.xwiki.wysiwyg.client.widget.PopupListener;
import com.xpn.xwiki.wysiwyg.client.widget.SourcesPopupEvents;
import com.xpn.xwiki.wysiwyg.client.widget.rta.RichTextArea;
import com.xpn.xwiki.wysiwyg.client.widget.rta.SelectionPreserver;
import com.xpn.xwiki.wysiwyg.client.widget.rta.cmd.Command;

/**
 * Rich text editor plug-in for inserting images, using a dialog to get image data settings from the user. It installs
 * one button in the toolbar, to be used for both insert and edit image actions.
 * 
 * @version $Id$
 */
public class ImagePlugin extends AbstractPlugin implements ClickListener, PopupListener
{
    /**
     * Image toolbar button.
     */
    private PushButton image;

    /**
     * Dialog to get information about the inserted image from the user.
     */
    private ImageDialog imageDialog;

    /**
     * The toolbar extension used to add the link buttons to the toolbar.
     */
    private final FocusWidgetUIExtension toolBarExtension = new FocusWidgetUIExtension("toolbar");

    /**
     * Selection preserver to store the selection before and after the dialog showing.
     */
    private SelectionPreserver selectionPreserver;

    /**
     * {@inheritDoc}
     * 
     * @see AbstractPlugin#init(Wysiwyg, XRichTextArea, Config)
     */
    public void init(Wysiwyg wysiwyg, RichTextArea textArea, Config config)
    {
        super.init(wysiwyg, textArea, config);

        textArea.getCommandManager().registerCommand(Command.INSERT_IMAGE, new InsertImageExecutable());
        if (getTextArea().getCommandManager().isSupported(Command.INSERT_IMAGE)) {
            image = new PushButton(Images.INSTANCE.image().createImage(), this);
            image.setTitle(Strings.INSTANCE.image());
            toolBarExtension.addFeature("image", image);
        }

        if (toolBarExtension.getFeatures().length > 0) {
            getTextArea().addClickListener(this);
            getUIExtensionList().add(toolBarExtension);
            selectionPreserver = new SelectionPreserver(textArea);
            ImageMetaDataExtractor extractor = new ImageMetaDataExtractor();
            // do the initial extracting on the loaded document
            extractor.onInnerHTMLChange(getTextArea().getDocument().getDocumentElement());
            getTextArea().getDocument().addInnerHTMLListener(new ImageMetaDataExtractor());

            // Create an image behavior adjuster for this text area
            new ImageBehaviorAdjuster(getTextArea());
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see AbstractPlugin#destroy()
     */
    public void destroy()
    {
        image.removeFromParent();
        image.removeClickListener(this);
        image = null;

        imageDialog.hide();
        imageDialog.removeFromParent();
        imageDialog = null;

        toolBarExtension.clearFeatures();

        getTextArea().removeClickListener(this);

        super.destroy();
    }

    /**
     * {@inheritDoc}
     * 
     * @see ClickListener#onClick(Widget)
     */
    public void onClick(Widget sender)
    {
        if (sender == image) {
            onImage(true);
        }
    }

    /**
     * Function to handle the image event, either the image inserting start (when the button is clicked), either on the
     * image inserting finish, when the image dialog is closed.
     * 
     * @param show whether the image dialog needs to be shown or not.
     */
    public void onImage(boolean show)
    {
        if (show) {
            // store current selection, we'll need it after to add the image in the right place
            selectionPreserver.saveSelection();
            ImageConfig config = new ImageConfig();
            String imageParam =
                getTextArea().getCommandManager().getExecutable(Command.INSERT_IMAGE).getParameter(getTextArea());
            if (imageParam != null) {
                config.fromJSON(imageParam);
            } else {
                // get selection, textify and set as the default alternative text
                config.setAltText(getTextArea().getDocument().getSelection().getRangeAt(0).toString());
            }
            getImageDialog().setImageConfig(config);
            getImageDialog().center();
        } else {
            // restore the selection before executing the command.
            selectionPreserver.restoreSelection();
            String imageHTML = getImageDialog().getImageHTMLBlock();
            if (imageHTML != null) {
                getTextArea().getCommandManager().execute(Command.INSERT_IMAGE, imageHTML);
            } else {
                getTextArea().setFocus(true);
            }
        }
    }

    /**
     * Lazy creation of the image dialog, to optimize editor loading time.
     * 
     * @return the image dialog to be used for inserting the image.
     */
    private ImageDialog getImageDialog()
    {
        if (imageDialog == null) {
            imageDialog =
                new ImageDialog(getConfig().getParameter("wiki", "xwiki"), getConfig().getParameter("space", "Main"),
                    getConfig().getParameter("page", "WebHome"));
            imageDialog.addPopupListener(this);
        }
        return imageDialog;
    }

    /**
     * {@inheritDoc}
     * 
     * @see PopupListener#onPopupClosed(SourcesPopupEvents, boolean)
     */
    public void onPopupClosed(SourcesPopupEvents sender, boolean autoClosed)
    {
        if (sender == imageDialog && !autoClosed) {
            onImage(false);
        }

    }
}
