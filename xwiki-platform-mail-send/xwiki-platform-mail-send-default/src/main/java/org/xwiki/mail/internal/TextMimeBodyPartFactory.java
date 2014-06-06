package org.xwiki.mail.internal;

import java.nio.charset.StandardCharsets;
import java.util.Map;

import javax.inject.Named;
import javax.mail.MessagingException;
import javax.mail.internet.MimeBodyPart;

import org.xwiki.mail.MimeBodyPartFactory;

/**
 * Creates text message body Part to be added to a Multi Part message.
 *
 * @version $Id$
 * @since 6.1M2
 */
@Named("text")
public class TextMimeBodyPartFactory implements MimeBodyPartFactory<String>
{
    @Override public MimeBodyPart create(String content, Map<String, Object> parameters)
    {
        // Check if existing headers
        Boolean hasHeaders = parameters.containsKey("headers");

        // Create the text part of the email
        MimeBodyPart textPart = new MimeBodyPart();
        try {
            textPart.setContent(content, "text/plain; charset=" + StandardCharsets.UTF_8.name());
            if (hasHeaders && parameters.get("headers") instanceof Map) {
                Map<String, String> headers = (Map<String, String>) parameters.get("headers");
                for (Map.Entry<String, String> header : headers.entrySet()) {
                    textPart.setHeader(header.getKey(), header.getValue());
                }
            }
        } catch (MessagingException e) {
            e.printStackTrace();
        }
        return textPart;
    }
}