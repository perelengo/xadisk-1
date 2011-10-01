package org.xadisk.bridge.proxies.impl;

import org.xadisk.filesystem.TransactionInformation;

public class RemoteTransactionInformation extends TransactionInformation {

    private static final long serialVersionUID = 1L;
    
    private String serverAddress;
    private Integer serverPort;

    public RemoteTransactionInformation(TransactionInformation transactionInformation, String serverAddress, Integer serverPort) {
        super(transactionInformation.getGlobalTransactionId(), transactionInformation.getBranchQualifier(), transactionInformation.getFormatId());
        this.serverAddress = serverAddress;
        this.serverPort = serverPort;
    }

    public String getServerAddress() {
        return serverAddress;
    }

    public Integer getServerPort() {
        return serverPort;
    }

    @Override
    public boolean equals(Object obj) {
        if(obj instanceof RemoteTransactionInformation) {
            RemoteTransactionInformation remoteTransactionInformation = (RemoteTransactionInformation) obj;
            return super.equals(obj) && remoteTransactionInformation.getServerAddress().equals(serverAddress)
                    && remoteTransactionInformation.getServerPort().equals(serverPort);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return super.hashCode();
    }
}
