package org.xadisk.connector;

import javax.resource.spi.endpoint.MessageEndpointFactory;

public class EndPointActivation {

    private final MessageEndpointFactory messageEndpointFactory;
    private final ActivationSpecImpl activationSpecImpl;

    public EndPointActivation(MessageEndpointFactory messageEndpointFactory, ActivationSpecImpl activationSpecImpl) {
        this.messageEndpointFactory = messageEndpointFactory;
        this.activationSpecImpl = activationSpecImpl;
    }

    public MessageEndpointFactory getMessageEndpointFactory() {
        return messageEndpointFactory;
    }

    public ActivationSpecImpl getActivationSpecImpl() {
        return activationSpecImpl;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof EndPointActivation) {
            EndPointActivation epActivation = (EndPointActivation) obj;
            return epActivation.activationSpecImpl.equals(this.activationSpecImpl) &&
                    epActivation.messageEndpointFactory.equals(this.messageEndpointFactory);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return messageEndpointFactory.hashCode() + activationSpecImpl.hashCode();
    }
}
