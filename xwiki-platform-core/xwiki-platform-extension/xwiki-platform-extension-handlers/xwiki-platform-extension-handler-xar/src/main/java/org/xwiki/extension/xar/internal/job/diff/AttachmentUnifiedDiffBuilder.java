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
package org.xwiki.extension.xar.internal.job.diff;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import javax.inject.Singleton;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.xwiki.component.annotation.Component;
import org.xwiki.diff.display.UnifiedDiffBlock;
import org.xwiki.extension.xar.job.diff.DocumentUnifiedDiff;
import org.xwiki.extension.xar.job.diff.EntityUnifiedDiff;
import org.xwiki.model.reference.AttachmentReference;
import org.xwiki.model.reference.DocumentVersionReference;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.doc.XWikiAttachment;

/**
 * Computes the differences, in unified format, between two versions of an attachment.
 * 
 * @version $Id$
 * @since 7.0RC1
 */
@Component(roles = AttachmentUnifiedDiffBuilder.class)
@Singleton
public class AttachmentUnifiedDiffBuilder extends AbstractUnifiedDiffBuilder
{
    /**
     * The list of media type patterns that match text files.
     */
    private static final List<String> TEXT_MEDIA_TYPE_PATTERNS = Arrays.asList("text/", "application/xml",
        "application/javascript", "application/ecmascript", "application/json", "application/x-sh", "+xml");

    /**
     * Show content differences only for text files under 50KB.
     */
    private static final long TEXT_FILE_SIZE_LIMIT = 50000;

    /**
     * Computes the differences, in unified format, between two versions of an attachment and adds the result to the
     * given {@link DocumentUnifiedDiff}.
     * 
     * @param previousAttachment the previous version of the attachment
     * @param nextAttachment the next version of the attachment
     * @param documentDiff where to add the differences
     */
    public void addAttachmentDiff(XWikiAttachment previousAttachment, XWikiAttachment nextAttachment,
        DocumentUnifiedDiff documentDiff)
    {
        AttachmentReference previousReference =
            getAttachmentVersionReference(previousAttachment, documentDiff.getPreviousReference());
        AttachmentReference nextReference =
            getAttachmentVersionReference(nextAttachment, documentDiff.getNextReference());
        EntityUnifiedDiff<AttachmentReference> attachmentDiff =
            new EntityUnifiedDiff<>(previousReference, nextReference);

        if ((previousAttachment == null || isSmallTextFile(previousAttachment))
            && (nextAttachment == null || isSmallTextFile(nextAttachment))) {
            // Compute content differences.
            maybeAddDiff(attachmentDiff, CONTENT, getContentAsString(previousAttachment),
                getContentAsString(nextAttachment));
        } else {
            // The size difference is useful when the content difference is not shown.
            maybeAddDiff(attachmentDiff, "size", previousAttachment == null ? null : previousAttachment.getLongSize(),
                nextAttachment == null ? null : nextAttachment.getLongSize());
            if (attachmentDiff.isEmpty() && !contentEquals(previousAttachment, nextAttachment)) {
                // If the files have the same size but the content is different then show an empty list.
                attachmentDiff.put(CONTENT, Collections.<UnifiedDiffBlock<String, Character>>emptyList());
            }
        }

        if (attachmentDiff.size() > 0) {
            documentDiff.getAttachmentDiffs().add(attachmentDiff);
        }
    }

    private AttachmentReference getAttachmentVersionReference(XWikiAttachment attachment,
        DocumentVersionReference documentVersionReference)
    {
        return attachment == null ? null : new AttachmentReference(attachment.getFilename(), documentVersionReference);
    }

    private boolean isSmallTextFile(XWikiAttachment attachment)
    {
        if (attachment != null && attachment.getLongSize() < TEXT_FILE_SIZE_LIMIT) {
            String mediaType = attachment.getMimeType(xcontextProvider.get());
            for (String mediaTypePattern : TEXT_MEDIA_TYPE_PATTERNS) {
                if (mediaType.equals(mediaTypePattern) || mediaType.startsWith(mediaTypePattern)
                    || mediaType.endsWith(mediaTypePattern)) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean contentEquals(XWikiAttachment previousAttachment, XWikiAttachment nextAttachment)
    {
        if (previousAttachment == null || nextAttachment == null) {
            return previousAttachment == nextAttachment;
        } else {
            XWikiContext xcontext = this.xcontextProvider.get();
            InputStream previousContent = null;
            InputStream nextContent = null;
            try {
                previousContent = previousAttachment.getContentInputStream(xcontext);
                nextContent = nextAttachment.getContentInputStream(xcontext);
                return IOUtils.contentEquals(previousContent, nextContent);
            } catch (Exception e) {
                this.logger.warn("Failed to compare the content of attachment [{}]. Root cause: {}",
                    previousAttachment.getFilename(), ExceptionUtils.getRootCauseMessage(e));
                return false;
            } finally {
                try {
                    if (previousContent != null) {
                        previousContent.close();
                    }
                    if (nextContent != null) {
                        nextContent.close();
                    }
                } catch (IOException e) {
                    // Ignore.
                }
            }
        }
    }

    private String getContentAsString(XWikiAttachment attachment)
    {
        try {
            return attachment == null ? null : IOUtils.toString(attachment.getContentInputStream(this.xcontextProvider
                .get()));
        } catch (Exception e) {
            this.logger.warn("Failed to read the content of attachment [{}]. Root cause: {}", attachment.getFilename(),
                ExceptionUtils.getRootCauseMessage(e));
            return null;
        }
    }
}
