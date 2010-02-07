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
package org.xwiki.gwt.wysiwyg.client.plugin.link;

import java.util.HashMap;
import java.util.Map;

import org.xwiki.gwt.dom.client.Element;
import org.xwiki.gwt.dom.client.Range;
import org.xwiki.gwt.user.client.Config;
import org.xwiki.gwt.user.client.ui.rta.RichTextArea;
import org.xwiki.gwt.user.client.ui.rta.cmd.Command;
import org.xwiki.gwt.user.client.ui.rta.cmd.Executable;
import org.xwiki.gwt.user.client.ui.wizard.Wizard;
import org.xwiki.gwt.user.client.ui.wizard.WizardListener;
import org.xwiki.gwt.wysiwyg.client.plugin.image.ImageConfig;
import org.xwiki.gwt.wysiwyg.client.plugin.internal.AbstractPlugin;
import org.xwiki.gwt.wysiwyg.client.plugin.link.exec.CreateLinkExecutable;
import org.xwiki.gwt.wysiwyg.client.plugin.link.exec.LinkExecutableUtils;
import org.xwiki.gwt.wysiwyg.client.plugin.link.exec.UnlinkExecutable;
import org.xwiki.gwt.wysiwyg.client.plugin.link.ui.LinkWizard;
import org.xwiki.gwt.wysiwyg.client.plugin.link.ui.LinkWizard.LinkWizardSteps;
import org.xwiki.gwt.wysiwyg.client.wiki.ResourceName;
import org.xwiki.gwt.wysiwyg.client.wiki.WikiServiceAsync;


/**
 * Rich text editor plug-in for inserting links, using a dialog to get link settings from the user. It installs a menu
 * bar extension, with entries for all its actions.
 * 
 * @version $Id$
 */
public class LinkPlugin extends AbstractPlugin implements WizardListener
{
    /**
     * The wizard to create a link.
     */
    private Wizard linkWizard;

    /**
     * The menu extension of this plugin.
     */
    private LinkMenuExtension menuExtension;

    /**
     * The link metadata extractor, to handle the link metadata.
     */
    private LinkMetaDataExtractor metaDataExtractor;

    /**
     * The empty link filter, to prevent the submission of empty links.
     */
    private EmptyLinkFilter linkFilter;

    /**
     * Map of the original link related executables, which will be replaced with custom executables by this plugin.
     */
    private Map<Command, Executable> originalExecutables;

    /**
     * The service used to access the wiki.
     */
    private final WikiServiceAsync wikiService;

    /**
     * Creates a new link plugin that will use the specified wiki service.
     * 
     * @param wikiService the service used to access the wiki
     */
    public LinkPlugin(WikiServiceAsync wikiService)
    {
        this.wikiService = wikiService;
    }

    /**
     * {@inheritDoc}
     * 
     * @see AbstractPlugin#init(RichTextArea, Config)
     */
    public void init(RichTextArea textArea, Config config)
    {
        super.init(textArea, config);

        // add the custom executables
        Executable createLinkExec =
            getTextArea().getCommandManager().registerCommand(Command.CREATE_LINK, new CreateLinkExecutable(textArea));
        Executable unlinkExec =
            getTextArea().getCommandManager().registerCommand(Command.UNLINK, new UnlinkExecutable(textArea));
        if (createLinkExec != null || unlinkExec != null) {
            originalExecutables = new HashMap<Command, Executable>();
        }
        if (createLinkExec != null) {
            originalExecutables.put(Command.CREATE_LINK, createLinkExec);
        }
        if (unlinkExec != null) {
            originalExecutables.put(Command.UNLINK, unlinkExec);
        }

        menuExtension = new LinkMenuExtension(this);
        getUIExtensionList().add(menuExtension);

        // Initialize the metadata extractor, to handle link metadatas
        metaDataExtractor = new LinkMetaDataExtractor();
        // do the initial extracting on the loaded document
        metaDataExtractor.onInnerHTMLChange((Element) getTextArea().getDocument().getDocumentElement());
        getTextArea().getDocument().addInnerHTMLListener(metaDataExtractor);

        // create an empty link handler and add it to the RTA command manager
        linkFilter = new EmptyLinkFilter(getTextArea());
        getTextArea().getCommandManager().addCommandListener(linkFilter);
    }

    /**
     * {@inheritDoc}
     * 
     * @see AbstractPlugin#destroy()
     */
    public void destroy()
    {
        // restore previous executables
        if (originalExecutables != null) {
            for (Map.Entry<Command, Executable> entry : originalExecutables.entrySet()) {
                getTextArea().getCommandManager().registerCommand(entry.getKey(), entry.getValue());
            }
        }

        if (metaDataExtractor != null) {
            getTextArea().getDocument().removeInnerHTMLListener(metaDataExtractor);
            metaDataExtractor = null;
        }

        // remove the empty link filter from the text area
        getTextArea().getCommandManager().removeCommandListener(linkFilter);

        // destroy menu extension
        menuExtension.destroy();
        super.destroy();
    }

    /**
     * Handles the creation of a link of the specified type: prepares and shows the link wizard.
     * 
     * @param linkType the type of link to insert
     */
    public void onLinkInsert(LinkConfig.LinkType linkType)
    {
        LinkConfig linkParams = getLinkParams();
        linkParams.setType(linkType);
        dispatchLinkWizard(linkParams);
    }

    /**
     * Handles the edit of a link: prepares and shows the link wizard.
     */
    public void onLinkEdit()
    {
        LinkConfig editParams = getLinkParams();
        dispatchLinkWizard(editParams);
    }

