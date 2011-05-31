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
package org.xwiki.gwt.wysiwyg.client.plugin.alfresco;

import org.xwiki.gwt.user.client.Config;
import org.xwiki.gwt.user.client.ui.rta.RichTextArea;
import org.xwiki.gwt.user.client.ui.rta.cmd.Command;
import org.xwiki.gwt.user.client.ui.wizard.Wizard;
import org.xwiki.gwt.user.client.ui.wizard.WizardListener;
import org.xwiki.gwt.wysiwyg.client.plugin.alfresco.AlfrescoWizardStepProvider.AlfrescoWizardStep;
import org.xwiki.gwt.wysiwyg.client.plugin.image.ImageConfig;
import org.xwiki.gwt.wysiwyg.client.plugin.image.ImageConfigJSONParser;
import org.xwiki.gwt.wysiwyg.client.plugin.image.ImageConfigJSONSerializer;
import org.xwiki.gwt.wysiwyg.client.plugin.internal.AbstractPlugin;
import org.xwiki.gwt.wysiwyg.client.plugin.link.LinkConfig;
import org.xwiki.gwt.wysiwyg.client.plugin.link.LinkConfig.LinkType;
import org.xwiki.gwt.wysiwyg.client.plugin.link.LinkConfigFactory;
import org.xwiki.gwt.wysiwyg.client.plugin.link.LinkConfigJSONSerializer;
import org.xwiki.gwt.wysiwyg.client.wiki.EntityConfig;
import org.xwiki.gwt.wysiwyg.client.wiki.EntityLink;
import org.xwiki.gwt.wysiwyg.client.wiki.ResourceReference;
import org.xwiki.gwt.wysiwyg.client.wiki.ResourceReference.ResourceType;
import org.xwiki.gwt.wysiwyg.client.wiki.WikiPageReference;
import org.xwiki.gwt.wysiwyg.client.wiki.WikiServiceAsync;

import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.core.client.Scheduler.ScheduledCommand;
import com.google.gwt.user.client.ui.Image;

/**
 * WYSIWYG editor plug-in for inserting Alfresco links and images.
 * 
 * @version $Id$
 */
public class AlfrescoPlugin extends AbstractPlugin implements WizardListener
{
    /**
     * The magic wand used to cast <em>Alfresco</em> spells on the underlying rich text area.
     */
    private Wizard wizard;

    /**
     * Extends the top-level menu of the WYSIWYG editor with entries for inserting Alfresco links and images.
     */
    private AlfrescoMenuExtension menuExtension;

    /**
     * The object used to create link configuration objects.
     */
    private LinkConfigFactory linkConfigFactory;

    /**
     * The service used to parse and serialize resource references.
     */
    private final WikiServiceAsync wikiService;

    /**
     * The object used to create an {@link ImageConfig} from JSON.
     */
    private final ImageConfigJSONParser imageConfigJSONParser = new ImageConfigJSONParser();

    /**
     * The object used to serialize an {@link ImageConfig} instance to JSON.
     */
    private final ImageConfigJSONSerializer imageConfigJSONSerializer = new ImageConfigJSONSerializer();

    /**
     * The object used to serialize a {@link LinkConfig} instance to JSON.
     */
    private final LinkConfigJSONSerializer linkConfigJSONSerializer = new LinkConfigJSONSerializer();

    /**
     * Creates a new instance.
     * 
     * @param wikiService the service used to parse and serialize resource references
     */
    public AlfrescoPlugin(WikiServiceAsync wikiService)
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

        linkConfigFactory = new LinkConfigFactory(textArea);

        menuExtension = new AlfrescoMenuExtension(this);
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
    }

    /**
     * {@inheritDoc}
     * 
     * @see AbstractPlugin#destroy()
     */
    public void destroy()
    {
        menuExtension.clearFeatures();
        if (wizard != null) {
            wizard.removeWizardListener(this);
        }

        super.destroy();
    }

    /**
     * Start the insert/edit Alfresco link wizard.
     */
    public void link()
    {
        LinkConfig linkConfig = linkConfigFactory.createLinkConfig();
        linkConfig.setType(LinkType.EXTERNAL);
        getWizard().start(AlfrescoWizardStep.RESOURCE_REFERENCE_PARSER.toString(), createEntityLink(linkConfig));
    }

    /**
     * Start the insert/edit Alfresco image wizard.
     */
    public void image()
    {
        ImageConfig imageConfig;
        String imageJSON = getTextArea().getCommandManager().getStringValue(Command.INSERT_IMAGE);
        if (imageJSON != null) {
            imageConfig = imageConfigJSONParser.parse(imageJSON);
        } else {
            imageConfig = new ImageConfig();
            imageConfig.setAltText(getTextArea().getDocument().getSelection().getRangeAt(0).toString());
        }
        getWizard().start(AlfrescoWizardStep.RESOURCE_REFERENCE_PARSER.toString(), createEntityLink(imageConfig));
    }

    /**
     * {@inheritDoc}
     * 
     * @see WizardListener#onCancel(Wizard)
     */
    public void onCancel(Wizard sender)
    {
        getTextArea().setFocus(true);
    }

    /**
     * {@inheritDoc}
     * 
     * @see WizardListener#onFinish(Wizard, Object)
     */
    public void onFinish(Wizard sender, Object result)
    {
        getTextArea().setFocus(true);

        @SuppressWarnings("unchecked")
        EntityLink<EntityConfig> entityLink = (EntityLink<EntityConfig>) result;
        if (entityLink.getData() instanceof LinkConfig) {
            // Insert or update the link.
            String linkJSON = linkConfigJSONSerializer.serialize((LinkConfig) entityLink.getData());
            getTextArea().getCommandManager().execute(Command.CREATE_LINK, linkJSON);
        } else if (entityLink.getData() instanceof ImageConfig) {
            // Insert or update the image.
            String imageJSON = imageConfigJSONSerializer.serialize((ImageConfig) entityLink.getData());
            getTextArea().getCommandManager().execute(Command.INSERT_IMAGE, imageJSON);
        }
    }

    /**
     * @param entityConfig the configuration object specific to the link type
     * @return an entity link object that can be passed to the wizard
     */
    private EntityLink<EntityConfig> createEntityLink(EntityConfig entityConfig)
    {
        WikiPageReference origin = new WikiPageReference();
        origin.setWikiName(getConfig().getParameter("wiki"));
        origin.setSpaceName(getConfig().getParameter("space"));
        origin.setPageName(getConfig().getParameter("page"));

        ResourceReference destination = new ResourceReference();
        destination.setEntityReference(origin.getEntityReference().clone());
        destination.setType(ResourceType.URL);
        destination.setTyped(false);

        return new EntityLink<EntityConfig>(origin.getEntityReference(), destination, entityConfig);
    }

    /**
     * @return the Alfresco wizard
     */
    private Wizard getWizard()
    {
        if (wizard == null) {
            Image alfrescoIcon = new Image(AlfrescoImages.INSTANCE.alfrescoIcon());
            wizard = new Wizard(AlfrescoConstants.INSTANCE.wizardTitle(), alfrescoIcon);
            AlfrescoServiceAsync alfrescoService = GWT.create(AlfrescoService.class);
            wizard.setProvider(new AlfrescoWizardStepProvider(wikiService, alfrescoService));
            wizard.addWizardListener(this);
        }
        return wizard;
    }
}
