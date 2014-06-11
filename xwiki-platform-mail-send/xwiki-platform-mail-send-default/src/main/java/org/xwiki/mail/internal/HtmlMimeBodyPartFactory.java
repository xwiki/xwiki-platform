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
public class HtmlMimeBodyPartFactory extends AbstractMimeBodyPartFactory<String>
{
    @Inject
    private MimeBodyPartFactory defaultPartFactory;

    @Inject
    @Named("attachment")
    private MimeBodyPartFactory attachmentPartFactory;

    private List<Attachment> attachments = new ArrayList<>();

    private List<Attachment> embeddedImages = new ArrayList<>();

    private String htmlContent;

    @Override public MimeBodyPart create(String content, Map<String, Object> parameters) throws MessagingException
    {

        this.htmlContent = content;

        boolean hasAttachments = parameters.containsKey("attachments");

        if (hasAttachments) {
            this.attachments = (List<Attachment>) parameters.<String, Object>get("attachments");
        }

        MimeBodyPart bodyPart = new MimeBodyPart();

        MimeMultipart multipart = new MimeMultipart("mixed");

        MimeBodyPart htmlBodyPart = new MimeBodyPart();
        if (this.attachments != null) {
            // separate attachment and embedded images
            this.foundEmbeddedImages();
            htmlBodyPart.setContent(this.getRelatedPart());
        } else {
            // Create the HTML body part of the email
            htmlBodyPart.setContent(content, "text/plain; charset=" + StandardCharsets.UTF_8.name());
            htmlBodyPart.setHeader("Content-Type", "text/html");
        }

        String alternaniveText = (String) parameters.get("alternate");
        if (alternaniveText != null && !alternaniveText.equals("")) {
            MimeMultipart alternaniveMultiPart = new MimeMultipart("alternative");

            alternaniveMultiPart.addBodyPart(defaultPartFactory.create(alternaniveText));
            alternaniveMultiPart.addBodyPart(htmlBodyPart);

            MimeBodyPart alternanivePartWrapper = new MimeBodyPart();
            alternanivePartWrapper.setContent(alternaniveMultiPart);

            multipart.addBodyPart(alternanivePartWrapper);
        } else {
            multipart.addBodyPart(htmlBodyPart);
        }

        // Add attachments part to multipart
        for (Attachment attachment : this.attachments) {
            multipart.addBodyPart(attachmentPartFactory.create(attachment));
        }

        bodyPart.setContent(multipart);

        // Handle headers passed as parameter
        addHeaders(bodyPart, parameters);
        return bodyPart;
    }

    /**
     * @return Multipart part of the email, define the html as a multipart/related in case there are images
     */
    private MimeMultipart getRelatedPart() throws MessagingException
    {
        MimeMultipart htmlMultipart = new MimeMultipart("related");
        MimeBodyPart htmlPart = new MimeBodyPart();

        htmlPart.setContent(this.htmlContent, "text/html; charset=" + StandardCharsets.UTF_8.name());
        htmlPart.setHeader("Content-Disposition", "inline");
        htmlPart.setHeader("Content-Transfer-Encoding", "quoted-printable");

        htmlMultipart.addBodyPart(htmlPart);

        // Add the images to the HTML multipart
        for (Attachment attachment : this.embeddedImages) {
            htmlMultipart.addBodyPart(attachmentPartFactory.create(attachment));
        }
        return htmlMultipart;
    }

    /**
     * Separate embedded images from attachments list
     */
    private void foundEmbeddedImages()
    {
        // Find images used with src="cid:" in the email HTML part
        Pattern cidPattern =
                Pattern.compile("src=('|\")cid:([^'\"]*)('|\")", Pattern.CASE_INSENSITIVE | Pattern.MULTILINE);
        Matcher matcher = cidPattern.matcher(this.htmlContent);
        List<String> foundEmbeddedImages = new ArrayList<String>();
        while (matcher.find()) {
            foundEmbeddedImages.add(matcher.group(2));
        }

        // Loop over the attachments of the email, add images used from the HTML to the list of attachments to be
        // embedded with the HTML part, add the other attachements to the list of attachments to be attached to the
        // email.
        for (Attachment attachment : this.attachments) {
            if (foundEmbeddedImages.contains(attachment.getFilename())) {
                this.embeddedImages.add(attachment);
                this.attachments.remove(attachment);
            }
        }
    }
}