    /**
     * Instantiates and runs the correct wizard for the passed link.
     * 
     * @param linkParams the parameters of the link to be configured through the wizard
     */
    protected void dispatchLinkWizard(LinkConfig linkParams)
    {
        switch (linkParams.getType()) {
            case WIKIPAGE:
            case NEW_WIKIPAGE:
                getLinkWizard().start(LinkWizardSteps.WIKI_PAGE.toString(), linkParams);
                break;
            case ATTACHMENT:
                getLinkWizard().start(LinkWizardSteps.ATTACHMENT.toString(), linkParams);
                break;
            case EMAIL:
                getLinkWizard().start(LinkWizardSteps.EMAIL.toString(), linkParams);
                break;
            case EXTERNAL:
            default:
                getLinkWizard().start(LinkWizardSteps.WEB_PAGE.toString(), linkParams);
                break;
        }
    }

    /**
     * Returns the link wizard.
     * 
     * @return the link wizard.
     */
    private Wizard getLinkWizard()
    {
        if (linkWizard == null) {
            linkWizard = new LinkWizard(this.getConfig(), wikiService);
            linkWizard.addWizardListener(this);
        }
        return linkWizard;
    }

    /**
     * @return the link parameters for the current position of the cursor.
     */
    protected LinkConfig getLinkParams()
    {
        String configString = getTextArea().getCommandManager().getStringValue(Command.CREATE_LINK);
        if (configString != null) {
            return getEditLinkParams(configString);
        } else {
            return getCreateLinkParams();
        }
    }

    /**
     * Prepares the link parameters for a link edition, from the passed link parameter, as returned by the
     * {@link CreateLinkExecutable#getParameter(RichTextArea)}.
     * 
     * @param linkCommandParameter the parameter of the executed {@link Command#CREATE_LINK} command.
     * @return the link parameters for link editing
     */
    protected LinkConfig getEditLinkParams(String linkCommandParameter)
    {
        LinkConfig linkParam = new LinkConfig();
        linkParam.fromJSON(linkCommandParameter);
        Range range = getTextArea().getDocument().getSelection().getRangeAt(0);
        Element wrappingAnchor = LinkExecutableUtils.getSelectedAnchor(getTextArea());
        // check the content of the wrapping anchor, if it's an image, it should be handled specially
        if (wrappingAnchor.getChildNodes().getLength() == 1
            && wrappingAnchor.getChildNodes().getItem(0).getNodeName().equalsIgnoreCase("img")) {
            Range imageRange = getTextArea().getDocument().createRange();
            imageRange.selectNode(wrappingAnchor.getChildNodes().getItem(0));
            getTextArea().getDocument().getSelection().removeAllRanges();
            getTextArea().getDocument().getSelection().addRange(imageRange);
            String imageParam = getTextArea().getCommandManager().getStringValue(Command.INSERT_IMAGE);
            if (imageParam != null) {
                // it's an image selection, set the label readonly and put the image filename in the label text
                parseLabelFromImage(linkParam, imageParam);
            } else {
                linkParam.setLabelText(wrappingAnchor.getInnerText());
            }
        }
        // move the selection around the link, to replace it properly upon edit
        range.selectNode(wrappingAnchor);
        getTextArea().getDocument().getSelection().removeAllRanges();
        getTextArea().getDocument().getSelection().addRange(range);

        return linkParam;
    }

    /**
     * Prepares the link parameters for a link creation, i.e. sets the link labels.
     * 
     * @return the link parameters for link creation
     */
    protected LinkConfig getCreateLinkParams()
    {
        LinkConfig config = new LinkConfig();
        config.setLabel(getTextArea().getDocument().getSelection().getRangeAt(0).toHTML());
        // Check the special case when the selection is an image and add a link on an image
        String imageParam = getTextArea().getCommandManager().getStringValue(Command.INSERT_IMAGE);
        if (imageParam != null) {
            // it's an image selection, set the label readonly and put the image filename in the label text
            parseLabelFromImage(config, imageParam);
        } else {
            config.setLabelText(getTextArea().getDocument().getSelection().getRangeAt(0).toString());
            config.setReadOnlyLabel(false);
        }
        return config;
    }

    /**
     * Helper method to parse an image execution String parameter and fill the passed {@link LinkConfig} from it.
     * 
     * @param linkConfig the link config to set the label to
     * @param imageParam the image parameter, as returned by the command manager
     */
    protected void parseLabelFromImage(LinkConfig linkConfig, String imageParam)
    {
        ImageConfig imgConfig = new ImageConfig();
        imgConfig.fromJSON(imageParam);
        ResourceName imageResource = new ResourceName(imgConfig.getReference(), true);
        linkConfig.setLabelText(imageResource.getFile());
        linkConfig.setReadOnlyLabel(true);
    }

    /**
     * Executed when the unlink button is clicked.
     */
    public void onUnlink()
    {
        getTextArea().setFocus(true);
        getTextArea().getCommandManager().execute(Command.UNLINK);
    }

    /**
     * {@inheritDoc}. Handles wizard finish by creating the link HTML block from the {@link LinkConfig} setup through
     * the wizard and executing the {@link Command#CREATE_LINK} with it.
     */
    public void onFinish(Wizard sender, Object result)
    {
        // build the HTML block from the configuration data
        String linkHTML = LinkHTMLGenerator.getInstance().getLinkHTML((LinkConfig) result);
        // Return the focus to the rich text area.
        getTextArea().setFocus(true);
        // insert the built HTML
        getTextArea().getCommandManager().execute(Command.CREATE_LINK, linkHTML);
    }

    /**
     * {@inheritDoc}.
     */
    public void onCancel(Wizard sender)
    {
        // return the focus to the text area
        getTextArea().setFocus(true);
    }
}
