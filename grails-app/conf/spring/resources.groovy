import edu.usf.cims.MessageService.LdapUserDetailsContextMapper
import edu.usf.cims.MessageService.MessageContainerType
import edu.usf.cims.MessageService.MemcachedTokenStorageService


beans = {
   ldapUserDetailsMapper(LdapUserDetailsContextMapper) { }
   messageContainerType(MessageContainerType) { }
   tokenStorageService(MemcachedTokenStorageService) {
      memcachedClient = ref('memcachedClient')
      expiration = conf.rest.token.storage.memcached.expiration
    }
}
