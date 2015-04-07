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
package org.xwiki.annotation.internal;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.xwiki.annotation.Annotation;
import org.xwiki.annotation.AnnotationConfiguration;
import org.xwiki.component.annotation.Component;
import org.xwiki.model.reference.EntityReference;

import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.internal.mandatory.AbstractMandatoryDocumentInitializer;
import com.xpn.xwiki.objects.classes.BaseClass;

/**
 * When the wiki is initialized, make sure that the configured annotation class contains the minimum required properties
 * for the Annotation Application to function properly.
 * 
 * @version $Id$
 * @since 6.2RC1
 */
@Component
// The role hint is not a document name since it can change based the configuration
@Named(AnnotationClassDocumentInitializer.HINT)
@Singleton
public class AnnotationClassDocumentInitializer extends AbstractMandatoryDocumentInitializer
{
    static final String HINT = "annotationclass";

    /**
     * The Annotation Application's configuration.
     */
    @Inject
    protected AnnotationConfiguration configuration;

    /**
     * Default constructor.
     */
    public AnnotationClassDocumentInitializer()
    {
        super(null);
    }

    @Override
    public EntityReference getDocumentReference()
    {
        return this.configuration.getAnnotationClassReference();
    }

    @Override
    public boolean updateDocument(XWikiDocument document)
    {
        if (!this.configuration.isInstalled()) {
            // If the Annotations Application is not installed on the current wiki, do nothing.
            return false;
        }

        BaseClass annotationClass = document.getXClass();

        boolean needsUpdate = false;

        needsUpdate |= annotationClass.addTextField(Annotation.AUTHOR_FIELD, "Author", 30);
        needsUpdate |= annotationClass.addDateField(Annotation.DATE_FIELD, "Date");

        needsUpdate |= annotationClass.addTextAreaField(Annotation.SELECTION_FIELD, "Selection", 40, 5);
        needsUpdate |=
            annotationClass.addTextAreaField(Annotation.SELECTION_LEFT_CONTEXT_FIELD, "Selection Left Context", 40, 5);
        needsUpdate |=
            annotationClass
                .addTextAreaField(Annotation.SELECTION_RIGHT_CONTEXT_FIELD, "Selection Right Context", 40, 5);
        needsUpdate |=
            annotationClass.addTextAreaField(Annotation.ORIGINAL_SELECTION_FIELD, "Original Selection", 40, 5);
        needsUpdate |= annotationClass.addTextField(Annotation.TARGET_FIELD, "Target", 30);
        needsUpdate |= annotationClass.addTextField(Annotation.STATE_FIELD, "State", 30);

        needsUpdate |= setClassDocumentFields(document, "Annotation Class");

        return needsUpdate;
    }
}
