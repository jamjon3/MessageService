grails.servlet.version = "2.5" // Change depending on target container compliance (2.5 or 3.0)
grails.project.class.dir = "target/classes"
grails.project.test.class.dir = "target/test-classes"
grails.project.test.reports.dir = "target/test-reports"
grails.project.target.level = 1.6
grails.project.source.level = 1.6
grails.project.war.file = "target/${appName}.war"

grails.project.dependency.resolution = {
    inherits("global")
    log "warn" // log level of Ivy resolver, either 'error', 'warn', 'info', 'debug' or 'verbose'
    checksums true // Whether to verify checksums on resolve
    legacyResolve true

    repositories {
        inherits true // Whether to inherit repository definitions from plugins
        grailsPlugins()
        grailsHome()
        mavenLocal()
        grailsCentral()
        mavenCentral()
   }
    dependencies {
      compile "commons-lang:commons-lang:2.5"
    }

    plugins {
        compile ":mongodb:1.2.0"
        compile ":spring-security-core:1.2.7.3"
        compile ":spring-security-cas-usf:1.2.0"
        compile ":spring-security-ldap:1.0.6"

/*
        compile ":resources:1.2.RC2"
        runtime ":cached-resources:1.0"
        runtime ":angularjs-resources:1.0.2"
        runtime ":twitter-bootstrap:2.3.2"
*/
        build ":tomcat:$grailsVersion"
    }
}
