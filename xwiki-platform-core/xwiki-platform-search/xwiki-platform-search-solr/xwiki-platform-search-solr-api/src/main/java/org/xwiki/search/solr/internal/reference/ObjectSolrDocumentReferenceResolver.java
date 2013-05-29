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
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.ObjectPropertyReference;
import org.xwiki.search.solr.internal.api.SolrIndexException;

import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;
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
public class ObjectSolrDocumentReferenceResolver extends AbstractSolrDocumentReferenceResolver
{
    /**
     * Used to resolve object property references.
     */
    @Inject
    @Named("object_property")
    private SolrDocumentReferenceResolver objectPropertyResolver;

    @Override
    public List<EntityReference> getReferences(EntityReference objectReference) throws SolrIndexException
    {
        List<EntityReference> result = new ArrayList<EntityReference>();

        // Object itself
        result.add(objectReference);

        // Object properties
        DocumentReference documentReference = new DocumentReference(objectReference.getParent());

        XWikiDocument document;
        try {
            document = getDocument(documentReference);
        } catch (Exception e) {
            throw new SolrIndexException("Failed to get document for object [" + objectReference + "]");
        }

        BaseObject object = document.getXObject(objectReference);
        if (object != null) {
            for (Object field : object.getFieldList()) {
                BaseProperty<ObjectPropertyReference> objectProperty = (BaseProperty<ObjectPropertyReference>) field;

                ObjectPropertyReference objectPropertyReference = objectProperty.getReference();

                try {
                    result.addAll(this.objectPropertyResolver.getReferences(objectPropertyReference));
                } catch (Exception a) {
                    this.logger.error("Failed to resolve references for object property [" + objectPropertyReference
                        + "]");
                }
            }
        }

        return result;
    }
}
