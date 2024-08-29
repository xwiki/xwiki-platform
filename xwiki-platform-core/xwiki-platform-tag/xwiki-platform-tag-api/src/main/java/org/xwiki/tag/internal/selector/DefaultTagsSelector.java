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
import javax.inject.Singleton;

import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.component.phase.Initializable;
import org.xwiki.component.phase.InitializationException;
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
public class DefaultTagsSelector implements TagsSelector, Initializable
{
    @Inject
    @Named("xwikiproperties")
    private ConfigurationSource configurationSource;

    @Inject
    @Named(HINT)
    private TagsSelector exhaustiveTagsSelector;

    @Inject
    private ComponentManager componentManager;

    @Inject
    private Logger logger;

    private TagsSelector tagsSelector;

    @Override
    public void initialize() throws InitializationException
    {
        String hint = this.configurationSource.getProperty("tag.rightCheckStrategy.hint");
        if (hint == null) {
            this.tagsSelector = this.exhaustiveTagsSelector;
        } else {
            try {
                this.tagsSelector = this.componentManager.getInstance(TagsSelector.class, hint);
            } catch (ComponentLookupException e) {
                this.logger.error("Failed to get component [{}] with hint [{}]. Falling back to [{}].",
                    TagsSelector.class, hint, HINT);
                this.tagsSelector = this.exhaustiveTagsSelector;
            }
        }
    }

    @Override
    public List<String> getAllTags() throws TagException
    {
        return this.tagsSelector.getAllTags();
    }

    @Override
    public Map<String, Integer> getTagCountForQuery(String fromHql, String whereHql, List<?> parameterValues)
        throws TagException
    {
        return this.tagsSelector.getTagCountForQuery(fromHql, whereHql, parameterValues);
    }

    @Override
    public Map<String, Integer> getTagCountForQuery(String fromHql, String whereHql, Map<String, ?> parameters)
        throws TagException
    {
        return this.tagsSelector.getTagCountForQuery(fromHql, whereHql, parameters);
    }

    @Override
    public List<String> getDocumentsWithTag(String tag, boolean includeHiddenDocuments) throws TagException
    {
        return this.tagsSelector.getDocumentsWithTag(tag, includeHiddenDocuments);
    }
}
