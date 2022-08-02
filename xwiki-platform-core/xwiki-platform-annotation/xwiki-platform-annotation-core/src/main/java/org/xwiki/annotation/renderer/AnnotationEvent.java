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
package org.xwiki.annotation.renderer;

import org.xwiki.annotation.Annotation;

/**
 * Class to hold information about an annotation event, namely its type (annotation start or end) and the annotation for
 * which the event takes place.
 *
 * @version $Id$
 * @since 2.3M1
 */
public class AnnotationEvent
{
    /**
     * The type of annotation event that can occur during the processing.
     *
     * @version $Id$
     */
    public enum AnnotationEventType
    {
        /**
         * Marks the start of an annotation.
         */
        START,
        /**
         * Marks the end of an annotation.
         */
        END
    }

    /**
     * The type of the annotation event that took place.
     */
    private AnnotationEventType type;

    /**
     * The annotation for which this event took place.
     */
    private Annotation annotation;

    /**
     * Builds an annotation event for the passed annotation and type.
     *
     * @param type the type of the annotation event
     * @param ann the annotation for which the event took place
     */
    public AnnotationEvent(AnnotationEventType type, Annotation ann)
    {
        this.type = type;
        annotation = ann;
    }

    /**
     * @return the type
     */
    public AnnotationEventType getType()
    {
        return type;
    }

    /**
     * @return the annotation
     */
    public Annotation getAnnotation()
    {
        return annotation;
    }
}
