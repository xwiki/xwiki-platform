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
package org.xwiki.livedata.internal.livetable;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import org.apache.commons.lang3.StringUtils;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.livedata.LiveDataException;
import org.xwiki.livedata.livetable.LiveTableNewRowNamingStrategy;
import org.xwiki.model.reference.DocumentReference;

import com.xpn.xwiki.XWikiException;

/**
 * Resolves the page a new live table entry is created as, and the template it starts from.
 *
 * @version $Id$
 * @since 18.10.0RC1
 */
@Component(roles = LiveTableNewEntryResolver.class)
@Singleton
public class LiveTableNewEntryResolver
{
    /**
     * A new entry.
     *
     * @param reference the page the entry is created as
     * @param template the template the entry starts from, or {@code null} if it has none
     */
    public record NewEntry(DocumentReference reference, DocumentReference template)
    {
    }

    @Inject
    private ComponentManager componentManager;

    @Inject
    private NewRowTemplateProviderResolver templateProviderResolver;

    /**
     * Resolves the page and the template of a new entry.
     *
     * @param parameters the live data source parameters
     * @param entryValues the values the entry is created with
     * @return the new entry
     * @throws LiveDataException if the entry cannot be created
     */
    public NewEntry resolve(Map<String, Object> parameters, Map<String, Object> entryValues)
        throws LiveDataException
    {
        DocumentReference reference = generateReference(parameters, entryValues);
        Optional<DocumentReference> template = this.templateProviderResolver.getTemplate(parameters);
        if (template.isPresent() && !this.templateProviderResolver.hasRequiredRights(parameters, reference)) {
            throw new LiveDataException(String.format(
                "Cannot create [%s]: the current user doesn't have the required rights of the template [%s].",
                reference, template.get()));
        }
        return new NewEntry(reference, template.orElse(null));
    }

    private DocumentReference generateReference(Map<String, Object> parameters, Map<String, Object> entryValues)
        throws LiveDataException
    {
        String namingStrategy = (String) parameters.get("newRowNamingStrategy");
        if (!this.componentManager.hasComponent(LiveTableNewRowNamingStrategy.class, namingStrategy)) {
            throw new LiveDataException(String.format("Unsupported row naming strategy [%s].", namingStrategy));
        }
        try {
            LiveTableNewRowNamingStrategy strategy =
                this.componentManager.getInstance(LiveTableNewRowNamingStrategy.class, namingStrategy);
            checkMandatoryProperties(strategy.getMandatoryProperties(parameters), entryValues);
            return strategy.generate(parameters, entryValues);
        } catch (ComponentLookupException e) {
            throw new LiveDataException(
                String.format("Failed to instantiate the row naming strategy [%s].", namingStrategy), e);
        } catch (XWikiException e) {
            throw new LiveDataException("An error occurred while generating the new entry reference.", e);
        }
    }

    private void checkMandatoryProperties(Set<String> mandatoryProperties, Map<String, Object> entryValues)
        throws LiveDataException
    {
        List<String> missingProperties = mandatoryProperties.stream()
            .filter(property -> StringUtils.isBlank(Objects.toString(entryValues.get(property), null)))
            .sorted()
            .toList();
        if (!missingProperties.isEmpty()) {
            throw new LiveDataException(
                String.format("Cannot create an entry without a value for %s.", missingProperties));
        }
    }
}
