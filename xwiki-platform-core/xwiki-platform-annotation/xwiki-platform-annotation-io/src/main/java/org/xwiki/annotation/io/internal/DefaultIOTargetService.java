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
package org.xwiki.annotation.io.internal;

import java.io.StringReader;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.xwiki.annotation.io.IOServiceException;
import org.xwiki.annotation.io.IOTargetService;
import org.xwiki.annotation.reference.TypedStringEntityReferenceResolver;
import org.xwiki.bridge.DocumentAccessBridge;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.display.internal.DocumentDisplayer;
import org.xwiki.display.internal.DocumentDisplayerParameters;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.ObjectPropertyReference;
import org.xwiki.rendering.block.XDOM;
import org.xwiki.rendering.parser.ParseException;
import org.xwiki.rendering.parser.Parser;
import org.xwiki.rendering.syntax.Syntax;
import org.xwiki.rendering.transformation.RenderingContext;
import org.xwiki.rendering.transformation.TransformationContext;
import org.xwiki.rendering.transformation.TransformationException;
import org.xwiki.rendering.transformation.TransformationManager;

import com.xpn.xwiki.objects.BaseObjectReference;

/**
 * Default {@link IOTargetService} implementation, based on resolving XWiki documents and object properties as
 * annotations targets. The references manipulated by this implementation are XWiki references, such as xwiki:Space.Page
 * for documents or with an object and property reference if the target is an object property. Use the reference module
 * to generate the references passed to this module, so that they can be resolved to XWiki content back by this
 * implementation.
 *
 * @version $Id$
 * @since 2.3M1
 */
@Component
@Singleton
public class DefaultIOTargetService implements IOTargetService
{
    /**
     * Component manager used to lookup the parsers.
     */
    @Inject
    private ComponentManager componentManager;

    /**
     * Document access bridge to manipulate xwiki documents.
     */
    @Inject
    private DocumentAccessBridge dab;

    /**
     * Document displayer.
     */
    @Inject
    @Named("configured")
    private DocumentDisplayer documentDisplayer;

    /**
     * Entity reference handler to resolve the reference.
     */
    @Inject
    private TypedStringEntityReferenceResolver referenceResolver;

    @Inject
    private RenderingContext renderingContext;

    @Override
    public String getSource(String reference) throws IOServiceException
    {
        try {
            EntityReference ref = this.referenceResolver.resolve(reference, EntityType.DOCUMENT);
            if (ref.getType() == EntityType.OBJECT_PROPERTY) {
                return getObjectPropertyContent(new ObjectPropertyReference(ref));
            } else if (ref.getType() == EntityType.DOCUMENT) {
                return this.dab.getTranslatedDocumentInstance(new DocumentReference(ref)).getContent();
            } else {
                // it was parsed as something else, just ignore the parsing and get the document content as its initial
                // name was
                return this.dab.getDocumentContent(reference);
            }
        } catch (Exception e) {
            throw new IOServiceException("An exception has occurred while getting the source for " + reference, e);
        }
    }

    @Override
    public String getSourceSyntax(String reference) throws IOServiceException
    {
        try {
            EntityReference ref = this.referenceResolver.resolve(reference, EntityType.DOCUMENT);
            EntityReference docRef = ref.extractReference(EntityType.DOCUMENT);
            if (docRef != null) {
                // return the syntax of the document in this reference, regardless of the type of reference, obj prop or
                // doc
                return this.dab.getTranslatedDocumentInstance(new DocumentReference(docRef)).getSyntax().toIdString();
            } else {
                return this.dab.getDocumentSyntaxId(reference);
            }
        } catch (Exception e) {
            throw new IOServiceException(
                "An exception has occurred while getting the syntax of the source for " + reference, e);
        }
    }

    @Override
    public XDOM getXDOM(String reference) throws IOServiceException
    {
        return getXDOM(reference, null);
    }

    @Override
    public XDOM getXDOM(String reference, String syntax) throws IOServiceException
    {
        String sourceSyntaxId = syntax;
        // get if unspecified, get the source from the io service
        if (sourceSyntaxId == null) {
            sourceSyntaxId = getSourceSyntax(reference);
        }
        try {
            EntityReference ref = this.referenceResolver.resolve(reference, EntityType.DOCUMENT);
            if (ref.getType() == EntityType.OBJECT_PROPERTY) {
                return getTransformedXDOM(getObjectPropertyContent(new ObjectPropertyReference(ref)), sourceSyntaxId);
            } else if (ref.getType() == EntityType.DOCUMENT) {
                return getDocumentXDOM(new DocumentReference(ref));
            } else {
                // it was parsed as something else, just ignore the parsing and get the document content as its initial
                // name was
                return getTransformedXDOM(this.dab.getDocumentContent(reference), sourceSyntaxId);
            }
        } catch (Exception e) {
            throw new IOServiceException("An exception has occurred while getting the XDOM for " + reference, e);
        }
    }

    private XDOM getDocumentXDOM(DocumentReference reference) throws Exception
    {
        DocumentDisplayerParameters parameters = new DocumentDisplayerParameters();
        parameters.setExecutionContextIsolated(true);
        parameters.setContentTranslated(true);
        parameters.setTargetSyntax(this.renderingContext.getTargetSyntax());

        return this.documentDisplayer.display(this.dab.getDocumentInstance(reference), parameters);
    }

    private XDOM getTransformedXDOM(String content, String sourceSyntaxId)
        throws ParseException, org.xwiki.component.manager.ComponentLookupException, TransformationException
    {
        Parser parser = this.componentManager.getInstance(Parser.class, sourceSyntaxId);
        XDOM xdom = parser.parse(new StringReader(content));

        // run transformations
        TransformationContext txContext =
            new TransformationContext(xdom, Syntax.valueOf(sourceSyntaxId));
        TransformationManager transformationManager = this.componentManager.getInstance(TransformationManager.class);
        transformationManager.performTransformations(xdom, txContext);

        return xdom;
    }

    private String getObjectPropertyContent(ObjectPropertyReference reference)
    {
        BaseObjectReference objRef = new BaseObjectReference(reference.getParent());
        DocumentReference docRef = new DocumentReference(objRef.getParent());
        if (objRef.getObjectNumber() != null) {
            return this.dab.getProperty(docRef, objRef.getXClassReference(), objRef.getObjectNumber(),
                    reference.getName())
                .toString();
        } else {
            return this.dab.getProperty(docRef, objRef.getXClassReference(), reference.getName()).toString();
        }
    }
}
