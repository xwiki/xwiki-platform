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

package org.xwiki.mailSender.internal;

import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.activation.DataHandler;
import javax.activation.DataSource;
import javax.activation.FileDataSource;
import javax.mail.BodyPart;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMultipart;

import org.xwiki.mailSender.MailSenderUtils;

import com.xpn.xwiki.api.Attachment;

/**
 * To create Mime Part for the mailSender.
 * 
 * @version $Id$
 */
public class MimePartCreator
{
    /** The encoding used for the mails. */
    private static final String EMAIL_ENCODING = "UTF-8";

    /** Inline diposition for Mime Part. */
    private static final String INLINE_DISPOSITION = "inline";

    /** For plain text content. */
    private static final String PLAIN_TEXT = "text/plain";

    /** For html content. */
    private static final String TEXT_HTML = "text/html";

    /**
     * Default constructor.
     */
    public MimePartCreator()
    {
    }

    /**
     * Generates a Mime Multipart from a Mail object.
     * 
     * @param mail Mail used to define the multipart
     * @param util Component
     * @throws MessagingException if a problem occurs while sending the mail
     * @return the multipart corresponding to the mail object
     */
    public Multipart generateMimeMultipart(Mail mail, MailSenderUtils util) throws MessagingException
    {
        MimePartCreator creator = new MimePartCreator();
        Multipart contentsMultipart = new MimeMultipart("alternative");
        List<String> foundEmbeddedImages = new ArrayList<String>();
        boolean hasAttachment = false;

        /* To add an alternative plain part. */
        if (mail.getContents().size() == 1) {
            String[] content = mail.getContents().get(0);
            if (content[0].equals(PLAIN_TEXT) || content[0].equals(TEXT_HTML)) {
                BodyPart alternativePart = creator.createAlternativePart(content[0], content[1], util);
                contentsMultipart.addBodyPart(alternativePart);
            }
        }
        for (String[] content : mail.getContents()) {
            BodyPart contentPart = new MimeBodyPart();
            setPartHeaders(contentPart, content[0], content[1]);
            /* Specific method if this is html content : we must verify whether there are embedded images or not */
            if (content[0].equals(TEXT_HTML)) {
                BodyPart htmlPart = creator.createHtmlPart(foundEmbeddedImages, content[1], mail, contentPart);
                contentsMultipart.addBodyPart(htmlPart);
            } else {
                contentsMultipart.addBodyPart(contentPart);
            }
        }

        Multipart attachmentsMultipart = new MimeMultipart();
        for (Attachment attachment : mail.getAttachments()) {
            if (!foundEmbeddedImages.contains(attachment.getFilename())) {
                hasAttachment = true;
                MimeBodyPart part = creator.createAttachmentPart(attachment);
                attachmentsMultipart.addBodyPart(part);
            }
        }
        if (hasAttachment) {
            Multipart wrapper = new MimeMultipart("mixed");
            BodyPart body = new MimeBodyPart();
            body.setContent(contentsMultipart);
            wrapper.addBodyPart(body);
            BodyPart attachments = new MimeBodyPart();
            attachments.setContent(attachmentsMultipart);
            wrapper.addBodyPart(attachments);
            return wrapper;
        }
        return contentsMultipart;
    }

    /**
     * @param foundEmbeddedImages List of the embedded images found
     * @param content Html content
     * @param mail Mail object, containing the attachments
     * @param contentPart The original Body Part of the html (without the embedded images)
     * @throws MessagingException mex
     * @return a Body Part cointaining the embedded images
     */
    public BodyPart createHtmlPart(List<String> foundEmbeddedImages, String content, Mail mail, BodyPart contentPart)
        throws MessagingException
    {
        boolean hasEmbeddedImages = false;
        List<MimeBodyPart> embeddedImages = new ArrayList<MimeBodyPart>();
        Pattern cidPattern =
            Pattern.compile("src=('|\")cid:([^'\"]*)('|\")", Pattern.CASE_INSENSITIVE | Pattern.MULTILINE);
        Matcher matcher = cidPattern.matcher(content);
        String filename;
        while (matcher.find()) {
            filename = matcher.group(2);
            for (Attachment attachment : mail.getAttachments()) {
                if (filename.equals(attachment.getFilename())) {
                    hasEmbeddedImages = true;
                    MimeBodyPart part = createAttachmentPart(attachment);
                    embeddedImages.add(part);
                    foundEmbeddedImages.add(filename);
                }
            }
        }
        if (hasEmbeddedImages) {
            Multipart htmlMultipart = new MimeMultipart("related");
            htmlMultipart.addBodyPart(contentPart);
            for (MimeBodyPart imagePart : embeddedImages) {
                htmlMultipart.addBodyPart(imagePart);
            }
            BodyPart htmlWrapper = new MimeBodyPart();
            htmlWrapper.setContent(htmlMultipart);
            return htmlWrapper;
        } else {
            return contentPart;
        }
    }

    /**
     * Create a Mime part for the attachment.
     * 
     * @param attachment Attachment to create a Mime part for
     * @return the MimeBodyPart for the attachment
     */
    public MimeBodyPart createAttachmentPart(Attachment attachment)
    {
        try {
            String name = attachment.getFilename();
            byte[] stream = attachment.getContent();
            File temp = File.createTempFile("tmpfile", ".tmp");
            FileOutputStream fos = new FileOutputStream(temp);
            fos.write(stream);
            fos.close();
            DataSource source = new FileDataSource(temp);
            MimeBodyPart part = new MimeBodyPart();
            String attachmentMimeType = attachment.getMimeType();

            part.setDataHandler(new DataHandler(source));
            part.setHeader("Content-Type", attachmentMimeType);
            part.setFileName(name);
            part.setContentID("<" + name + ">");
            part.setDisposition(INLINE_DISPOSITION);

            temp.deleteOnExit();
            return part;
        } catch (Exception e) {
            return new MimeBodyPart();
        }
    }

    /**
     * To create automatically an alternative text part if the mail has only one content.
     * 
     * @param type Mime type of the lone content
     * @param content Content
     * @param utils Component
     * @throws MessagingException mex
     * @return An alternative Bodypart
     */
    public BodyPart createAlternativePart(String type, String content, MailSenderUtils utils) throws MessagingException
    {
        BodyPart alternativePart = new MimeBodyPart();
        if (type.equals(PLAIN_TEXT)) {
            setPartHeaders(alternativePart, type, content);
        }
        if (type.equals(TEXT_HTML)) {
            String parsedText = utils.createPlain(content);
            setPartHeaders(alternativePart, PLAIN_TEXT, parsedText);
        }
        return alternativePart;
    }

    /**
     * To define the headers of a Mime part.
     * 
     * @param part The Mime Part to set
     * @param type The Mime type of the part content
     * @param content The content of the Mime part
     * @throws MessagingException mex
     */
    private void setPartHeaders(BodyPart part, String type, String content) throws MessagingException
    {
        part.setContent(content, type + "; charset=" + EMAIL_ENCODING);
        part.setHeader("Content-Disposition", INLINE_DISPOSITION);
        part.setHeader("Content-Transfer-Encoding", "quoted-printable");
    }

}
