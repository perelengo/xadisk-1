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

public class XADiskActivationSpecImpl implements ActivationSpec, Serializable {

    private transient ResourceAdapter ra;
    private final HashMap<File, String> fileNamesAndInterests = new HashMap<File, String>(10);
    private static final String interestSymbol = "::";
    private static final String seperator = "\\|";
    private String fileNamesAndEventInterests;
    private Boolean areFilesRemote;
    private String remoteServerAddress;
    private Integer remoteServerPort;
    private boolean originalObjectIsRemote = false;
    private int originalObjectsHashCode = this.hashCode();//this will get serialized too.

    public XADiskActivationSpecImpl() {
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
        this.areFilesRemote = Boolean.valueOf(areFilesRemote);
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

    public void setOriginalObjectIsRemote(boolean originalObjectIsRemote) {
        this.originalObjectIsRemote = originalObjectIsRemote;
    }

    public void validate() throws InvalidPropertyException {
        Iterator iter = fileNamesAndInterests.values().iterator();
        while (iter.hasNext()) {
            String interest = (String) iter.next();
            try {
                Integer.valueOf(interest);
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

    /*
     * From JCA Spec:
    "These objects, in general, should not override the default equals and hashCode methods."
     * From Java API:
     * "As much as is reasonably practical, the hashCode method defined by class Object
    does return distinct integers for distinct objects."
     * We always compare the asSpec as part of Activation comparison, so by chance if
     * remote asSpec equals the local one, the MEF won't equal due to different classes.
     * But, anyway, for clarity keep a separate flag to know if remote.
     */
    @Override
    public boolean equals(Object obj) {
        if (obj instanceof XADiskActivationSpecImpl) {
            XADiskActivationSpecImpl that = (XADiskActivationSpecImpl) obj;
            return this.originalObjectIsRemote == that.originalObjectIsRemote
                    && this.originalObjectsHashCode == that.originalObjectsHashCode;
        }
        return false;
    }

    @Override
    public int hashCode() {
        return this.originalObjectsHashCode;
    }

    public int getOriginalObjectsHashCode() {
        return originalObjectsHashCode;
    }

    public void setOriginalObjectsHashCode(int originalObjectsHashCode) {
        this.originalObjectsHashCode = originalObjectsHashCode;
    }
}
