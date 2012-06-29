/*
Copyright 2012 Adam Bien, adam-bien.com

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

  http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
*/
package org.connectorz.files.store;

import java.io.PrintWriter;
import java.util.LinkedList;
import java.util.List;
import javax.resource.ResourceException;
import static javax.resource.spi.ConnectionEvent.*;
import javax.resource.spi.*;
import javax.security.auth.Subject;
import javax.transaction.xa.XAResource;

public class GenericManagedConnection
        implements ManagedConnection, LocalTransaction {

    private ManagedConnectionFactory mcf;
    private PrintWriter out;
    private FileBucket fileConnection;
    private ConnectionRequestInfo connectionRequestInfo;
    private List<ConnectionEventListener> listeners;
    private final String rootDirectory;

    GenericManagedConnection(PrintWriter out,String rootDirectory,ManagedConnectionFactory mcf, ConnectionRequestInfo connectionRequestInfo) {
        this.out = out;
        this.rootDirectory = rootDirectory;
        out.println("#GenericManagedConnection");
        this.mcf = mcf;
        this.connectionRequestInfo = connectionRequestInfo;
        this.listeners = new LinkedList<ConnectionEventListener>();
    }

    public Object getConnection(Subject subject, ConnectionRequestInfo connectionRequestInfo)
            throws ResourceException {
        out.println("#GenericManagedConnection.getConnection");
        fileConnection = new FileBucket(out,this.rootDirectory,this, connectionRequestInfo);
        return fileConnection;
    }

    public void destroy() {
        out.println("#GenericManagedConnection.destroy");
        this.fileConnection.destroy();
    }

    public void cleanup() {
        out.println("#GenericManagedConnection.cleanup");
    }

    public void associateConnection(Object connection) {
        out.println("#GenericManagedConnection.associateConnection " + connection);
        this.fileConnection = (FileBucket) connection;

    }

    public void addConnectionEventListener(ConnectionEventListener listener) {
        out.println("#GenericManagedConnection.addConnectionEventListener");
        this.listeners.add(listener);
    }

    public void removeConnectionEventListener(ConnectionEventListener listener) {
        out.println("#GenericManagedConnection.removeConnectionEventListener");
        this.listeners.remove(listener);
    }

    public XAResource getXAResource()
            throws ResourceException {
        out.println("#GenericManagedConnection.getXAResource");
        return null;
    }

    public LocalTransaction getLocalTransaction() {
        out.println("#GenericManagedConnection.getLocalTransaction");
        return this;
    }

    public ManagedConnectionMetaData getMetaData()
            throws ResourceException {
        out.println("#GenericManagedConnection.getMetaData");
        return new ManagedConnectionMetaData() {

            public String getEISProductName()
                    throws ResourceException {
                out.println("#MyConnectionMetaData.getEISProductName");
                return "Generic JCA";
            }

            public String getEISProductVersion()
                    throws ResourceException {
                out.println("#MyConnectionMetaData.getEISProductVersion");
                return "1.0";
            }

            public int getMaxConnections()
                    throws ResourceException {
                out.println("#MyConnectionMetaData.getMaxConnections");
                return 5;
            }

            public String getUserName()
                    throws ResourceException {
                return null;
            }
        };
    }

    public void setLogWriter(PrintWriter out)
            throws ResourceException {
        System.out.println("#GenericManagedConnection.setLogWriter");
        this.out = out;
    }

    public PrintWriter getLogWriter()
            throws ResourceException {
        System.out.println("#GenericManagedConnection.getLogWriter");
        return out;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final GenericManagedConnection other = (GenericManagedConnection) obj;
        if (this.connectionRequestInfo != other.connectionRequestInfo && (this.connectionRequestInfo == null || !this.connectionRequestInfo.equals(other.connectionRequestInfo))) {
            return false;
        }
        return true;
    }

    @Override
    public int hashCode() {
        int hash = 5;
        hash = 83 * hash + (this.connectionRequestInfo != null ? this.connectionRequestInfo.hashCode() : 0);
        return hash;
    }

    public ConnectionRequestInfo getConnectionRequestInfo() {
        return connectionRequestInfo;
    }

    public void begin() throws ResourceException {
        this.fileConnection.begin();
        this.fireConnectionEvent(LOCAL_TRANSACTION_STARTED);
    }

    public void commit() throws ResourceException {
        this.fileConnection.commit();
        this.fireConnectionEvent(LOCAL_TRANSACTION_COMMITTED);
    }

    public void rollback() throws ResourceException {
        this.fileConnection.rollback();
        this.fireConnectionEvent(LOCAL_TRANSACTION_ROLLEDBACK);
    }

    public void fireConnectionEvent(int event) {
        ConnectionEvent connnectionEvent = new ConnectionEvent(this, event);
        connnectionEvent.setConnectionHandle(this.fileConnection);
        for (ConnectionEventListener listener : this.listeners) {
            switch (event) {
                case LOCAL_TRANSACTION_STARTED:
                    listener.localTransactionStarted(connnectionEvent);
                    break;
                case LOCAL_TRANSACTION_COMMITTED:
                    listener.localTransactionCommitted(connnectionEvent);
                    break;
                case LOCAL_TRANSACTION_ROLLEDBACK:
                    listener.localTransactionRolledback(connnectionEvent);
                    break;
                case CONNECTION_CLOSED:
                    listener.connectionClosed(connnectionEvent);
                    break;
                default:
                    throw new IllegalArgumentException("Unknown event: " + event);
            }
        }
    }

    public void close() {
        this.fireConnectionEvent(CONNECTION_CLOSED);
    }
}