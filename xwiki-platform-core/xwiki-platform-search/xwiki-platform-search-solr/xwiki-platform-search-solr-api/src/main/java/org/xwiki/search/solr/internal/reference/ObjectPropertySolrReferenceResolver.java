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

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import javax.inject.Named;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.search.solr.internal.api.SolrIndexerException;

import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObjectReference;
import com.xpn.xwiki.objects.classes.PasswordClass;

/**
 * Resolve object properties references.
 * 
 * @version $Id$
 * @since 5.1M2
 */
@Component
@Named("object_property")
@Singleton
public class ObjectPropertySolrReferenceResolver extends AbstractSolrReferenceResolver
{
    @Override
    public List<EntityReference> getReferences(EntityReference objectPropertyReference) throws SolrIndexerException
    {
        // Avoid indexing passwords.
        BaseObjectReference objectReference = new BaseObjectReference(objectPropertyReference.getParent());
        DocumentReference classReference = objectReference.getXClassReference();

        // FIXME: This is very bad, it's not this code job to know about things like PasswordClass
        XWikiDocument xclassDocument;
        try {
            xclassDocument = getDocument(classReference);
        } catch (Exception e) {
            throw new SolrIndexerException("Failed to get document for xclass [" + classReference + "]", e);
        }

        if (!(xclassDocument.getXClass().get(objectPropertyReference.getName()) instanceof PasswordClass)) {
            return Arrays.asList(objectPropertyReference);
        }

        return Collections.EMPTY_LIST;
    }
}
