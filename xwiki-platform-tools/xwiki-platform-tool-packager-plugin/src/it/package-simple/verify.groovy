import java.io.*

// Verify that the build log doesn't contain low level logging since we disable that in the Mojo
def found = true
new File(basedir, "build.log").eachLine {
    line ->
        if (line.contains("ISPN000128: Infinispan version: Infinispan")
            || line.contains("Hibernate Commons Annotations")
            || line.contains("hibernate.properties not found")
            || line.contains("Reflections took"))
        {
            found = false
        }
}
assert found:"Too low-level logs in the build logs, our log configuration is no longer working and needs to be fixed!"