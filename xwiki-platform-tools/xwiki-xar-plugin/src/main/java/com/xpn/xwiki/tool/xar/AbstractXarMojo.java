package com.xpn.xwiki.tool.xar;

import java.io.File;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.codehaus.plexus.archiver.zip.ZipUnArchiver;
import org.codehaus.plexus.components.io.fileselectors.FileSelector;
import org.codehaus.plexus.components.io.fileselectors.IncludeExcludeFileSelector;
import org.codehaus.plexus.logging.Logger;
import org.codehaus.plexus.logging.console.ConsoleLogger;

/**
 * Base class for xar and unxar mojos.
 * 
 * @version $Id: $
 */
abstract class AbstractXarMojo extends AbstractMojo
{
    /**
     * Open hook.
     */
    protected static final String HOOK_OPEN = "[";

    /**
     * Close hook.
     */
    protected static final String HOOK_CLOSE = "]";

    /**
     * The name of the file in the package when to find general informations.
     */
    protected static final String PACKAGE_XML = "package.xml";

    /**
     * List of files to include. Specified as fileset patterns.
     *
     * @parameter
     */
    protected String[] includes;
 
    /**
     * List of files to exclude. Specified as fileset patterns.
     *
     * @parameter
     */
    protected String[] excludes;
    
	/**
	 * Default excludes
	 */
    private static final String[] DEFAULT_EXCLUDES = null;

	/**
	 * Default includes
	 */
    private static final String[] DEFAULT_INCLUDES = new String[] { "**/**" };
    
    /**
     * @return the includes
     */
    protected String[] getIncludes()
    {
        if ( includes != null && includes.length > 0 )
        {
            return includes;
        }
        return DEFAULT_INCLUDES;
    }

    /**
     * @return the excludes
     */
    protected String[] getExcludes()
    {
        if ( excludes != null && excludes.length > 0 )
        {
            return excludes;
        }
        return DEFAULT_EXCLUDES; 
    }
    
    /**
     * Unpacks the XAR file (exclude the package.xml file if it exists).
     * 
     * @param file the file to be unpacked.
     * @param location the location where to put the unpacket files.
     * @param logName the name use with {@link ConsoleLogger}.
     * @param overwrite indicate if extracted files has to overwrite existing ones.
     * @throws MojoExecutionException error when unpacking the file.
     */
    protected void unpack(File file, File location, String logName, boolean overwrite)
        throws MojoExecutionException
    {
        try {
            ZipUnArchiver unArchiver = new ZipUnArchiver();
            unArchiver.enableLogging(new ConsoleLogger(Logger.LEVEL_ERROR, logName));
            unArchiver.setSourceFile(file);
            unArchiver.setDestDirectory(location);
            
            FileSelector[] selectors;
            
            IncludeExcludeFileSelector fs = new IncludeExcludeFileSelector();
            fs.setIncludes(getIncludes());
            fs.setExcludes(getExcludes());
                        
            // Ensure that we don't overwrite XML document files present in this project since
            // we want those to be used and not the ones in the dependent XAR.
            unArchiver.setOverwrite(overwrite);

            if (!overwrite) {
                // Do not unpack any package.xml file in dependant XARs. We'll generate a complete
                // one automatically.
                IncludeExcludeFileSelector fs2 = new IncludeExcludeFileSelector();
                fs2.setExcludes(new String[]{PACKAGE_XML});
                selectors = new FileSelector[]{fs, fs2};
            }
            else {
                selectors = new FileSelector[]{fs};
            }

            unArchiver.setFileSelectors(selectors);
            
            unArchiver.extract();
        } catch (Exception e) {
            throw new MojoExecutionException("Error unpacking file " + HOOK_OPEN + file
                + HOOK_CLOSE + " to " + HOOK_OPEN + location + HOOK_CLOSE, e);
        }
    }
}
