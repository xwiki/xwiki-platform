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
package org.xwiki.display.internal;

import org.xwiki.rendering.syntax.Syntax;
import org.xwiki.rendering.util.IdGenerator;

/**
 * {@link DocumentDisplayer} parameters.
 * 
 * @version $Id$
 * @since 3.2M3
 */
public class DocumentDisplayerParameters implements Cloneable
{
    /**
     * The id of the document section to display. E.g. "HSectionTitle".
     */
    private String sectionId;

    /**
     * Flag indicating if the title should be displayed instead of the content.
     */
    private boolean titleDisplayed;

    /**
     * Flag indicating if the document should be displayed in an isolated execution context rather than in the current
     * execution context. When {@code true}, the current execution context is cloned and the displayed document is set
     * as the context document.
     */
    private boolean executionContextIsolated;

    /**
     * Flag indicating if the XDOM transformations should be executed in an isolated context or not. Set this to
     * {@code false} if you expect transformations to have side effects. This flag controls for instance if the Velocity
     * macros defined by the Velocity transformations are added to the current name-space or to a new one (isolated).
     */
    private boolean transformationContextIsolated = true;

    /**
     * Flag indicating if the transformation context should be restricted so that potentially harmful transformations
     * are not executed.
     */
    private boolean transformationContextRestricted;

    /**
     * Flag indicating if the content should be transformed or not. When this flag is set the rendering transformations
     * are performed on the content XDOM.
     */
    private boolean contentTransformed = true;

    /**
     * When this flag is set the displayer should look for a document translation matching the current language on the
     * execution context. Otherwise the displayer should simply display the content of the provided document.
     */
    private boolean contentTranslated;

    /**
     * The target syntax to put in the transformation context.
     */
    private Syntax targetSyntax;

    private boolean asyncAllowed = true;

    /**
     * The id generator to generate ids that are unique also in the parent document.
     *
     * @since 14.2RC1
     */
    private IdGenerator idGenerator;

    /**
     * @return the id of the document section to display
     */
    public String getSectionId()
    {
        return sectionId;
    }

    /**
     * Sets the id of the document section to display.
     * 
     * @param sectionId the id of the document section to display
     */
    public void setSectionId(String sectionId)
    {
        this.sectionId = sectionId;
    }

    /**
     * @return {@code true} if the title should be displayed instead of the content, {@code false} otherwise
     */
    public boolean isTitleDisplayed()
    {
        return titleDisplayed;
    }

    /**
     * Sets whether the title should be displayed instead of the content.
     * 
     * @param titleDisplayed {@code true} to display the title, {@code false} to display the content
     */
    public void setTitleDisplayed(boolean titleDisplayed)
    {
        this.titleDisplayed = titleDisplayed;
    }

    /**
     * @return {@code true} if the execution context should be isolated while the document is displayed, {@code false}
     *         otherwise
     */
    public boolean isExecutionContextIsolated()
    {
        return executionContextIsolated;
    }

    /**
     * Sets whether the execution context should be isolated while the document is displayed.
     * 
     * @param executionContextIsolated {@code true} to isolate the execution context while the document is displayed,
     *            {@code false} to display the document in the current execution context
     */
    public void setExecutionContextIsolated(boolean executionContextIsolated)
    {
        this.executionContextIsolated = executionContextIsolated;
    }

    /**
     * @return {@code true} if the transformation context should be isolated while the document is displayed,
     *         {@code false} otherwise
     */
    public boolean isTransformationContextIsolated()
    {
        return transformationContextIsolated;
    }

    /**
     * Sets whether the transformation context should be isolated while the document is displayed.
     * 
     * @param transformationContextIsolated {@code true} to isolate the transformation context while the document is
     *            displayed, {@code false} to use a transformation context based on the current context document
     */
    public void setTransformationContextIsolated(boolean transformationContextIsolated)
    {
        this.transformationContextIsolated = transformationContextIsolated;
    }

    /**
     * @return {@code true} if the transformation context should be restricted, {@code false} otherwise.
     */
    public boolean isTransformationContextRestricted()
    {
        return transformationContextRestricted;
    }

    /**
     * Set the flag indicating whether the transformation context should be restricted or not.
     *
     * @param transformationContextRestricted {@code true} to indicate that potentially harmful transformations should
     *            not be executed.
     */
    public void setTransformationContextRestricted(boolean transformationContextRestricted)
    {
        this.transformationContextRestricted = transformationContextRestricted;
    }

    /**
     * @return {@code true} if the content is transformed, {@code false} otherwise
     */
    public boolean isContentTransformed()
    {
        return contentTransformed;
    }

    /**
     * Sets whether the content is transformed.
     * 
     * @param contentTransformed {@code true} to transform the content, {@code false} otherwise
     */
    public void setContentTransformed(boolean contentTransformed)
    {
        this.contentTransformed = contentTransformed;
    }

    /**
     * @return {@code true} if the displayer should look for a document translation matching the current language,
     *         {@code false} if the displayer should simply display the content of the provided document
     */
    public boolean isContentTranslated()
    {
        return contentTranslated;
    }

    /**
     * Sets whether the displayer should display the translated content or not.
     * 
     * @param contentTranslated {@code true} to force the display to look for a document translation matching the
     *            current language, {@code false} to tell the displayer to simply display the content of the provided
     *            document
     */
    public void setContentTranslated(boolean contentTranslated)
    {
        this.contentTranslated = contentTranslated;
    }

    /**
     * @return the target syntax to put in the transformation context.
     */
    public Syntax getTargetSyntax()
    {
        return this.targetSyntax;
    }

    /**
     * @param targetSyntax the target syntax to put in the transformation context.
     */
    public void setTargetSyntax(Syntax targetSyntax)
    {
        this.targetSyntax = targetSyntax;
    }

    /**
     * @return false if the document should be always executed synchronously
     * @since 11.8RC1
     */
    public boolean isAsyncAllowed()
    {
        return this.asyncAllowed;
    }

    /**
     * @param asyncAllowed false if the document should be always executed synchronously
     * @since 11.8RC1
     */
    public void setAsyncAllowed(boolean asyncAllowed)
    {
        this.asyncAllowed = asyncAllowed;
    }

    /**
     * @return the id generator to use for unique ids
     * @since 14.2RC1
     */
    public IdGenerator getIdGenerator()
    {
        return this.idGenerator;
    }

    /**
     * @param idGenerator the id generator to make sure ids are unique
     * @since 14.2RC1
     */
    public void setIdGenerator(IdGenerator idGenerator)
    {
        this.idGenerator = idGenerator;
    }

    @Override
    public DocumentDisplayerParameters clone()
    {
        DocumentDisplayerParameters clone;
        try {
            clone = (DocumentDisplayerParameters) super.clone();
        } catch (CloneNotSupportedException e) {
            // Should never happen.
            throw new RuntimeException("Failed to clone object", e);
        }
        clone.setContentTransformed(contentTransformed);
        clone.setContentTranslated(contentTranslated);
        clone.setExecutionContextIsolated(executionContextIsolated);
        clone.setSectionId(sectionId);
        clone.setTitleDisplayed(titleDisplayed);
        clone.setTransformationContextIsolated(transformationContextIsolated);
        // No copy of the id generator as we explicitly want that it is shared.
        clone.setIdGenerator(this.idGenerator);
        return clone;
    }
}
