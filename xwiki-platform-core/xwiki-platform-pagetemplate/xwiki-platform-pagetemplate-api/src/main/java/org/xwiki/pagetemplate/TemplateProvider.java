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
package org.xwiki.pagetemplate;

import java.util.List;
import java.util.Optional;

import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.SpaceReference;
import org.xwiki.stability.Unstable;
import org.xwiki.text.XWikiToStringBuilder;

/**
 * A template provider, which offers a page template when creating new pages, as defined by an
 * {@code XWiki.TemplateProviderClass} object.
 *
 * @version $Id$
 * @since 18.10.0RC1
 */
@Unstable
public class TemplateProvider
{
    private final DocumentReference reference;

    private String name;

    private String description;

    private String icon;

    private DocumentReference templateReference;

    private String action;

    private boolean terminal;

    private List<SpaceReference> visibilityRestrictions = List.of();

    private List<SpaceReference> creationRestrictions = List.of();

    private boolean creationRestrictionsAreSuggestions;

    /**
     * @param reference the reference of the document holding the template provider object
     */
    public TemplateProvider(DocumentReference reference)
    {
        this.reference = reference;
    }

    /**
     * @return the reference of the document holding the template provider object
     */
    public DocumentReference getReference()
    {
        return this.reference;
    }

    /**
     * @return the name of the template provider, either plain text or a translation key, {@code null} if not set
     */
    public String getName()
    {
        return this.name;
    }

    /**
     * @param name the name of the template provider, either plain text or a translation key
     */
    public void setName(String name)
    {
        this.name = name;
    }

    /**
     * @return the description of the template provider, either plain text or a translation key, {@code null} if
     *     not set
     */
    public String getDescription()
    {
        return this.description;
    }

    /**
     * @param description the description of the template provider, either plain text or a translation key
     */
    public void setDescription(String description)
    {
        this.description = description;
    }

    /**
     * @return the name of the icon representing the template provider, {@code null} if not set
     */
    public String getIcon()
    {
        return this.icon;
    }

    /**
     * @param icon the name of the icon representing the template provider
     */
    public void setIcon(String icon)
    {
        this.icon = icon;
    }

    /**
     * @return the reference of the template document, empty if the template provider doesn't define one
     */
    public Optional<DocumentReference> getTemplateReference()
    {
        return Optional.ofNullable(this.templateReference);
    }

    /**
     * @param templateReference the reference of the template document
     */
    public void setTemplateReference(DocumentReference templateReference)
    {
        this.templateReference = templateReference;
    }

    /**
     * @return the action to perform after creating a page ({@code edit}, {@code saveandedit} or
     *     {@code saveandview}), {@code null} if not set
     */
    public String getAction()
    {
        return this.action;
    }

    /**
     * @param action the action to perform after creating a page
     */
    public void setAction(String action)
    {
        this.action = action;
    }

    /**
     * @return {@code true} if the created pages are terminal, {@code false} if they are nested pages
     */
    public boolean isTerminal()
    {
        return this.terminal;
    }

    /**
     * @param terminal {@code true} if the created pages are terminal, {@code false} if they are nested pages
     */
    public void setTerminal(boolean terminal)
    {
        this.terminal = terminal;
    }

    /**
     * @return the spaces where the template provider is offered, empty if it is offered everywhere
     */
    public List<SpaceReference> getVisibilityRestrictions()
    {
        return this.visibilityRestrictions;
    }

    /**
     * @param visibilityRestrictions the spaces where the template provider is offered
     */
    public void setVisibilityRestrictions(List<SpaceReference> visibilityRestrictions)
    {
        this.visibilityRestrictions = List.copyOf(visibilityRestrictions);
    }

    /**
     * @return the spaces where pages can be created with the template provider, empty if they can be created anywhere
     */
    public List<SpaceReference> getCreationRestrictions()
    {
        return this.creationRestrictions;
    }

    /**
     * @param creationRestrictions the spaces where pages can be created with the template provider
     */
    public void setCreationRestrictions(List<SpaceReference> creationRestrictions)
    {
        this.creationRestrictions = List.copyOf(creationRestrictions);
    }

    /**
     * @return {@code true} if the creation restrictions are only suggestions, {@code false} if they are enforced
     */
    public boolean areCreationRestrictionsSuggestions()
    {
        return this.creationRestrictionsAreSuggestions;
    }

    /**
     * @param creationRestrictionsAreSuggestions {@code true} if the creation restrictions are only suggestions,
     *     {@code false} if they are enforced
     */
    public void setCreationRestrictionsAreSuggestions(boolean creationRestrictionsAreSuggestions)
    {
        this.creationRestrictionsAreSuggestions = creationRestrictionsAreSuggestions;
    }

    @Override
    public String toString()
    {
        return new XWikiToStringBuilder(this)
            .append("reference", this.reference)
            .append("name", this.name)
            .append("templateReference", this.templateReference)
            .append("action", this.action)
            .append("terminal", this.terminal)
            .append("visibilityRestrictions", this.visibilityRestrictions)
            .append("creationRestrictions", this.creationRestrictions)
            .append("creationRestrictionsAreSuggestions", this.creationRestrictionsAreSuggestions)
            .toString();
    }
}
