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
package org.xwiki.gwt.wysiwyg.client.plugin.link.ui;

import java.util.HashMap;
import java.util.Map;

import org.xwiki.gwt.user.client.Config;
import org.xwiki.gwt.user.client.ui.wizard.Wizard;
import org.xwiki.gwt.wysiwyg.client.Images;
import org.xwiki.gwt.wysiwyg.client.Strings;
import org.xwiki.gwt.wysiwyg.client.plugin.link.LinkConfig;
import org.xwiki.gwt.wysiwyg.client.plugin.link.LinkConfig.LinkType;
import org.xwiki.gwt.wysiwyg.client.wiki.EntityLink;
import org.xwiki.gwt.wysiwyg.client.wiki.ResourceReference;
import org.xwiki.gwt.wysiwyg.client.wiki.ResourceReference.ResourceType;
import org.xwiki.gwt.wysiwyg.client.wiki.WikiPageReference;
import org.xwiki.gwt.wysiwyg.client.wiki.WikiServiceAsync;

import com.google.gwt.user.client.ui.Image;

/**
 * The link wizard, used to configure link parameters in a {@link LinkConfig} object, in successive steps.
 * 
 * @version $Id$
 */
public class LinkWizard extends Wizard
{
    /**
     * Enumeration steps handled by this link wizard.
     */
    public static enum LinkWizardStep
    {
        /** The step that parses the link reference. */
        LINK_REFERENCE_PARSER,

        /** The step that select a wiki page. */
        WIKI_PAGE,

        /** The step that selects a new wiki page. */
        WIKI_PAGE_CREATOR,

        /** The step that selects an attachment. */
        ATTACHMENT,

        /** The step that uploads a new attachment. */
        ATTACHMENT_UPLOAD,

        /** The step that select a web page specified by its URL. */
        WEB_PAGE,

        /** The step that selects an email address. */
        EMAIL,

        /** The step that configures the link parameters. */
        LINK_CONFIG,

        /** The step that serializes the link reference. */
        LINK_REFERENCE_SERIALIZER
    };

    /**
     * Maps a link wizard step to the type of resource that step creates links to.
     */
    private static final Map<LinkType, ResourceType> LINK_TYPE_TO_RESOURCE_TYPE_MAP;

    /**
     * The resource currently edited by this WYSIWYG, used to determine the context in which link creation takes place.
     */
    private final Config config;

    static {
        LINK_TYPE_TO_RESOURCE_TYPE_MAP = new HashMap<LinkType, ResourceType>();
        LINK_TYPE_TO_RESOURCE_TYPE_MAP.put(LinkType.WIKIPAGE, ResourceType.DOCUMENT);
        LINK_TYPE_TO_RESOURCE_TYPE_MAP.put(LinkType.NEW_WIKIPAGE, ResourceType.DOCUMENT);
        LINK_TYPE_TO_RESOURCE_TYPE_MAP.put(LinkType.ATTACHMENT, ResourceType.ATTACHMENT);
        LINK_TYPE_TO_RESOURCE_TYPE_MAP.put(LinkType.EXTERNAL, ResourceType.URL);
        LINK_TYPE_TO_RESOURCE_TYPE_MAP.put(LinkType.EMAIL, ResourceType.MAILTO);
    }

    /**
     * Builds a {@link LinkWizard} from the passed {@link Config}. The configuration is used to get WYSIWYG editor
     * specific information for this wizard, such as the current page, etc.
     * 
     * @param config the context configuration for this {@link LinkWizard}
     * @param wikiService the service used to access the wiki
     */
    public LinkWizard(Config config, WikiServiceAsync wikiService)
    {
        super(Strings.INSTANCE.link(), new Image(Images.INSTANCE.link()));
        this.config = config;
        this.setProvider(new LinkWizardStepProvider(config, wikiService));
    }

    /**
     * {@inheritDoc}
     * 
     * @see Wizard#start(String, Object)
     */
    @Override
    public void start(String startStep, Object data)
    {
        WikiPageReference origin = new WikiPageReference();
        origin.setWikiName(config.getParameter("wiki"));
        origin.setSpaceName(config.getParameter("space"));
        origin.setPageName(config.getParameter("page"));

        LinkConfig linkConfig = (LinkConfig) data;
        ResourceReference destination = new ResourceReference();
        destination.setType(LINK_TYPE_TO_RESOURCE_TYPE_MAP.get(linkConfig.getType()));

        super.start(startStep, new EntityLink<LinkConfig>(origin.getEntityReference(), destination, linkConfig));
    }

    /**
     * {@inheritDoc}
     * 
     * @see Wizard#getResult()
     */
    @SuppressWarnings("unchecked")
    @Override
    protected Object getResult()
    {
        return ((EntityLink<LinkConfig>) super.getResult()).getData();
    }
}
