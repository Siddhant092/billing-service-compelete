package com.broadnet.billing.exception;

/**
 * Thrown when pessimistic lock fails or times out
 * HTTP 409
 */
public class PessimisticLockingException extends BillingException {
    public PessimisticLockingException(String message) {
        super(message, 409, "PESSIMISTIC_LOCKING_FAILURE");
    }
}
