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
package org.xwiki.wikistream.instance.internal.input;

import java.lang.reflect.ParameterizedType;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.util.DefaultParameterizedType;
import org.xwiki.filter.FilterEventParameters;
import org.xwiki.wikistream.WikiStreamException;
import org.xwiki.wikistream.filter.xwiki.XWikiWikiDocumentFilter;
import org.xwiki.wikistream.instance.input.EntityEventGenerator;
import org.xwiki.wikistream.instance.internal.XWikiDocumentFilter;
import org.xwiki.wikistream.model.filter.WikiDocumentFilter;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiAttachment;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.objects.classes.BaseClass;

/**
 * @version $Id$
 * @since 5.2M2
 */
@Component
@Singleton
// TODO: add support for real revision events (instead of the jrcs archive)
public class XWikiDocumentLocaleEventGenerator extends
    AbstractBeanEntityEventGenerator<XWikiDocument, XWikiDocumentFilter, XWikiDocumentInputProperties>
{
    public static final ParameterizedType ROLE = new DefaultParameterizedType(null, EntityEventGenerator.class,
        XWikiDocument.class, XWikiDocumentInputProperties.class);

    @Inject
    private Provider<XWikiContext> xcontextProvider;

    @Inject
    private Logger logger;

    @Inject
    private EntityEventGenerator<XWikiAttachment> attachmentEventGenerator;

    @Inject
    private EntityEventGenerator<BaseClass> classEventGenerator;

    @Inject
    private EntityEventGenerator<BaseObject> objectEventGenerator;

    @Override
    public void write(XWikiDocument document, Object filter, XWikiDocumentFilter documentFilter,
        XWikiDocumentInputProperties properties) throws WikiStreamException
    {
        XWikiContext xcontext = this.xcontextProvider.get();

        // > WikiDocumentLocale

        FilterEventParameters localeParameters = new FilterEventParameters();

        if (properties.isWithWikiDocumentRevisions()) {
            try {
                localeParameters.put(XWikiWikiDocumentFilter.PARAMETER_JRCSREVISIONS,
                    document.getDocumentArchive(xcontext).getArchive(xcontext));
            } catch (XWikiException e) {
                this.logger.error("Document [{}] has malformed history", document.getDocumentReference(), e);
            }
        }

        localeParameters.put(WikiDocumentFilter.PARAMETER_CREATION_AUTHOR, document.getCreator());
        localeParameters.put(WikiDocumentFilter.PARAMETER_CREATION_DATE, document.getCreationDate());

        documentFilter.beginWikiDocumentLocale(document.getLocale(), localeParameters);

        // > WikiDocumentRevision

        FilterEventParameters revisionParameters = new FilterEventParameters();

        revisionParameters.put(WikiDocumentFilter.PARAMETER_LOCALE, document.getLocale());
        revisionParameters.put(WikiDocumentFilter.PARAMETER_PARENT, document.getParent());
        revisionParameters.put(WikiDocumentFilter.PARAMETER_TITLE, document.getTitle());
        revisionParameters.put(WikiDocumentFilter.PARAMETER_CUSTOMCLASS, document.getCustomClass());
        revisionParameters.put(WikiDocumentFilter.PARAMETER_DEFAULTTEMPLATE, document.getDefaultTemplate());
        revisionParameters.put(WikiDocumentFilter.PARAMETER_VALIDATIONSCRIPT, document.getValidationScript());
        revisionParameters.put(WikiDocumentFilter.PARAMETER_SYNTAX, document.getSyntax());
        revisionParameters.put(WikiDocumentFilter.PARAMETER_HIDDEN, document.isHidden());

        revisionParameters.put(WikiDocumentFilter.PARAMETER_REVISION_AUTHOR, document.getAuthor());
        revisionParameters.put(WikiDocumentFilter.PARAMETER_REVISION_COMMENT, document.getComment());
        revisionParameters.put(WikiDocumentFilter.PARAMETER_REVISION_DATE, document.getDate());
        revisionParameters.put(WikiDocumentFilter.PARAMETER_REVISION_MINOR, document.isMinorEdit());

        revisionParameters.put(WikiDocumentFilter.PARAMETER_CONTENT_AUTHOR, document.getContentAuthor());
        revisionParameters.put(WikiDocumentFilter.PARAMETER_CONTENT_DATE, document.getContentUpdateDate());
        revisionParameters.put(WikiDocumentFilter.PARAMETER_CONTENT, document.getContent());
        if (properties.isWithWikiDocumentContentHTML()) {
            try {
                revisionParameters
                    .put(WikiDocumentFilter.PARAMETER_CONTENT_HTML, document.getRenderedContent(xcontext));
            } catch (XWikiException e) {
                this.logger.error("Failed to render content of document [{}] as HTML", document.getDocumentReference(),
                    e);
            }
        }

        documentFilter.beginWikiDocumentRevision(document.getVersion(), revisionParameters);

        // Attachments

        if (properties.isWithWikiAttachments()) {
            List<XWikiAttachment> sortedAttachments = new ArrayList<XWikiAttachment>(document.getAttachmentList());
            Collections.sort(sortedAttachments, new Comparator<XWikiAttachment>()
            {
                @Override
                public int compare(XWikiAttachment attachement1, XWikiAttachment attachement2)
                {
                    if (attachement1 == null || attachement2 == null) {
                        int result = 0;
                        if (attachement1 != null) {
                            result = -1;
                        } else if (attachement2 != null) {
                            result = 1;
                        }
                        return result;
                    }
                    return attachement1.getFilename().compareTo(attachement2.getFilename());
                }
            });

            for (XWikiAttachment attachment : sortedAttachments) {
                ((XWikiAttachmentEventGenerator) this.attachmentEventGenerator).write(attachment, filter,
                    documentFilter, properties);
            }
        }

        // Document Class
        if (properties.isWithWikiClass()) {
            BaseClass xclass = document.getXClass();
            if (!xclass.getFieldList().isEmpty()) {
                ((BaseClassEventGenerator) this.classEventGenerator).write(xclass, filter, documentFilter, properties);
            }
        }

        // Objects (THEIR ORDER IS MOLDED IN STONE!)
        if (properties.isWithWikiObjects()) {
            for (List<BaseObject> xobjects : document.getXObjects().values()) {
                for (BaseObject xobject : xobjects) {
                    if (xobject != null) {
                        ((BaseObjectEventGenerator) this.objectEventGenerator).write(xobject, filter, documentFilter,
                            properties);
                    }
                }
            }
        }

        // < WikiDocumentRevision

        documentFilter.endWikiDocumentRevision(document.getVersion(), revisionParameters);

        // < WikiDocumentLocale

        documentFilter.endWikiDocumentLocale(document.getLocale(), FilterEventParameters.EMPTY);
    }
}
