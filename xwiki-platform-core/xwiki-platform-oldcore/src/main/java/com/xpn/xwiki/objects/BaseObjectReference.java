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
package com.xpn.xwiki.objects;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.xwiki.model.EntityType;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.model.reference.ObjectReference;
import org.xwiki.model.reference.RegexEntityReference;

import com.xpn.xwiki.web.Utils;

/**
 * Object reference implementation for object names generated in {@code className[objectNumber]} format. It provides
 * helper functions to extract the class name as specified by the caller, and object number.
 * <p>
 * Accepted formats for the object name are:
 * <dl>
 * <dt>className[objectNumber]
 * <dd>interpreted as the object of class className with number objectNumber. refers the object returned by
 * XWikiDocument.getObject(String className, int objectNumber). In this case, className is obtained by calling
 * {@link #getXClassReference()} and object index by calling {@link #getObjectNumber()}.
 * <dt>className
 * <dd>interpreted as the first object of class className. refers the object returned by XWikiDocument.getObject(String
 * className). In this case, {@link #getObjectNumber()} will return {@code null} and {@code className} is obtained by
 * calling {@link #getXClassReference()}.
 * </dl>
 *
 * @version $Id$
 */
public class BaseObjectReference extends ObjectReference
{
    /**
     * The version identifier for this Serializable class. Increment only if the <i>serialized</i> form of the class
     * changes.
     */
    private static final long serialVersionUID = 1L;

    /**
     * Used to match number part of the object reference name.
     */
    private static final Pattern NUMBERPATTERN = Pattern.compile("(\\\\*)\\[(\\d*)\\]$");

    /**
     * The class reference of this object.
     */
    protected DocumentReference xclassReference;

    /**
     * The number of this object.
     */
    protected Integer objectNumber;

    /**
     * True if the name and the metadata (class reference and number) are in sync.
     */
    private boolean nameParsed;

    /**
     * Constructor which would raise exceptions if the source entity reference does not have the appropriate type or
     * parent, etc.
     *
     * @param reference the raw reference to build this object reference from
     */
    public BaseObjectReference(EntityReference reference)
    {
        super(reference);
    }

    /**
     * Builds an indexed object reference for the object of class {@code className} with index {@code objectNumber} in
     * the document referenced by {@code parent}.
     *
     * @param classReference the name of the class of the object
     * @param objectNumber the number of the object in the document, or {@code null} if the default object should be
     *            referenced
     * @param parent reference to the parent document where the object is
     */
    public BaseObjectReference(DocumentReference classReference, Integer objectNumber, DocumentReference parent)
    {
        this((EntityReference) classReference, objectNumber, parent);
    }

    /**
     * Builds an indexed object reference for the object of class {@code className} with index {@code objectNumber} in
     * the document referenced by {@code parent}.
     *
     * @param classReference the name of the class of the object
     * @param objectNumber the number of the object in the document, or {@code null} if the default object should be
     *            referenced
     * @param parent reference to the parent document where the object is
     */
    public BaseObjectReference(EntityReference classReference, Integer objectNumber, DocumentReference parent)
    {
        super(toName(classReference, objectNumber, parent), parent);

        this.xclassReference = resolveClassReference(classReference);
        this.objectNumber = objectNumber;

        // Indicate that the name does not need to be parsed
        this.nameParsed = true;
    }

    /**
     * Clone an BaseObjectReference, but use the specified parent for its new parent.
     *
     * @param reference the reference to clone
     * @param parent the new parent to use
     * @since 10.8RC1
     */
    public BaseObjectReference(EntityReference reference, EntityReference parent)
    {
        super(reference, parent);
    }

    /**
     * @param classReference the local reference of the class (e.g. "XWiki.XWikiPreferences")
     * @return a reference used to match on object reference with the passed xclass
     * @since 9.4RC1
     */
    public static RegexEntityReference any(String classReference)
    {
        return any(classReference, null);
    }

    /**
     * @param classReference the local reference of the class (e.g. "XWiki.XWikiPreferences")
     * @param parent the parent of the object reference
     * @return a reference used to match on object reference with the passed xclass
     * @since 9.4RC1
     */
    public static RegexEntityReference any(String classReference, EntityReference parent)
    {
        return new RegexEntityReference(Pattern.compile("([^:]*:)?" + Pattern.quote(classReference) + "\\[\\d*\\]"),
            EntityType.OBJECT, parent);
    }

    private DocumentReference resolveClassReference(EntityReference classReference)
    {
        if (classReference instanceof DocumentReference) {
            return (DocumentReference) classReference;
        }

        return Utils.<DocumentReferenceResolver<EntityReference>>getComponent(DocumentReferenceResolver.TYPE_REFERENCE)
            .resolve(classReference, getParent());
    }

    /**
     * Convert provided class reference and object number into object reference name.
     *
     * @param classReference the class reference
     * @param objectNumber the object number
     * @return the object reference name
     */
    private static String toName(EntityReference classReference, Integer objectNumber, DocumentReference parent)
    {
        // Optimize the reference of the class to avoid useless wiki part in most cases (which also make impossible to
        // get a local version of the object reference)
        String name =
            Utils.<EntityReferenceSerializer<String>>getComponent(EntityReferenceSerializer.TYPE_STRING, "compactwiki")
                .serialize(classReference, parent);

        if (objectNumber != null) {
            StringBuilder builder = new StringBuilder(name);
            builder.append('[');
            builder.append(objectNumber);
            builder.append(']');
            name = builder.toString();
        } else {
            Matcher matcher = NUMBERPATTERN.matcher(name);
            if (matcher.find()) {
                if (matcher.group(1).length() % 2 == 0) {
                    StringBuilder builder = new StringBuilder(name);
                    builder.insert(matcher.start(), '\\');
                    name = builder.toString();
                }
            }
        }

        return name;
    }

    private void maybeParseName()
    {
        if (!this.nameParsed) {
            String name = getName();

            String classReferenceStr;
            String objectNumberStr;

            Matcher matcher = NUMBERPATTERN.matcher(name);
            if (matcher.find()) {
                if (matcher.group(1).length() % 2 == 0) {
                    classReferenceStr = name.substring(0, matcher.end(1));
                    objectNumberStr = matcher.group(2);
                } else {
                    classReferenceStr = name;
                    objectNumberStr = null;
                }
            } else {
                classReferenceStr = name;
                objectNumberStr = null;
            }

            this.xclassReference =
                Utils.<DocumentReferenceResolver<String>>getComponent(DocumentReferenceResolver.TYPE_STRING)
                    .resolve(classReferenceStr, getParent());
            if (objectNumberStr != null) {
                this.objectNumber = Integer.valueOf(objectNumberStr);
            }

            // Indicate that the name does not need to be parsed
            this.nameParsed = true;
        }
    }

    /**
     * @return the reference of the class of this object.
     */
    public DocumentReference getXClassReference()
    {
        // Make sure metadata are in sync with the name
        maybeParseName();

        return this.xclassReference;
    }

    /**
     * @return the number of this object among the objects of the same class in the document, as set by the caller in
     *         [objectNumber] format after the class name (i.e. no resolving is done, existence of this object is not
     *         guaranteed). If no number can be parsed (i.e. [number] cannot be parsed) this function returns
     *         {@code null} and object should be interpreted as the first object of this class in the document.
     */
    public Integer getObjectNumber()
    {
        // Make sure metadata are in sync with the name
        maybeParseName();

        return this.objectNumber;
    }

    @Override
    public BaseObjectReference replaceParent(EntityReference newParent)
    {
        if (newParent == getParent()) {
            return this;
        }

        return new BaseObjectReference(this, newParent);
    }
}
