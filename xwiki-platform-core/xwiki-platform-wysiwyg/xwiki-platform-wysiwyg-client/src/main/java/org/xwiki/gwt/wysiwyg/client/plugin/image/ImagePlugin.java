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
package org.xwiki.gwt.wysiwyg.client.plugin.image;

import org.xwiki.gwt.dom.client.Element;
import org.xwiki.gwt.user.client.Config;
import org.xwiki.gwt.user.client.ui.rta.RichTextArea;
import org.xwiki.gwt.user.client.ui.rta.cmd.Command;
import org.xwiki.gwt.user.client.ui.wizard.Wizard;
import org.xwiki.gwt.user.client.ui.wizard.WizardListener;
import org.xwiki.gwt.wysiwyg.client.Images;
import org.xwiki.gwt.wysiwyg.client.Strings;
import org.xwiki.gwt.wysiwyg.client.plugin.image.exec.InsertImageExecutable;
import org.xwiki.gwt.wysiwyg.client.plugin.image.ui.ImageWizard;
import org.xwiki.gwt.wysiwyg.client.plugin.image.ui.ImageWizard.ImageWizardStep;
import org.xwiki.gwt.wysiwyg.client.plugin.internal.AbstractPlugin;
import org.xwiki.gwt.wysiwyg.client.plugin.internal.FocusWidgetUIExtension;
import org.xwiki.gwt.wysiwyg.client.wiki.WikiServiceAsync;

import com.google.gwt.core.client.Scheduler;
import com.google.gwt.core.client.Scheduler.ScheduledCommand;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.PushButton;

/**
 * Rich text editor plug-in for inserting images, using a dialog to get image data settings from the user. It installs
 * one button in the toolbar, to be used for both insert and edit image actions.
 * 
 * @version $Id$
 */
public class ImagePlugin extends AbstractPlugin implements ClickHandler, WizardListener
{
    /**
     * Image toolbar button.
     */
    private PushButton imageButton;

    /**
     * The toolbar extension used to add the link buttons to the toolbar. <br>
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
     * The service used to access the wiki.
     */
    private final WikiServiceAsync wikiService;

    /**
     * The object used to serialize an {@link ImageConfig} instance to JSON.
     */
    private final ImageConfigJSONSerializer imageConfigJSONSerializer = new ImageConfigJSONSerializer();

    /**
     * The object used to create an {@link ImageConfig} from JSON.
     */
    private final ImageConfigJSONParser imageConfigJSONParser = new ImageConfigJSONParser();

    /**
     * Create a new image plugin that used the specified wiki service.
     * 
     * @param wikiService the service used to access the wiki
     */
    public ImagePlugin(WikiServiceAsync wikiService)
    {
        this.wikiService = wikiService;
    }

    @Override
    public void init(RichTextArea textArea, Config config)
    {
        super.init(textArea, config);

        // register the custom command
        textArea.getCommandManager().registerCommand(Command.INSERT_IMAGE, new InsertImageExecutable(textArea));

        // add the toolbar extension
        if (getTextArea().getCommandManager().isSupported(Command.INSERT_IMAGE)) {
            imageButton = new PushButton(new Image(Images.INSTANCE.image()));
            saveRegistration(imageButton.addClickHandler(this));
            imageButton.setTitle(Strings.INSTANCE.imageTooltip());
            toolBarExtension.addFeature("image", imageButton);
            getUIExtensionList().add(toolBarExtension);

            // add the menu extension
            menuExtension = new ImageMenuExtension(this);
            getUIExtensionList().add(menuExtension);
            // Hack: We can access the menus where each menu item was placed only after the main menu bar is
            // initialized, which happens after all the plugins are loaded.
            Scheduler.get().scheduleDeferred(new ScheduledCommand()
            {
                @Override
                public void execute()
                {
                    menuExtension.registerAttachHandlers();
                }
            });

            imageWizard = new ImageWizard(getConfig(), wikiService);
            imageWizard.addWizardListener(this);
        }

        // Create an image metadata extractor for this text area
        metaDataExtractor = new ImageMetaDataExtractor();
        // do the initial extracting on the loaded document
        metaDataExtractor.onInnerHTMLChange((Element) getTextArea().getDocument().getDocumentElement());
        getTextArea().getDocument().addInnerHTMLListener(metaDataExtractor);

        // Create an image behavior adjuster for this text area
        behaviorAdjuster = new ImageBehaviorAdjuster();
        behaviorAdjuster.setTextArea(getTextArea());
        saveRegistration(getTextArea().addKeyDownHandler(behaviorAdjuster));
        saveRegistration(getTextArea().addKeyUpHandler(behaviorAdjuster));
        saveRegistration(getTextArea().addKeyPressHandler(behaviorAdjuster));
    }

