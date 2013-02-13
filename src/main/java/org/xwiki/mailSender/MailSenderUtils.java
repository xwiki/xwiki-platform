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
package org.xwiki.mailSender;

import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;

import org.apache.velocity.VelocityContext;
import org.xwiki.component.annotation.ComponentRole;
import org.xwiki.model.reference.DocumentReference;

import com.xpn.xwiki.XWikiContext;

/**
 * Interface (aka Role) of the Component.
 * 
 * @version $Id$
 */
@ComponentRole
public interface MailSenderUtils
{
    /**
     * Filters a list of emails : removes illegal addresses.
     * 
     * @param email List of emails
     * @throws AddressException adress exception
     * @return An Array containing the correct adresses
     */
    InternetAddress[] toInternetAddresses(String email) throws AddressException;
    
    /**
     * Transforms an HTML text into plain text.
     * 
     * @param html Html text to parse
     * @return the plain text corresponding to the html
     */
    String createPlain(String html);
    
    /**
     * Retrieves the reference of a document given under the form "spaceName.docName".
     * 
     * @param docName Name of the document
     * @return the reference of the document
     */
    DocumentReference getDocumentRef(String docName);

    /**
     * Verifies whether the current user is authorized view the document given as argument.
     * 
     * @param document Document where rights should be checked
     * @param context Current XWiki context
     * @return true if the user has the right to view the document
     */
    boolean checkAccess(DocumentReference document, XWikiContext context);
    
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
    VelocityContext getVelocityContext(VelocityContext velocityContext, String from, String to, String cc,
        String bcc);

}
