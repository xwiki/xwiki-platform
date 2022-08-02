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
package org.xwiki.rendering.internal.transformation;

import java.util.Arrays;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.apache.commons.lang3.StringUtils;
import org.xwiki.component.annotation.Component;
import org.xwiki.container.Container;
import org.xwiki.rendering.transformation.Transformation;

/**
 * Override the default component implementation to be able to define the list of transformations to execute defined
 * in the query string using the {@code transformations} parameter.
 *
 * @version $Id$
 * @since 12.10.4
 * @since 13.1RC1
 */
@Component
@Singleton
public class XWikiTransformationManager extends DefaultTransformationManager
{
    @Inject
    private Container container;

    /**
     * @return the ordered list of Transformations to execute
     */
    public List<Transformation> getTransformations()
    {
        // The transformations to execute are computed using the following order:
        // - If the query string contains a "transformations" parameter, use it.
        // - Otherwise, get the list from XWiki's configuration.
        List<String> transformationNames;
        String transformationNamesString = getTransformationsRequestProperty();
        if (transformationNamesString != null) {
            transformationNames = Arrays.asList(StringUtils.split(transformationNamesString, ","));
        } else {
            transformationNames = this.configuration.getTransformationNames();
        }
        return getTransformations(transformationNames);
    }

    private String getTransformationsRequestProperty()
    {
        String transformationNamesString = null;
        if (this.container.getRequest() != null) {
            Object object = this.container.getRequest().getProperty("transformations");
            if (object instanceof String) {
                transformationNamesString = (String) object;
            }
        }
        return transformationNamesString;
    }
}
