package dev.canhdominich.boot.tx;

import java.io.IOException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Service
public class TransferService {

    private final AccountRepository accounts;
    private final AuditService audit;

    public TransferService(AccountRepository accounts, AuditService audit) {
        this.accounts = accounts;
        this.audit = audit;
    }

    @Transactional
    public void debitThenFailUnchecked(long id, int amount) {
        Account account = accounts.findById(id).orElseThrow();
        account.setBalance(account.getBalance() - amount);
        throw new IllegalStateException("unchecked failure after the debit");
    }

    @Transactional
    public void debitThenFailChecked(long id, int amount) throws IOException {
        Account account = accounts.findById(id).orElseThrow();
        account.setBalance(account.getBalance() - amount);
        throw new IOException("checked failure after the debit");
    }

    @Transactional(rollbackFor = Exception.class)
    public void debitThenFailCheckedWithRollbackFor(long id, int amount) throws IOException {
        Account account = accounts.findById(id).orElseThrow();
        account.setBalance(account.getBalance() - amount);
        throw new IOException("checked failure after the debit");
    }

    @Transactional
    public void debitAuditJoiningThenFail(long id, int amount) {
        Account account = accounts.findById(id).orElseThrow();
        account.setBalance(account.getBalance() - amount);
        audit.logJoining("debit " + amount);
        throw new IllegalStateException("failure after the audit");
    }

    @Transactional
    public void debitAuditInNewTransactionThenFail(long id, int amount) {
        Account account = accounts.findById(id).orElseThrow();
        account.setBalance(account.getBalance() - amount);
        audit.logInNewTransaction("debit " + amount);
        throw new IllegalStateException("failure after the audit");
    }

    @Transactional(readOnly = true)
    public void changeInsideReadOnly(long id) {
        accounts.findById(id).orElseThrow().setBalance(-1);
    }

    @Transactional
    public void swallowFailureOfInnerTransaction() {
        try {
            audit.failInside();
        } catch (IllegalStateException e) {
            return;
        }
    }

    public boolean callsOwnTransactionalMethod() {
        return newTransactionIsActive();
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public boolean newTransactionIsActive() {
        return TransactionSynchronizationManager.isActualTransactionActive();
    }

    public boolean callsAnotherBeansTransactionalMethod() {
        return audit.newTransactionIsActive();
    }
}
