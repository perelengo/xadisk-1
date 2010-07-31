package org.xadisk.connector.inbound;

import java.io.File;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Iterator;
import javax.resource.ResourceException;
import javax.resource.spi.ActivationSpec;
import javax.resource.spi.InvalidPropertyException;
import javax.resource.spi.ResourceAdapter;
import org.xadisk.filesystem.FileStateChangeEvent;

public class ActivationSpecImpl implements ActivationSpec, Serializable {

    private ResourceAdapter ra;
    private final HashMap<File, String> fileNamesAndInterests = new HashMap<File, String>(10);
    private static final String interestSymbol = "::";
    private static final String seperator = ",";
    private String fileNamesAndEventInterests;
    private Boolean areFilesRemote;
    private String remoteServerAddress;
    private Integer remoteServerPort;

    public ActivationSpecImpl() {
    }

    public void setFileNamesAndEventInterests(String filesNamesAndEventInterests) {
        this.fileNamesAndEventInterests = filesNamesAndEventInterests;
        setupFileNamesAndEventInterests(filesNamesAndEventInterests.split(seperator));
    }

    public String getFileNamesAndEventInterests() {
        return fileNamesAndEventInterests;
    }

    //we needed to make these types Strings as the config properties in ra.xml don't have type for a-specs.
    public String getAreFilesRemote() {
        return areFilesRemote.toString();
    }

    public void setAreFilesRemote(String areFilesRemote) {
        this.areFilesRemote = new Boolean(areFilesRemote);
    }

    public String getRemoteServerAddress() {
        return remoteServerAddress;
    }

    public void setRemoteServerAddress(String remoteServerAddress) {
        this.remoteServerAddress = remoteServerAddress;
    }

    public String getRemoteServerPort() {
        return remoteServerPort.toString();
    }

    public void setRemoteServerPort(String remoteServerPort) {
        this.remoteServerPort = new Integer(remoteServerPort);
    }

    private void setupFileNamesAndEventInterests(String fileNamesAndEventInterest[]) {
        this.fileNamesAndInterests.clear();
        for (int i = 0; i < fileNamesAndEventInterest.length; i++) {
            String temp[] = fileNamesAndEventInterest[i].split(interestSymbol);
            fileNamesAndInterests.put(new File(temp[0]), temp[1]);
        }
    }

    public ResourceAdapter getResourceAdapter() {
        return this.ra;
    }

    public void setResourceAdapter(ResourceAdapter ra) throws ResourceException {
        this.ra = ra;
    }

    public void validate() throws InvalidPropertyException {
        Iterator iter = fileNamesAndInterests.values().iterator();
        while (iter.hasNext()) {
            String interest = (String) iter.next();
            try {
                new Integer(interest);
            } catch (NumberFormatException nfe) {
                throw new InvalidPropertyException("Invalid event-interest specification : " + interest);
            }
        }
    }

    public boolean isEndpointInterestedIn(FileStateChangeEvent event) {
        String interested = fileNamesAndInterests.get(event.getFile());
        if (interested != null) {
            byte interestedBits = Byte.parseByte(interested, 2);
            byte queriedInterest = event.getEventType();
            if ((interestedBits & queriedInterest) > 0) {
                return true;
            }
        }
        return false;
    }
}
