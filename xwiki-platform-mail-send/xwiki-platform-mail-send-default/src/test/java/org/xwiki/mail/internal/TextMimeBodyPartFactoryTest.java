package org.xwiki.mail.internal;

import java.util.HashMap;
import java.util.Map;

import javax.mail.internet.MimeBodyPart;

import org.junit.Test;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

public class TextMimeBodyPartFactoryTest
{
    @Test
    public void createTextBodyPart() throws Exception
    {
        // Step 1: Create a TextMimeBodyPartFactory object
        TextMimeBodyPartFactory textPartFactory = new TextMimeBodyPartFactory();

        // Step 2: Add parameters Map
        Map<String, Object> parameters = new HashMap<>();

        // Step 3: Create the text body part
        MimeBodyPart textPart = textPartFactory.create("Lorem ipsum", parameters);

        assertEquals("Lorem ipsum", textPart.getContent());
        assertEquals("text/plain", textPart.getContentType());
    }

    @Test
    public void createTextBodyPartWithHeaders() throws Exception
    {
        // Step 1: Create a TextMimeBodyPartFactory object
        TextMimeBodyPartFactory textPartFactory = new TextMimeBodyPartFactory();

        // Step 2: Add parameters Map
        Map<String, String> headers = new HashMap<>();
        headers.put("Content-Transfer-Encoding", "quoted-printable");

        Map<String, Object> parameters = new HashMap<>();
        parameters.put("headers", headers);

        // Step 3: Create the text body part
        MimeBodyPart textPart = textPartFactory.create("Lorem ipsum", parameters);

        assertEquals("Lorem ipsum", textPart.getContent());
        assertEquals("text/plain", textPart.getContentType());
        String[] headersList = new String[1];
        headersList[0] = "quoted-printable";
        assertArrayEquals(headersList, textPart.getHeader("Content-Transfer-Encoding"));
    }
}