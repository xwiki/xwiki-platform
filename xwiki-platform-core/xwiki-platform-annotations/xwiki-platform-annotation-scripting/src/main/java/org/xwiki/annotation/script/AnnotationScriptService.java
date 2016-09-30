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
package org.xwiki.annotation.script;

import java.util.Collection;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.xwiki.annotation.Annotation;
import org.xwiki.annotation.AnnotationService;
import org.xwiki.annotation.AnnotationServiceException;
import org.xwiki.annotation.rights.AnnotationRightService;
import org.xwiki.component.annotation.Component;
import org.xwiki.context.Execution;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.script.service.ScriptService;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;

/**
 * Wrapper for the annotation service functions to be exposed to scripting contexts.
 * 
 * @version $Id$
 * @since 3.0RC1
 */
@Component
@Named("annotations")
@Singleton
public class AnnotationScriptService implements ScriptService
{
    /**
     * The annotation service to execute annotation functions.
     */
    @Inject
    private AnnotationService annotationService;

    /**
     * The annotations rights service.
     */
    @Inject
    private AnnotationRightService rightsService;

    /**
     * The execution to get the context.
     */
    @Inject
    private Execution execution;

    /**
     * Entity reference serializer, to create references to the documents to which annotation targets refer.
     */
    @Inject
    private EntityReferenceSerializer<String> serializer;

    /**
     * Adds an the specified annotation for the specified target.
     * 
     * @param target serialized reference of the target of the annotation
     * @param selection HTML selection concerned by annotations
     * @param selectionContext HTML selection context
     * @param offset offset of the selection in context
     * @param author the author of the annotation
     * @param metadata annotation metadata, as key, value pairs
     * @return {@code true} if the adding succeeds, {@code false} if an exception occurs and the exception is saved on
     *         the xwiki context
     */
    public boolean addAnnotation(String target, String selection, String selectionContext, int offset, String author,
        Map<String, Object> metadata)
    {
        if (!this.rightsService.canAddAnnotation(target, getCurrentUser())) {
            setAccessExceptionOnContext();
            return false;
        }
        try {
            this.annotationService.addAnnotation(target, selection, selectionContext, offset, author, metadata);
            return true;
        } catch (AnnotationServiceException e) {
            setExceptionOnContext(e);
            return false;
        }
    }

    /**
     * Returns the XHTML of the requested source, along with annotations inserted as {@code span} elements inside it.
     * It's a particular case of {@link #getAnnotatedRenderedContent(String, String, String, Collection)} for
     * unspecified input syntax, {@code xhtml/1.0} output syntax and the list of annotations returned by
     * {@link #getValidAnnotations(String)} for this source reference.
     * 
     * @param sourceReference reference to the source to be rendered in XHTML with annotations
     * @return rendered and annotated document or {@code null} if an exception occurs and the exception is saved on the
     *         xwiki context
     * @see #getAnnotatedRenderedContent(String, String, String, Collection)
     * @see AnnotationService#getAnnotatedHTML(String)
     */
    public String getAnnotatedHTML(String sourceReference)
    {
        if (!this.rightsService.canViewAnnotatedTarget(sourceReference, getCurrentUser())) {
            setAccessExceptionOnContext();
            return null;
        }
        try {
            return this.annotationService.getAnnotatedHTML(sourceReference);
        } catch (AnnotationServiceException e) {
            setExceptionOnContext(e);
            return null;
        }
    }

    /**
     * Returns result obtained by rendering with annotations markers the source referenced by the
     * {@code sourceReference} parsed in {@code sourceSyntax}. The list of annotations to be added markers for is passed
     * in the {@code annotations} parameter. Note that no test is done on the actual target of the annotations in the
     * passed list, they will all be rendered, as long as their selected text and context can be identified in the
     * content.
     * 
     * @param sourceReference the reference to the source to be rendered in XHTML with annotations
     * @param sourceSyntax the syntax to parse the source in. If this parameter is null, the default source syntax will
     *            be used, as returned by the target IO service.
     * @param outputSyntax the syntax to render in (e.g. "xhtml/1.0")
     * @param annotations the annotations to render on the content referred by the {@code sourceReference}. Can be the
     *            whole set of annotations on that source or a subset, filtered by various criteria
     * @return the annotated rendered source, or @code null} if an exception occurs and the exception is saved on the
     *         xwiki context
     * @see AnnotationService#getAnnotatedRenderedContent(String, String, String, Collection)
     */
    public String getAnnotatedRenderedContent(String sourceReference, String sourceSyntax, String outputSyntax,
        Collection<Annotation> annotations)
    {
        if (!this.rightsService.canViewAnnotatedTarget(sourceReference, getCurrentUser())) {
            setAccessExceptionOnContext();
            return null;
        }
        try {
            return this.annotationService.getAnnotatedRenderedContent(sourceReference, sourceSyntax, outputSyntax,
                annotations);
        } catch (AnnotationServiceException e) {
            setExceptionOnContext(e);
            return null;
        }
    }

