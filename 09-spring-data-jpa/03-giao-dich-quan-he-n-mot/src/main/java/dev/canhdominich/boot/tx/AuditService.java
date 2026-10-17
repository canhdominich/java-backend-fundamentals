package dev.canhdominich.boot.tx;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Service
public class AuditService {

    private final AuditRepository audits;

    public AuditService(AuditRepository audits) {
        this.audits = audits;
    }

    @Transactional(propagation = Propagation.REQUIRED)
    public void logJoining(String message) {
        audits.save(new AuditEntry(message));
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logInNewTransaction(String message) {
        audits.save(new AuditEntry(message));
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public boolean newTransactionIsActive() {
        return TransactionSynchronizationManager.isActualTransactionActive();
    }

    @Transactional
    public void failInside() {
        throw new IllegalStateException("failed inside the audit service");
    }
}
