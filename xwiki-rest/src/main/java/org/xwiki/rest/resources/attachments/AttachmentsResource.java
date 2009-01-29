package org.xwiki.rest.resources.attachments;

import java.util.List;

import org.restlet.data.Form;
import org.restlet.data.Status;
import org.restlet.resource.Representation;
import org.restlet.resource.Variant;
import org.xwiki.rest.Constants;
import org.xwiki.rest.DomainObjectFactory;
import org.xwiki.rest.RangeIterable;
import org.xwiki.rest.Utils;
import org.xwiki.rest.XWikiResource;
import org.xwiki.rest.model.Attachments;

import com.xpn.xwiki.api.Document;
import com.xpn.xwiki.doc.XWikiDocument;

public class AttachmentsResource extends XWikiResource
{

    @Override
    public Representation represent(Variant variant)
    {
        try {
            DocumentInfo documentInfo = getDocumentFromRequest(getRequest(), getResponse(), true, false);
            if (documentInfo == null) {
                getResponse().setStatus(Status.CLIENT_ERROR_NOT_FOUND);
                return null;
            }

            Document doc = documentInfo.getDocument();

            List<com.xpn.xwiki.api.Attachment> xwikiAttachments = doc.getAttachmentList();

            Form queryForm = getRequest().getResourceRef().getQueryAsForm();
            RangeIterable<com.xpn.xwiki.api.Attachment> ri =
                new RangeIterable<com.xpn.xwiki.api.Attachment>(xwikiAttachments, Utils.parseInt(queryForm
                    .getFirstValue(Constants.START_PARAMETER), 0), Utils.parseInt(queryForm
                    .getFirstValue(Constants.NUMBER_PARAMETER), -1));

            /*
             * We need to retrieve the base XWiki documents because Document doesn't have a method for retrieving the
             * external URL for an attachment
             */
            XWikiDocument xwikiDocument = xwiki.getDocument(doc.getPrefixedFullName(), xwikiContext);

            Attachments attachments = new Attachments();

            for (com.xpn.xwiki.api.Attachment xwikiAttachment : ri) {
                String attachmentXWikiUrl =
                    xwikiDocument.getExternalAttachmentURL(xwikiAttachment.getFilename(), "download", xwikiContext)
                        .toString();

                attachments.addAttachment(DomainObjectFactory.createAttachment(getRequest(), resourceClassRegistry,
                    xwikiAttachment, attachmentXWikiUrl));
            }

            return getRepresenterFor(variant).represent(getContext(), getRequest(), getResponse(), attachments);
        } catch (Exception e) {
            e.printStackTrace();
            getResponse().setStatus(Status.SERVER_ERROR_INTERNAL);
        }

        return null;
    }
}
