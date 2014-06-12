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
package org.xwiki.mail.internal;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
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

import org.xwiki.component.annotation.Component;
import org.xwiki.mail.MimeBodyPartFactory;

import com.xpn.xwiki.api.Attachment;

/**
 * Creates an html Body Part object. This will be added to a Multi Part message.
 *
 * @version $Id$
 * @since 6.1M2
 */
@Component
@Named("html")
@Singleton
@SuppressWarnings("unchecked")
public class HtmlMimeBodyPartFactory extends AbstractMimeBodyPartFactory<String>
{
    @Inject
    @Named("attachment")
    private MimeBodyPartFactory attachmentPartFactory;

    @Inject
    private MimeBodyPartFactory defaultPartFactory;

    private static final Pattern CID_PATTERN =
            Pattern.compile("src=('|\")cid:([^'\"]*)('|\")", Pattern.CASE_INSENSITIVE | Pattern.MULTILINE);

    @Override public MimeBodyPart create(String content, Map<String, Object> parameters) throws MessagingException
    {

        MimeMultipart multipart = new MimeMultipart("mixed");

        List<Attachment> attachments = (List<Attachment>) parameters.<String, Object>get("attachments");
        List<Attachment> embeddedImages = new ArrayList<>();

        MimeBodyPart htmlBodyPart = new MimeBodyPart();

        if (attachments != null) {
            // separate attachment and embedded images
            Map<String, List> attachmentsMap = handleAttachments(content, attachments);

            embeddedImages = attachmentsMap.get("embeddedImages");
            attachments = attachmentsMap.get("attachments");

            htmlBodyPart.setContent(createRelatedBodyPart(content, embeddedImages));
        } else {
            // Create the HTML body part of the email
            htmlBodyPart.setContent(content, "text/plain; charset=" + StandardCharsets.UTF_8.name());
            htmlBodyPart.setHeader("Content-Type", "text/html");
        }

        //
        String alternaniveText = (String) parameters.get("alternate");
        if (alternaniveText != null) {
            multipart.addBodyPart(createAlternativePart(htmlBodyPart, defaultPartFactory.create(alternaniveText)));
        } else {
            multipart.addBodyPart(htmlBodyPart);
        }
        addAttachments(multipart, attachments);

        MimeBodyPart bodyPart = new MimeBodyPart();
        bodyPart.setContent(multipart);

        // Handle headers passed as parameter
        addHeaders(bodyPart, parameters);
        return bodyPart;
    }

    private void addAttachments(MimeMultipart multipart, List<Attachment> attachments) throws MessagingException
    {
        // Add attachments part to multipart
        if (attachments != null) {
            for (Attachment attachment : attachments) {
                multipart.addBodyPart(attachmentPartFactory.create(attachment));
            }
        }
    }

    /**
     * @return Multipart part of the email, define the html as a multipart/alternative
     */
    private MimeBodyPart createAlternativePart(MimeBodyPart htmlBodyPart, MimeBodyPart textBodyPart)
            throws MessagingException
    {
        MimeMultipart alternativeMultiPart = new MimeMultipart("alternative");

        alternativeMultiPart.addBodyPart(textBodyPart);
        alternativeMultiPart.addBodyPart(htmlBodyPart);

        MimeBodyPart alternativePartWrapper = new MimeBodyPart();
        alternativePartWrapper.setContent(alternativeMultiPart);
        return alternativePartWrapper;
    }

    /**
     * @return Multipart part of the email, define the html as a multipart/related in case there are images
     */
    private MimeMultipart createRelatedBodyPart(String content, List<Attachment> embeddedImages)
            throws MessagingException
    {
        MimeMultipart htmlMultipart = new MimeMultipart("related");

        htmlMultipart.addBodyPart(createRelatedHtmlPart(content));

        // Add the images to the HTML multipart
        addAttachments(htmlMultipart, embeddedImages);
        return htmlMultipart;
    }

    /**
     * Create the Html part of the related body part
     */
    private MimeBodyPart createRelatedHtmlPart(String content) throws MessagingException
    {
        MimeBodyPart htmlPart = new MimeBodyPart();
        htmlPart.setContent(content, "text/html; charset=" + StandardCharsets.UTF_8.name());
        htmlPart.setHeader("Content-Disposition", "inline");
        htmlPart.setHeader("Content-Transfer-Encoding", "quoted-printable");
        return htmlPart;
    }

    /**
     * Separate embedded images from attachments list
     */
    private Map<String, List> handleAttachments(String content, List<Attachment> attachments)
    {
        List<Attachment> embeddedImages = new ArrayList<>();

        // Find images used with src="cid:" in the email HTML part
        Matcher matcher = CID_PATTERN.matcher(content);
        List<String> foundEmbeddedImages = new ArrayList<String>();
        while (matcher.find()) {
            foundEmbeddedImages.add(matcher.group(2));
        }

        // Loop over the attachments of the email, add images used from the HTML to the list of attachments to be
        // embedded with the HTML part, add the other attachements to the list of attachments to be attached to the
        // email.
        for (Attachment attachment : attachments) {
            if (foundEmbeddedImages.contains(attachment.getFilename())) {
                embeddedImages.add(attachment);
                attachments.remove(attachment);
            }
        }

        Map<String, List> attachmentsMap = new HashMap<>();

        attachmentsMap.put("attachments", attachments);
        attachmentsMap.put("embeddedImages", embeddedImages);

        return attachmentsMap;
    }
}
