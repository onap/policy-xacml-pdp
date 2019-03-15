package org.onap.policy.pdp.xacml.application.common;

public class OnapPolicyFinderFactoryException extends Exception {

    private static final long serialVersionUID = -1643639780835366726L;

    public OnapPolicyFinderFactoryException() {
        super();
    }

    public OnapPolicyFinderFactoryException(String message) {
        super(message);
    }

    public OnapPolicyFinderFactoryException(Throwable cause) {
        super(cause);
    }

    public OnapPolicyFinderFactoryException(String message, Throwable cause) {
        super(message, cause);
    }

    public OnapPolicyFinderFactoryException(String message, Throwable cause, boolean enableSuppression,
            boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }

}