    /**
     * Returns the annotation identified by {@code id} on the specified target.
     * 
     * @param target the serialized reference to the content on which the annotation is added
     * @param id the identifier of the annotation
     * @return the annotation identified by {@code id}, or {@code null} if an exception occurs and the exception is
     *         saved on the xwiki context
     * @see AnnotationService#getAnnotation(String, String)
     */
    public Annotation getAnnotation(String target, String id)
    {
        if (!this.rightsService.canViewAnnotations(target, getCurrentUser())) {
            setAccessExceptionOnContext();
            return null;
        }
        try {
            return this.annotationService.getAnnotation(target, id);
        } catch (AnnotationServiceException e) {
            setExceptionOnContext(e);
            return null;
        }
    }

    /**
     * Returns the annotation identified by {@code id} on the specified target.
     * 
     * @param reference the reference to the content on which the annotation is added
     * @param id the identifier of the annotation
     * @return the annotation identified by {@code id}, or {@code null} if an exception occurs and the exception is
     *         saved on the xwiki context
     * @see AnnotationService#getAnnotation(String, String)
     */
    public Annotation getAnnotation(EntityReference reference, String id)
    {
        String serializedRef = this.serializer.serialize(reference);

        return getAnnotation(serializedRef, id);
    }

    /**
     * Returns all the annotations on the passed content.
     * 
     * @param target the string serialized reference to the content for which to get the annotations
     * @return all annotations which target the specified content, or {@code null} if an exception occurs and the
     *         exception is saved on the xwiki context
     * @see AnnotationService#getAnnotations(String)
     */
    public Collection<Annotation> getAnnotations(String target)
    {
        if (!this.rightsService.canViewAnnotations(target, getCurrentUser())) {
            setAccessExceptionOnContext();
            return null;
        }
        try {
            return this.annotationService.getAnnotations(target);
        } catch (AnnotationServiceException e) {
            setExceptionOnContext(e);
            return null;
        }
    }

    /**
     * Shortcut function to get all annotations which are valid on the specified target, regardless of the updates the
     * document and its annotations suffered from creation ('safe' or 'updated' state).
     * 
     * @param target the string serialized reference to the content for which to get the annotations
     * @return all annotations which are valid on the specified content, or {@code null} if an exception occurs and the
     *         exception is saved on the xwiki context
     * @see org.xwiki.annotation.maintainer.AnnotationState
     * @see AnnotationService#getValidAnnotations(String)
     */
    public Collection<Annotation> getValidAnnotations(String target)
    {
        if (!this.rightsService.canViewAnnotations(target, getCurrentUser())) {
            setAccessExceptionOnContext();
            return null;
        }
        try {
            return this.annotationService.getValidAnnotations(target);
        } catch (AnnotationServiceException e) {
            setExceptionOnContext(e);
            return null;
        }
    }

    /**
     * Remove an annotation given by its identifier, which should be unique among all annotations on the same target.
     * 
     * @param target the string serialized reference to the content on which the annotation is added
     * @param annotationID annotation identifier
     * @return {@code true} if removing succeeds, {@code false} if an exception occurs and the exception is saved on the
     *         xwiki context
     * @see AnnotationService#removeAnnotation(String, String)
     */
    public boolean removeAnnotation(String target, String annotationID)
    {
        if (!this.rightsService.canEditAnnotation(annotationID, target, getCurrentUser())) {
            setAccessExceptionOnContext();
            return false;
        }
        try {
            this.annotationService.removeAnnotation(target, annotationID);
            return true;
        } catch (AnnotationServiceException e) {
            setExceptionOnContext(e);
            return false;
        }
    }

