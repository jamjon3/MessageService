import edu.usf.MessageService.LdapUserDetailsContextMapper
import edu.usf.MessageService.MessageContainerType


beans = {
   ldapUserDetailsMapper(LdapUserDetailsContextMapper) {
      // bean attributes
   }
   messageContainerType(MessageContainerType)
}

