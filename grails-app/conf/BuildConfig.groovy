grails.servlet.version = "3.0" // Change depending on target container compliance (2.5 or 3.0)
grails.project.class.dir = "target/classes"
grails.project.test.class.dir = "target/test-classes"
grails.project.test.reports.dir = "target/test-reports"
grails.project.target.level = 1.7
grails.project.source.level = 1.7
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
        mavenRepo 'http://repo.spring.io/milestone'
        mavenRepo 'https://oss.sonatype.org/content/repositories/snapshots'
   }
    dependencies {
      compile 'commons-lang:commons-lang:2.5'
      compile 'org.pac4j:pac4j-core:1.5.0'
      compile 'org.pac4j:pac4j-oauth:1.5.0'
    }

    plugins {
        compile ":mongodb:1.2.0"
        compile ":spring-security-core:2.0-RC2"
        compile ":spring-security-ldap:2.0-RC2"
        compile ":spring-security-rest:1.3.2", {
            excludes: 'spring-security-core'
        }

        build ":tomcat:$grailsVersion"
    }
}