    /**
     * Updates the passed annotation with the new values. Matching of the annotation is done by the annotation id field,
     * among all annotations on the same target.
     * 
     * @param target the string serialized reference to the content on which the annotation is added
     * @param annotation the new description of the annotation to update, with a valid id
     * @return {@code true} if update succeeds, {@code false} if an exception occurs and the exception is saved on the
     *         xwiki context
     * @see AnnotationService#updateAnnotation(String, Annotation)
     */
    public boolean updateAnnotation(String target, Annotation annotation)
    {
        if (!this.rightsService.canEditAnnotation(annotation.getId(), target, getCurrentUser())) {
            setAccessExceptionOnContext();
            return false;
        }
        try {
            this.annotationService.updateAnnotation(target, annotation);
            return true;
        } catch (AnnotationServiceException e) {
            setExceptionOnContext(e);
            return false;
        }
    }

    /**
     * Checks if the current user can edit an annotation on the document given by wiki, space and page. This function is
     * a helper function, using wiki, space and page instead of the target to avoid target generation in velocity
     * scripting.<br>
     * TODO: annotations should only operate on targets, and velocity context should only operate with targets so that
     * they can pass a target to this function. This also assumes refactoring of the REST service to always get target
     * references instead of wiki, space and pages in the path.
     * 
     * @param annotationId the id of the annotation to edit
     * @param wiki the wiki of the document where the annotation is added
     * @param space the space of the document where the annotation is added
     * @param page the document page
     * @return {@code true} if the current user can edit the annotation identified by the id on the specified document,
     *         {@code false} otherwise
     * @deprecated since 7.2M3, use {@link #canEditAnnotation(String, DocumentReference)} instead
     */
    @Deprecated
    public boolean canEditAnnotation(String annotationId, String wiki, String space, String page)
    {
        return canEditAnnotation(annotationId, new DocumentReference(wiki, space, page));
    }

    /**
     * Checks if the current user can edit an annotation on the document given by wiki, space and page.
     * 
     * @param annotationId the id of the annotation to edit
     * @param reference the reference of the document where the annotation is added
     * @return {@code true} if the current user can edit the annotation identified by the id on the specified document,
     *         {@code false} otherwise
     * @since 7.2M3
     */
    public boolean canEditAnnotation(String annotationId, DocumentReference reference)
    {
        String target = this.serializer.serialize(reference);

        return this.rightsService.canEditAnnotation(annotationId, target, getCurrentUser());
    }

    /**
     * Checks if the current user can add an annotation on the document given by the wiki, space and page. This function
     * is a helper function, using wiki, space and page instead of the target to avoid target generation in velocity
     * scripting.<br>
     * 
     * @param wiki the wiki of the document where the annotation is added
     * @param space the space of the document where the annotation is added
     * @param page the document page
     * @return {@code true} if the current user can add an annotation on the specified document, {@code false} otherwise
     * @see #canEditAnnotation(String, String, String, String)
     * @deprecated since 7.2M3, use {@link #canAddAnnotation(DocumentReference)} instead
     */
    @Deprecated
    public boolean canAddAnnotation(String wiki, String space, String page)
    {
        return canAddAnnotation(new DocumentReference(wiki, space, page));
    }

    /**
     * Checks if the current user can add an annotation on the document given by the reference.
     * 
     * @param reference the reference of the document where the annotation is added
     * @return {@code true} if the current user can add an annotation on the specified document, {@code false} otherwise
     * @see #canEditAnnotation(String, String, String, String)
     * @since 7.2M3
     */
    public boolean canAddAnnotation(DocumentReference reference)
    {
        String target = this.serializer.serialize(reference);

        return this.rightsService.canAddAnnotation(target, getCurrentUser());
    }

    /**
     * Helper function to get the currently logged in user.
     * 
     * @return the currently logged in user
     */
    private String getCurrentUser()
    {
        return getXWikiContext().getUser();
    }

    /**
     * Helper function to get the XWiki Context.
     * 
     * @return the xwiki context
     */
    private XWikiContext getXWikiContext()
    {
        return (XWikiContext) this.execution.getContext().getProperty("xwikicontext");
    }

    /**
     * Helper function to set an exception on the xwiki context, as the last exception.
     * 
     * @param exception the exception to set on the context
     */
    private void setExceptionOnContext(Exception exception)
    {
        getXWikiContext().put("lastexception", exception);
    }

    /**
     * Helper function to set an access exception on the xwiki context, as the last exception.
     */
    private void setAccessExceptionOnContext()
    {
        setExceptionOnContext(new XWikiException(XWikiException.MODULE_XWIKI_ACCESS,
            XWikiException.ERROR_XWIKI_ACCESS_DENIED, "You are not allowed to perform this action"));
    }
}
