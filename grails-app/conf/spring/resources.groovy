import edu.usf.cims.MessageService.LdapUserDetailsContextMapper
import edu.usf.cims.MessageService.MessageContainerType


beans = {
   ldapUserDetailsMapper(LdapUserDetailsContextMapper) {
      // bean attributes
   }
   messageContainerType(MessageContainerType)
}

