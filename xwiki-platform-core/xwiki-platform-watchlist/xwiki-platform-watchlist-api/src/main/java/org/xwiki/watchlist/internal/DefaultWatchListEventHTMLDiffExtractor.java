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
package org.xwiki.watchlist.internal;

import java.util.List;

import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.apache.commons.lang3.StringUtils;
import org.apache.ecs.html.Div;
import org.apache.ecs.html.Span;
import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.watchlist.internal.api.WatchListEvent;
import org.xwiki.watchlist.internal.api.WatchListEventType;
import org.xwiki.watchlist.internal.api.WatchListException;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.AttachmentDiff;
import com.xpn.xwiki.doc.MetaDataDiff;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.ObjectDiff;
import com.xpn.xwiki.plugin.diff.DiffPluginApi;

/**
 * Default implementation for {@link WatchListEventHTMLDiffExtractor}.
 * <p>
 * TODO: Use the new diff module instead of the old diff plugin.
 * 
 * @version $Id$
 */
@Component
@Singleton
public class DefaultWatchListEventHTMLDiffExtractor implements WatchListEventHTMLDiffExtractor
{
    /**
     * Prefix used in inline style we put in HTML diffs.
     */
    private static final String HTML_STYLE_PLACEHOLDER_PREFIX = "WATCHLIST_STYLE_DIFF_";

    /**
     * Suffix used to insert images later in HTML diffs.
     */
    private static final String HTML_IMG_PLACEHOLDER_SUFFIX = "_WATCHLIST_IMG_PLACEHOLDER";

    /**
     * Prefix used to insert the metadata icon later in HTML diffs.
     */
    private static final String HTML_IMG_METADATA_PREFIX = "metadata";

    /**
     * Prefix used to insert the attachment icon later in HTML diffs.
     */
    private static final String HTML_IMG_ATTACHMENT_PREFIX = "attach";

    /**
     * Value to display in diffs for hidden properties (email, password, etc).
     */
    private static final String HIDDEN_PROPERTIES_OBFUSCATED_VALUE = "******************";

    /**
     * Name of the password class.
     */
    private static final String PASSWORD_CLASS_NAME = "Password";

    /**
     * Name of email property.
     */
    private static final String EMAIL_PROPERTY_NAME = "email";

    /**
     * Default document version on creation.
     */
    private static final String INITIAL_DOCUMENT_VERSION = "1.1";

    @Inject
    private Logger logger;

    @Inject
    private Provider<XWikiContext> contextProvider;

    @Override
    public String getHTMLDiff(WatchListEvent event) throws WatchListException
    {
        XWikiContext context = contextProvider.get();

        StringBuffer result = new StringBuffer();

        try {
            DiffPluginApi diff = (DiffPluginApi) context.getWiki().getPluginApi("diff", context);

            XWikiDocument d2 = context.getWiki().getDocument(event.getDocumentReference(), context);

            if (event.getType().equals(WatchListEventType.CREATE)) {
                d2 = context.getWiki().getDocument(d2, INITIAL_DOCUMENT_VERSION, context);
            }

            XWikiDocument d1 = context.getWiki().getDocument(d2, event.getPreviousVersion(), context);
            List<AttachmentDiff> attachDiffs = d2.getAttachmentDiff(d1, d2, context);
            List<List<ObjectDiff>> objectDiffs = d2.getObjectDiff(d1, d2, context);
            List<List<ObjectDiff>> classDiffs = d2.getClassDiff(d1, d2, context);
            List<MetaDataDiff> metaDiffs = d2.getMetaDataDiff(d1, d2, context);

            if (!d1.getContent().equals(d2.getContent())) {
                Div contentDiv = createDiffDiv("contentDiff");
                String contentDiff = diff.getDifferencesAsHTML(d1.getContent(), d2.getContent(), false);
                contentDiv.addElement(contentDiff);
                result.append(contentDiv);
            }

            for (AttachmentDiff aDiff : attachDiffs) {
                Div attachmentDiv = createDiffDiv("attachmentDiff");
                attachmentDiv.addElement(HTML_IMG_ATTACHMENT_PREFIX + HTML_IMG_PLACEHOLDER_SUFFIX);
                attachmentDiv.addElement(aDiff.toString());
                result.append(attachmentDiv);
            }

            result.append(getObjectsHTMLDiff(objectDiffs, false, event.getFullName(), diff));
            result.append(getObjectsHTMLDiff(classDiffs, true, event.getFullName(), diff));

            for (MetaDataDiff mDiff : metaDiffs) {
                Div metaDiv = createDiffDiv("metaDiff");
                metaDiv.addElement(HTML_IMG_METADATA_PREFIX + HTML_IMG_PLACEHOLDER_SUFFIX);
                metaDiv.addElement(mDiff.toString());
                result.append(metaDiv);
            }

            return result.toString();
        } catch (Exception e) {
            throw new WatchListException(String.format("Failed to compute HTML diff for event type [%s] on [%s]",
                event.getType(), event.getPrefixedFullName()), e);
        }
    }

