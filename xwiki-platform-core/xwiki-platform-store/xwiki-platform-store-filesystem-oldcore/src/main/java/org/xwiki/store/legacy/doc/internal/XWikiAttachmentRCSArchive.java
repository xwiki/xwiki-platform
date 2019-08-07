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
package org.xwiki.store.legacy.doc.internal;

import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

import org.suigeneris.jrcs.diff.DiffException;
import org.suigeneris.jrcs.rcs.Archive;
import org.suigeneris.jrcs.rcs.InvalidFileFormatException;
import org.suigeneris.jrcs.rcs.Version;
import org.suigeneris.jrcs.util.ToString;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiAttachment;

/**
 * Extends JRCS's {@link Archive} class in order to control the data ot the TrunkNode that are created. By default
 * they are created with the current date/time but we want them to have the date of the attachment so that we can
 * build a JRCS-based string representation of a list of attachments (used to serialize attachment history into
 * filesystem files or into XAR XML format).
 *
 * @version $Id$
 * @since 11.7RC1
 */
public class XWikiAttachmentRCSArchive extends Archive
{
    /**
     * @param revisionAttachment the attachment object for a given version of the attachment. Its date will be used to
     *        overwrite the JRCS's TrunkNode date
     * @param context the xwiki context, used to get the XML representation of the attachment for that revision
     * @throws XWikiException in case of failure to get the XML data
     */
    public XWikiAttachmentRCSArchive(XWikiAttachment revisionAttachment, XWikiContext context) throws
        XWikiException
    {
        super(ToString.stringToArray(revisionAttachment.toStringXML(true, false, context)),
            revisionAttachment.getFilename(), revisionAttachment.getVersion());

        // Change the date of the newly created TrunkNode to be the date from the versioned attachment.
        setTrunkNodeDate(revisionAttachment.getDate());
    }

    /**
     * @param revisionAttachment the attachment object for a given version of the attachment. Its date will be used to
     *        overwrite the JRCS's TrunkNode date
     * @param context the xwiki context, used to get the XML representation of the attachment for that revision
     * @return the added version
     * @throws XWikiException in case of failure to get the XML data
     * @throws InvalidFileFormatException if an error occurs when adding a revision in JRCS
     * @throws DiffException if an error occurs when adding a revision in JRCS
     */
    public Version addRevision(XWikiAttachment revisionAttachment, XWikiContext context) throws XWikiException,
        InvalidFileFormatException, DiffException
    {
        Version version = super.addRevision(
            ToString.stringToArray(revisionAttachment.toStringXML(true, false, context)), "");
        setTrunkNodeDate(revisionAttachment.getDate());
        return version;
    }

    private void setTrunkNodeDate(Date newDate)
    {
        Calendar calendar = new GregorianCalendar();
        calendar.setTime(newDate);
        this.head.setDate(new int[]{
            calendar.get(Calendar.YEAR) - 1900,
            calendar.get(Calendar.MONTH) + 1,
            calendar.get(Calendar.DAY_OF_MONTH),
            calendar.get(Calendar.HOUR_OF_DAY),
            calendar.get(Calendar.MINUTE),
            calendar.get(Calendar.SECOND)
        });
    }
}
