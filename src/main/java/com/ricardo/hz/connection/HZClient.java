package com.ricardo.hz.connection;

import com.hazelcast.client.config.ClientConfig;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.IMap;

public class HZClient {

    private HazelcastInstance client;
    private IMap<String,String> sessionMap;

    public static void main(String[] args) throws Exception {
        HZClient hzClient = new HZClient();
        hzClient.init("sdc-nppf01-dev.vpymnts.net:5702", "ums-hz", "ums-hz-pass");
        hzClient.disconnect();
    }

    public void init(String url, String name, String password) {
        ClientConfig clientConfig = new ClientConfig();
        clientConfig.getNetworkConfig().addAddress(url);
        clientConfig.getGroupConfig().setName(name);
        clientConfig.getGroupConfig().setPassword(password);

        client = com.hazelcast.client.HazelcastClient.newHazelcastClient(clientConfig);
        sessionMap = client.getMap("scSessionCache");
        sessionMap.addEntryListener(new SessionMapListener(), true);

        sessionMap.forEach((k,v) -> System.out.println("Key : " + k + " Value : " + v));
    }

    public IMap<String, String> getSessionMap() {
        return sessionMap;
    }

    public void disconnect() {
        if(client != null)
            client.shutdown();
    }

    public void deleteEntry(String key) {
        sessionMap.delete(key);
    }
}
