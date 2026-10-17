package dev.canhdominich.boot.jdbc;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RegistrationService {

    private final StudentDao dao;

    public RegistrationService(StudentDao dao) {
        this.dao = dao;
    }

    public void registerTwoWithoutTransaction(String first, String second) {
        dao.insert(first, first + "@example.com");
        throw new IllegalStateException("failure after the first insert");
    }

    @Transactional
    public void registerTwoInTransaction(String first, String second) {
        dao.insert(first, first + "@example.com");
        throw new IllegalStateException("failure after the first insert");
    }
}
