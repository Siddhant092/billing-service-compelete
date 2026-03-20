package com.broadnet.billing.exception;

/**
 * Thrown when optimistic locking fails (version conflict)
 * HTTP 409
 */
public class OptimisticLockingException extends BillingException {
    public OptimisticLockingException(String entityName, Long entityId) {
        super(String.format("Version conflict for %s with ID: %d. Please retry.", entityName, entityId),
                409, "OPTIMISTIC_LOCKING_FAILURE");
    }
}
