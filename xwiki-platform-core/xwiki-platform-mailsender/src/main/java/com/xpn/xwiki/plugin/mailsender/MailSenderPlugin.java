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
package com.xpn.xwiki.plugin.mailsender;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.activation.DataHandler;
import javax.activation.DataSource;
import javax.activation.FileDataSource;
import javax.mail.BodyPart;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.SendFailedException;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;

import org.apache.commons.lang3.StringUtils;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.Velocity;
import org.apache.velocity.context.Context;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xwiki.localization.LocaleUtils;
import org.xwiki.rendering.syntax.Syntax;
import org.xwiki.velocity.VelocityManager;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.api.Api;
import com.xpn.xwiki.api.Attachment;
import com.xpn.xwiki.api.Document;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.objects.classes.BaseClass;
import com.xpn.xwiki.objects.classes.TextAreaClass;
import com.xpn.xwiki.plugin.XWikiDefaultPlugin;
import com.xpn.xwiki.plugin.XWikiPluginInterface;
import com.xpn.xwiki.util.Util;
import com.xpn.xwiki.web.ExternalServletURLFactory;
import com.xpn.xwiki.web.Utils;
import com.xpn.xwiki.web.XWikiURLFactory;

/**
 * Plugin that brings powerful mailing capabilities.
 * 
 * @see MailSender
 * @version $Id$
 */
public class MailSenderPlugin extends XWikiDefaultPlugin
{
    /** Logging helper object. */
    private static final Logger LOGGER = LoggerFactory.getLogger(MailSenderPlugin.class);

    /**
     * Since Java uses full Unicode Strings and email clients manage it we force email encoding to UTF-8. XWiki encoding
     * must be used when working with storage or the container since they can be configured to use another encoding,
     * this constraint does not apply here.
     */
    private static final String EMAIL_ENCODING = "UTF-8";

    /**
     * Error code signaling that the mail template requested for
     * {@link #sendMailFromTemplate(String, String, String, String, String, String, VelocityContext, XWikiContext)} was
     * not found.
     */
    public static int ERROR_TEMPLATE_EMAIL_OBJECT_NOT_FOUND = -2;

    /** Generic error code for plugin failures. */
    public static int ERROR = -1;

    /** The name of the Object Type holding mail templates. */
    public static final String EMAIL_XWIKI_CLASS_NAME = "XWiki.Mail";

    /** The name of the plugin, used for accessing it from scripting environments. */
    public static final String ID = "mailsender";

    protected static final String URL_SEPARATOR = "/";

    /** A pattern for determining if a line represents a SMTP header, conforming to RFC 2822. */
    private static final Pattern SMTP_HEADER = Pattern.compile("^([\\x21-\\x7E&&[^\\x3A]]++):(.*+)$");

    /** The name of the header that specifies the subject of the mail. */
    private static final String SUBJECT = "Subject";

    /** The name of the header that specifies the sender of the mail. */
    private static final String FROM = "From";

    /**
     * Default plugin constructor.
     * 
     * @see XWikiDefaultPlugin#XWikiDefaultPlugin(String,String,com.xpn.xwiki.XWikiContext)
     */
    public MailSenderPlugin(String name, String className, XWikiContext context)
    {
        super(name, className, context);
    }

