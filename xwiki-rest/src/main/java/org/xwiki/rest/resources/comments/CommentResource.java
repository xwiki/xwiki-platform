package org.xwiki.rest.resources.comments;

import java.util.Vector;

import org.restlet.data.Status;
import org.restlet.resource.Representation;
import org.restlet.resource.Variant;
import org.xwiki.rest.Constants;
import org.xwiki.rest.DomainObjectFactory;
import org.xwiki.rest.XWikiResource;
import org.xwiki.rest.model.Comment;

import com.xpn.xwiki.api.Document;

public class CommentResource extends XWikiResource
{
    @Override
    public Representation represent(Variant variant)
    {
        try {
            String commentIdString = (String) getRequest().getAttributes().get(Constants.COMMENT_ID_PARAMETER);
            int commentId;

            try {
                commentId = Integer.parseInt(commentIdString);
            } catch (NumberFormatException e) {
                getResponse().setStatus(Status.CLIENT_ERROR_BAD_REQUEST);
                return null;
            }

            DocumentInfo documentInfo = getDocumentFromRequest(getRequest(), getResponse(), true, false);
            if (documentInfo == null) {
                return null;
            }

            Document doc = documentInfo.getDocument();

            Comment comment = null;

            Vector<com.xpn.xwiki.api.Object> xwikiComments = doc.getComments();
            for (com.xpn.xwiki.api.Object xwikiComment : xwikiComments) {
                if (xwikiComment.getNumber() == commentId) {
                    comment =
                        DomainObjectFactory
                            .createComment(getRequest(), resourceClassRegistry, doc, xwikiComment, false);
                    break;
                }
            }

            if (comment == null) {
                getResponse().setStatus(Status.CLIENT_ERROR_NOT_FOUND);
                return null;
            }

            return getRepresenterFor(variant).represent(getContext(), getRequest(), getResponse(), comment);
        } catch (Exception e) {
            e.printStackTrace();
            getResponse().setStatus(Status.SERVER_ERROR_INTERNAL);
        }

        return null;
    }
}
