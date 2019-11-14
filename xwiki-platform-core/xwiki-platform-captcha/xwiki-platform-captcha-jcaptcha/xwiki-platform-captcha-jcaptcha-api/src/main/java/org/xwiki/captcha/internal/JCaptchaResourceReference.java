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
package org.xwiki.captcha.internal;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.xwiki.resource.AbstractResourceReference;
import org.xwiki.resource.ResourceType;

/**
 * Represents a reference to a JCaptcha image.
 *
 * @since 11.10RC1
 * @version $Id$
 */
public class JCaptchaResourceReference extends AbstractResourceReference
{
    /**
     * Represents a JCaptcha Resource Type.
     */
    public static final ResourceType TYPE = new ResourceType("jcaptcha");

    private String captchaType;

    private String engine;

    /**
     * @param captchaType see {@link #getCaptchaType()}
     * @param engine see {@link #getEngine()}
     */
    public JCaptchaResourceReference(String captchaType, String engine)
    {
        setType(TYPE);
        this.captchaType = captchaType;
        this.engine = engine;
    }

    /**
     * @return the type of captcha to be displayed.
     */
    public String getCaptchaType()
    {
        return captchaType;
    }

    /**
     * @return the engine to be used for displaying this captcha.
     */
    public String getEngine()
    {
        return engine;
    }

    @Override
    public int hashCode()
    {
        return new HashCodeBuilder(5, 5)
            .append(getType())
            .append(getParameters())
            .append(getEngine())
            .append(getCaptchaType())
            .toHashCode();
    }

    @Override
    public boolean equals(Object object)
    {
        if (object == null) {
            return false;
        }
        if (object == this) {
            return true;
        }
        if (object.getClass() != getClass()) {
            return false;
        }
        JCaptchaResourceReference rhs = (JCaptchaResourceReference) object;
        return new EqualsBuilder()
            .append(getType(), rhs.getType())
            .append(getParameters(), rhs.getParameters())
            .append(getEngine(), rhs.getEngine())
            .append(getCaptchaType(), rhs.getCaptchaType())
            .isEquals();
    }
}