    @Override
    public void init(XWikiContext context)
    {
        try {
            initMailClass(context);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void virtualInit(XWikiContext context)
    {
        try {
            initMailClass(context);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public String getName()
    {
        return ID;
    }

    @Override
    public Api getPluginApi(XWikiPluginInterface plugin, XWikiContext context)
    {
        return new MailSenderPluginApi((MailSenderPlugin) plugin, context);
    }

    /**
     * Split comma separated list of emails
     * 
     * @param email comma separated list of emails
     * @return An array containing the emails
     */
    public static String[] parseAddresses(String email)
    {
        if (email == null) {
            return null;
        }
        email = email.trim();
        String[] emails = email.split(",");
        for (int i = 0; i < emails.length; i++) {
            emails[i] = emails[i].trim();
        }
        return emails;
    }

    /**
     * Filters a list of emails : removes illegal addresses
     * 
     * @param email List of emails
     * @return An Array containing the correct adresses
     */
    private static InternetAddress[] toInternetAddresses(String email) throws AddressException
    {
        String[] mails = parseAddresses(email);
        if (mails == null) {
            return null;
        }

        InternetAddress[] address = new InternetAddress[mails.length];
        for (int i = 0; i < mails.length; i++) {
            address[i] = new InternetAddress(mails[i]);
        }
        return address;
    }

    /**
     * Creates the Mail XWiki Class
     * 
     * @param context Context of the request
     * @return the Mail XWiki Class
     */
    protected BaseClass initMailClass(XWikiContext context) throws XWikiException
    {
        XWikiDocument doc;
        XWiki xwiki = context.getWiki();
        boolean needsUpdate = false;

        try {
            doc = xwiki.getDocument(EMAIL_XWIKI_CLASS_NAME, context);
        } catch (Exception e) {
            doc = new XWikiDocument();
            String[] spaceAndName = EMAIL_XWIKI_CLASS_NAME.split(".");
            doc.setSpace(spaceAndName[0]);
            doc.setName(spaceAndName[1]);
            needsUpdate = true;
        }

        BaseClass bclass = doc.getXClass();
        bclass.setName(EMAIL_XWIKI_CLASS_NAME);
        needsUpdate |= bclass.addTextField("subject", "Subject", 40);
        needsUpdate |= bclass.addTextField("language", "Language", 5);
        needsUpdate |= bclass.addTextAreaField("text", "Text", 80, 15, TextAreaClass.EditorType.PURE_TEXT);
        needsUpdate |= bclass.addTextAreaField("html", "HTML", 80, 15, TextAreaClass.EditorType.PURE_TEXT);

        if (StringUtils.isBlank(doc.getCreator())) {
            needsUpdate = true;
            doc.setCreator("superadmin");
        }
        if (StringUtils.isBlank(doc.getAuthor())) {
            needsUpdate = true;
            doc.setAuthor(doc.getCreator());
        }
        if (StringUtils.isBlank(doc.getParent())) {
            needsUpdate = true;
            doc.setParent("XWiki.XWikiClasses");
        }
        if (StringUtils.isBlank(doc.getTitle())) {
            needsUpdate = true;
            doc.setTitle("XWiki Mail Class");
        }
        if (StringUtils.isBlank(doc.getContent()) || !Syntax.XWIKI_2_0.equals(doc.getSyntax())) {
            needsUpdate = true;
            doc.setContent("{{include reference=\"XWiki.ClassSheet\" /}}");
            doc.setSyntax(Syntax.XWIKI_2_0);
        }
        if (!doc.isHidden()) {
            needsUpdate = true;
            doc.setHidden(true);
        }

        if (needsUpdate) {
            xwiki.saveDocument(doc, context);
        }
        return bclass;
    }

    /**
     * Creates a MIME message (message with binary content carrying capabilities) from an existing Mail
     * 
     * @param mail The original Mail object
     * @param session Mail session
     * @return The MIME message
     */
    private MimeMessage createMimeMessage(Mail mail, Session session, XWikiContext context) throws MessagingException,
        XWikiException, IOException
    {
        // this will also check for email error
        InternetAddress from = new InternetAddress(mail.getFrom());
        String recipients = mail.getHeader("To");
        if (StringUtils.isBlank(recipients)) {
            recipients = mail.getTo();
        } else {
            recipients = mail.getTo() + "," + recipients;
        }
        InternetAddress[] to = toInternetAddresses(recipients);
        recipients = mail.getHeader("Cc");
        if (StringUtils.isBlank(recipients)) {
            recipients = mail.getCc();
        } else {
            recipients = mail.getCc() + "," + recipients;
        }
        InternetAddress[] cc = toInternetAddresses(recipients);
        recipients = mail.getHeader("Bcc");
        if (StringUtils.isBlank(recipients)) {
            recipients = mail.getBcc();
        } else {
            recipients = mail.getBcc() + "," + recipients;
        }
        InternetAddress[] bcc = toInternetAddresses(recipients);

        if ((to == null) && (cc == null) && (bcc == null)) {
            LOGGER.info("No recipient -> skipping this email");
            return null;
        }

        MimeMessage message = new MimeMessage(session);
        message.setSentDate(new Date());
        message.setFrom(from);

        if (to != null) {
            message.setRecipients(javax.mail.Message.RecipientType.TO, to);
        }

        if (cc != null) {
            message.setRecipients(javax.mail.Message.RecipientType.CC, cc);
        }

        if (bcc != null) {
            message.setRecipients(javax.mail.Message.RecipientType.BCC, bcc);
        }

        message.setSubject(mail.getSubject(), EMAIL_ENCODING);

        for (Map.Entry<String, String> header : mail.getHeaders().entrySet()) {
            message.setHeader(header.getKey(), header.getValue());
        }

        if (mail.getHtmlPart() != null || mail.getAttachments() != null) {
            Multipart multipart = createMimeMultipart(mail, context);
            message.setContent(multipart);
        } else {
            message.setText(mail.getTextPart());
        }

        message.setSentDate(new Date());
        message.saveChanges();
        return message;
    }

    /**
     * Add attachments to a multipart message
     * 
     * @param attachment the attachment to create the body part for.
     * @param context the XWiki context.
     * @return the body part for the given attachment.
     */
    public MimeBodyPart createAttachmentBodyPart(Attachment attachment, XWikiContext context) throws XWikiException,
        IOException, MessagingException
    {
        String name = attachment.getFilename();
        byte[] stream = attachment.getContent();
        File temp = File.createTempFile("tmpfile", ".tmp");
        FileOutputStream fos = new FileOutputStream(temp);
        fos.write(stream);
        fos.close();
        DataSource source = new FileDataSource(temp);
        MimeBodyPart part = new MimeBodyPart();
        String mimeType = MimeTypesUtil.getMimeTypeFromFilename(name);

        part.setDataHandler(new DataHandler(source));
        part.setHeader("Content-Type", mimeType);
        part.setFileName(name);
        part.setContentID("<" + name + ">");
        part.setDisposition("inline");

        temp.deleteOnExit();

        return part;
    }

    /**
     * Creates a Multipart MIME Message (multiple content-types within the same message) from an existing mail
     * 
     * @param mail The original Mail
     * @return The Multipart MIME message
     */
    public Multipart createMimeMultipart(Mail mail, XWikiContext context) throws MessagingException, XWikiException,
        IOException
    {
        Multipart multipart;
        List<Attachment> rawAttachments =
            mail.getAttachments() != null ? mail.getAttachments() : new ArrayList<Attachment>();

        if (mail.getHtmlPart() == null && mail.getAttachments() != null) {
            multipart = new MimeMultipart("mixed");

            // Create the text part of the email
            BodyPart textPart = new MimeBodyPart();
            textPart.setContent(mail.getTextPart(), "text/plain; charset=" + EMAIL_ENCODING);
            multipart.addBodyPart(textPart);

            // Add attachments to the main multipart
            for (Attachment attachment : rawAttachments) {
                multipart.addBodyPart(createAttachmentBodyPart(attachment, context));
            }
        } else {
            multipart = new MimeMultipart("mixed");
            List<Attachment> attachments = new ArrayList<Attachment>();
            List<Attachment> embeddedImages = new ArrayList<Attachment>();

            // Create the text part of the email
            BodyPart textPart;
            textPart = new MimeBodyPart();
            textPart.setText(mail.getTextPart());

            // Create the HTML part of the email, define the html as a multipart/related in case there are images
            Multipart htmlMultipart = new MimeMultipart("related");
            BodyPart htmlPart = new MimeBodyPart();
            htmlPart.setContent(mail.getHtmlPart(), "text/html; charset=" + EMAIL_ENCODING);
            htmlPart.setHeader("Content-Disposition", "inline");
            htmlPart.setHeader("Content-Transfer-Encoding", "quoted-printable");
            htmlMultipart.addBodyPart(htmlPart);

            // Find images used with src="cid:" in the email HTML part
            Pattern cidPattern =
                Pattern.compile("src=('|\")cid:([^'\"]*)('|\")", Pattern.CASE_INSENSITIVE | Pattern.MULTILINE);
            Matcher matcher = cidPattern.matcher(mail.getHtmlPart());
            List<String> foundEmbeddedImages = new ArrayList<String>();
            while (matcher.find()) {
                foundEmbeddedImages.add(matcher.group(2));
            }

            // Loop over the attachments of the email, add images used from the HTML to the list of attachments to be
            // embedded with the HTML part, add the other attachements to the list of attachments to be attached to the
            // email.
            for (Attachment attachment : rawAttachments) {
                if (foundEmbeddedImages.contains(attachment.getFilename())) {
                    embeddedImages.add(attachment);
                } else {
                    attachments.add(attachment);
                }
            }

            // Add the images to the HTML multipart (they should be hidden from the mail reader attachment list)
            for (Attachment image : embeddedImages) {
                htmlMultipart.addBodyPart(createAttachmentBodyPart(image, context));
            }

            // Wrap the HTML and text parts in an alternative body part and add it to the main multipart
            Multipart alternativePart = new MimeMultipart("alternative");
            BodyPart alternativeMultipartWrapper = new MimeBodyPart();
            BodyPart htmlMultipartWrapper = new MimeBodyPart();
            alternativePart.addBodyPart(textPart);
            htmlMultipartWrapper.setContent(htmlMultipart);
            alternativePart.addBodyPart(htmlMultipartWrapper);
            alternativeMultipartWrapper.setContent(alternativePart);
            multipart.addBodyPart(alternativeMultipartWrapper);

            // Add attachments to the main multipart
            for (Attachment attachment : attachments) {
                multipart.addBodyPart(createAttachmentBodyPart(attachment, context));
            }
        }

        return multipart;
    }

    /**
     * Splits a raw mail into headers and the actual content, filling in a {@link Mail} object. This method should be
     * compliant with RFC 2822 as much as possible. If the message accidentally starts with what looks like a mail
     * header, then that line <strong>WILL</strong> be considered a header; no check on the semantics of the header is
     * performed.
     * 
     * @param rawMessage the raw content of the message that should be parsed
     * @param toMail the {@code Mail} to create
     * @throws IllegalArgumentException if the target Mail or the content to parse are null or the empty string
     */
    protected void parseRawMessage(String rawMessage, Mail toMail)
    {
        // Sanity check
        if (toMail == null) {
            throw new IllegalArgumentException("The target Mail can't be null");
        } else if (rawMessage == null) {
            throw new IllegalArgumentException("rawMessage can't be null");
        } else if (StringUtils.isBlank(rawMessage)) {
            throw new IllegalArgumentException("rawMessage can't be empty");
        }

        try {
            // The message is read line by line
            BufferedReader input = new BufferedReader(new StringReader(rawMessage));
            String line;
            StringWriter result = new StringWriter();
            PrintWriter output = new PrintWriter(result);
            boolean headersFound = false;

            line = input.readLine();
            // Additional headers are at the start. Parse them and put them in the Mail object.
            // Warning: no empty lines are allowed before the headers.
            Matcher m = SMTP_HEADER.matcher(line);
            while (line != null && m.matches()) {
                String header = m.group(1);
                String value = m.group(2);
                line = input.readLine();
                while (line != null && (line.startsWith(" ") || line.startsWith("\t"))) {
                    value += line;
                    line = input.readLine();
                }
                if (header.equals(SUBJECT)) {
                    toMail.setSubject(value);
                } else if (header.equals(FROM)) {
                    toMail.setFrom(value);
                } else {
                    toMail.setHeader(header, value);
                }
                if (line != null) {
                    m.reset(line);
                }
                headersFound = true;
            }

            // There should be one empty line here, separating the body from the headers.
            if (headersFound && line != null && StringUtils.isBlank(line)) {
                line = input.readLine();
            } else {
                if (headersFound) {
                    LOGGER.warn("Mail body does not contain an empty line between the headers and the body.");
                }
            }

            // If no text exists after the headers, return
            if (line == null) {
                toMail.setTextPart("");
                return;
            }

            do {
                // Mails always use \r\n as EOL
                output.print(line + "\r\n");
            } while ((line = input.readLine()) != null);

            toMail.setTextPart(result.toString());
        } catch (IOException ioe) {
            // Can't really happen here
            LOGGER.error("Unexpected IO exception while preparing a mail", ioe);
        }
    }

    /**
     * Evaluates a String property containing Velocity
     * 
     * @param property The String property
     * @param context Context of the request
     * @return The evaluated String
     */
    protected String evaluate(String property, Context context) throws Exception
    {
        String value = (String) context.get(property);
        StringWriter stringWriter = new StringWriter();
        Velocity.evaluate(context, stringWriter, property, value);
        stringWriter.close();
        return stringWriter.toString();
    }

    /**
     * Get a file name from its path
     * 
     * @param path The file path
     * @return The file name
     */
    protected String getFileName(String path)
    {
        return path.substring(path.lastIndexOf(URL_SEPARATOR) + 1);
    }

    /**
     * Init a Mail Properties map (exs: smtp, host)
     * 
     * @return The properties
     */
    private Properties initProperties(MailConfiguration mailConfiguration)
    {
        Properties properties = new Properties();

        // Note: The full list of available properties that we can set is defined here:
        // http://java.sun.com/products/javamail/javadocs/com/sun/mail/smtp/package-summary.html

        properties.put("mail.smtp.port", Integer.toString(mailConfiguration.getPort()));
        properties.put("mail.smtp.host", mailConfiguration.getHost());
        properties.put("mail.smtp.localhost", "localhost");
        properties.put("mail.host", "localhost");
        properties.put("mail.debug", "false");

        if (mailConfiguration.getFrom() != null) {
            properties.put("mail.smtp.from", mailConfiguration.getFrom());
        }

        if (mailConfiguration.usesAuthentication()) {
            properties.put("mail.smtp.auth", "true");
        }

        mailConfiguration.appendExtraPropertiesTo(properties, true);

        return properties;
    }

    /**
     * Prepares a Mail Velocity context
     * 
     * @param fromAddr Mail from
     * @param toAddr Mail to
     * @param ccAddr Mail cc
     * @param bccAddr Mail bcc
     * @param vcontext The Velocity context to prepare
     * @return The prepared context
     */
    public VelocityContext prepareVelocityContext(String fromAddr, String toAddr, String ccAddr, String bccAddr,
        VelocityContext vcontext, XWikiContext context)
    {
        if (vcontext == null) {
            // Use the original velocity context as a starting point
            VelocityManager velocityManager = Utils.getComponent(VelocityManager.class);
            vcontext = new VelocityContext(velocityManager.getVelocityContext());
        }

        vcontext.put("from.name", fromAddr);
        vcontext.put("from.address", fromAddr);
        vcontext.put("to.name", toAddr);
        vcontext.put("to.address", toAddr);
        vcontext.put("to.cc", ccAddr);
        vcontext.put("to.bcc", bccAddr);
        vcontext.put("bounce", fromAddr);

        return vcontext;
    }

    /**
     * Prepares a Mail Velocity context based on a map of parameters
     *
     * @param fromAddr Mail from
     * @param toAddr Mail to
     * @param ccAddr Mail cc
     * @param bccAddr Mail bcc
     * @param parameters variables to be passed to the velocity context
     * @return The prepared context
     */
    public VelocityContext prepareVelocityContext(String fromAddr, String toAddr, String ccAddr, String bccAddr,
        Map<String, Object> parameters, XWikiContext context)
    {
        VelocityManager velocityManager = Utils.getComponent(VelocityManager.class);
        VelocityContext vcontext = new VelocityContext(velocityManager.getVelocityContext());

        if (parameters != null) {
            for (Map.Entry<String, Object> entry : parameters.entrySet()) {
                vcontext.put(entry.getKey(), entry.getValue());
            }
        }

        return vcontext;
    }

    /**
     * Send a single Mail
     * 
     * @param mailItem The Mail to send
     * @return True if the the email has been sent
     */
    public boolean sendMail(Mail mailItem, XWikiContext context) throws MessagingException,
        UnsupportedEncodingException
    {
        // TODO: Fix the need to instantiate a new XWiki API object
        com.xpn.xwiki.api.XWiki xwikiApi = new com.xpn.xwiki.api.XWiki(context.getWiki(), context);
        return sendMail(mailItem, new MailConfiguration(xwikiApi), context);
    }

    /**
     * Send a single Mail
     *
     * @param mailItem The Mail to send
     * @return True if the the email has been sent
     */
    public boolean sendMail(Mail mailItem, MailConfiguration mailConfiguration, XWikiContext context)
        throws MessagingException, UnsupportedEncodingException
    {
        ArrayList<Mail> mailList = new ArrayList<Mail>();
        mailList.add(mailItem);
        return sendMails(mailList, mailConfiguration, context);
    }

    /**
     * Send a Collection of Mails (multiple emails)
     * 
     * @param emails Mail Collection
     * @return True in any case (TODO ?)
     */
    public boolean sendMails(Collection<Mail> emails, XWikiContext context) throws MessagingException,
        UnsupportedEncodingException
    {
        // TODO: Fix the need to instantiate a new XWiki API object
        com.xpn.xwiki.api.XWiki xwikiApi = new com.xpn.xwiki.api.XWiki(context.getWiki(), context);
        return sendMails(emails, new MailConfiguration(xwikiApi), context);
    }

    /**
     * Send a Collection of Mails (multiple emails)
     * 
     * @param emails Mail Collection
     * @return True in any case (TODO ?)
     */
    public boolean sendMails(Collection<Mail> emails, MailConfiguration mailConfiguration, XWikiContext context)
        throws MessagingException, UnsupportedEncodingException
    {
        Session session = null;
        Transport transport = null;
        int emailCount = emails.size();
        int count = 0;
        int sendFailedCount = 0;
        try {
            for (Iterator<Mail> emailIt = emails.iterator(); emailIt.hasNext();) {
                count++;

                Mail mail = emailIt.next();
                LOGGER.info("Sending email: " + mail.toString());

                if ((transport == null) || (session == null)) {
                    // initialize JavaMail Session and Transport
                    Properties props = initProperties(mailConfiguration);
                    session = Session.getInstance(props, null);
                    transport = session.getTransport("smtp");
                    if (!mailConfiguration.usesAuthentication()) {
                        // no auth info - typical 127.0.0.1 open relay scenario
                        transport.connect();
                    } else {
                        // auth info present - typical with external smtp server
                        transport.connect(mailConfiguration.getSmtpUsername(), mailConfiguration.getSmtpPassword());
                    }
                }

                try {
                    MimeMessage message = createMimeMessage(mail, session, context);
                    if (message == null) {
                        continue;
                    }

                    transport.sendMessage(message, message.getAllRecipients());

                    // close the connection every other 100 emails
                    if ((count % 100) == 0) {
                        try {
                            if (transport != null) {
                                transport.close();
                            }
                        } catch (MessagingException ex) {
                            LOGGER.error("MessagingException has occured.", ex);
                        }
                        transport = null;
                        session = null;
                    }
                } catch (SendFailedException ex) {
                    sendFailedCount++;
                    LOGGER.error("SendFailedException has occured.", ex);
                    LOGGER.error("Detailed email information" + mail.toString());
                    if (emailCount == 1) {
                        throw ex;
                    }
                    if ((emailCount != 1) && (sendFailedCount > 10)) {
                        throw ex;
                    }
                } catch (MessagingException mex) {
                    LOGGER.error("MessagingException has occured.", mex);
                    LOGGER.error("Detailed email information" + mail.toString());
                    if (emailCount == 1) {
                        throw mex;
                    }
                } catch (XWikiException e) {
                    LOGGER.error("XWikiException has occured.", e);
                } catch (IOException e) {
                    LOGGER.error("IOException has occured.", e);
                }
            }
        } finally {
            try {
                if (transport != null) {
                    transport.close();
                }
            } catch (MessagingException ex) {
                LOGGER.error("MessagingException has occured.", ex);
            }

            LOGGER.info("sendEmails: Email count = " + emailCount + " sent count = " + count);
        }
        return true;
    }

    /**
     * Uses an XWiki document to build the message subject and context, based on variables stored in the
     * VelocityContext. Sends the email.
     * 
     * @param templateDocFullName Full name of the template to be used (example: XWiki.MyEmailTemplate). The template
     *            needs to have an XWiki.Email object attached
     * @param from Email sender
     * @param to Email recipient
     * @param cc Email Carbon Copy
     * @param bcc Email Hidden Carbon Copy
     * @param language Language of the email
     * @param vcontext Velocity context passed to the velocity renderer
     * @return True if the email has been sent
     */
    public int sendMailFromTemplate(String templateDocFullName, String from, String to, String cc, String bcc,
        String language, VelocityContext vcontext, XWikiContext context) throws XWikiException
    {
        XWikiURLFactory originalURLFactory = context.getURLFactory();
        Locale originalLocale = context.getLocale();
        try {
            context.setURLFactory(new ExternalServletURLFactory(context));
            context.setLocale(LocaleUtils.toLocale(language));
            VelocityContext updatedVelocityContext = prepareVelocityContext(from, to, cc, bcc, vcontext, context);
            XWiki xwiki = context.getWiki();
            XWikiDocument doc = xwiki.getDocument(templateDocFullName, context);
            Document docApi = new Document(doc, context);

            BaseObject obj = doc.getObject(EMAIL_XWIKI_CLASS_NAME, "language", language);
            if (obj == null) {
                obj = doc.getObject(EMAIL_XWIKI_CLASS_NAME, "language", "en");
            }
            if (obj == null) {
                LOGGER.error("No mail object found in the document " + templateDocFullName);
                return ERROR_TEMPLATE_EMAIL_OBJECT_NOT_FOUND;
            }
            String subjectContent = obj.getStringValue("subject");
            String txtContent = obj.getStringValue("text");
            String htmlContent = obj.getStringValue("html");

            String subject = evaluate(subjectContent, templateDocFullName, updatedVelocityContext, context);
            String msg = evaluate(txtContent, templateDocFullName, updatedVelocityContext, context);
            String html = evaluate(htmlContent, templateDocFullName, updatedVelocityContext, context);

            Mail mail = new Mail();
            mail.setFrom((String) updatedVelocityContext.get("from.address"));
            mail.setTo((String) updatedVelocityContext.get("to.address"));
            mail.setCc((String) updatedVelocityContext.get("to.cc"));
            mail.setBcc((String) updatedVelocityContext.get("to.bcc"));
            mail.setSubject(subject);
            mail.setTextPart(msg);
            mail.setHtmlPart(html);
            mail.setAttachments(docApi.getAttachmentList());

            try {
                sendMail(mail, context);
                return 0;
            } catch (Exception e) {
                LOGGER.error("sendEmailFromTemplate: " + templateDocFullName + " vcontext: " + updatedVelocityContext, e);
                return ERROR;
            }
        } finally {
            context.setURLFactory(originalURLFactory);
            context.setLocale(originalLocale);
        }
    }

    private String evaluate(String content, String name, VelocityContext vcontext, XWikiContext context)
    {
        StringWriter writer = new StringWriter();
        try {
            VelocityManager velocityManager = Utils.getComponent(VelocityManager.class);
            velocityManager.getVelocityEngine().evaluate(vcontext, writer, name, content);
            return writer.toString();
        } catch (Exception e) {
            LOGGER.error("Error while parsing velocity template namespace [{}]", name, e);
            Object[] args = { name };
            XWikiException xe =
                new XWikiException(XWikiException.MODULE_XWIKI_RENDERING,
                    XWikiException.ERROR_XWIKI_RENDERING_VELOCITY_EXCEPTION, "Error while parsing velocity page {0}",
                    e, args);
            return Util.getHTMLExceptionMessage(xe, context);
        }
    }

    /**
     * Uses an XWiki document to build the message subject and context, based on variables stored in a map.
     * Sends the email.
     *
     * @param templateDocFullName Full name of the template to be used (example: XWiki.MyEmailTemplate). The template
     *            needs to have an XWiki.Email object attached
     * @param from Email sender
     * @param to Email recipient
     * @param cc Email Carbon Copy
     * @param bcc Email Hidden Carbon Copy
     * @param language Language of the email
     * @param parameters variables to be passed to the velocity context
     * @return True if the email has been sent
     */
    public int sendMailFromTemplate(String templateDocFullName, String from, String to, String cc, String bcc,
        String language, Map<String, Object> parameters, XWikiContext context) throws XWikiException
    {
        VelocityContext vcontext = prepareVelocityContext(from, to, cc, bcc, parameters, context);
        return sendMailFromTemplate(templateDocFullName, from, to, cc, bcc, language, vcontext, context);
    }
}
