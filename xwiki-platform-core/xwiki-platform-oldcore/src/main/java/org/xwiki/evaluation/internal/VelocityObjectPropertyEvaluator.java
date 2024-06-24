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
package org.xwiki.evaluation.internal;

import java.io.StringWriter;
import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.velocity.VelocityContext;
import org.xwiki.component.annotation.Component;
import org.xwiki.evaluation.ObjectEvaluatorException;
import org.xwiki.evaluation.ObjectPropertyEvaluator;
import org.xwiki.model.document.DocumentAuthors;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.security.authorization.AuthorExecutor;
import org.xwiki.security.authorization.AuthorizationManager;
import org.xwiki.security.authorization.Right;
import org.xwiki.user.UserReferenceSerializer;
import org.xwiki.velocity.VelocityManager;

import com.xpn.xwiki.objects.BaseObject;

/**
 * ObjectPropertyEvaluator that supports the evaluation of Velocity properties.
 *
 * @version $Id$
 * @since 14.10.21
 * @since 15.5.5
 * @since 15.10.2
 */
@Component
@Singleton
@Named("velocity")
public class VelocityObjectPropertyEvaluator implements ObjectPropertyEvaluator
{
    @Inject
    private AuthorizationManager authorizationManager;

    @Inject
    private AuthorExecutor authorExecutor;

    @Inject
    private VelocityManager velocityManager;

    @Inject
    @Named("document")
    private UserReferenceSerializer<DocumentReference> documentUserSerializer;

    @Inject
    private EntityReferenceSerializer<String> entityReferenceSerializer;

    /**
     * Evaluates Velocity properties of an object with an author executor.
     */
    @Override
    public Map<String, String> evaluateProperties(BaseObject object, String... properties)
        throws ObjectEvaluatorException
    {
        Map<String, String> evaluatedProperties = new HashMap<>();
        for (String property : properties) {
            evaluatedProperties.put(property, object.getStringValue(property));
        }

        DocumentReference documentReference = object.getDocumentReference();
        DocumentAuthors documentAuthors = object.getOwnerDocument().getAuthors();
        DocumentReference authorReference =
            this.documentUserSerializer.serialize(documentAuthors.getEffectiveMetadataAuthor());

        if (this.authorizationManager.hasAccess(Right.SCRIPT, authorReference, documentReference)) {
            try {
                this.authorExecutor.call(() -> {
                    VelocityContext context = this.velocityManager.getVelocityContext();
                    for (Map.Entry<String, String> propertyEntry : evaluatedProperties.entrySet()) {
                        StringWriter writer = new StringWriter();
                        String serializedPropertyReference = this.entityReferenceSerializer.serialize(
                            object.getField(propertyEntry.getKey()).getReference());
                        this.velocityManager.getVelocityEngine().evaluate(context, writer, serializedPropertyReference,
                            propertyEntry.getValue());
                        propertyEntry.setValue(writer.toString());
                    }
                    return null;
                }, authorReference, documentReference);
            } catch (Exception e) {
                throw new ObjectEvaluatorException("Failed to run Velocity engine.", e);
            }
        }

        return evaluatedProperties;
    }
}
