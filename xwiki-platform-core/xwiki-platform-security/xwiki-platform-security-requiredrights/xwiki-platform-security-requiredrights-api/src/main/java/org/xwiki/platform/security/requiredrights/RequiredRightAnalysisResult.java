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
package org.xwiki.platform.security.requiredrights;

import java.util.List;

import org.slf4j.event.Level;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.security.authorization.Right;
import org.xwiki.stability.Unstable;

import static org.slf4j.event.Level.ERROR;

/**
 * Represents the result of a required right analysis. This class provides getters and setters for various properties
 * related to the analysis result.
 *
 * @version $Id$
 * @since 15.8RC1
 */
@Unstable
public class RequiredRightAnalysisResult
{
    private Level level = ERROR;

    private EntityReference entityReference;

    private String analyzerHint;

    private String message;

    private List<Object> messageParameters;

    private Right requiredRight;

    private EntityType entityType;

    public RequiredRightAnalysisResult(EntityReference entityReference, String analyzerHint, String message,
        List<Object> messageParameters, Right requiredRight, EntityType entityType)
    {
        this.entityReference = entityReference;
        this.analyzerHint = analyzerHint;
        this.message = message;
        this.messageParameters = messageParameters;
        this.requiredRight = requiredRight;
        this.entityType = entityType;
    }

    public RequiredRightAnalysisResult(String analyzerHint, String message, List<Object> messageParameters,
        Right requiredRight, EntityType entityType)
    {
        this.analyzerHint = analyzerHint;
        this.message = message;
        this.messageParameters = messageParameters;
        this.requiredRight = requiredRight;
        this.entityType = entityType;
    }

    /**
     * @return the level of the right violation. A value of {@link Level#ERROR} indicates that the right is absolutely
     *     required, {@link Level#WARN} indicates that the right might be required and behavior might change with it but
     *     the analyzer is not certain.
     */
    public Level getLevel()
    {
        return this.level;
    }

    /**
     * @param level the level of the right violation.
     * @see #getLevel()
     * @return this current object
     */
    public RequiredRightAnalysisResult setLevel(Level level)
    {
        this.level = level;
        return this;
    }

    /**
     * @return the location of the analyzed entity (e.g., a document content, or a field in an XObject)
     */
    public EntityReference getEntityReference()
    {
        return this.entityReference;
    }

    /**
     * @param entityReference the location of the analyzed entity (e.g., a document content, or a field in an
     *     XObject)
     * @see #getEntityReference()
     */
    public RequiredRightAnalysisResult setEntityReference(EntityReference entityReference)
    {
        this.entityReference = entityReference;
        return this;
    }

    /**
     * @return the hint of the analyzer that produced this analysis result
     */
    public String getAnalyzerHint()
    {
        return this.analyzerHint;
    }

    /**
     * @param analyzerHint the hint of the analyzer
     * @see #getAnalyzerHint()
     */
    public RequiredRightAnalysisResult setAnalyzerHint(String analyzerHint)
    {
        this.analyzerHint = analyzerHint;
        return this;
    }

    /**
     * @return the translation key for the message to display to the user for this required right violation
     */
    public String getMessage()
    {
        return this.message;
    }

    /**
     * @param message the translation key for the message
     * @see #getMessage()
     */
    public RequiredRightAnalysisResult setMessage(String message)
    {
        this.message = message;
        return this;
    }

    /**
     * @return the parameters for the translation message like the offending content
     */
    public List<Object> getMessageParameters()
    {
        return this.messageParameters;
    }

    /**
     * @param messageParameters the parameters for the message
     * @see #getMessageParameters()
     */
    public RequiredRightAnalysisResult setMessageParameters(List<Object> messageParameters)
    {
        this.messageParameters = messageParameters;
        return this;
    }

    /**
     * @return the right that would be required by the entity to fully work according to the analyzer. This may be
     *     inaccurate in particular if {@link #getLevel()} is not {@link Level#ERROR}.
     */
    public Right getRequiredRight()
    {
        return this.requiredRight;
    }

    /**
     * @param requiredRight the right that would be required by the entity
     * @see #getRequiredRight()
     */
    public RequiredRightAnalysisResult setRequiredRight(Right requiredRight)
    {
        this.requiredRight = requiredRight;
        return this;
    }

    /**
     * @return the type of the entity for which the right is required, should be {@link EntityType#DOCUMENT} in most
     *     cases but can be used to indicate, e.g., wiki admin right using {@link EntityType#WIKI}.
     */
    public EntityType getEntityType()
    {
        return this.entityType;
    }

    /**
     * @param entityType the type of the entity for which the right is required
     * @see #getEntityType()
     */
    public RequiredRightAnalysisResult setEntityType(EntityType entityType)
    {
        this.entityType = entityType;
        return this;
    }
}
