package com.xpn.xwiki.util;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.apache.commons.lang.builder.ToStringBuilder;

/**
 * Class with {@link Object#equals(Object)}, {@link Object#hashCode()}
 *  and {@link Object#toString()} implemented via commons-lang builders.
 * Useful for extend simple POJO classes.
 * @version $Id$
 * @since 1.2M1
 */
public abstract class AbstractSimpleClass
{
    /** {@inheritDoc} */
    public boolean equals(Object obj)
    {
        return EqualsBuilder.reflectionEquals(this, obj);        
    }
    /** {@inheritDoc} */
    public int hashCode()
    {
        return HashCodeBuilder.reflectionHashCode(this);
    }
    /** {@inheritDoc} */
    public String toString()
    {
        return ToStringBuilder.reflectionToString(this);
    }
}
