package org.xwiki.wikistream.instance.internal.input;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xwiki.filter.FilterEventParameters;
import org.xwiki.wikistream.WikiStreamException;
import org.xwiki.wikistream.filter.WikiDocumentFilter;
import org.xwiki.wikistream.input.InputWikiStream;
import org.xwiki.wikistream.instance.internal.XWikiDocumentFilter;
import org.xwiki.wikistream.instance.internal.XWikiDocumentProperties;
import org.xwiki.wikistream.xwiki.filter.XWikiWikiDocumentFilter;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiAttachment;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.objects.classes.BaseClass;

// TODO: add support for real revision events (instead of the jrcs archive)
public class XWikiDocumentInputWikiStream implements InputWikiStream
{
    private static final Logger LOGGER = LoggerFactory.getLogger(XWikiDocumentInputWikiStream.class);

    private XWikiDocument document;

    private XWikiDocumentProperties properties;

    private XWikiContext xcontext;

    public XWikiDocumentInputWikiStream(XWikiDocument document, XWikiContext xcontext,
        XWikiDocumentProperties properties)
    {
        this.document = document;
        this.properties = properties;
        this.xcontext = xcontext;
    }

    @Override
    public void read(Object filter) throws WikiStreamException
    {
        XWikiDocumentFilter documentFilter = (XWikiDocumentFilter) filter;

        // WikiDocument

        FilterEventParameters documentParameters = new FilterEventParameters();

        documentParameters.put(WikiDocumentFilter.PARAMETER_LOCALE, this.document.getDefaultLocale());

        documentFilter.beginWikiDocument(this.document.getDocumentReference().getName(), documentParameters);

        // WikiDocumentLocale

        FilterEventParameters documentLocaleParameters = new FilterEventParameters();

        if (this.properties.isWithWikiDocumentRevisions()) {
            try {
                documentLocaleParameters.put(XWikiWikiDocumentFilter.PARAMETER_JRCSREVISIONS, this.document
                    .getDocumentArchive(this.xcontext).getArchive(this.xcontext));
            } catch (XWikiException e) {
                LOGGER.error("Document [{}] has malformed history", this.document.getDocumentReference(), e);
            }
        }

        documentFilter.beginWikiDocumentLocale(this.document.getLocale(), FilterEventParameters.EMPTY);

        // WikiDocumentRevision

        FilterEventParameters parameters = new FilterEventParameters();

        parameters.put(WikiDocumentFilter.PARAMETER_LOCALE, this.document.getLocale());
        parameters.put(WikiDocumentFilter.PARAMETER_PARENT, this.document.getParent());
        parameters.put(WikiDocumentFilter.PARAMETER_TITLE, this.document.getTitle());
        parameters.put(WikiDocumentFilter.PARAMETER_CUSTOMCLASS, this.document.getCustomClass());
        parameters.put(WikiDocumentFilter.PARAMETER_DEFAULTTEMPLATE, this.document.getDefaultTemplate());
        parameters.put(WikiDocumentFilter.PARAMETER_VALIDATIONSCRIPT, this.document.getValidationScript());
        parameters.put(WikiDocumentFilter.PARAMETER_SYNTAX, this.document.getValidationScript());
        parameters.put(WikiDocumentFilter.PARAMETER_HIDDEN, this.document.isHidden());

        parameters.put(WikiDocumentFilter.PARAMETER_REVISION_AUTHOR, this.document.getAuthor());
        parameters.put(WikiDocumentFilter.PARAMETER_REVISION_COMMENT, this.document.getComment());
        parameters.put(WikiDocumentFilter.PARAMETER_REVISION_DATE, this.document.getDate());
        parameters.put(WikiDocumentFilter.PARAMETER_REVISION_MINOR, this.document.isMinorEdit());

        parameters.put(WikiDocumentFilter.PARAMETER_CONTENT_AUTHOR, this.document.getContentAuthor());
        parameters.put(WikiDocumentFilter.PARAMETER_CONTENT_DATE, this.document.getContentUpdateDate());
        parameters.put(WikiDocumentFilter.PARAMETER_CONTENT, this.document.getContent());
        if (this.properties.isWithWikiDocumentContentHTML()) {
            try {
                parameters.put(WikiDocumentFilter.PARAMETER_CONTENT_HTML,
                    this.document.getRenderedContent(this.xcontext));
            } catch (XWikiException e) {
                LOGGER.error("Failed to render content of document [{}] as HTML", this.document.getDocumentReference(),
                    e);
            }
        }

        parameters.put(WikiDocumentFilter.PARAMETER_CREATION_AUTHOR, this.document.getCreator());
        parameters.put(WikiDocumentFilter.PARAMETER_CREATION_DATE, this.document.getCreationDate());

        documentFilter.beginWikiDocumentRevision(this.document.getVersion(), parameters);

        // Attachments

        if (this.properties.isWithWikiAttachments()) {
            List<XWikiAttachment> sortedAttachments = new ArrayList<XWikiAttachment>(this.document.getAttachmentList());
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
                XWikiAttachmentInputWikiStream attachmentInputString =
                    new XWikiAttachmentInputWikiStream(attachment, this.xcontext, this.properties);
                attachmentInputString.read(filter);
            }
        }

        if (this.properties.isWithWikiObjects()) {
            // Document Class
            BaseClass xclass = this.document.getXClass();
            if (!xclass.getFieldList().isEmpty()) {
                BaseClassInputWikiStream classStream = new BaseClassInputWikiStream(xclass, this.properties);
                classStream.read(documentFilter);
            }

            // Objects (THEIR ORDER IS MOLDED IN STONE!)
            for (List<BaseObject> xobjects : this.document.getXObjects().values()) {
                for (BaseObject xobject : xobjects) {
                    if (xobject != null) {
                        BaseObjectInputWikiStream objectStream =
                            new BaseObjectInputWikiStream(xobject, this.xcontext, this.properties);
                        objectStream.read(documentFilter);
                    }
                }
            }
        }

        // /WikiDocumentRevision

        documentFilter.endWikiDocumentRevision(this.document.getVersion(), parameters);

        // /WikiDocumentLocale

        documentFilter.endWikiDocumentLocale(this.document.getLocale(), FilterEventParameters.EMPTY);

        // /WikiDocument

        documentFilter.endWikiDocument(this.document.getDocumentReference().getName(), documentParameters);
    }
}
