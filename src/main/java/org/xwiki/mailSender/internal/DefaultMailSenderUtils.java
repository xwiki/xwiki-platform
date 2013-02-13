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

import java.io.StringReader;
import java.util.Scanner;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;

import org.apache.velocity.VelocityContext;
import org.slf4j.Logger;
import org.xwiki.bridge.DocumentAccessBridge;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.mailSender.MailSenderUtils;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.rendering.parser.StreamParser;
import org.xwiki.rendering.renderer.PrintRendererFactory;
import org.xwiki.rendering.renderer.printer.DefaultWikiPrinter;
import org.xwiki.rendering.renderer.printer.WikiPrinter;
import org.xwiki.rendering.syntax.Syntax;
import org.xwiki.velocity.VelocityManager;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.api.Document;
import com.xpn.xwiki.doc.XWikiDocument;

/**
 * Mail sender utilities.
 * 
 * @version $Id$
 *
 */
@Component
@Singleton
public class DefaultMailSenderUtils implements MailSenderUtils
{
    /** Provides access to documents. Injected by the Component Manager. */
    @Inject
    private DocumentAccessBridge documentAccessBridge;
    
    /** Provides access to the logger. */
    @Inject
    private Logger logger;

    /** Provides access to the velocity context. **/
    @Inject
    private VelocityManager velocityManager;
    
    /** Enables parsing html to plain text. */
    @Inject
    @Named("html/4.01")
    private StreamParser htmlStreamParser;

    /** Component Manager. Used to access PrintRendererFactory. */
    @Inject
    private ComponentManager componentManager;
    
    /**
     * Split comma separated list of emails.
     * 
     * @param adresses comma separated list of emails
     * @return An array containing the emails
     */
    private String[] parseAddresses(String adresses)
    {
        if (adresses == null) {
            return null;
        }
        String emailList = adresses.trim();
        String[] emails = emailList.split(",");
        for (int i = 0; i < emails.length; i++) {
            emails[i] = emails[i].trim();
        }
        return emails;
    }

    /**
     * Filters a list of emails : removes illegal addresses.
     * 
     * @param email List of emails
     * @throws AddressException adress exception
     * @return An Array containing the correct adresses
     */
    public InternetAddress[] toInternetAddresses(String email) throws AddressException
    {
        String[] mails = parseAddresses(email);
        if (mails == null) {
            return null;
        }
        if (mails.length == 0) {
            return null;
        }
        InternetAddress[] address = new InternetAddress[mails.length];
        for (int i = 0; i < mails.length; i++) {
            address[i] = new InternetAddress(mails[i]);
        }
        return address;
    }
    
    /**
     * Transforms an HTML text into plain text.
     * 
     * @param html Html text to parse
     * @return the plain text corresponding to the html
     */
    public String createPlain(String html)
    {
        logger.info("Enter createPlain method");
        String converted = null;
        try {

            WikiPrinter printer = new DefaultWikiPrinter();
            PrintRendererFactory printRendererFactory =
                componentManager.getInstance(PrintRendererFactory.class, Syntax.PLAIN_1_0.toIdString());
            htmlStreamParser.parse(new StringReader(html), printRendererFactory.createRenderer(printer));
            converted = printer.toString();
        } catch (Throwable t) {
            logger.warn("Conversion from HTML to plain text threw exception", t);
            converted = null;
        }
        return converted;
    }
    
    /**
     * Retrieves the reference of a document given under the form "spaceName.docName".
     * 
     * @param docName Name of the document
     * @return the reference of the document
     */
    public DocumentReference getDocumentRef(String docName)
    {
        String wiki = documentAccessBridge.getCurrentDocumentReference().getWikiReference().getName();
        String templateSpace = "";
        String templatePage = "";
        Scanner scan = new Scanner(docName);
        scan.useDelimiter("\\.");
        if (scan.hasNext()) {
            templateSpace = scan.next();
        }
        if (scan.hasNext()) {
            templatePage = scan.next();
        } else {
            return null;
        }
        return new DocumentReference(wiki, templateSpace, templatePage);
    }

    /**
     * Verifies whether the current user is authorized view the document given as argument.
     * 
     * @param document Document where rights should be checked
     * @param context Current XWiki context
     * @return true if the user has the right to view the document
     */
    public boolean checkAccess(DocumentReference document, XWikiContext context)
    {
        XWikiDocument xdoc = new XWikiDocument(document);
        Document doc = new Document(xdoc, context);
        return doc.checkAccess("view");
    }
    
    /**
     * Generates a velocity context to use for interpreting the mail object content.
     * 
     * @param velocityContext Original context
     * @param from Mail sender
     * @param to Mail recipient
     * @param cc Mail carbon copy
     * @param bcc Mail hidden carbon copy
     * @return the velocity context to use to interpret the template content
     */
    public VelocityContext getVelocityContext(VelocityContext velocityContext, String from, String to, String cc,
        String bcc)
    {
        VelocityContext vContext = new VelocityContext();
        if (velocityContext == null) {
            vContext = velocityManager.getVelocityContext();
        } else {
            vContext = velocityContext;
        }
        vContext.put("from.name", from);
        vContext.put("from.address", from);
        vContext.put("to.name", to);
        vContext.put("to.address", to);
        vContext.put("to.cc", cc);
        vContext.put("to.bcc", bcc);
        vContext.put("bounce", from);
        return vContext;
    }
}