    /**
     * @param classAttr The class of the div to create
     * @return a HTML div element
     */
    private static Div createDiffDiv(String classAttr)
    {
        Div div = new Div();
        div.setClass(classAttr);
        div.setStyle(HTML_STYLE_PLACEHOLDER_PREFIX + classAttr);

        return div;
    }

    /**
     * @param classAttr The class of the span to create
     * @return an opening span markup
     */
    private static Span createDiffSpan(String classAttr)
    {
        Span span = new Span();
        span.setClass(classAttr);
        span.setStyle(HTML_STYLE_PLACEHOLDER_PREFIX + classAttr);

        return span;
    }

    /**
     * Compute the HTML diff for a given property.
     * 
     * @param objectDiff object diff object
     * @param diff the diff plugin API
     * @return the HTML diff
     * @throws XWikiException if the diff plugin fails to compute the HTML diff
     */
    private static String getPropertyHTMLDiff(ObjectDiff objectDiff, DiffPluginApi diff) throws XWikiException
    {
        String propDiff =
            diff.getDifferencesAsHTML(objectDiff.getPrevValue().toString(), objectDiff.getNewValue().toString(), false);

        // We hide PasswordClass properties and properties named "email" from notifications for security reasons.
        if ((objectDiff.getPropType().equals(PASSWORD_CLASS_NAME) || objectDiff.getPropName().equals(
            EMAIL_PROPERTY_NAME))
            && !StringUtils.isBlank(propDiff)) {
            propDiff = HIDDEN_PROPERTIES_OBFUSCATED_VALUE;
        }

        return propDiff;
    }

    /**
     * @param objectDiffs the list of object diff
     * @param isXWikiClass true if the diff to compute is for an XWiki class, false if it's a plain XWiki object
     * @param documentFullName full name of the document the diff is computed for
     * @param diff the diff plugin API
     * @return the HTML string displaying the specified list of diffs
     */
    private String getObjectsHTMLDiff(List<List<ObjectDiff>> objectDiffs, boolean isXWikiClass,
        String documentFullName, DiffPluginApi diff)
    {
        StringBuffer result = new StringBuffer();
        String propSeparator = ": ";
        String prefix = (isXWikiClass) ? "class" : "object";

        try {
            for (List<ObjectDiff> oList : objectDiffs) {
                if (oList.isEmpty()) {
                    continue;
                }

                // The main container
                Div mainDiv = createDiffDiv(prefix + "Diff");

                // Class name
                Span objectName = createDiffSpan(prefix + "ClassName");
                if (isXWikiClass) {
                    objectName.addElement(documentFullName);
                } else {
                    objectName.addElement(oList.get(0).getClassName());
                }
                mainDiv.addElement(prefix + HTML_IMG_PLACEHOLDER_SUFFIX);
                mainDiv.addElement(objectName);

                // Diffs for each property
                for (ObjectDiff oDiff : oList) {
                    String propDiff = getPropertyHTMLDiff(oDiff, diff);
                    if (StringUtils.isBlank(oDiff.getPropName()) || StringUtils.isBlank(propDiff)) {
                        // Skip invalid properties or the ones that have no changed.
                        continue;
                    }

                    Div propDiv = createDiffDiv("propDiffContainer");

                    // Property name
                    Span propNameSpan = createDiffSpan("propName");
                    propNameSpan.addElement(oDiff.getPropName() + propSeparator);
                    String shortPropType = StringUtils.removeEnd(oDiff.getPropType(), "Class").toLowerCase();
                    if (StringUtils.isBlank(shortPropType)) {
                        // When the diff shows a property that has been deleted, its type is not available.
                        shortPropType = HTML_IMG_METADATA_PREFIX;
                    }
                    propDiv.addElement(shortPropType + HTML_IMG_PLACEHOLDER_SUFFIX);
                    propDiv.addElement(propNameSpan);

                    // Property diff
                    Div propDiffDiv = createDiffDiv("propDiff");
                    propDiffDiv.addElement(propDiff);
                    propDiv.addElement(propDiffDiv);

                    // Add it to the main div
                    mainDiv.addElement(propDiv);
                }

                result.append(mainDiv);
            }
        } catch (XWikiException e) {
            // Catch the exception to be sure we won't send emails containing stacktraces to users.
            logger.error("Failed to compute HTML objects or class diff", e);
        }

        return result.toString();
    }
}
