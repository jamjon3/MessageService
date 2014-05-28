package edu.usf.cims.MessageService

import edu.usf.cims.MessageService.AuditEntry
import org.bson.types.ObjectId

class AuditService {
  def grailsApplication

    def writeAuditEntry(Map auditDetails) {

      // If auditing is disabled, just exit
      if (! grailsApplication.config.audit.enabled) return true

      if(grailsApplication.config.audit.target == 'mongodb') {

        def auditEntry = new AuditEntry(auditDetails)

        if(! auditEntry.save(flush: true)) {
          log.warn("Failed saving auditMessage for action ${auditDetails.action} on ${auditDetails.target}")
          return false
        } else {
          return true
        }
      }

      if(grailsApplication.config.audit.target == 'log4j') {

        def formattedDate = new Date().format("E, dd MMM yyyy HH:mm:ss Z")
        def who = auditDetails.actor
        def what = auditDetails.action
        def reason = ": ${auditDetails.reason}" ?: ''
        def containerType = auditDetails.containerType ?: ''
        def containerName = auditDetails.containerName ?: ''
        def details = "${containerType} ${containerName} ${reason}"
        def ipAddress = auditDetails.ipAddress

        def separator = grailsApplication.config.audit.separator ?: "|"
        def auditData = formattedDate + separator + grailsApplication.metadata['app.name'] + separator + who + separator + ipAddress + separator + what + separator + details

        log.info(auditData)
        return true
      }

    }
}
