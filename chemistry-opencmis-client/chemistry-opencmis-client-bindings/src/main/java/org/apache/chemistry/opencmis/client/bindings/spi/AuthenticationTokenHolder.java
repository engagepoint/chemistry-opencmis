package org.apache.chemistry.opencmis.client.bindings.spi;

import java.io.Serializable;

public interface AuthenticationTokenHolder<T> extends Serializable {

    T getToken();

    boolean isExpired();
}