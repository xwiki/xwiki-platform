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
package org.xwiki.tag.internal.selector;

import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.configuration.ConfigurationSource;
import org.xwiki.tag.internal.TagException;
import org.xwiki.tag.internal.TagsSelector;

import static org.xwiki.tag.internal.selector.ExhaustiveCheckTagsSelector.HINT;

/**
 * Default of {@link TagsSelector}. Selects an implementation of {@link TagsSelector} using the hint provided by the
 * {@code tag.rightCheckStrategy.hint} property from {@code xwiki.properties}. The method call are then redirected to
 * this implementation.
 *
 * @version $Id$
 * @since 15.0RC1
 * @since 14.4.8
 * @since 14.10.4
 */
@Component
@Singleton
public class DefaultTagsSelector implements TagsSelector
{
    @Inject
    @Named("xwikiproperties")
    private ConfigurationSource configurationSource;

    @Inject
    @Named(HINT)
    private TagsSelector exhaustiveTagsSelector;

    @Inject
    @Named("context")
    private Provider<ComponentManager> componentManagerProvider;

    @Inject
    private Logger logger;

    @Override
    public List<String> getAllTags() throws TagException
    {
        return getTagsSelector().getAllTags();
    }

    @Override
    public Map<String, Integer> getTagCountForQuery(String fromHql, String whereHql, List<?> parameterValues)
        throws TagException
    {
        return getTagsSelector().getTagCountForQuery(fromHql, whereHql, parameterValues);
    }

    @Override
    public Map<String, Integer> getTagCountForQuery(String fromHql, String whereHql, Map<String, ?> parameters)
        throws TagException
    {
        return getTagsSelector().getTagCountForQuery(fromHql, whereHql, parameters);
    }

    @Override
    public List<String> getDocumentsWithTag(String tag, boolean includeHiddenDocuments, boolean caseSensitive)
        throws TagException
    {
        return getTagsSelector().getDocumentsWithTag(tag, includeHiddenDocuments, caseSensitive);
    }

    /**
     * The configuration is read on each call, and not once at initialization, so that the strategy can be changed
     * without restarting the instance. The lookup is negligible compared to the queries the returned implementation
     * performs.
     *
     * @return the implementation configured by the {@code tag.rightCheckStrategy.hint} property, falling back on the
     *     exhaustive one when the property is unset or names an unknown component
     */
    private TagsSelector getTagsSelector()
    {
        String hint = this.configurationSource.getProperty("tag.rightCheckStrategy.hint");

        if (hint == null || HINT.equals(hint)) {
            return this.exhaustiveTagsSelector;
        }

        try {
            return this.componentManagerProvider.get().getInstance(TagsSelector.class, hint);
        } catch (ComponentLookupException e) {
            this.logger.error("Failed to get component [{}] with hint [{}]. Falling back to [{}].", TagsSelector.class,
                hint, HINT, e);
            return this.exhaustiveTagsSelector;
        }
    }
}
