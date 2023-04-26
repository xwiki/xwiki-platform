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
package org.xwiki.image.style.internal;

import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.context.ExecutionContext;
import org.xwiki.context.ExecutionContextException;
import org.xwiki.context.ExecutionContextManager;
import org.xwiki.image.style.ImageStyleException;
import org.xwiki.image.style.ImageStyleManager;
import org.xwiki.image.style.model.ImageStyle;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.model.reference.LocalDocumentReference;
import org.xwiki.query.Query;
import org.xwiki.query.QueryException;
import org.xwiki.query.QueryManager;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;

import static org.apache.commons.lang3.exception.ExceptionUtils.getRootCauseMessage;

/**
 * Provide the operations to interact with the stored images style.
 *
 * @version $Id$
 * @since 14.3RC1
 */
@Component
@Singleton
public class DefaultImageStyleManager implements ImageStyleManager
{
    private static final LocalDocumentReference IMAGE_STYPE_CLASS_REFERENCE =
        new LocalDocumentReference(List.of("Image", "Style", "Code"), "ImageStyleClass");

    @Inject
    private QueryManager queryManager;

    @Inject
    private Provider<XWikiContext> xcontextProvider;

    @Inject
    private ExecutionContextManager contextManager;

    @Inject
    @Named("current")
    private DocumentReferenceResolver<String> documentReferenceResolver;

    @Inject
    private Logger logger;

    @Override
    public Set<ImageStyle> getImageStyles(String wikiName) throws ImageStyleException
    {
        try {
            this.contextManager.pushContext(new ExecutionContext(), true);
            this.xcontextProvider.get().setWikiId(wikiName);
            return this.queryManager.createQuery("select doc.fullName "
                        + "from Document doc, doc.object(Image.Style.Code.ImageStyleClass) as obj "
                        + "where doc.space = 'Image.Style.Code.ImageStyles'",
                    Query.XWQL)
                .setWiki(wikiName)
                .<String>execute()
                .stream()
                .map(this::convert)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        } catch (QueryException e) {
            throw new ImageStyleException("Failed to retrieve the list of image styles", e);
        } catch (ExecutionContextException e) {
            throw new ImageStyleException(String.format("Failed to initialize a context for wiki [%s]", wikiName), e);
        } finally {
            this.contextManager.popContext();
        }
    }

    private ImageStyle convert(String documentReference)
    {
        try {
            XWikiContext context = this.xcontextProvider.get();
            DocumentReference resolve = this.documentReferenceResolver.resolve(documentReference);
            XWikiDocument document = context.getWiki().getDocument(resolve, context);
            BaseObject xObject = document.getXObject(IMAGE_STYPE_CLASS_REFERENCE);
            return new ImageStyle()
                .setIdentifier(document.getDocumentReference().getName())
                .setPrettyName(xObject.getStringValue("prettyName"))
                .setType(xObject.getStringValue("type"))
                .setAdjustableSize(getBoolean(xObject, "adjustableSize"))
                .setDefaultWidth(getLongIfNotEmpty(xObject, "defaultWidth"))
                .setDefaultHeight(getLongIfNotEmpty(xObject, "defaultHeight"))
                .setAdjustableBorder(getBoolean(xObject, "adjustableBorder"))
                .setDefaultBorder(getBoolean(xObject, "defaultBorder"))
                .setAdjustableAlignment(getBoolean(xObject, "adjustableAlignment"))
                .setDefaultAlignment(xObject.getStringValue("defaultAlignment"))
                .setAdjustableTextWrap(getBoolean(xObject, "adjustableTextWrap"))
                .setDefaultTextWrap(getBoolean(xObject, "defaultTextWrap"));
        } catch (XWikiException e) {
            this.logger.warn("Failed to resolve document reference [{}]. Cause: [{}].", documentReference,
                getRootCauseMessage(e));
            return null;
        }
    }

    /**
     * Return {@code true} of the property is number value is {@code 1}, {@code false} otherwise.
     *
     * @param xObject an XObject
     * @param propertyName the property name in the XObject
     * @return {@code true} of the property is number value is {@code 1}, {@code false} otherwise.
     */
    private boolean getBoolean(BaseObject xObject, String propertyName)
    {
        return 1 == xObject.getLongValue(propertyName);
    }

    /**
     * Return the long value of the property only if it is explicitly defined (i.e., its string representation is not
     * the empty string).
     *
     * @param xObject an XObject
     * @param propertyName the property name in the XObject
     * @return {@code null} if the string representation of the property is the empty string, the long value of the
     *     property otherwise
     */
    private Long getLongIfNotEmpty(BaseObject xObject, String propertyName)
    {
        if (Objects.equals(xObject.getStringValue(propertyName), "")) {
            return null;
        }
        return xObject.getLongValue(propertyName);
    }
}
