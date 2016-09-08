package communicator;

import jvmmonitor.model.CPULoadLog;
import jvmmonitor.model.GarbageCollectionLog;
import jvmmonitor.model.MemoryUsageLog;
import org.apache.log4j.Logger;
import org.wso2.carbon.databridge.agent.AgentHolder;
import org.wso2.carbon.databridge.agent.DataPublisher;
import org.wso2.carbon.databridge.agent.exception.DataEndpointAgentConfigurationException;
import org.wso2.carbon.databridge.agent.exception.DataEndpointAuthenticationException;
import org.wso2.carbon.databridge.agent.exception.DataEndpointConfigurationException;
import org.wso2.carbon.databridge.agent.exception.DataEndpointException;
import org.wso2.carbon.databridge.commons.exception.TransportException;
import org.wso2.carbon.databridge.commons.utils.DataBridgeCommonsUtils;

import java.io.File;
import java.io.FileNotFoundException;
import java.net.*;
import java.util.Enumeration;
import java.util.LinkedList;
import java.util.List;

/*
*  Copyright (c) ${date}, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
*
*  WSO2 Inc. licenses this file to you under the Apache License,
*  Version 2.0 (the "License"); you may not use this file except
*  in compliance with the License.
*  You may obtain a copy of the License at
*
*    http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing,
* software distributed under the License is distributed on an
* "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
* KIND, either express or implied.  See the License for the
* specific language governing permissions and limitations
* under the License.
*/

public class DASPublisher {

    private EventPublisher eventAgent;
    private String memoryStream;
    private String gcStream;
    private String cpuStream;
    private String gcLogStream;

    private DataPublisher dataPublisher;

    final static Logger logger = Logger.getLogger(DASPublisher.class);

    public DASPublisher(int defaultThriftPort, int defaultBinaryPort, String username, String password) throws SocketException, UnknownHostException, DataEndpointAuthenticationException, DataEndpointAgentConfigurationException, TransportException, DataEndpointException, DataEndpointConfigurationException {

        logger.info("Starting DAS HttpLog Agent");
        String currentDir = System.getProperty("user.dir");
        System.setProperty("javax.net.ssl.trustStore", currentDir + "/jvm-monitor-agent/src/main/resources/client-truststore.jks");
        System.setProperty("javax.net.ssl.trustStorePassword", "wso2carbon");

        AgentHolder.setConfigPath(getDataAgentConfigPath());
        String host = getLocalAddress().getHostAddress();

        String type = getProperty("type", "Thrift");
        int receiverPort = defaultThriftPort;
        if (type.equals("Binary")) {
            receiverPort = defaultBinaryPort;
        }
        int securePort = receiverPort + 100;

        String url = getProperty("url", "tcp://" + host + ":" + receiverPort);
        String authURL = getProperty("authURL", "ssl://" + host + ":" + securePort);
        username = getProperty("username", username);
        password = getProperty("password", password);

        dataPublisher = new DataPublisher(type, url, authURL, username, password);

        setMemoryStream();
        setGcStream();
        setCpuStream();
        setGcLogStream();

        eventAgent = new EventPublisher();

    }

    public void shutdownDataPublisher() throws DataEndpointException {
        dataPublisher.shutdown();
    }

    private void setMemoryStream() {
        String HTTPD_LOG_STREAM = "memoryStream";
        String VERSION = "1.0.0";
        memoryStream = DataBridgeCommonsUtils.generateStreamId(HTTPD_LOG_STREAM, VERSION);
    }

    private void setGcStream() {
        String HTTPD_LOG_STREAM = "gcStream";
        String VERSION = "1.0.0";
        gcStream = DataBridgeCommonsUtils.generateStreamId(HTTPD_LOG_STREAM, VERSION);
    }

    private void setCpuStream() {
        String HTTPD_LOG_STREAM = "cpuStream";
        String VERSION = "1.0.0";
        cpuStream = DataBridgeCommonsUtils.generateStreamId(HTTPD_LOG_STREAM, VERSION);
    }

    private void setGcLogStream() {
        String HTTPD_LOG_STREAM = "gcLogStream";
        String VERSION = "1.0.0";
        gcLogStream = DataBridgeCommonsUtils.generateStreamId(HTTPD_LOG_STREAM, VERSION);
    }

    public void publishMemoryData(long date, MemoryUsageLog memoryUsageLog) throws DataEndpointAuthenticationException,
            DataEndpointAgentConfigurationException,
            DataEndpointException,
            DataEndpointConfigurationException,
            TransportException {

        //HTTPD_LOG_STREAM = "memoryStream"
        //VERSION = "1.0.0"

        eventAgent.publishLogEvents(dataPublisher, memoryStream, date, memoryUsageLog);

    }

    public void publishGCData(LinkedList<GarbageCollectionLog> garbageCollectionLog) throws DataEndpointAuthenticationException,
            DataEndpointAgentConfigurationException,
            DataEndpointException,
            DataEndpointConfigurationException,
            TransportException {

        //HTTPD_LOG_STREAM = "gcStream"
        //VERSION = "1.0.0"

        while (!garbageCollectionLog.isEmpty()) {
            eventAgent.publishLogEvents(dataPublisher, gcStream, garbageCollectionLog.poll());
        }

    }

    public void publishCPUData(long date, CPULoadLog cpuLoadLog) throws DataEndpointAuthenticationException,
            DataEndpointAgentConfigurationException,
            DataEndpointException,
            DataEndpointConfigurationException,
            TransportException {

        //HTTPD_LOG_STREAM = "cpuStream"
        //VERSION = "1.0.0"

        eventAgent.publishLogEvents(dataPublisher, cpuStream, date, cpuLoadLog);

    }

    public void publishXXgcLogData(String fileName) throws TransportException,
            DataEndpointConfigurationException,
            FileNotFoundException,
            DataEndpointAuthenticationException,
            DataEndpointException,
            DataEndpointAgentConfigurationException {

        //HTTPD_LOG_STREAM = "gcLogStream"
        //VERSION = "1.0.0"

        eventAgent.publishLogEvents(dataPublisher, gcLogStream, fileName);

    }

    public static String getDataAgentConfigPath() {
        File filePath = new File("jvm-monitor-agent" + File.separator + "src" + File.separator + "main" + File.separator + "resources");
        if (!filePath.exists()) {
            filePath = new File("test" + File.separator + "resources");
        }
        if (!filePath.exists()) {
            filePath = new File("resources");
        }
        return filePath.getAbsolutePath() + File.separator + "data-agent-conf.xml";
    }

    public static InetAddress getLocalAddress() throws SocketException, UnknownHostException {
        Enumeration<NetworkInterface> ifaces = NetworkInterface.getNetworkInterfaces();
        while (ifaces.hasMoreElements()) {
            NetworkInterface iface = ifaces.nextElement();
            Enumeration<InetAddress> addresses = iface.getInetAddresses();

            while (addresses.hasMoreElements()) {
                InetAddress addr = addresses.nextElement();
                if (addr instanceof Inet4Address && !addr.isLoopbackAddress()) {
                    return addr;
                }
            }
        }
        return InetAddress.getLocalHost();
    }

    private static String getProperty(String name, String def) {
        String result = System.getProperty(name);
        if (result == null || result.length() == 0 || result == "") {
            result = def;
        }
        return result;
    }


}
