package org.xwiki.rest.resources.attachments;

import java.io.IOException;
import java.io.OutputStream;

import org.restlet.Context;
import org.restlet.data.MediaType;
import org.restlet.data.Request;
import org.restlet.data.Response;
import org.restlet.data.Status;
import org.restlet.resource.OutputRepresentation;
import org.restlet.resource.Representation;
import org.restlet.resource.StringRepresentation;
import org.restlet.resource.Variant;
import org.xwiki.rest.Constants;
import org.xwiki.rest.DomainObjectFactory;
import org.xwiki.rest.Utils;
import org.xwiki.rest.XWikiResource;

import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.api.Document;
import com.xpn.xwiki.doc.XWikiAttachment;
import com.xpn.xwiki.doc.XWikiDocument;

public class AttachmentResource extends XWikiResource
{
    @Override
    public void init(Context context, Request request, Response response)
    {
        super.init(context, request, response);
        getVariants().add(new Variant(MediaType.ALL));
    }

    @Override
    public Representation represent(Variant variant)
    {
        try {
            DocumentInfo documentInfo = getDocumentFromRequest(getRequest(), getResponse(), true, false);
            if (documentInfo == null) {
                return null;
            }

            Document doc = documentInfo.getDocument();

            String attachmentName = (String) getRequest().getAttributes().get(Constants.ATTACHMENT_NAME_PARAMETER);

            final com.xpn.xwiki.api.Attachment xwikiAttachment = doc.getAttachment(attachmentName);
            if (xwikiAttachment == null) {
                /* If the attachment doesn't exist send a not found header */
                getResponse().setStatus(Status.CLIENT_ERROR_NOT_FOUND);
                return null;
            }

            return new OutputRepresentation(MediaType.valueOf(xwikiAttachment.getMimeType()))
            {
                @Override
                public void write(OutputStream outputStream) throws IOException
                {
                    /* TODO: Maybe we should write the content N bytes at a time */
                    try {
                        outputStream.write(xwikiAttachment.getContent());
                    } catch (XWikiException e) {
                        getResponse().setStatus(Status.SERVER_ERROR_INTERNAL);
                    } finally {
                        outputStream.close();
                    }
                }
            };
        } catch (Exception e) {
            e.printStackTrace();
            getResponse().setStatus(Status.SERVER_ERROR_INTERNAL);
        }

        return null;
    }

    @Override
    public boolean allowPut()
    {
        return true;
    }

    @Override
    public boolean allowDelete()
    {
        return true;
    }

    @Override
    public void handlePut()
    {
        try {
            DocumentInfo documentInfo = getDocumentFromRequest(getRequest(), getResponse(), false, true);
            if (documentInfo == null) {
                getResponse().setStatus(Status.SERVER_ERROR_INTERNAL);
                return;

            }

            Document doc = documentInfo.getDocument();

            String attachmentName = (String) getRequest().getAttributes().get(Constants.ATTACHMENT_NAME_PARAMETER);
            boolean existed = false;

            if (doc.getAttachment(attachmentName) != null) {
                existed = true;
            }

            doc.addAttachment(attachmentName, getRequest().getEntity().getStream());

            doc.save();

            if (existed) {
                getResponse().setStatus(Status.SUCCESS_ACCEPTED);
            } else {
                getResponse().setStatus(Status.SUCCESS_CREATED);
            }

            /*
             * We need to retrieve the base XWiki documents because Document doesn't have a method for retrieving the
             * external URL for an attachment
             */
            XWikiDocument xwikiDocument = xwiki.getDocument(doc.getPrefixedFullName(), xwikiContext);
            String attachmentXWikiUrl =
                xwikiDocument.getExternalAttachmentURL(attachmentName, "download", xwikiContext).toString();

            getResponse().setEntity(
                new StringRepresentation(Utils.toXml(DomainObjectFactory.createAttachment(getRequest(),
                    resourceClassRegistry, doc.getAttachment(attachmentName), attachmentXWikiUrl))));

        } catch (XWikiException e) {
            if (e.getCode() == XWikiException.ERROR_XWIKI_ACCESS_DENIED) {
                getResponse().setStatus(Status.CLIENT_ERROR_FORBIDDEN);
            } else {
                getResponse().setStatus(Status.SERVER_ERROR_INTERNAL);
            }
        } catch (IOException e) {
            e.printStackTrace();
            getResponse().setStatus(Status.SERVER_ERROR_INTERNAL);
        }
    }

    @Override
    public void handleDelete()
    {
        try {
            DocumentInfo documentInfo = getDocumentFromRequest(getRequest(), getResponse(), true, true);
            if (documentInfo == null) {
                getResponse().setStatus(Status.SERVER_ERROR_INTERNAL);
                return;

            }

            Document doc = documentInfo.getDocument();

            String attachmentName = (String) getRequest().getAttributes().get(Constants.ATTACHMENT_NAME_PARAMETER);

            com.xpn.xwiki.api.Attachment xwikiAttachment = doc.getAttachment(attachmentName);
            if (xwikiAttachment == null) {
                getResponse().setStatus(Status.CLIENT_ERROR_NOT_FOUND);
                return;
            }

            XWikiDocument xwikiDocument = xwiki.getDocument(doc.getPrefixedFullName(), xwikiContext);
            XWikiAttachment baseXWikiAttachment = xwikiDocument.getAttachment(attachmentName);
            if (doc.hasAccessLevel("edit", xwikiUser)) {
                xwikiDocument.deleteAttachment(baseXWikiAttachment, xwikiContext);
                getResponse().setStatus(Status.SUCCESS_NO_CONTENT);
            } else {
                getResponse().setStatus(Status.CLIENT_ERROR_FORBIDDEN);
            }
        } catch (Exception e) {
            getResponse().setStatus(Status.SERVER_ERROR_INTERNAL);
        }
    }

}