    @Override
    public void destroy()
    {
        if (imageButton != null) {
            imageButton.removeFromParent();
            imageButton = null;
        }

        toolBarExtension.clearFeatures();

        if (menuExtension != null) {
            menuExtension.clearFeatures();
        }

        // If a metadata extractor was created and setup, remove it
        if (metaDataExtractor != null) {
            getTextArea().getDocument().removeInnerHTMLListener(metaDataExtractor);
            metaDataExtractor = null;
        }
        behaviorAdjuster = null;

        super.destroy();
    }

    @Override
    public void onClick(ClickEvent event)
    {
        if (event.getSource() == imageButton) {
            onImage();
        }
    }

    /**
     * Function to handle the image event, when the tool bar button is clicked or the menu command is issued: either
     * create a new image, or edit an existing image.
     */
    public void onImage()
    {
        ImageConfig imageConfig;
        String imageJSON = getTextArea().getCommandManager().getStringValue(Command.INSERT_IMAGE);
        String startStep = ImageWizardStep.ATTACHED_IMAGE_SELECTOR.toString();
        if (imageJSON != null) {
            startStep = ImageWizardStep.IMAGE_REFERENCE_PARSER.toString();
            imageConfig = imageConfigJSONParser.parse(imageJSON);
        } else {
            imageConfig = new ImageConfig();
            imageConfig.setAltText(getTextArea().getDocument().getSelection().getRangeAt(0).toString());
        }
        imageWizard.start(startStep, imageConfig);
    }

    /**
     * Start the edit image wizard.
     */
    public void onImageEdit()
    {
        String imageJSON = getTextArea().getCommandManager().getStringValue(Command.INSERT_IMAGE);
        ImageConfig imageConfig = imageConfigJSONParser.parse(imageJSON);
        imageWizard.start(ImageWizardStep.IMAGE_REFERENCE_PARSER.toString(), imageConfig);
    }

    /**
     * Start the insert attached image wizard.
     */
    public void onAttachedImage()
    {
        ImageConfig imageConfig = new ImageConfig();
        imageConfig.setAltText(getTextArea().getDocument().getSelection().getRangeAt(0).toString());
        imageWizard.start(ImageWizardStep.ATTACHED_IMAGE_SELECTOR.toString(), imageConfig);
    }

    /**
     * Start the insert external image wizard.
     */
    public void onURLImage()
    {
        ImageConfig imageConfig = new ImageConfig();
        imageConfig.setAltText(getTextArea().getDocument().getSelection().getRangeAt(0).toString());
        imageWizard.start(ImageWizardStep.URL_IMAGE_SELECTOR.toString(), imageConfig);
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

    @Override
    public void onFinish(Wizard sender, Object result)
    {
        getTextArea().setFocus(true);
        String imageJSON = imageConfigJSONSerializer.serialize((ImageConfig) result);
        getTextArea().getCommandManager().execute(Command.INSERT_IMAGE, imageJSON);
    }

    @Override
    public void onCancel(Wizard sender)
    {
        // return the focus to the text area
        getTextArea().setFocus(true);
    }
}
