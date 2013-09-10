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
import org.xwiki.wikistream.filter.WikiDocumentFilter;
import org.xwiki.wikistream.instance.input.AbstractEntityEventGenerator;
import org.xwiki.wikistream.instance.input.EntityEventGenerator;
import org.xwiki.wikistream.instance.internal.BaseClassProperties;
import org.xwiki.wikistream.instance.internal.BaseObjectProperties;
import org.xwiki.wikistream.instance.internal.XWikiAttachmentProperties;
import org.xwiki.wikistream.instance.internal.XWikiDocumentFilter;
import org.xwiki.wikistream.instance.internal.XWikiDocumentProperties;
import org.xwiki.wikistream.xwiki.filter.XWikiWikiDocumentFilter;

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
public class XWikiDocumentEventGenerator extends
    AbstractEntityEventGenerator<XWikiDocument, XWikiDocumentProperties, XWikiDocumentFilter>
{
    public static final ParameterizedType ROLE = new DefaultParameterizedType(null, EntityEventGenerator.class,
        XWikiDocument.class, XWikiDocumentProperties.class);

    @Inject
    private Provider<XWikiContext> xcontextProvider;

    @Inject
    private Logger logger;

    @Inject
    private EntityEventGenerator<XWikiAttachment, XWikiAttachmentProperties> attachmentEventGenerator;

    @Inject
    private EntityEventGenerator<BaseClass, BaseClassProperties> classEventGenerator;

    @Inject
    private EntityEventGenerator<BaseObject, BaseObjectProperties> objectEventGenerator;

    @Override
    public void write(XWikiDocument document, Object filter, XWikiDocumentFilter documentFilter,
        XWikiDocumentProperties properties) throws WikiStreamException
    {
        XWikiContext xcontext = this.xcontextProvider.get();

        // WikiDocument

        FilterEventParameters documentParameters = new FilterEventParameters();

        documentParameters.put(WikiDocumentFilter.PARAMETER_LOCALE, document.getDefaultLocale());

        documentFilter.beginWikiDocument(document.getDocumentReference().getName(), documentParameters);

        // WikiDocumentLocale

        FilterEventParameters documentLocaleParameters = new FilterEventParameters();

        if (properties.isWithWikiDocumentRevisions()) {
            try {
                documentLocaleParameters.put(XWikiWikiDocumentFilter.PARAMETER_JRCSREVISIONS, document
                    .getDocumentArchive(xcontext).getArchive(xcontext));
            } catch (XWikiException e) {
                this.logger.error("Document [{}] has malformed history", document.getDocumentReference(), e);
            }
        }

        documentFilter.beginWikiDocumentLocale(document.getLocale(), FilterEventParameters.EMPTY);

        // WikiDocumentRevision

        FilterEventParameters parameters = new FilterEventParameters();

        parameters.put(WikiDocumentFilter.PARAMETER_LOCALE, document.getLocale());
        parameters.put(WikiDocumentFilter.PARAMETER_PARENT, document.getParent());
        parameters.put(WikiDocumentFilter.PARAMETER_TITLE, document.getTitle());
        parameters.put(WikiDocumentFilter.PARAMETER_CUSTOMCLASS, document.getCustomClass());
        parameters.put(WikiDocumentFilter.PARAMETER_DEFAULTTEMPLATE, document.getDefaultTemplate());
        parameters.put(WikiDocumentFilter.PARAMETER_VALIDATIONSCRIPT, document.getValidationScript());
        parameters.put(WikiDocumentFilter.PARAMETER_SYNTAX, document.getValidationScript());
        parameters.put(WikiDocumentFilter.PARAMETER_HIDDEN, document.isHidden());

        parameters.put(WikiDocumentFilter.PARAMETER_REVISION_AUTHOR, document.getAuthor());
        parameters.put(WikiDocumentFilter.PARAMETER_REVISION_COMMENT, document.getComment());
        parameters.put(WikiDocumentFilter.PARAMETER_REVISION_DATE, document.getDate());
        parameters.put(WikiDocumentFilter.PARAMETER_REVISION_MINOR, document.isMinorEdit());

        parameters.put(WikiDocumentFilter.PARAMETER_CONTENT_AUTHOR, document.getContentAuthor());
        parameters.put(WikiDocumentFilter.PARAMETER_CONTENT_DATE, document.getContentUpdateDate());
        parameters.put(WikiDocumentFilter.PARAMETER_CONTENT, document.getContent());
        if (properties.isWithWikiDocumentContentHTML()) {
            try {
                parameters.put(WikiDocumentFilter.PARAMETER_CONTENT_HTML, document.getRenderedContent(xcontext));
            } catch (XWikiException e) {
                this.logger.error("Failed to render content of document [{}] as HTML", document.getDocumentReference(),
                    e);
            }
        }

        parameters.put(WikiDocumentFilter.PARAMETER_CREATION_AUTHOR, document.getCreator());
        parameters.put(WikiDocumentFilter.PARAMETER_CREATION_DATE, document.getCreationDate());

        documentFilter.beginWikiDocumentRevision(document.getVersion(), parameters);

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
                this.attachmentEventGenerator.write(attachment, documentFilter, properties);
            }
        }

        // Document Class
        if (properties.isWithWikiClass()) {
            BaseClass xclass = document.getXClass();
            if (!xclass.getFieldList().isEmpty()) {
                this.classEventGenerator.write(xclass, documentFilter, properties);
            }
        }

        // Objects (THEIR ORDER IS MOLDED IN STONE!)
        if (properties.isWithWikiObjects()) {
            for (List<BaseObject> xobjects : document.getXObjects().values()) {
                for (BaseObject xobject : xobjects) {
                    if (xobject != null) {
                        this.objectEventGenerator.write(xobject, documentFilter, properties);
                    }
                }
            }
        }

        // /WikiDocumentRevision

        documentFilter.endWikiDocumentRevision(document.getVersion(), parameters);

        // /WikiDocumentLocale

        documentFilter.endWikiDocumentLocale(document.getLocale(), FilterEventParameters.EMPTY);

        // /WikiDocument

        documentFilter.endWikiDocument(document.getDocumentReference().getName(), documentParameters);
    }
}
