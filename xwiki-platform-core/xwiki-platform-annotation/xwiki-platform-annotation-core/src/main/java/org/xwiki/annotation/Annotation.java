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

package org.xwiki.annotation;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.xwiki.annotation.maintainer.AnnotationState;

/**
 * This class wraps together the data needed to describe an annotation.
 *
 * @version $Id$
 * @since 2.3M1
 */
public class Annotation
{
    /**
     * The name of the field of this annotation selection. <br>
     * The text on which this annotation is added.
     */
    public static final String SELECTION_FIELD = "selection";

    /**
     * The name of the field of this annotation selection context to the left of the annotation. <br>
     * The context of the selection is used to uniquely localize an annotation on the content where is added. Or, if the
     * context appears twice, semantically speaking it shouldn't make any difference if the annotation is displayed and
     * handled in one or other of the occurrences.
     */
    public static final String SELECTION_LEFT_CONTEXT_FIELD = "selectionLeftContext";

    /**
     * The name of the field of this annotation selection context to the right of the annotation. <br>
     * The context of the selection is used to uniquely localize an annotation on the content where is added. Or, if the
     * context appears twice, semantically speaking it shouldn't make any difference if the annotation is displayed and
     * handled in one or other of the occurrences.
     */
    public static final String SELECTION_RIGHT_CONTEXT_FIELD = "selectionRightContext";

    /**
     * The name of the field of this annotation state. <br>
     * TODO: find out if it's the right place to put the state information, as it's a maintainer particular information.
     */
    public static final String STATE_FIELD = "state";

    /**
     * The name of the field of this annotation original selection.
     */
    public static final String ORIGINAL_SELECTION_FIELD = "originalSelection";

    /**
     * The name of the field of this annotation author.
     */
    public static final String AUTHOR_FIELD = "author";

    /**
     * The name of the field of this annotation serialized date.
     */
    public static final String DATE_FIELD = "date";

    /**
     * The name of the field of this annotation's reference to the target content.
     */
    public static final String TARGET_FIELD = "target";

    /**
     * The name of the field of this annotation start offset relative to the plain text.
     *
     * @since 13.10RC1
     */
    public static final String PLAIN_TEXT_START_OFFSET_FIELD = "plainTextStartOffset";

    /**
     * The name of the field of this annotation end offset relative to the plain text.
     *
     * @since 13.10RC1
     */
    public static final String PLAIN_TEXT_END_OFFSET_FIELD = "plainTextEndOffset";

    /**
     * The unique identifier of this annotation, which should be unique among all the annotations on the same target.
     */
    protected final String id;

    /**
     * The values of the fields of this annotation.
     */
    protected Map<String, Object> fields = new HashMap<String, Object>();

    /**
     * Builds an annotation description for the annotation with the passed id: used for annotation updates where only a
     * part of the fields my need to be set.
     *
     * @param id the id of this annotation
     */
    public Annotation(String id)
    {
        this.id = id;
    }

    /**
     * Builds an annotation for the passed selection in the context, used to pass an annotation to be added (which does
     * not have an id yet since it hasn't been stored yet).
     *
     * @param initialSelection the selected text of this annotation
     * @param leftContext the context to the left of the selection, which makes the selection uniquely identifiable in
     *            the content on which this annotation is added. Can be void if selection itself is unique
     * @param rightContext the context to the right of the selection, which makes the selection uniquely identifiable in
     *            the content on which this annotation is added. Can be void if selection itself is unique
     */
    public Annotation(String initialSelection, String leftContext, String rightContext)
    {
        this(null);
        setSelection(initialSelection, leftContext, rightContext);
        // also initialize the state of this annotation to safe
        setState(AnnotationState.SAFE);
    }

    /**
     * @return author of annotation.
     */
    public String getAuthor()
    {
        return (String) fields.get(AUTHOR_FIELD);
    }

    /**
     * Sets the author of this annotation.
     *
     * @param author the author of this annotation.
     */
    public void setAuthor(String author)
    {
        fields.put(AUTHOR_FIELD, author);
    }

    /**
     * @return date of annotation
     */
    public Date getDate()
    {
        return (Date) fields.get(DATE_FIELD);
    }

    /**
     * @param date the serialized date to set
     */
    public void setDate(Date date)
    {
        fields.put(DATE_FIELD, date);
    }

