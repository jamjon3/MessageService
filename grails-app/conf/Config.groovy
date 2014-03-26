grails.config.locations = [ "file:/usr/local/etc/grails/MessageService.groovy"]

grails.project.groupId = MessageService // change this to alter the default package name and Maven publishing destination
grails.mime.file.extensions = true // enables the parsing of file extensions from URLs into the request format
grails.mime.use.accept.header = true // Had to turn this on so it would negotiate content types JPJ
grails.mime.types = [ html: ['text/html','application/xhtml+xml'],
                      xml: ['text/xml', 'application/xml'],
                      text: 'text/plain',
                      js: 'text/javascript',
                      rss: 'application/rss+xml',
                      atom: 'application/atom+xml',
                      css: 'text/css',
                      csv: 'text/csv',
                      all: '*/*',
                      json: ['application/json','text/json'],
                      form: 'application/x-www-form-urlencoded',
                      multipartForm: 'multipart/form-data'
                    ]

// URL Mapping Cache Max Size, defaults to 5000
//grails.urlmapping.cache.maxsize = 1000

// What URL patterns should be processed by the resources plugin
grails.resources.adhoc.patterns = ['/images/*', '/css/*', '/js/*', '/plugins/*']

// The default codec used to encode data with ${}
grails.views.default.codec = "none" // none, html, base64
grails.views.gsp.encoding = "UTF-8"
grails.converters.encoding = "UTF-8"
// enable Sitemesh preprocessing of GSP pages
grails.views.gsp.sitemesh.preprocess = true
// scaffolding templates configuration
grails.scaffolding.templates.domainSuffix = 'Instance'

// Set to false to use the new Grails 1.2 JSONBuilder in the render method
grails.json.legacy.builder = false
// enabled native2ascii conversion of i18n properties files
grails.enable.native2ascii = true
// packages to include in Spring bean scanning
grails.spring.bean.packages = []
// whether to disable processing of multi part requests
grails.web.disable.multipart=false

// request parameters to mask when logging exceptions
grails.exceptionresolver.params.exclude = ['password']

// set per-environment serverURL stem for creating absolute links
environments {
    development {
        grails.logging.jul.usebridge = true
    }
    production {
        grails.logging.jul.usebridge = false
        // TODO: grails.serverURL = "http://www.changeme.com"
    }
}

///////////////////////////////////////////////////////////////////////////////////////////////
// log4j configuration

//Use the Tomcat log directory
def catalinaBase = System.properties.getProperty('catalina.base')
if (!catalinaBase) catalinaBase = '.'   // just in case
def logDirectory = "${catalinaBase}/logs/MessageService"
def statsLogFile = "${logDirectory}/perfStats.log"
def perfLogFile = "${logDirectory}/perfStats.log"


// default for all environments
log4j = { root ->
     appenders {
            rollingFile name:'stdout', file:"${logDirectory}/MessageService.log".toString(), maxFileSize:'100MB'
            rollingFile name:'stacktrace', file:"${logDirectory}/stacktrace.log".toString(), maxFileSize:'100MB'


    }

    error 'org.codehaus.groovy.grails.web.servlet',  //  controllers
          'org.codehaus.groovy.grails.web.pages', //  GSP
          'org.codehaus.groovy.grails.web.sitemesh', //  layouts
          'org.codehaus.groovy.grails.web.mapping.filter', // URL mapping
          'org.codehaus.groovy.grails.web.mapping', // URL mapping
          'org.codehaus.groovy.grails.commons', // core / classloading
          'org.codehaus.groovy.grails.plugins', // plugins
          'org.codehaus.groovy.grails.orm.hibernate', // hibernate integration
          'org.springframework',
          'groovyx.net.http.ParserRegistry'

    info 'grails.app'

    debug 'edu.usf'

    root.level = org.apache.log4j.Level.INFO
}
/////////////////////////////////////////////////////////////////////////////

environments {
    development {

    }
    test {
      grails.plugins.springsecurity.active = false
    }
    production {

    }
}

grails.plugins.springsecurity.userLookup.userDomainClassName = 'edu.usf.cims.UsfCasUser'
grails.plugins.springsecurity.securityConfigType = "Annotation"
grails.plugins.springsecurity.sessionFixationPrevention.alwaysCreateSession = true
grails.plugins.springsecurity.requestCache.createSession = true

grails.plugins.springsecurity.cas.active = true
grails.plugins.springsecurity.cas.sendRenew = false
grails.plugins.springsecurity.cas.key = '2c5b4d75940033d70035f0b10d1b8e65' //unique value for each app
grails.plugins.springsecurity.cas.artifactParameter = 'ticket'
grails.plugins.springsecurity.cas.serviceParameter = 'service'
grails.plugins.springsecurity.cas.filterProcessesUrl = '/j_spring_cas_security_check'
grails.plugins.springsecurity.cas.proxyReceptorUrl = '/secure/receptor'
grails.plugins.springsecurity.cas.useSingleSignout = false
grails.plugins.springsecurity.cas.driftTolerance = 120000
grails.plugins.springsecurity.cas.loginUri = '/login'
grails.plugins.springsecurity.cas.useSamlValidator = true
grails.plugins.springsecurity.cas.authorityAttribute = 'eduPersonEntitlement'

//Update these for your environment
grails.plugins.springsecurity.cas.serverUrlPrefix = 'https://authtest.it.usf.edu'
grails.plugins.springsecurity.cas.serviceUrl = 'http://localhost:8080/MessageService/j_spring_cas_security_check'
grails.plugins.springsecurity.cas.proxyCallbackUrl = 'http://localhost:8080/MessageService/secure/receptor'
grails.plugins.springsecurity.ldap.context.managerDn = ''
grails.plugins.springsecurity.ldap.context.managerPassword = ''
grails.plugins.springsecurity.ldap.context.server = ''
grails.plugins.springsecurity.ldap.search.base = ''

grails.plugins.springsecurity.ldap.useRememberMe = false
grails.plugins.springsecurity.ldap.auth.hideUserNotFoundExceptions = true

grails.plugins.springsecurity.ldap.search.filter="uid={0}"
grails.plugins.springsecurity.ldap.search.searchSubtree = true

grails.plugins.springsecurity.ldap.authorities.ignorePartialResultException = false
grails.plugins.springsecurity.ldap.authorities.prefix = 'ROLE_'
grails.plugins.springsecurity.ldap.authorities.retrieveGroupRoles = false

grails.plugins.springsecurity.ldap.mapper.userDetailsClass = 'inetOrgPerson'
grails.plugins.springsecurity.ldap.mapper.convertToUpperCase = true

grails.plugins.springsecurity.useBasicAuth = true
grails.plugins.springsecurity.basic.realmName = "Message Service REST API"

/*
  Enable HTTP-BASIC for /basic/* URLs and CAS everything else
*/
grails.plugins.springsecurity.filterChain.chainMap = [
  '/basic/**': 'JOINED_FILTERS,-casAuthenticationFilter,-exceptionTranslationFilter',
  '/**': 'JOINED_FILTERS,-basicAuthenticationFilter,-basicExceptionTranslationFilter'
]
