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
package org.xwiki.mail.internal.factory.html;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import javax.mail.MessagingException;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMultipart;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.mail.MimeBodyPartFactory;
import org.xwiki.mail.internal.factory.AbstractMimeBodyPartFactory;

import com.xpn.xwiki.api.Attachment;

/**
 * Creates an HTML {@link javax.mail.BodyPart} object that supports a text alternative and a list of attachments
 * that will be added to the mail as standard attachments and also as embedded images if they are referenced in the
 * passed HTML using the format {@code <img src="cid:(attachment name)"/>}.
 *
 * @version $Id$
 * @since 6.1M2
 */
@Component
@Named("text/html")
@Singleton
@SuppressWarnings("unchecked")
public class HTMLMimeBodyPartFactory extends AbstractMimeBodyPartFactory<String>
{
    private static final Pattern CID_PATTERN =
        Pattern.compile("src=('|\")cid:([^'\"]*)('|\")", Pattern.CASE_INSENSITIVE | Pattern.MULTILINE);

    private static final String TEXT_HTML_CONTENT_TYPE = "text/html; charset=" + StandardCharsets.UTF_8.name();

    @Inject
    @Named("xwiki/attachment")
    private MimeBodyPartFactory<Attachment> attachmentPartFactory;

    @Inject
    private MimeBodyPartFactory<String> defaultPartFactory;

    @Inject
    private Logger logger;

    @Override
    public MimeBodyPart create(String content, Map<String, Object> parameters) throws MessagingException
    {
        MimeBodyPart resultBodyPart;

        // Separate normal attachment from embedded image attachments
        List<Attachment> allAttachments = (List<Attachment>) parameters.get("attachments");
        Pair<List<Attachment>, List<Attachment>> attachmentPairs = separateAttachments(content, allAttachments);
        List<Attachment> embeddedImageAttachments = attachmentPairs.getLeft();
        List<Attachment> normalAttachments = attachmentPairs.getRight();

        // Step 1: Handle the HTML section of the mail.
        MimeBodyPart htmlBodyPart;
        if (!embeddedImageAttachments.isEmpty()) {
            htmlBodyPart = new MimeBodyPart();
            htmlBodyPart.setContent(createHTMLMultipart(content, embeddedImageAttachments));
        } else {
            // Create the HTML body part of the email
            htmlBodyPart = createHTMLBodyPart(content, false);
        }

        // Step 2: Handle the optional alternative text
        String alternativeText = (String) parameters.get("alternate");
        if (alternativeText != null) {
            resultBodyPart = createAlternativePart(htmlBodyPart,
                this.defaultPartFactory.create(alternativeText, Collections.<String, Object>emptyMap()));
        } else {
            // No alternative text, just add the HTML body part to the Multipart
            resultBodyPart = htmlBodyPart;
        }

        // Step 3 Add the normal attachments (if any). Any embedded images have already been handled in the HTML body
        // part. Note: If there are attachments we need to wrap our body part inside a "mixed" Multipart.
        if (!normalAttachments.isEmpty()) {
            MimeMultipart multipart = new MimeMultipart("mixed");
            multipart.addBodyPart(resultBodyPart);
            handleAttachments(multipart, normalAttachments);
            resultBodyPart = new MimeBodyPart();
            resultBodyPart.setContent(multipart);
        }

        // Handle headers passed as parameter
        addHeaders(resultBodyPart, parameters);

        return resultBodyPart;
    }

    private void handleAttachments(MimeMultipart multipart, List<Attachment> attachments)
    {
        for (Attachment attachment : attachments) {
            try {
                multipart.addBodyPart(
                    this.attachmentPartFactory.create(attachment, Collections.<String, Object>emptyMap()));
            } catch (MessagingException e) {
                // There is no reason to fail the whole mail for a broken attachment
                this.logger.warn("Failed to add attachment [{}@{}] to the mail: {}",
                    attachment.getDocument().getDocumentReference(), attachment.getFilename(),
                    ExceptionUtils.getRootCauseMessage(e));
            }
        }
    }

    /**
     * @return Multipart part of the email, define the HTML as a multipart/alternative
     */
    private MimeBodyPart createAlternativePart(MimeBodyPart htmlBodyPart, MimeBodyPart textBodyPart)
        throws MessagingException
    {
        MimeMultipart alternativeMultiPart = new MimeMultipart("alternative");

        // Note: it's important to have the text before the HTML, from low fidelity to high fidelity according to the
        // MIME specification. Otherwise some readers will display text even though there's HTML in your mail message.
        alternativeMultiPart.addBodyPart(textBodyPart);
        alternativeMultiPart.addBodyPart(htmlBodyPart);

        MimeBodyPart alternativePartWrapper = new MimeBodyPart();
        alternativePartWrapper.setContent(alternativeMultiPart);
        return alternativePartWrapper;
    }

    private MimeMultipart createHTMLMultipart(String content, List<Attachment> embeddedImages)
        throws MessagingException
    {
        MimeMultipart htmlMultipart = new MimeMultipart("related");
        htmlMultipart.addBodyPart(createHTMLBodyPart(content, true));
        handleAttachments(htmlMultipart, embeddedImages);
        return htmlMultipart;
    }

    private MimeBodyPart createHTMLBodyPart(String content, boolean hasAttachments) throws MessagingException
    {
        MimeBodyPart htmlPart = new MimeBodyPart();
        htmlPart.setContent(content, TEXT_HTML_CONTENT_TYPE);
        htmlPart.setHeader("Content-Type", TEXT_HTML_CONTENT_TYPE);
        if (hasAttachments) {
            htmlPart.setHeader("Content-Disposition", "inline");
            htmlPart.setHeader("Content-Transfer-Encoding", "quoted-printable");
        }
        return htmlPart;
    }

    /**
     * Separate embedded images from attachments list.
     *
     * @return the embedded attachments on the left and the normal attachments on the right of the Pair
     */
    private Pair<List<Attachment>, List<Attachment>> separateAttachments(String content, List<Attachment> attachments)
    {
        if (attachments == null) {
            return new ImmutablePair<>(Collections.<Attachment>emptyList(), Collections.<Attachment>emptyList());
        }

        // Copy all attachments in the list of attachments to add to the email. We'll then remove the attachments
        // that are embedded from the list below.
        List<Attachment> normalAttachments = new ArrayList<>(attachments);

        // Find images used with src="cid:" in the email HTML part
        Matcher matcher = CID_PATTERN.matcher(content);
        List<String> embeddedImageNames = new ArrayList<>();
        while (matcher.find()) {
            embeddedImageNames.add(matcher.group(2));
        }

        // Loop over the attachments of the email, add images used from the HTML to the list of attachments to be
        // embedded with the HTML part, add the other attachments to the list of attachments to be attached to the
        // email.
        List<Attachment> embeddedImageAttachments = new ArrayList<>();
        for (Attachment attachment : attachments) {
            if (embeddedImageNames.contains(attachment.getFilename())) {
                embeddedImageAttachments.add(attachment);
                normalAttachments.remove(attachment);
            }
        }

        return new ImmutablePair<>(embeddedImageAttachments, normalAttachments);
    }
}
