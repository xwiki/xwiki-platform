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
package org.xwiki.display.internal.job;

import org.xwiki.job.AbstractRequest;
import org.xwiki.rendering.syntax.Syntax;

/**
 * The request of the {@link DisplayJob}.
 * 
 * @version $Id$
 * @since 10.9RC1
 */
public class DisplayJobRequest extends AbstractRequest
{
    private static final String PROPERTY_SOURCE = "source";

    private static final String PROPERTY_SOURCECONFIGURATION = "sourceConfiguration";

    private static final String PROPERTY_TARGETSYNTAX = "targetSyntax";

    /**
     * @return the source to display (document, Blocks, etc.)
     */
    public Object getSource()
    {
        return getProperty(PROPERTY_SOURCE);
    }

    /**
     * @param source the source to display
     */
    public void setSource(Object source)
    {
        setProperty(PROPERTY_SOURCE, source);
    }

    /**
     * @return the configuration to pass to the source displayer
     */
    public Object getSourceConfiguration()
    {
        return getProperty(PROPERTY_SOURCECONFIGURATION);
    }

    /**
     * @param sourceConfiguration the configuration to pass to the source displayer
     */
    public void setSourceConfiguration(Object sourceConfiguration)
    {
        setProperty(PROPERTY_SOURCE, sourceConfiguration);
    }

    /**
     * @return the syntax to use to render the result
     */
    public Syntax getTargetSyntax()
    {
        return getProperty(PROPERTY_TARGETSYNTAX);
    }

    /**
     * @param targetSyntax the syntax to use to render the result
     */
    public void setTargetSyntax(Syntax targetSyntax)
    {
        setProperty(PROPERTY_SOURCE, targetSyntax);
    }
}
