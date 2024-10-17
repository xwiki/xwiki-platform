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
package com.xpn.xwiki.internal.filter.input;

import java.lang.reflect.ParameterizedType;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.slf4j.Logger;
import org.suigeneris.jrcs.rcs.Version;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.util.DefaultParameterizedType;
import org.xwiki.filter.FilterEventParameters;
import org.xwiki.filter.FilterException;
import org.xwiki.filter.event.model.WikiDocumentFilter;
import org.xwiki.filter.event.xwiki.XWikiWikiDocumentFilter;
import org.xwiki.filter.instance.input.DocumentInstanceInputProperties;
import org.xwiki.filter.instance.input.EntityEventGenerator;
import org.xwiki.filter.instance.internal.input.AbstractBeanEntityEventGenerator;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.user.UserReference;
import org.xwiki.user.UserReferenceSerializer;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiAttachment;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.internal.filter.XWikiDocumentFilter;
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.objects.classes.BaseClass;
import com.xpn.xwiki.user.api.XWikiRightService;

/**
 * @version $Id$
 * @since 6.2M1
 */
@Component
@Singleton
public class XWikiDocumentLocaleEventGenerator
    extends AbstractBeanEntityEventGenerator<XWikiDocument, XWikiDocumentFilter, DocumentInstanceInputProperties>
{
    /**
     * The role of this component.
     */
    public static final ParameterizedType ROLE = new DefaultParameterizedType(null, EntityEventGenerator.class,
        XWikiDocument.class, DocumentInstanceInputProperties.class);

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

    @Inject
    @Named("document")
    private UserReferenceSerializer<DocumentReference> userSerializer;

    @Inject
    @Named("compactwiki")
    private EntityReferenceSerializer<String> compactwikiSerializer;

    private String toString(UserReference userReference2)
    {
        DocumentReference userDocumentReference = this.userSerializer.serialize(userReference2);

        String userString;

        if (userDocumentReference != null) {
            userString = this.compactwikiSerializer.serialize(userDocumentReference);
        } else {
            userString = XWikiRightService.GUEST_USER_FULLNAME;
        }

        return userString;
    }

    @Override
    public void write(XWikiDocument document, Object filter, XWikiDocumentFilter documentFilter,
        DocumentInstanceInputProperties properties) throws FilterException
    {
        XWikiContext xcontext = this.xcontextProvider.get();
        String currentWiki = xcontext.getWikiId();

        try {
            // the document might belong to another wiki, so we need to setup the context
            xcontext.setWikiId(document.getDocumentReference().getWikiReference().getName());

            // > WikiDocumentLocale

            FilterEventParameters localeParameters = new FilterEventParameters();

            if (properties.isWithJRCSRevisions()) {
                try {
                    localeParameters.put(XWikiWikiDocumentFilter.PARAMETER_JRCSREVISIONS,
                        document.getDocumentArchive(xcontext).getArchive(xcontext));
                } catch (XWikiException e) {
                    this.logger.error("Document [{}] has malformed history", document.getDocumentReference(), e);
                }
            }

            localeParameters.put(WikiDocumentFilter.PARAMETER_CREATION_AUTHOR, toString(document.getAuthors().getCreator()));
            localeParameters.put(WikiDocumentFilter.PARAMETER_CREATION_DATE, document.getCreationDate());
            localeParameters.put(WikiDocumentFilter.PARAMETER_LASTREVISION, document.getVersion());

            documentFilter.beginWikiDocumentLocale(document.getLocale(), localeParameters);

            if (properties.isWithRevisions()) {
                try {
                    for (Version version : document.getRevisions(xcontext)) {
                        XWikiDocument revisionDocument =
                            xcontext.getWiki().getDocument(document, version.toString(), xcontext);

                        writeRevision(revisionDocument, filter, documentFilter, properties);
                    }
                } catch (XWikiException e) {
                    this.logger.error("Failed to get document [{}] history", document.getDocumentReference(), e);
                }
            }

            writeRevision(document, filter, documentFilter, properties);

            // < WikiDocumentLocale

            documentFilter.endWikiDocumentLocale(document.getLocale(), FilterEventParameters.EMPTY);
        } finally {
            xcontext.setWikiId(currentWiki);
        }
    }

    private void writeRevision(XWikiDocument document, Object filter, XWikiDocumentFilter documentFilter,
        DocumentInstanceInputProperties properties) throws FilterException
    {
        // > WikiDocumentRevision

        FilterEventParameters revisionParameters = new FilterEventParameters();

        if (document.getRelativeParentReference() != null) {
            revisionParameters.put(WikiDocumentFilter.PARAMETER_PARENT, document.getRelativeParentReference());
        }
        revisionParameters.put(WikiDocumentFilter.PARAMETER_TITLE, document.getTitle());
        if (!document.getCustomClass().isEmpty()) {
            revisionParameters.put(WikiDocumentFilter.PARAMETER_CUSTOMCLASS, document.getCustomClass());
        }
        if (!document.getDefaultTemplate().isEmpty()) {
            revisionParameters.put(WikiDocumentFilter.PARAMETER_DEFAULTTEMPLATE, document.getDefaultTemplate());
        }
        if (!document.getValidationScript().isEmpty()) {
            revisionParameters.put(WikiDocumentFilter.PARAMETER_VALIDATIONSCRIPT, document.getValidationScript());
        }
        revisionParameters.put(WikiDocumentFilter.PARAMETER_SYNTAX, document.getSyntax());
        revisionParameters.put(WikiDocumentFilter.PARAMETER_HIDDEN, document.isHidden());
        revisionParameters.put(XWikiWikiDocumentFilter.PARAMETER_ENFORCE_REQUIRED_RIGHTS,
            document.isEnforceRequiredRights());

        revisionParameters.put(WikiDocumentFilter.PARAMETER_REVISION_EFFECTIVEMETADATA_AUTHOR, toString(document.getAuthors().getEffectiveMetadataAuthor()));
        revisionParameters.put(WikiDocumentFilter.PARAMETER_REVISION_ORIGINALMETADATA_AUTHOR, toString(document.getAuthors().getOriginalMetadataAuthor()));
        revisionParameters.put(WikiDocumentFilter.PARAMETER_REVISION_COMMENT, document.getComment());
        revisionParameters.put(WikiDocumentFilter.PARAMETER_REVISION_DATE, document.getDate());
        revisionParameters.put(WikiDocumentFilter.PARAMETER_REVISION_MINOR, document.isMinorEdit());

        revisionParameters.put(WikiDocumentFilter.PARAMETER_CONTENT_AUTHOR, toString(document.getAuthors().getContentAuthor()));
        revisionParameters.put(WikiDocumentFilter.PARAMETER_CONTENT_DATE, document.getContentUpdateDate());
        revisionParameters.put(WikiDocumentFilter.PARAMETER_CONTENT, document.getContent());

        if (properties.isWithWikiDocumentContentHTML()) {
            try {
                XWikiContext xcontext = this.xcontextProvider.get();

                revisionParameters.put(WikiDocumentFilter.PARAMETER_CONTENT_HTML,
                    document.getRenderedContent(xcontext));
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
    }
}
