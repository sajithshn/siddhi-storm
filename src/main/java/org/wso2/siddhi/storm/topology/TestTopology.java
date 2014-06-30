package org.wso2.siddhi.storm.topology;

import backtype.storm.Config;
import backtype.storm.LocalCluster;
import backtype.storm.topology.TopologyBuilder;
import org.wso2.siddhi.core.SiddhiManager;
import org.wso2.siddhi.storm.component.EventPublisherBolt;
import org.wso2.siddhi.storm.component.EventReceiverSpout;
import org.wso2.siddhi.storm.component.SiddhiBolt;

//TODO : Move the code inside CEP
/**
 * Created by sajith on 6/2/14.
 */
public class TestTopology {
    public static void main(String[] args) throws Exception {

        String authStreamStreamDef = "define stream authStream (username string, ipAddress string, browser string);";
        String query = "from every a1 = authStream " +
                       "-> b1 = authStream[username == a1.username and ipAddress != a1.ipAddress] " +
                       "within 10000 " +
                       "select a1.username as username, a1.ipAddress as ip1, b1.ipAddress as ip2 " +
                       "insert into alertStream;";

        SiddhiManager siddhiManager = new SiddhiManager();
        siddhiManager.defineStream(authStreamStreamDef);
        siddhiManager.addQuery(query);

        String[] importedStreams = new String[1];
        importedStreams[0] = authStreamStreamDef;

        TopologyBuilder builder = new TopologyBuilder();

        builder.setSpout("EventReceiverSpout", new EventReceiverSpout(12000, importedStreams), 1);
        builder.setBolt("Siddhibolt", new SiddhiBolt(importedStreams, new String[]{query}, new String[]{"alertStream"})).allGrouping("EventReceiverSpout", "authStream");
        builder.setBolt("EventPublisherBolt", new EventPublisherBolt(importedStreams, new String[]{query}, new String[]{"alertStream"})).allGrouping("Siddhibolt", "alertStream");

        Config conf = new Config();
        conf.setDebug(true);

        conf.setMaxTaskParallelism(3);
        LocalCluster cluster = new LocalCluster();
        cluster.submitTopology("TriftTopology", conf, builder.createTopology());
    }
}
