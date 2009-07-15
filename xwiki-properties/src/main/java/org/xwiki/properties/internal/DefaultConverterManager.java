package org.xwiki.properties.internal;

import org.xwiki.component.annotation.Component;
import org.xwiki.component.annotation.Requirement;
import org.xwiki.component.logging.AbstractLogEnabled;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.properties.ConverterManager;
import org.xwiki.properties.converter.ConversionException;
import org.xwiki.properties.converter.Converter;

/**
 * Default implementation for {@link ConverterManager}.
 * <p>
 * It try to find a {@link Converter} for the provided target type. If it can't find:
 * <ul>
 * <li>if the type is an {@link Enum}, it use the {@link Converter} with component hint "enum"</li>
 * <li>then it use the default {@link Converter} (which is based on {@link org.apache.commons.beanutils.ConvertUtils} by
 * default)</li>
 * </ul>
 * 
 * @version $Id$
 * @since 2.0M2
 */
@Component
public class DefaultConverterManager extends AbstractLogEnabled implements ConverterManager
{
    /**
     * Use to find the proper {@link Converter} component for provided target type.
     */
    @Requirement
    private ComponentManager componentManager;

    /**
     * Used when no direct {@link Converter} can be found for provided target type and the target type is an
     * {@link Enum}.
     */
    @Requirement("enum")
    private Converter enumConverter;

    /**
     * Used when no direct {@link Converter} can be found for provided target type.
     */
    @Requirement
    private Converter defaultConverter;

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.properties.ConverterManager#convert(java.lang.Class, java.lang.Object)
     */
    public <T> T convert(Class<T> targetClass, Object value)
    {
        // Convert
        Converter converter = lookupConverter(targetClass);

        if (converter != null) {
            return converter.convert(targetClass, value);
        } else {
            throw new ConversionException("Can't find converter to convert value [" + value + "] to type ["
                + targetClass + "] ");
        }
    }

    /**
     * Find the right {@link Converter} for the provided {@link Class}.
     * 
     * @param targetType the class
     * @return the {@link Converter} corresponding to the class
     */
    private Converter lookupConverter(Class< ? > targetType)
    {
        Converter converter = null;
        try {
            converter = this.componentManager.lookup(Converter.class, targetType.getName());
        } catch (ComponentLookupException e) {
            getLogger().debug("Failed to find a proper Converter for type [" + targetType.getName() + "]", e);
        }

        if (converter == null) {
            if (Enum.class.isAssignableFrom(targetType)) {
                converter = this.enumConverter;
            } else {
                getLogger().debug("Trying default Converter for target type [" + targetType.getName() + "]");

                converter = this.defaultConverter;
            }
        }

        return converter;
    }
}
