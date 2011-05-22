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
package org.xwiki.crypto.passwd.internal;

import java.util.Properties;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.annotation.Requirement;
import org.xwiki.configuration.ConfigurationSource;

import org.xwiki.crypto.passwd.PasswordCryptoServiceConfiguration;
import org.xwiki.crypto.passwd.KeyDerivationFunction;
import org.xwiki.crypto.passwd.PasswordVerificationFunction;
import org.xwiki.crypto.passwd.PasswordCiphertext;

/**
 * Default implementation of {@link PasswordCryptoServiceConfiguration}.
 * 
 * @version $Id$
 */
@Component
public class DefaultPasswordCryptoServiceConfiguration implements PasswordCryptoServiceConfiguration
{
    /**
     * The logger to log.
     */
    @Inject
    private Logger logger;

    /**
     * Default {@link java.util.Properties} for the {@link org.xwiki.crypto.passwd.KeyDerivationFunction}s.
     */
    private final Properties defaultKeyDerivationFunctionProperties = new Properties() {
        {
            setProperty("millisecondsOfProcessorTimeToSpend", "200");
            setProperty("numberOfKilobytesOfMemoryToUse", "1024");
            setProperty("derivedKeyLength", "32");
        }
    };

    /** All configuration keys used by this class will start with this prefix. */
    private final String configurationPrefix = "crypto.passwd.";

    /** configuration key for which {@link org.xwiki.crypto.passwd.PasswordCiphertext} class to use. */
    private final String cipherKey = "PasswordCiphertext";

    /** Default cipher to use if no other is defined or parsing fails. */
    private final Class<?> defaultCipherClass = CAST5PasswordCiphertext.class;

    /** configuration key for which {@link org.xwiki.crypto.passwd.KeyDerivationFunction} to use for encryption. */
    private final String keyDerivationFunctionClassForEncryption = "keyDerivationFunctionClassForEncryption";

    /** Default {@link org.xwiki.crypto.passwd.KeyDerivationFunction} class to use for encryption. */
    private final Class<?> defaultKeyDerivationFunctionClassForEncryption =
        PBKDF2KeyDerivationFunction.class;

    /**
     * Key for {@link java.util.Properties} for the {@link org.xwiki.crypto.passwd.KeyDerivationFunction} 
     * used for encryption.
     */
    private final String keyDerivationFunctionPropertiesForEncryption =
        "keyDerivationFunctionPropertiesForEncryption";

    /**
     * Default {@link java.util.Properties} for the {@link org.xwiki.crypto.passwd.KeyDerivationFunction} 
     * used for encryption.
     */
    private final Properties defaultKeyDerivationFunctionPropertiesForEncryption =
        this.defaultKeyDerivationFunctionProperties;

    /** configuration key for which {@link org.xwiki.crypto.passwd.PasswordVerificationFunction} class to use. */
    private final String passwordVerificationFunctionClass = "PasswordVerificationFunction";

    /** Default password verification function class to use if no other is defined or parsing fails. */
    private final Class<?> defaultPasswordVerificationFunctionClass = DefaultPasswordVerificationFunction.class;

    /** configuration key for which {@link org.xwiki.crypto.passwd.KeyDerivationFunction} to use for passwords. */
    private final String keyDerivationFunctionClassForPasswordVerification = 
        "keyDerivationFunctionClassForPasswordVerification";

    /** Default {@link org.xwiki.crypto.passwd.KeyDerivationFunction} class to use for password verification. */
    private final Class<?> defaultKeyDerivationFunctionClassForPasswordVerification = 
        PBKDF2KeyDerivationFunction.class;

    /**
     * Key for {@link java.util.Properties} for the {@link org.xwiki.crypto.passwd.KeyDerivationFunction} 
     * used for password verification.
     */
    private final String keyDerivationFunctionPropertiesForPasswordVerification =
        "keyDerivationFunctionPropertiesForPasswordVerification";