    /**
     * @return state of annotation
     */
    public AnnotationState getState()
    {
        // have a little bit of mercy, try to parse this from string if it's a string
        Object stateField = fields.get(STATE_FIELD);
        if (stateField == null) {
            // null stays null no matter what
            return null;
        }
        if (AnnotationState.class.isAssignableFrom(stateField.getClass())) {
            return (AnnotationState) stateField;
        } else if (String.class.isAssignableFrom(stateField.getClass())) {
            try {
                return AnnotationState.valueOf((String) stateField);
            } catch (IllegalArgumentException e) {
                return null;
            }
        }

        // return no state, can't actually make sense of the value in the map. Hopefully this will never happen
        return null;
    }

    /**
     * @param state to set
     */
    public void setState(AnnotationState state)
    {
        fields.put(STATE_FIELD, state);
    }

    /**
     * @return selected text of this annotation
     */
    public String getSelection()
    {
        return (String) fields.get(SELECTION_FIELD);
    }

    /**
     * Helper method to get the selection of this annotation in its context, with the context left to the left and
     * context right to the right. This method ensures that a non-null value will be returned, even if some of the
     * components of the selection are missing.
     *
     * @return the selection of this annotation in its context, with the context left to the left and context right to
     *         the right
     */
    public String getSelectionInContext()
    {
        return (StringUtils.isEmpty(getSelectionLeftContext()) ? "" : getSelectionLeftContext())
            + (StringUtils.isEmpty(getSelection()) ? "" : getSelection())
            + (StringUtils.isEmpty(getSelectionRightContext()) ? "" : getSelectionRightContext());
    }

    /**
     * Sets the selection of this annotation and the context along with it.
     *
     * @param selection the selection of this annotation
     * @param contextLeft the context to the left of the annotation
     * @param contextRight the context to the right of the annotation
     */
    public void setSelection(String selection, String contextLeft, String contextRight)
    {
        fields.put(SELECTION_FIELD, selection);
        fields.put(SELECTION_LEFT_CONTEXT_FIELD, contextLeft);
        fields.put(SELECTION_RIGHT_CONTEXT_FIELD, contextRight);
    }

    /**
     * Sets the selection of this annotation.
     *
     * @param selection the selection of the annotation
     */
    public void setSelection(String selection)
    {
        setSelection(selection, "", "");
    }

    /**
     * @return selection context to the left of annotation
     */
    public String getSelectionLeftContext()
    {
        return (String) fields.get(SELECTION_LEFT_CONTEXT_FIELD);
    }

    /**
     * @return selection context to the right of annotation
     */
    public String getSelectionRightContext()
    {
        return (String) fields.get(SELECTION_RIGHT_CONTEXT_FIELD);
    }

    /**
     * @return id of annotation
     */
    public String getId()
    {
        return id;
    }

    @Override
    public String toString()
    {
        return "id: " + getId() + " | author: " + getAuthor() + " | selection left context: "
            + getSelectionLeftContext() + " | selection: " + getSelection() + " | selection right context: "
            + getSelectionRightContext();
    }

    @Override
    public boolean equals(Object obj)
    {
        if (!(obj instanceof Annotation)) {
            return false;
        }
        Annotation other = (Annotation) obj;
        if (other.getId() != null || getId() != null) {
            // if they have ids, compare ids
            return ("" + getId()).equals(other.getId());
        } else {
            // else compare selection and selection context
            return ("" + getSelection()).equals(other.getSelection())
                && ("" + getSelectionLeftContext()).equals(other.getSelectionLeftContext())
                && ("" + getSelectionRightContext()).equals(other.getSelectionRightContext());
        }
    }

    @Override
    public int hashCode()
    {
        return (getId() != null ? getId() : getSelectionLeftContext() + getSelection() + getSelectionRightContext())
            .hashCode();
    }

    /**
     * @return the originalSelection
     */
    public String getOriginalSelection()
    {
        return (String) fields.get(ORIGINAL_SELECTION_FIELD);
    }

    /**
     * @param originalSelection the originalSelection to set
     */
    public void setOriginalSelection(String originalSelection)
    {
        fields.put(ORIGINAL_SELECTION_FIELD, originalSelection);
    }

    /**
     * @param key the key of the field to get
     * @return the value of the field
     */
    public Object get(String key)
    {
        return fields.get(key);
    }

    /**
     * Sets / adds a value in the fields of this annotation.
     *
     * @param key the key of the field
     * @param value the value to set for the field
     * @return the old value of this field, or null if none was set
     */
    public Object set(String key, Object value)
    {
        return fields.put(key, value);
    }

    /**
     * @return a set of names of the fields set in this annotation object
     */
    public Set<String> getFieldNames()
    {
        return fields.keySet();
    }
}
