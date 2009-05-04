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
import com.xpn.xwiki.wysiwyg.client.plugin.image.ui.ImageWizard;
import com.xpn.xwiki.wysiwyg.client.plugin.image.ui.ImageWizard.ImageWizardSteps;
import com.xpn.xwiki.wysiwyg.client.plugin.internal.AbstractPlugin;
import com.xpn.xwiki.wysiwyg.client.plugin.internal.FocusWidgetUIExtension;
import com.xpn.xwiki.wysiwyg.client.util.Config;
import com.xpn.xwiki.wysiwyg.client.widget.rta.RichTextArea;
import com.xpn.xwiki.wysiwyg.client.widget.rta.cmd.Command;
import com.xpn.xwiki.wysiwyg.client.widget.wizard.Wizard;
import com.xpn.xwiki.wysiwyg.client.widget.wizard.WizardListener;

/**
 * Rich text editor plug-in for inserting images, using a dialog to get image data settings from the user. It installs
 * one button in the toolbar, to be used for both insert and edit image actions.
 * 
 * @version $Id$
 */
public class ImagePlugin extends AbstractPlugin implements ClickListener, WizardListener
{
    /**
     * Image toolbar button.
     */
    private PushButton imageButton;

    /**
     * The toolbar extension used to add the link buttons to the toolbar. <br />
     * TODO: move this in its own extension, just like {@link ImageMenuExtension}
     */
    private final FocusWidgetUIExtension toolBarExtension = new FocusWidgetUIExtension("toolbar");

    /**
     * The menu extension of this plugin.
     */
    private ImageMenuExtension menuExtension;

    /**
     * Image medadata extractor, to handle the images metadata.
     */
    private ImageMetaDataExtractor metaDataExtractor;

    /**
     * Behavior adjuster to handle the images correctly.
     */
    private ImageBehaviorAdjuster behaviorAdjuster;

    /**
     * The image insert or edit wizard.
     */
    private ImageWizard imageWizard;

    /**
     * {@inheritDoc}
     * 
     * @see AbstractPlugin#init(Wysiwyg, XRichTextArea, Config)
     */
    public void init(Wysiwyg wysiwyg, RichTextArea textArea, Config config)
    {
        super.init(wysiwyg, textArea, config);

        // register the custom command
        textArea.getCommandManager().registerCommand(Command.INSERT_IMAGE, new InsertImageExecutable());

        // add the toolbar extension
        if (getTextArea().getCommandManager().isSupported(Command.INSERT_IMAGE)) {
            imageButton = new PushButton(Images.INSTANCE.image().createImage(), this);
            imageButton.setTitle(Strings.INSTANCE.imageTooltip());
            toolBarExtension.addFeature("image", imageButton);
            getUIExtensionList().add(toolBarExtension);

            // add the menu extension
            menuExtension = new ImageMenuExtension(this);
            getUIExtensionList().add(menuExtension);

            imageWizard = new ImageWizard(getConfig());
            imageWizard.addWizardListener(this);
        }

        // Create an image metadata extractor for this text area
        metaDataExtractor = new ImageMetaDataExtractor();
        // do the initial extracting on the loaded document
        metaDataExtractor.onInnerHTMLChange(getTextArea().getDocument().getDocumentElement());
        getTextArea().getDocument().addInnerHTMLListener(metaDataExtractor);

        // Create an image behavior adjuster for this text area
        behaviorAdjuster = new ImageBehaviorAdjuster();
        behaviorAdjuster.setTextArea(getTextArea());
        getTextArea().addKeyboardListener(behaviorAdjuster);
    }

    /**
     * {@inheritDoc}
     * 
     * @see AbstractPlugin#destroy()
     */
    public void destroy()
    {
        if (imageButton != null) {
            imageButton.removeFromParent();
            imageButton.removeClickListener(this);
            imageButton = null;
        }

        toolBarExtension.clearFeatures();

        if (menuExtension != null) {
            menuExtension.destroy();
        }

        // If a metadata extractor was created and setup, remove it
        if (metaDataExtractor != null) {
            getTextArea().getDocument().removeInnerHTMLListener(metaDataExtractor);
            metaDataExtractor = null;
        }
        // If a behavior adjuster was created and setup, remove it
        if (behaviorAdjuster != null) {
            getTextArea().removeKeyboardListener(behaviorAdjuster);
            behaviorAdjuster = null;
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
        if (sender == imageButton) {
            onImage();
        }
    }

    /**
     * Function to handle the image event, when the toolbar button is clicked or the menu command is issued: either
     * create a new image, or edit an existing image.
     */
    public void onImage()
    {
        ImageConfig config = new ImageConfig();
        String imageParam = getTextArea().getCommandManager().getStringValue(Command.INSERT_IMAGE);
        if (imageParam != null) {
            config.fromJSON(imageParam);
        } else {
            // get selection, textify and set as the default alternative text
            config.setAltText(getTextArea().getDocument().getSelection().getRangeAt(0).toString());
        }
        imageWizard.start(ImageWizardSteps.IMAGESELECTOR.toString(), config);
    }

    /**
     * Removes the selection if the insert image command is executed.
     */
    public void onImageRemove()
    {
        if (getTextArea().getCommandManager().isExecuted(Command.INSERT_IMAGE)) {
            getTextArea().setFocus(true);
            getTextArea().getCommandManager().execute(Command.DELETE);
        }
    }

    /**
     * {@inheritDoc}
     */
    public void onFinish(Wizard sender, Object result)
    {
        String imageHTML = ImageHTMLGenerator.getInstance().getAttachedImageHTML((ImageConfig) result);
        getTextArea().setFocus(true);
        getTextArea().getCommandManager().execute(Command.INSERT_IMAGE, imageHTML);
    }

    /**
     * {@inheritDoc}
     */
    public void onCancel(Wizard sender)
    {
        // return the focus to the text area
        getTextArea().setFocus(true);
    }
}