    /**
     * Default {@link java.util.Properties} for the {@link org.xwiki.crypto.passwd.KeyDerivationFunction} 
     * used for password verification.
     */
    private final Properties defaultKeyDerivationFunctionPropertiesForPasswordVerification =
        this.defaultKeyDerivationFunctionProperties;


    /** Configuration is loaded from this source. */
    @Requirement
    private ConfigurationSource source;

    /**
     * {@inheritDoc}
     *
     * @see PasswordCryptoServiceConfiguration#getCipherClass()
     */
    public Class<? extends PasswordCiphertext> getCipherClass()
    {
        return this.getClass(this.configurationPrefix + this.cipherKey,
                             PasswordCiphertext.class,
                             this.defaultCipherClass);
    }

    /**
     * {@inheritDoc}
     *
     * @see PasswordCryptoServiceConfiguration#getKeyDerivationFunctionClassForEncryption()
     */
    public Class<? extends KeyDerivationFunction> getKeyDerivationFunctionClassForEncryption()
    {
        return this.getClass(this.configurationPrefix + this.keyDerivationFunctionClassForEncryption,
                             KeyDerivationFunction.class,
                             this.defaultKeyDerivationFunctionClassForEncryption);
    }

    /**
     * {@inheritDoc}
     *
     * @see PasswordCryptoServiceConfiguration#getKeyDerivationFunctionPropertiesForEncryption()
     */
    public Properties getKeyDerivationFunctionPropertiesForEncryption()
    {
        return new Properties(this.source.getProperty(this.keyDerivationFunctionPropertiesForEncryption,
                                                      this.defaultKeyDerivationFunctionPropertiesForEncryption));
    }

    /**
     * {@inheritDoc}
     *
     * @see PasswordCryptoServiceConfiguration#getPasswordVerificationFunctionClass()
     */
    public Class<? extends PasswordVerificationFunction> getPasswordVerificationFunctionClass()
    {
        return this.getClass(this.configurationPrefix + this.passwordVerificationFunctionClass,
                             PasswordVerificationFunction.class,
                             this.defaultPasswordVerificationFunctionClass);
    }

    /**
     * {@inheritDoc}
     *
     * @see PasswordCryptoServiceConfiguration#getKeyDerivationFunctionClassForPasswordVerification()
     */
    public Class<? extends KeyDerivationFunction> getKeyDerivationFunctionClassForPasswordVerification()
    {
        return this.getClass(this.configurationPrefix + this.keyDerivationFunctionClassForPasswordVerification,
                             KeyDerivationFunction.class,
                             this.defaultKeyDerivationFunctionClassForPasswordVerification);
    }

    /**
     * {@inheritDoc}
     *
     * @see PasswordCryptoServiceConfiguration#getKeyDerivationFunctionPropertiesForPasswordVerification()
     */
    public Properties getKeyDerivationFunctionPropertiesForPasswordVerification()
    {
        return new Properties(
            this.source.getProperty(
                this.keyDerivationFunctionPropertiesForPasswordVerification,
                this.defaultKeyDerivationFunctionPropertiesForPasswordVerification));
    }

    /**
     * @param configurationKey look up this configuration parameter in the configuration source to get the name of the
     *                         class to use.
     * @param <T> output will extend or implement this class or interface.
     * @param mustExtend the output will be cast to an extension of this class.
     * @param defaultOut return this class if there is an error or the configuration parameter is not defined.
     * @return a class by the name of the configuration entry, or defaultOut.
     */
    private <T> Class<? extends T> getClass(final String configurationKey,
                                            final Class<T> mustExtend,
                                            final Class<?> defaultOut)
    {
        try {
            final String value = this.source.getProperty(configurationKey, String.class);
            if (value != null) {
                return Class.forName(value).asSubclass(mustExtend);
            }
        } catch (Exception e) {
            this.logger.info("Unable to read configuration for [" + configurationKey + "] using default.");
        }
        return defaultOut.asSubclass(mustExtend);
    }
}
