package org.xwiki.velocity;

import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.apache.velocity.VelocityContext;
import org.xwiki.component.logging.AbstractLogEnabled;
import org.xwiki.component.phase.Initializable;
import org.xwiki.component.phase.InitializationException;

public class DefaultVelocityContextFactory extends AbstractLogEnabled
    implements VelocityContextFactory, Initializable
{
    private Properties properties;

    private Map<String, Object> tools;
    
    /**
     * {@inheritDoc}
     * @see Initializable#initialize()
     */
    public void initialize() throws InitializationException
    {
        this.tools = new HashMap<String, Object>();
        
        // Instantiate Velocity tools
        if (this.properties != null) {
            for (Enumeration props = this.properties.propertyNames(); props.hasMoreElements();) {
                String key = props.nextElement().toString();
                String value = this.properties.getProperty(key);
                Object toolInstance;
                try {
                    toolInstance = Class.forName(value).newInstance();
                } catch (Exception e) {
                    throw new InitializationException("Failed to initialize tool [" 
                        + value + "]", e);
                }
                this.tools.put(key, toolInstance);
                getLogger().debug("Setting tool [" + key + "] = [" + value + "]");
            }
        }
    }
    
    public VelocityContext createContext()
    {
        return new VelocityContext(this.tools);
    }

}
