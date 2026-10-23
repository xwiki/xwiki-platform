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

import java.util.Map;
import java.util.Objects;
import java.util.Set;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;

import org.apache.commons.lang3.StringUtils;
import org.xwiki.component.annotation.Component;
import org.xwiki.livedata.LiveDataException;
import org.xwiki.livedata.livetable.LiveTableNewRowNamingStrategy;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.SpaceReference;
import org.xwiki.model.validation.EntityNameValidation;
import org.xwiki.model.validation.EntityNameValidationConfiguration;
import org.xwiki.model.validation.EntityNameValidationManager;
import org.xwiki.stability.Unstable;

import com.xpn.xwiki.XWikiException;

/**
 * {@link LiveTableNewRowNamingStrategy} that generates a page in a configured location, named after the title given
 * by the user.
 *
 * @version $Id$
 * @since 18.10.0RC1
 */
@Component
@Named("title")
@Singleton
@Unstable
public class TitleLiveTableNewRowNamingStrategy extends AbstractLocationLiveTableNewRowNamingStrategy
{
    /**
     * The property the new entries are named after.
     */
    private static final String TITLE_PROPERTY = "doc.title";

    @Inject
    private ModelBridge modelBridge;

    @Inject
    private EntityNameValidationManager entityNameValidationManager;

    @Inject
    private EntityNameValidationConfiguration entityNameValidationConfiguration;

    @Override
    public DocumentReference generate(Map<String, Object> parameters, Map<String, Object> entryValues)
        throws LiveDataException, XWikiException
    {
        SpaceReference location = getRequiredLocation(parameters);
        String name = getName(entryValues);
        DocumentReference candidate = new DocumentReference(name, location);
        // The title is what the user typed, so two entries can legitimately want the same name. The extra entries get
        // a suffix rather than a failure.
        for (int suffix = 1; this.modelBridge.exists(candidate); suffix++) {
            candidate = new DocumentReference(String.format("%s-%d", name, suffix), location);
        }
        // Known limitation: if another user creates a page with the same name before this entry is saved, the save
        // fails. This is very unlikely, and saving the row again then picks the next free name.
        return candidate;
    }

    @Override
    public Set<String> getMandatoryProperties(Map<String, Object> parameters)
    {
        return Set.of(TITLE_PROPERTY);
    }

    /**
     * @param entryValues the values the entry is created with
     * @return the name of the page holding the entry, derived from its title
     * @throws LiveDataException when the title is missing, or holds nothing a name can be built from
     */
    protected String getName(Map<String, Object> entryValues) throws LiveDataException
    {
        String title = Objects.toString(entryValues.get(TITLE_PROPERTY), null);
        if (StringUtils.isBlank(title)) {
            throw new LiveDataException(
                String.format("Missing [%s] value, which the new entry is named after.", TITLE_PROPERTY));
        }
        String name = title.trim();
        EntityNameValidation nameStrategy = this.entityNameValidationManager.getEntityReferenceNameStrategy();
        // Admins can turn the transformation off, in which case the other ways of creating a page keep the title as is.
        if (this.entityNameValidationConfiguration.useTransformation() && nameStrategy != null) {
            name = nameStrategy.transform(name);
            // A name strategy is free to strip every character of the title.
            if (StringUtils.isBlank(name)) {
                throw new LiveDataException(String.format("No page name can be built from the title [%s].", title));
            }
        }
        return name;
    }
}
