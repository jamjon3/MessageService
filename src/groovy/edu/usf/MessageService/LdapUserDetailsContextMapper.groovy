package edu.usf.MessageService
import org.springframework.ldap.core.DirContextAdapter
import org.springframework.ldap.core.DirContextOperations
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.security.ldap.userdetails.UserDetailsContextMapper
import org.springframework.security.core.authority.GrantedAuthorityImpl

class LdapUserDetailsContextMapper implements UserDetailsContextMapper {

   private static final String NON_EXISTENT_PASSWORD_VALUE = "NO_PASSWORD";

   private static final Boolean ADD_PREFIX = true
   private static final String PREFIX = "ROLE_"
   private static final String ROLE_ATTR = "edupersonentitlement"

   /**
   * Some Spring Security classes (e.g. RoleHierarchyVoter) expect at least one role, so
   * we give a user with no granted roles this one which gets past that restriction but
   * doesn't grant anything.
   */
   private static final List NO_ROLES = [new GrantedAuthorityImpl("LDAP_AUTH")]

   UserDetails mapUserFromContext(DirContextOperations ctx, String username, Collection authorities) {
    
      def ldapAuthorities = (ctx?.originalAttrs?.attrs[ROLE_ATTR]?.values)?:NO_ROLES

      //Prefix all authorities with 'ROLE_'
      List Prefixed_Roles = []
      ldapAuthorities.each() { authority ->
            if(authority instanceof String) Prefixed_Roles.add(new GrantedAuthorityImpl(PREFIX + authority.toUpperCase()))
      }
      
      def attributes = new HashMap()
      ctx.originalAttrs.attrs.each() { attribute ->
         attributes.put(attribute.key,attribute.value.values)
      }
      new LdapUserDetails(username, NON_EXISTENT_PASSWORD_VALUE, true, true, true, true, Prefixed_Roles, attributes)      
   }

   void mapUserToContext(UserDetails user, DirContextAdapter ctx) {
      throw new IllegalStateException("Only retrieving data from LDAP is currently supported")
   }
}