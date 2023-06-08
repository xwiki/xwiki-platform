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
package org.xwiki.annotation.io.internal.migration.hibernate;

import java.util.Objects;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.apache.commons.lang3.StringUtils;
import org.xwiki.annotation.Annotation;
import org.xwiki.annotation.reference.TypedStringEntityReferenceResolver;
import org.xwiki.component.annotation.Component;
import org.xwiki.index.IndexException;
import org.xwiki.index.TaskConsumer;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReference;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;

/**
 * Replace the target of annotation referencing themselves to avoid having outdated targets in case of document move or
 * copy.
 *
 * @version $Id$
 * @see <a href="https://jira.xwiki.org/browse/XWIKI-20699">XWIKI-20699</a>
 * @since 15.5RC1
 */
@Component
@Singleton
@Named(AnnotationInternalTargetFixTaskConsumer.ID)
public class AnnotationInternalTargetFixTaskConsumer implements TaskConsumer
{
    /**
     * The component hint.
     */
    public static final String ID = "internal-annotation-target-fix";

    @Inject
    private Provider<XWikiContext> contextProvider;

    @Inject
    private TypedStringEntityReferenceResolver referenceResolver;

    @Override
    public void consume(DocumentReference documentReference, String version) throws IndexException
    {
        try {
            XWikiContext context = this.contextProvider.get();
            task(context.getWiki().getDocument(documentReference, context));
        } catch (XWikiException e) {
            throw new IndexException(String.format("Failed to resolve document [%s]", documentReference), e);
        }
    }

    private void task(XWikiDocument document) throws IndexException
    {
        BaseObject xObject = document.getXObject(XWikiDocument.COMMENTSCLASS_REFERENCE);
        String target = xObject.getStringValue(Annotation.TARGET_FIELD);
        boolean update = false;
        if (!StringUtils.isEmpty(target)) {
            EntityReference resolve = this.referenceResolver.resolve(target, EntityType.DOCUMENT);
            if (Objects.equals(resolve, document.getDocumentReference())) {
                xObject.setStringValue(Annotation.TARGET_FIELD, "");
                update = true;
            }
        }

        if (update) {
            XWikiContext context = this.contextProvider.get();
            try {
                context.getWiki().saveDocument(document, "Updating annotation targets", context);
            } catch (XWikiException e) {
                throw new IndexException(String.format("Failed to save document [%s]", document.getDocumentReference()),
                    e);
            }
        }
    }
}
