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
package org.xwiki.rest.internal.resources.classes;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.xwiki.component.annotation.Component;
import org.xwiki.icon.IconException;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.WikiReference;
import org.xwiki.query.QueryBuilder;
import org.xwiki.rendering.syntax.Syntax;
import org.xwiki.rest.model.jaxb.PropertyValues;
import org.xwiki.security.authorization.AuthorExecutor;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.classes.PageClass;

/**
 * Provides values for Page properties.
 *
 * @version $Id$
 * @since 10.6
 */
@Component
@Named("Page")
@Singleton
public class PageClassPropertyValuesProvider extends AbstractDocumentListClassPropertyValuesProvider<PageClass>
{
    private static final String DEFAULT_ICON_NAME = "page";

    @Inject
    private QueryBuilder<PageClass> allowedValuesQueryBuilder;

    @Inject
    private AuthorExecutor authorExecutor;

    @Override
    protected Class<PageClass> getPropertyType()
    {
        return PageClass.class;
    }

    @Override
    protected PropertyValues getAllowedValues(PageClass pageClass, int limit, String filter) throws Exception
    {
        // Execute the query with the rights of the class last author because the query may not be safe.
        return this.authorExecutor.call(() -> getValues(
            this.allowedValuesQueryBuilder.build(pageClass), limit, filter, pageClass
        ), pageClass.getOwnerDocument().getAuthorReference());
    }

    @Override
    protected Map<String, Object> getIcon(DocumentReference documentReference)
    {
        Map<String, Object> icon;
        try {
            icon = this.iconManager.getMetaData(DEFAULT_ICON_NAME);
        } catch (IconException e) {
            this.logger.warn("Error getting the icon [{}]. Root cause is [{}].", DEFAULT_ICON_NAME,
                ExceptionUtils.getRootCause(e));
            icon = new HashMap<>();
        }

        return icon;
    }

    private String getLabel(EntityReference entityReference)
    {
        String label;
        try {
            XWikiContext xcontext = this.xcontextProvider.get();
            XWikiDocument document = xcontext.getWiki().getDocument(entityReference, xcontext);
            label = document.getRenderedTitle(Syntax.PLAIN_1_0, xcontext);
        } catch (XWikiException e) {
            this.logger.error("Error while loading the document [{}]. Root cause is [{}]", entityReference,
                ExceptionUtils.getRootCause(e));
            if (entityReference instanceof DocumentReference) {
                label = super.getLabel((DocumentReference) entityReference, "");
            } else {
                label = entityReference.getName();
            }
        }
        return label;
    }

    @Override
    protected String getLabel(DocumentReference documentReference, Object currentLabel)
    {
        return getLabel(documentReference);
    }

    @Override
    protected String getHint(DocumentReference documentReference)
    {
        EntityReference parentSpace = documentReference.getParent();
        if (XWiki.DEFAULT_SPACE_HOMEPAGE.equals(documentReference.getName())) {
            parentSpace = parentSpace.getParent();
        }
        return parentSpace.getReversedReferenceChain().stream()
            .filter(entityReference -> !(entityReference instanceof WikiReference))
            .map(this::getLabel)
            .collect(Collectors.joining(" / "));
    }
}
