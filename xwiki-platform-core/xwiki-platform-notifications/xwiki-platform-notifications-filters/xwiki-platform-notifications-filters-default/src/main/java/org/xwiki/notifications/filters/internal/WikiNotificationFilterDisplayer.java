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
package org.xwiki.notifications.filters.internal;

import java.lang.reflect.Type;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.xwiki.component.manager.ComponentManager;
import org.xwiki.component.wiki.WikiComponent;
import org.xwiki.component.wiki.WikiComponentScope;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.notifications.NotificationException;
import org.xwiki.notifications.filters.NotificationFilter;
import org.xwiki.notifications.filters.NotificationFilterDisplayer;
import org.xwiki.notifications.filters.NotificationFilterPreference;
import org.xwiki.rendering.block.Block;
import org.xwiki.script.ScriptContextManager;
import org.xwiki.template.Template;
import org.xwiki.template.TemplateManager;
import org.xwiki.text.StringUtils;

import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.objects.BaseObjectReference;

/**
 * This class is meant to be instanciated and then registered against the Component Manager by the
 * {@link WikiNotificationFilterDisplayerComponentBuilder}.
 *
 * @version $Id$
 * @since 9.7RC1
 */
public class WikiNotificationFilterDisplayer extends AbstractNotificationFilterDisplayer implements WikiComponent
{
    private TemplateManager templateManager;

    private ScriptContextManager scriptContextManager;

    private ComponentManager componentManager;

    private BaseObjectReference objectReference;

    private DocumentReference authorReference;

    private Template filterTemplate;

    private Set<String> supportedFilters;

    private String componentHint;

    /**
     * Constructs a new {@link WikiNotificationFilterDisplayer}.
     *
     * @param authorReference the author reference of the document
     * @param componentManager the {@link ComponentManager} to use
     * @param baseObject the XObject which has the required properties to instantiate the component
     * @throws NotificationException if the properties of the given BaseObject could not be loaded
     */
    public WikiNotificationFilterDisplayer(DocumentReference authorReference, ComponentManager componentManager,
            BaseObject baseObject)
            throws NotificationException
    {
        this.objectReference = baseObject.getReference();
        this.componentManager = componentManager;
        this.authorReference = authorReference;

        try {
            // Extract the templateManager and the scriptContextManager
            templateManager = componentManager.getInstance(TemplateManager.class);
            scriptContextManager = componentManager.getInstance(ScriptContextManager.class);

            // Extract the supported displayer filters from the given baseObject
            supportedFilters = new HashSet<String>(
                    baseObject.getListValue(WikiNotificationFilterDisplayerDocumentInitializer.SUPPORTED_FILTERS));

            componentHint = generateComponentHint();

            // Create the template from the given BaseObject property
            String xObjectTemplate = baseObject.getStringValue(
                    WikiNotificationFilterDisplayerDocumentInitializer.FILTER_TEMPLATE);
            if (!StringUtils.isBlank(xObjectTemplate)) {
                filterTemplate = templateManager.createStringTemplate(
                        xObjectTemplate, this.getAuthorReference());
            } else {
                filterTemplate = null;
            }

        } catch (Exception e) {
            throw new NotificationException(
                    String.format("Unable to construct a new WikiNotificationFilterDisplayer from"
                                    + " the base object [%s]", baseObject), e);
        }
    }

    /**
     * Generate a string that will be the component hint based on the set of supported fiters.
     *
     * @return the stringified set of supported filters
     */
    private String generateComponentHint()
    {
        Iterator<String> it = supportedFilters.iterator();
        StringBuilder builder = new StringBuilder();


        while (it.hasNext()) {
            builder.append(String.format("%s-", it.next()));
        }

        builder.append("wikiComponent");
        return builder.toString();
    }

    @Override
    public Block display(NotificationFilter filter, NotificationFilterPreference preference)
        throws NotificationException
    {
        Map<String, Object> backup = setUpContext(this.scriptContextManager, filter, preference);

        try {
            // If we have no template defined, fallback on the default displayer.
            if (filterTemplate == null) {
                return ((NotificationFilterDisplayer) componentManager.getInstance(NotificationFilterDisplayer.class))
                    .display(filter, preference);
            }

            return templateManager.execute(filterTemplate);

        } catch (Exception e) {
            throw new NotificationException(String
                .format("Unable to display the notification filter template for the filters [%s].", componentHint), e);
        } finally {
            cleanUpContext(this.scriptContextManager, backup);
        }
    }

    @Override
    public Set<String> getSupportedFilters()
    {
        return supportedFilters;
    }

    @Override
    public DocumentReference getDocumentReference()
    {
        return (DocumentReference) objectReference.getParent();
    }

    @Override
    public EntityReference getEntityReference()
    {
        return objectReference;
    }

    @Override
    public DocumentReference getAuthorReference()
    {
        return authorReference;
    }

    @Override
    public Type getRoleType()
    {
        return NotificationFilterDisplayer.class;
    }

    @Override
    public String getRoleHint()
    {
        return componentHint;
    }

    @Override
    public WikiComponentScope getScope()
    {
        return WikiComponentScope.WIKI;
    }
}
