package edu.usf.MessageService

import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.userdetails.User
import org.apache.commons.logging.LogFactory
 
class LdapUserDetails extends User {
        private static final log = LogFactory.getLog(this)
 
        private final HashMap _attributes
 
        LdapUserDetails(String username,
                                String password,
                                boolean enabled,
                                boolean accountNonExpired,
                                boolean credentialsNonExpired,
                                boolean accountNonLocked,
                                Collection<GrantedAuthority> authorities,
                                HashMap attributes) {
                super(username, password, enabled, accountNonExpired, credentialsNonExpired,
                                accountNonLocked, authorities)
                _attributes = attributes

                log.debug("ROLES: ${authorities}")
                log.debug("ATTRIBUTES: ${_attributes}")
        }
        
        Map getAttributes() { _attributes }
        
        String findByUsername(username){
                def user = null
                user
        }
}