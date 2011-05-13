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
import org.xwiki.gwt.user.client.Config;
import org.xwiki.gwt.user.client.ui.rta.RichTextArea;
import org.xwiki.gwt.user.client.ui.rta.cmd.Command;
import org.xwiki.gwt.user.client.ui.rta.cmd.Executable;
import org.xwiki.gwt.user.client.ui.wizard.Wizard;
import org.xwiki.gwt.user.client.ui.wizard.WizardListener;
import org.xwiki.gwt.wysiwyg.client.plugin.internal.AbstractPlugin;
import org.xwiki.gwt.wysiwyg.client.plugin.link.exec.CreateLinkExecutable;
import org.xwiki.gwt.wysiwyg.client.plugin.link.exec.UnlinkExecutable;
import org.xwiki.gwt.wysiwyg.client.plugin.link.ui.LinkWizard;
import org.xwiki.gwt.wysiwyg.client.plugin.link.ui.LinkWizard.LinkWizardStep;
import org.xwiki.gwt.wysiwyg.client.wiki.WikiServiceAsync;

import com.google.gwt.core.client.Scheduler;
import com.google.gwt.core.client.Scheduler.ScheduledCommand;

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
     * The object used to create link configuration objects.
     */
    private LinkConfigFactory linkConfigFactory;

    /**
     * The service used to access the wiki.
     */
    private final WikiServiceAsync wikiService;

    /**
     * The object used to serialize a {@link LinkConfig} instance to JSON.
     */
    private final LinkConfigJSONSerializer linkConfigJSONSerializer = new LinkConfigJSONSerializer();

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

        // Register custom executables.
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
        // Hack: We can access the menus where each menu item was placed only after the main menu bar is initialized,
        // which happens after all the plugins are loaded.
        Scheduler.get().scheduleDeferred(new ScheduledCommand()
        {
            public void execute()
            {
                menuExtension.registerAttachHandlers();
            }
        });

        // Initialize the meta data extractor to handle link meta data.
        metaDataExtractor = new LinkMetaDataExtractor();
        // Do the initial extracting on the loaded document.
        metaDataExtractor.onInnerHTMLChange((Element) getTextArea().getDocument().getDocumentElement());
        getTextArea().getDocument().addInnerHTMLListener(metaDataExtractor);

        // Create an empty link handler and add it to the command manager
        linkFilter = new EmptyLinkFilter(getTextArea());
        getTextArea().getCommandManager().addCommandListener(linkFilter);

        // Initialize the link configuration factory.
        linkConfigFactory = new LinkConfigFactory(textArea);
    }

    /**
     * {@inheritDoc}
     * 
     * @see AbstractPlugin#destroy()
     */
    public void destroy()
    {
        // Restore previous executables.
        if (originalExecutables != null) {
            for (Map.Entry<Command, Executable> entry : originalExecutables.entrySet()) {
                getTextArea().getCommandManager().registerCommand(entry.getKey(), entry.getValue());
            }
        }

        if (metaDataExtractor != null) {
            getTextArea().getDocument().removeInnerHTMLListener(metaDataExtractor);
            metaDataExtractor = null;
        }

        // Remove the empty link filter from the text area.
        getTextArea().getCommandManager().removeCommandListener(linkFilter);

        // Destroy menu extension.
        menuExtension.clearFeatures();
        super.destroy();
    }

    /**
     * Handles the creation of a link of the specified type: prepares and shows the link wizard.
     * 
     * @param linkType the type of link to insert
     */
    public void onLinkInsert(LinkConfig.LinkType linkType)
    {
        LinkConfig linkConfig = linkConfigFactory.createLinkConfig();
        linkConfig.setType(linkType);
        dispatchLinkWizard(linkConfig);
    }

    /**
     * Handles the edit of a link: prepares and shows the link wizard.
     */
    public void onLinkEdit()
    {
        dispatchLinkWizard(linkConfigFactory.createLinkConfig());
    }

    /**
     * Instantiates and runs the correct wizard for the passed link.
     * 
     * @param linkConfig the link configuration object to be passed to the wizard
     */
    protected void dispatchLinkWizard(LinkConfig linkConfig)
    {
        switch (linkConfig.getType()) {
            case WIKIPAGE:
            case NEW_WIKIPAGE:
                getLinkWizard().start(LinkWizardStep.WIKI_PAGE.toString(), linkConfig);
                break;
            case ATTACHMENT:
                getLinkWizard().start(LinkWizardStep.ATTACHMENT.toString(), linkConfig);
                break;
            case EMAIL:
                getLinkWizard().start(LinkWizardStep.EMAIL.toString(), linkConfig);
                break;
            case EXTERNAL:
            default:
                getLinkWizard().start(LinkWizardStep.WEB_PAGE.toString(), linkConfig);
                break;
        }
    }

    /**
     * @return the link wizard
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
     * Executed when the unlink button is clicked.
     */
    public void onUnlink()
    {
        getTextArea().setFocus(true);
        getTextArea().getCommandManager().execute(Command.UNLINK);
    }

    /**
     * {@inheritDoc}
     * <p>
     * Handles wizard finish by creating the link HTML block from the {@link LinkConfig} setup through the wizard and
     * executing the {@link Command#CREATE_LINK} with it.
     * 
     * @see WizardListener#onFinish(Wizard, Object)
     */
    public void onFinish(Wizard sender, Object result)
    {
        // Return the focus to the rich text area.
        getTextArea().setFocus(true);
        // Insert of update the link.
        String linkJSON = linkConfigJSONSerializer.serialize((LinkConfig) result);
        getTextArea().getCommandManager().execute(Command.CREATE_LINK, linkJSON);
    }

    /**
     * {@inheritDoc}
     * 
     * @see WizardListener#onCancel(Wizard)
     */
    public void onCancel(Wizard sender)
    {
        // Return the focus to the text area.
        getTextArea().setFocus(true);
    }
}
