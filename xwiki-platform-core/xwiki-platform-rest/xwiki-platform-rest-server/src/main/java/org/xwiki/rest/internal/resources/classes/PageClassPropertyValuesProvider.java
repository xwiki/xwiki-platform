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
import java.util.function.Supplier;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xwiki.component.annotation.Component;
import org.xwiki.icon.IconException;
import org.xwiki.icon.IconManager;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.WikiReference;
import org.xwiki.query.QueryBuilder;
import org.xwiki.rendering.syntax.Syntax;
import org.xwiki.rest.model.jaxb.PropertyValue;
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
 * @since 10.6RC1
 */
@Component
@Named("Page")
@Singleton
public class PageClassPropertyValuesProvider extends AbstractDocumentListClassPropertyValuesProvider<PageClass>
{
    private static final Logger LOGGER = LoggerFactory.getLogger(PageClassPropertyValuesProvider.class);

    @Inject
    private QueryBuilder<PageClass> allowedValuesQueryBuilder;

    @Inject
    private AuthorExecutor authorExecutor;

    @Inject
    private IconManager iconManager;

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
    protected PropertyValue getValueFromQueryResult(Object result, PageClass propertyDefinition)
    {
        PropertyValue value = super.getValueFromQueryResult(result, propertyDefinition);
        // Get the document reference if it exists from the result as the parent method serialized it in the value
        DocumentReference documentReference = (
            (Supplier<DocumentReference>) () -> {
                if (result instanceof Object[]) {
                    Object[] row = (Object[]) result;
                    if (row.length > 0 && row[0] instanceof DocumentReference) {
                        return (DocumentReference) row[0];
                    }
                } else if (result instanceof DocumentReference) {
                    return (DocumentReference) result;
                }
                return null;
            }
        ).get();

        if (value != null && documentReference != null) {
            value.getMetaData().put(META_DATA_HINT, getHint(documentReference));
        }
        return value;
    }

    @Override
    protected Map<String, Object> getIcon(DocumentReference documentReference)
    {
        Map<String, Object> icon = new HashMap<>();
        try {
            icon.put(META_DATA_ICON_META_DATA, iconManager.getMetaData("page"));
        } catch (IconException e) {
            e.printStackTrace();
        }

        return icon;
    }

    @Override
    protected String getLabel(DocumentReference documentReference, Object currentLabel)
    {
        String label = currentLabel == null ? "" : currentLabel.toString().trim();
        if (label.isEmpty()) {
            try {
                XWikiContext xcontext = this.xcontextProvider.get();
                XWikiDocument document = xcontext.getWiki().getDocument(documentReference, xcontext);
                label = document.getRenderedTitle(Syntax.PLAIN_1_0, xcontext);
            } catch (XWikiException e) {
                LOGGER.error("Error while loading the document [{}]", documentReference,
                    ExceptionUtils.getRootCause(e));
                label = super.getLabel(documentReference, currentLabel);
            }
        }
        return label;
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
            .map(EntityReference::getName)
            .collect(Collectors.joining(" / "));
    }
}
