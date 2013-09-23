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
package org.xwiki.annotation.io;

import java.util.Collection;

import org.xwiki.annotation.Annotation;
import org.xwiki.component.annotation.Role;

/**
 * This component provides services related to annotations storage and retrieval. It operates with string serialized
 * references for the targets of annotations. This interface does not restrict the implementation of the annotation
 * targets, they can be anything referencable through a string.
 * 
 * @version $Id$
 * @since 2.3M1
 */
@Role
public interface IOService
{
    /**
     * Returns all the annotations on the passed content.
     * 
     * @param target the string serialized reference to the content for which to get the annotations
     * @return all annotations which target the specified content
     * @throws IOServiceException if any exception occurs while manipulating annotations store
     */
    Collection<Annotation> getAnnotations(String target) throws IOServiceException;

    /**
     * @param target the string serialized reference to the content for which the annotation is added
     * @param annotationID the identifier of the annotation
     * @return the annotation with the given id on the passed target
     * @throws IOServiceException if any exception occurs while manipulating annotations store
     */
    Annotation getAnnotation(String target, String annotationID) throws IOServiceException;

    /**
     * Adds annotation on the specified target.
     * 
     * @param target serialized reference of the target of the annotation
     * @param annotation annotation to add on the target
     * @throws IOServiceException can be thrown if any exception occurs while manipulating annotations store
     */
    void addAnnotation(String target, Annotation annotation) throws IOServiceException;

    /**
     * Removes an annotation given by its identifier, which should be unique among all annotations on the same target.
     * 
     * @param target serialized reference of the target of the annotation
     * @param annotationID annotation identifier
     * @throws IOServiceException can be thrown if any exception occurs while manipulating annotations store
     */
    void removeAnnotation(String target, String annotationID) throws IOServiceException;

    /**
     * Updates the set of annotations in the annotations store. They will be identified by their identifiers as returned
     * by {@link Annotation#getId()}, and updated each to match the fields in the Annotation objects.
     * 
     * @param target serialized reference of the target of the annotation
     * @param annotations collection of annotations to update
     * @throws IOServiceException can be thrown if any exception occurs while manipulating annotations store
     */
    void updateAnnotations(String target, Collection<Annotation> annotations) throws IOServiceException;
}
