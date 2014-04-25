package edu.usf.cims.MessageService

import edu.usf.cims.MessageService.AuditEntry
import org.bson.types.ObjectId

class AuditService {
  def grailsApplication

    def writeAuditEntry(Map auditDetails) {

      // If auditing is disabled, just exit
      if (grailsApplication.config.audit.disabled) return true

      def auditEntry = new AuditEntry(auditDetails)

      if(! auditEntry.save(flush: true)) {
        log.warn("Failed saving auditMessage for action ${auditDetails.action} on ${auditDetails.target}")
        return false
      } else {
        return true
      }

    }
}
