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

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.apache.solr.client.solrj.util.ClientUtils;
import org.xwiki.component.annotation.Component;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.model.reference.ObjectPropertyReference;
import org.xwiki.search.solr.internal.api.FieldUtils;
import org.xwiki.search.solr.internal.api.SolrIndexerException;

import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.objects.BaseObjectReference;
import com.xpn.xwiki.objects.BaseProperty;

/**
 * Resolve object references.
 * 
 * @version $Id$
 * @since 5.1M2
 */
@Component
@Named("object")
@Singleton
public class ObjectSolrReferenceResolver extends AbstractSolrReferenceResolver
{
    /**
     * Used to resolve object property references.
     */
    @Inject
    @Named("object_property")
    private Provider<SolrReferenceResolver> objectPropertyResolverProvider;

    /**
     * Used to resolve document references.
     */
    @Inject
    @Named("document")
    private Provider<SolrReferenceResolver> documentResolverProvider;

    /**
     * Reference to String serializer. Used for fields such as class and fullname that are relative to their wiki and
     * are stored without the wiki name.
     */
    @Inject
    @Named("local")
    private EntityReferenceSerializer<String> localSerializer;

    @Override
    public List<EntityReference> getReferences(EntityReference objectReference) throws SolrIndexerException
    {
        List<EntityReference> result = new ArrayList<>();

        // Object itself
        result.add(objectReference);

        // Object properties
        DocumentReference documentReference = new DocumentReference(objectReference.getParent());

        XWikiDocument document;
        try {
            document = getDocument(documentReference);
        } catch (Exception e) {
            throw new SolrIndexerException("Failed to get document for object [" + objectReference + "]", e);
        }

        BaseObject object = document.getXObject(objectReference);
        if (object != null) {
            for (Object field : object.getFieldList()) {
                BaseProperty<ObjectPropertyReference> objectProperty = (BaseProperty<ObjectPropertyReference>) field;

                ObjectPropertyReference objectPropertyReference = objectProperty.getReference();

                try {
                    this.objectPropertyResolverProvider.get().getReferences(objectPropertyReference)
                        .forEach(result::add);
                } catch (Exception e) {
                    this.logger.error("Failed to resolve references for object property [" + objectPropertyReference
                        + "]", e);
                }
            }
        }

        return result;
    }

    @Override
    public String getQuery(EntityReference reference) throws SolrIndexerException
    {
        BaseObjectReference objectReference = new BaseObjectReference(reference);

        StringBuilder builder = new StringBuilder();

        EntityReference documentReference = reference.extractReference(EntityType.DOCUMENT);
        builder.append(this.documentResolverProvider.get().getQuery(documentReference));

        builder.append(QUERY_AND);

        builder.append(FieldUtils.CLASS);
        builder.append(':');
        builder
            .append(ClientUtils.escapeQueryChars(this.localSerializer.serialize(objectReference.getXClassReference())));

        builder.append(QUERY_AND);

        builder.append(FieldUtils.NUMBER);
        builder.append(':');
        builder.append(ClientUtils.escapeQueryChars(String.valueOf(objectReference.getObjectNumber())));

        return builder.toString();
    }
}
