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
package org.xwiki.search.solr.internal.reference;

import java.lang.reflect.Type;
import java.util.Collection;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.commons.lang3.LocaleUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.solr.common.SolrDocument;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.util.DefaultParameterizedType;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.EntityReferenceResolver;
import org.xwiki.search.solr.internal.api.FieldUtils;

/**
 * Component used to extract an {@link EntityReference} from a {@link SolrDocument}.
 * 
 * @version $Id$
 * @since 7.2M2
 */
@Component
@Singleton
public class SolrEntityReferenceResolver implements EntityReferenceResolver<SolrDocument>
{
    /**
     * Helper for unit tests.
     */
    public static final Type TYPE =
        new DefaultParameterizedType(null, EntityReferenceResolver.class, SolrDocument.class);

    @Inject
    @Named("explicit")
    private EntityReferenceResolver<EntityReference> explicitReferenceEntityReferenceResolver;

    @Override
    public EntityReference resolve(SolrDocument solrDocument, EntityType type, Object... parameters)
    {
        EntityReference solrEntityReference = getEntityReference(solrDocument, type, parameters);
        return this.explicitReferenceEntityReferenceResolver.resolve(solrEntityReference, type, parameters);
    }

    private EntityReference getEntityReference(SolrDocument solrDocument, EntityType expectedEntityType,
        Object... parameters)
    {
        EntityReference wikiReference = getWikiReference(solrDocument, parameters);
        EntityReference spaceReference = getSpaceReference(solrDocument, wikiReference, parameters);
        EntityReference documentReference = getDocumentReferenceWithLocale(solrDocument, spaceReference, parameters);

        String indexedEntityType = getFieldStringValue(solrDocument, FieldUtils.TYPE);
        EntityType actualEntityType =
            StringUtils.isEmpty(indexedEntityType) ? expectedEntityType : EntityType.valueOf(indexedEntityType);

        switch (actualEntityType) {
            case ATTACHMENT:
                return getAttachmentReference(solrDocument, documentReference, parameters);
            case OBJECT:
                return getObjectReference(solrDocument, documentReference, parameters);
            case OBJECT_PROPERTY:
                EntityReference objectReference = getObjectReference(solrDocument, documentReference, parameters);
                return getObjectPropertyReference(solrDocument, objectReference, parameters);
            default:
                return documentReference;
        }
    }

    private EntityReference getWikiReference(SolrDocument solrDocument, Object... parameters)
    {
        String wikiName = getFieldStringValue(solrDocument, FieldUtils.WIKI);
        if (!StringUtils.isEmpty(wikiName)) {
            return new EntityReference(wikiName, EntityType.WIKI);
        } else {
            return resolveMissingReference(EntityType.WIKI, null, parameters);
        }
    }

    private EntityReference getSpaceReference(SolrDocument solrDocument, EntityReference parent, Object... parameters)
    {
        Collection<Object> spaceNames = solrDocument.getFieldValues(FieldUtils.SPACES);
        if (spaceNames != null && !spaceNames.isEmpty()) {
            EntityReference spaceReference = parent;
            for (Object spaceName : spaceNames) {
                spaceReference = new EntityReference(String.valueOf(spaceName), EntityType.SPACE, spaceReference);
            }
            return spaceReference;
        } else {
            return resolveMissingReference(EntityType.SPACE, parent, parameters);
        }
    }

    private EntityReference getDocumentReferenceWithLocale(SolrDocument solrDocument, EntityReference parent,
        Object... parameters)
    {
        EntityReference documentReference = getDocumentReference(solrDocument, parent, parameters);
        String localeString = getFieldStringValue(solrDocument, FieldUtils.DOCUMENT_LOCALE);
        if (!StringUtils.isEmpty(localeString)) {
            documentReference = new DocumentReference(documentReference, LocaleUtils.toLocale(localeString));
        }
        return documentReference;
    }

    private EntityReference getDocumentReference(SolrDocument solrDocument, EntityReference parent,
        Object... parameters)
    {
        String documentName = getFieldStringValue(solrDocument, FieldUtils.NAME);
        if (!StringUtils.isEmpty(documentName)) {
            return new EntityReference(documentName, EntityType.DOCUMENT, parent);
        } else {
            return resolveMissingReference(EntityType.DOCUMENT, parent, parameters);
        }
    }

    private EntityReference getAttachmentReference(SolrDocument solrDocument, EntityReference parent,
        Object... parameters)
    {
        String fileName = getFieldFirstStringValue(solrDocument, FieldUtils.FILENAME);
        if (!StringUtils.isEmpty(fileName)) {
            return new EntityReference(fileName, EntityType.ATTACHMENT, parent);
        } else {
            return resolveMissingReference(EntityType.ATTACHMENT, parent, parameters);
        }
    }

    private EntityReference getObjectReference(SolrDocument solrDocument, EntityReference parent, Object... parameters)
    {
        String classReference = getFieldFirstStringValue(solrDocument, FieldUtils.CLASS);
        Number objectNumber = getFieldNumberValue(solrDocument, FieldUtils.NUMBER);
        if (!StringUtils.isEmpty(classReference) && objectNumber != null) {
            return new EntityReference(String.format("%s[%s]", classReference, objectNumber), EntityType.OBJECT,
                parent);
        } else {
            return resolveMissingReference(EntityType.OBJECT, parent, parameters);
        }
    }

    private EntityReference getObjectPropertyReference(SolrDocument solrDocument, EntityReference parent,
        Object... parameters)
    {
        String propertyName = getFieldStringValue(solrDocument, FieldUtils.PROPERTY_NAME);
        if (!StringUtils.isEmpty(propertyName)) {
            return new EntityReference(propertyName, EntityType.OBJECT_PROPERTY, parent);
        } else {
            return resolveMissingReference(EntityType.OBJECT_PROPERTY, parent, parameters);
        }
    }

    private EntityReference resolveMissingReference(EntityType entityType, EntityReference parent, Object... parameters)
    {
        EntityReference entityReference =
            this.explicitReferenceEntityReferenceResolver.resolve(null, entityType, parameters);
        return entityReference.replaceParent(entityReference.getParent(), parent);
    }

    private String getFieldStringValue(SolrDocument solrDocument, String fieldName)
    {
        Object field = solrDocument.get(fieldName);

        return field != null ? field.toString() : null;
    }

    private String getFieldFirstStringValue(SolrDocument solrDocument, String fieldName)
    {
        Object field = solrDocument.getFirstValue(fieldName);

        return field != null ? field.toString() : null;
    }

    private Number getFieldNumberValue(SolrDocument solrDocument, String fieldName)
    {
        Object field = solrDocument.get(fieldName);

        return field != null ? (Number) field : null;
    }
}
