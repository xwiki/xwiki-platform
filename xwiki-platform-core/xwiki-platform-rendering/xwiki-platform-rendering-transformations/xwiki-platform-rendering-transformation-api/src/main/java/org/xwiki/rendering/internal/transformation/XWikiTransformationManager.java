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
import org.xwiki.bridge.DocumentAccessBridge;
import org.xwiki.component.annotation.Component;
import org.xwiki.container.Container;
import org.xwiki.rendering.block.Block;
import org.xwiki.rendering.transformation.Transformation;
import org.xwiki.rendering.transformation.TransformationContext;
import org.xwiki.rendering.transformation.TransformationException;
import org.xwiki.rendering.transformation.XWikiTransformationContext;
import org.xwiki.security.authorization.AuthorExecutor;

/**
 * Override the default component implementation to be able to define the list of transformations to execute defined in
 * the query string using the {@code transformations} parameter.
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

    @Inject
    private AuthorExecutor authorExecutor;

    @Inject
    private DocumentAccessBridge documentAccessBridge;

    @Override
    public void performTransformations(Block block, TransformationContext transformationContext)
        throws TransformationException
    {
        if (transformationContext instanceof XWikiTransformationContext xwikiTransformationContext) {
            try {
                // Even if the content on which transformations are executed has an associated document, we can't be
                // sure who is the author of the content so we assume it's the current user (i.e. the content may have
                // changes compared to the saved version of the document, and these changes are attributed to the
                // current user).
                //
                // Note that there are two main cases we need to protect against:
                // * a user executing transformations that require more access rights that they have; we hope to prevent
                // this by treating the current user as the last author of the transformed content and by associating
                // the proper content document (to check access rights for)
                // * a user executing transformations for which they have the required access rights but the content has
                // been modified by someone else, with less access rights; this is handled at the level of the script
                // calling this API, e.g. by checking the presence of the CSRF token, i.e. by making sure the current
                // user is aware that transformations are going to be executed.
                this.authorExecutor.call(() -> {
                    superPerformTransformations(block, transformationContext);
                    return null;
                }, this.documentAccessBridge.getCurrentUserReference(),
                    xwikiTransformationContext.getContentDocumentReference());
            } catch (Exception e) {
                throw new TransformationException("Failed to execute transformations", e);
            }
        } else {
            superPerformTransformations(block, transformationContext);
        }
    }

    void superPerformTransformations(Block block, TransformationContext transformationContext)
        throws TransformationException
    {
        super.performTransformations(block, transformationContext);
    }

    /**
     * @return the ordered list of Transformations to execute
     */
    @Override
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
            Object value = this.container.getRequest().getParameter("transformations");
            if (value instanceof String stringValue) {
                transformationNamesString = stringValue;
            }
        }
        return transformationNamesString;
    }
}
