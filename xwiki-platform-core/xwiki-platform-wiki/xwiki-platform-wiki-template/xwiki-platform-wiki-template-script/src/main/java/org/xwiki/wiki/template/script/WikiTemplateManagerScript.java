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
package org.xwiki.wiki.template.script;

import java.util.ArrayList;
import java.util.Collection;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.model.reference.WikiReference;
import org.xwiki.script.service.ScriptService;
import org.xwiki.security.authorization.AuthorizationManager;
import org.xwiki.security.authorization.Right;
import org.xwiki.wiki.WikiDescriptor;
import org.xwiki.wiki.template.WikiTemplateManager;
import org.xwiki.wiki.template.WikiTemplateManagerException;

import com.xpn.xwiki.XWikiContext;

@Component
@Named("wiki.template")
@Singleton
public class WikiTemplateManagerScript implements ScriptService
{
    @Inject
    private WikiTemplateManager wikiTemplateManager;

    @Inject
    private AuthorizationManager authorizationManager;

    /**
     * Used to access current {@link com.xpn.xwiki.XWikiContext}.
     */
    @Inject
    private Provider<XWikiContext> xcontextProvider;

    /**
     * Get the list of all wiki templates.
     *
     * @return list of wiki templates
     */
    public Collection<WikiDescriptor> getTemplates()
    {
        try {
            return wikiTemplateManager.getTemplates();
        } catch (WikiTemplateManagerException e) {
            return new ArrayList<WikiDescriptor>();
        }
    }

    /**
     * Set if the specified wiki is a template or not.
     *
     * @param wikiId the ID of the wiki to specify
     * @param value whether or not the wiki is a template
     * @return true if the action succeed
     */
    public boolean setTemplate(String wikiId, boolean value)
    {
        try {
            wikiTemplateManager.setTemplate(wikiId, value);
            return true;
        } catch (WikiTemplateManagerException e) {
            return false;
        }
    }

    /**
     * @param wikiId The id of the wiki to test
     * @return if the wiki is a template or not
     * @throws WikiTemplateManagerException if problems occur
     */
    public boolean isTemplate(String wikiId) throws WikiTemplateManagerException
    {
        return wikiTemplateManager.isTemplate(wikiId);
    }

    /**
     * Create a new wiki from the specified template.
     *
     * @param newWikiId ID of the wiki to create
     * @param newWikiAlias Default alias of the wiki to create
     * @param templateDescriptor Descriptor of the template to use
     * @return The descriptor of the new wiki or null if problems occur
     */
    public WikiDescriptor createWikiFromTemplate(String newWikiId, String newWikiAlias,
            WikiDescriptor templateDescriptor)
    {
        WikiDescriptor descriptor = null;
        try {
            XWikiContext context = xcontextProvider.get();
            if (authorizationManager.hasAccess(Right.CREATE_WIKI, context.getUserReference(),
                    new WikiReference(context.getMainXWiki())))
            {
                descriptor = wikiTemplateManager.createWikiFromTemplate(newWikiId, newWikiAlias, templateDescriptor);
            }
        } catch (WikiTemplateManagerException e) {
        }
        return descriptor;
    }
}
