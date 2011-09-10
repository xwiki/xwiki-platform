package com.xpn.xwiki.util;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;

/**
 * Class with {@link Object#equals(Object)}, {@link Object#hashCode()} and {@link Object#toString()} implemented via
 * commons-lang builders. Useful for extend simple POJO classes.
 * 
 * @version $Id$
 * @since 1.2M1
 */
public abstract class AbstractSimpleClass
{
    @Override
    public boolean equals(Object obj)
    {
        return EqualsBuilder.reflectionEquals(this, obj);
    }

    @Override
    public int hashCode()
    {
        return HashCodeBuilder.reflectionHashCode(this);
    }

    @Override
    public String toString()
    {
        return ToStringBuilder.reflectionToString(this);
    }
}
