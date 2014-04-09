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

    root.level = org.apache.log4j.Level.DEBUG
}
/////////////////////////////////////////////////////////////////////////////

environments {
    development {

    }
    test {
      grails.plugin.springsecurity.active = false
    }
    production {

    }
}

grails.plugin.springsecurity.userLookup.userDomainClassName = 'edu.usf.cims.UsfCasUser'
grails.plugin.springsecurity.securityConfigType = "Annotation"
grails.plugin.springsecurity.sessionFixationPrevention.alwaysCreateSession = true
grails.plugin.springsecurity.requestCache.createSession = true

grails.plugin.springsecurity.ldap.context.managerDn = ''
grails.plugin.springsecurity.ldap.context.managerPassword = ''
grails.plugin.springsecurity.ldap.context.server = ''
grails.plugin.springsecurity.ldap.search.base = ''

grails.plugin.springsecurity.ldap.useRememberMe = false
grails.plugin.springsecurity.ldap.auth.hideUserNotFoundExceptions = true

grails.plugin.springsecurity.ldap.search.filter="uid={0}"
grails.plugin.springsecurity.ldap.search.searchSubtree = true

grails.plugin.springsecurity.ldap.authorities.ignorePartialResultException = false
grails.plugin.springsecurity.ldap.authorities.prefix = 'ROLE_'
grails.plugin.springsecurity.ldap.authorities.retrieveGroupRoles = false

grails.plugin.springsecurity.ldap.mapper.userDetailsClass = 'inetOrgPerson'
grails.plugin.springsecurity.ldap.mapper.convertToUpperCase = true

grails.plugin.springsecurity.useBasicAuth    = true
grails.plugin.springsecurity.basic.realmName = "Message Service REST API"

grails.plugin.springsecurity.rest.login.endpointUrl                 = '/token/login'
grails.plugin.springsecurity.rest.logout.endpointUrl                = '/token/logout'
grails.plugin.springsecurity.rest.login.useRequestParamsCredentials = false
grails.plugin.springsecurity.rest.login.useJsonCredentials          = true
grails.plugin.springsecurity.rest.login.usernamePropertyName        = 'username'
grails.plugin.springsecurity.rest.login.passwordPropertyName        = 'password'

grails.plugin.springsecurity.rest.token.generation.useSecureRandom = true
grails.plugin.springsecurity.rest.token.validation.headerName      = 'X-Auth-Token'
grails.plugin.springsecurity.rest.token.validation.endpointUrl     = '/token/validate'

grails.plugin.springsecurity.rest.token.storage.useMemcached         = true
grails.plugin.springsecurity.rest.token.storage.memcached.hosts      = 'localhost:11211'
grails.plugin.springsecurity.rest.token.storage.memcached.username   = ''
grails.plugin.springsecurity.rest.token.storage.memcached.password   = ''
grails.plugin.springsecurity.rest.token.storage.memcached.expiration = 3600

/*
  Enable HTTP-BASIC for /basic/* URLs and CAS everything else
*/
grails.plugin.springsecurity.filterChain.chainMap = [
  '/basic/**': 'JOINED_FILTERS,-restAuthenticationFilter,-restTokenValidationFilter,-restLogoutFilter,-exceptionTranslationFilter',
  '/token/**': 'JOINED_FILTERS,-basicAuthenticationFilter,-basicExceptionTranslationFilter'
]

grails.plugin.springsecurity.controllerAnnotations.staticRules = [
   '/':               ['permitAll'],
   '/index':          ['permitAll'],
   '/index.gsp':      ['permitAll'],
   '/**/favicon.ico': ['permitAll'],
]

