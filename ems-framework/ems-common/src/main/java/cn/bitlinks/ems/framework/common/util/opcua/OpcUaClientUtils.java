package cn.bitlinks.ems.framework.common.util.opcua;


import lombok.extern.slf4j.Slf4j;
import org.eclipse.milo.opcua.sdk.client.OpcUaClient;
import org.eclipse.milo.opcua.sdk.client.api.identity.AnonymousProvider;
import org.eclipse.milo.opcua.stack.core.security.SecurityPolicy;
import org.eclipse.milo.opcua.stack.core.types.builtin.*;
import org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.Unsigned;
import org.eclipse.milo.opcua.stack.core.types.enumerated.TimestampsToReturn;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

@Slf4j
public class OpcUaClientUtils {

    //public static final String OPC_UA_URL = "opc.tcp://192.168.1.11:51310/CogentDataHub/DataAccess";
    //public static final String NODE_ID = "ns=2;s=opcda1:test1.111.通道 1.设备 1.标记 1";
    public static final String OPC_UA_URL = "opc.tcp://192.168.1.11:4998/Softing_dataFEED_OPC_Suite_Configuration1";
    public static final String NODE_ID = "ns=3;s=OPC_da.test1.111.通道 1.设备 1.标记 1";
    public static final String USERNAME = "opcua";
    public static final String PASSWORD = "123456";

    public static void main(String[] args) throws Exception {
        // 使用 create(String, Function, Function)
        OpcUaClient client = OpcUaClient.create(
                OPC_UA_URL,

                // Function<List<EndpointDescription>, Optional<EndpointDescription>>
                endpoints -> endpoints.stream()
                        .filter(e -> e.getSecurityPolicyUri().equals(SecurityPolicy.None.getUri()))
                        .findFirst(),

                // Function<OpcUaClientConfigBuilder, OpcUaClientConfig>
                builder -> builder
                        .setApplicationName(LocalizedText.english("MyOpcUaClient"))
                        //此设置必须唯一，也可以不设置Milo会自动设置，但当使用证书时，此为必填
                        .setApplicationUri("urn:my:opcua:client")
                        //.setIdentityProvider(new UsernameProvider(USERNAME, PASSWORD))
                        .setIdentityProvider(new AnonymousProvider())
                        .setRequestTimeout(Unsigned.uint(5000))
                        .build()
        );

        client.connect().get();
        System.out.println("✅ 已使用用户名密码连接到 OPC UA Server");
        List<DataValue> dataValues = client.readValues(0, TimestampsToReturn.Both, Arrays.asList(NodeId.parse(NODE_ID))).get();
        // 读取节点值
        NodeId nodeId = NodeId.parse(NODE_ID);
        DataValue value = client.readValue(0, TimestampsToReturn.Both, nodeId).get();
        System.out.println("📦 节点值!!!!!!!!!!!!!!!!!!!!!!!!: " + value.getValue().getValue());

        client.disconnect().get();
        System.out.println("🚪 已断开连接");
    }

    /**
     * 获取连接的客户端
     *
     * @param url opc url
     * @return OpcUaClient
     */
    public static OpcUaClient getClient(String url) {
        try {
            OpcUaClient opcUaClient = OpcUaClient.create(url);
            opcUaClient.connect();
            return opcUaClient;
        } catch (Exception e) {
            throw new RuntimeException("获取OPC UA 连接异常");
        }
    }


    /**
     * 关闭客户端
     */
    public static void closeClient(OpcUaClient opcUaClient) {

        try {
            opcUaClient.disconnect();
            log.info("{} is close success");
        } catch (Exception e) {
            log.error("Error running closeOpcUaClient: {}", e.getMessage(), e);
        }

    }


    /**
     * 获取单个节点值
     *
     * @param client client
     * @param nodeId nodeId
     */
    public static Object readValue(OpcUaClient client, NodeId nodeId) {
        // 第一个参数如果设置为0的话会获取最新的值，如果maxAge设置到Int32的最大值，则尝试从缓存中读取值。
        // 第二个参数为请求返回的时间戳,第三个参数为要读取的NodeId对象。
        DataValue value;
        try {
            value = client.readValue(0.0, TimestampsToReturn.Both, nodeId).get();
            return value.getValue().getValue();
        } catch (InterruptedException | ExecutionException e) {
            log.error("Error reading value: {}", e.getMessage(), e);
        }
        return null;
    }

    public static boolean writeValue(OpcUaClient client, NodeId nodeId, Object value) {
        try {
            Variant v = new Variant(value);
            DataValue dataValue = new DataValue(v, null, null);
            StatusCode statusCode = client.writeValue(nodeId, dataValue).get();
            if (statusCode.getValue() != 0) {
                log.error("writeValue:{},={},statusCode={}", nodeId, value, statusCode.getValue());
                return false;
            }
            return true;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 写变量
     *
     * @param client client
     * @param map    map
     */
    public static void writeValues(OpcUaClient client, Map<NodeId, Object> map) throws ExecutionException, InterruptedException {
        if (map == null || map.isEmpty())
            return;
        String url = client.getConfig().getEndpoint().getEndpointUrl();
        for (Map.Entry<NodeId, Object> entry : map.entrySet()) {
            Object value = entry.getValue();
            Variant v = new Variant(value);
            DataValue dataValue = new DataValue(v, null, null);
            StatusCode statusCode = client.writeValue(entry.getKey(), dataValue).get();
            if (statusCode.getValue() != 0) {
                log.error("writeValues:{},={},statusCode={}", entry.getKey(), value, statusCode.getValue());
                new Thread(() -> {
                    NodeId key_ = entry.getKey();
                    Object value_ = entry.getValue();
                    Variant v_ = new Variant(value_);
                    DataValue dataValue_ = new DataValue(v_, null, null);
                    for (int i = 0; i < 5; i++) {
                        try {
                            Thread.sleep(20);
                            StatusCode statusCode_ = client.writeValue(key_, dataValue_).get();
                            log.info("url:{},writeCount:{}", url, i);
                            log.info("writeValue:{},={},statusCode={},writeCount:{}", key_, value_, statusCode_.getValue(), i);
                            if (statusCode_.getValue() == 0) break;
                        } catch (Exception e) {
                            throw new RuntimeException(e);
                        }
                    }

                }).start();
            }
        }
    }

    /* *//**
     * 	创建订阅，添加受监视的项，然后等待值到达。
     * 	服务器断线重连，应该调用onSubscriptionTransferFailed（）回调，因为客户端重新连接服务器将无法在订阅丢失其所有状态后传输订阅。
     * @param nodeIds  创建订阅的变量
     * @param sf 订阅间隔，单位ms
     *//*
    public static void createSubscription(String url,List<NodeId> nodeIds, double sf){
        OpcUaClient client = getClient(url);
        HashSet<NodeId> set = new HashSet<>(nodeIds);
        nodeIds.clear();
        nodeIds.addAll(set);
        while(client==null){
            try {
                Thread.sleep(1000);
                if(OpcUaCache.clients.containsKey(url)) {
                    client = OpcUaCache.clients.get(url);
                }
            } catch (InterruptedException e) {
                log.error("Error sleeping: {}", e.getMessage(), e);
            }
        }
        try {
            log.info("{} createSubscription", url);
            OpcUaClient finalClient = client;
            client.getSubscriptionManager().addSubscriptionListener(
                    new org.eclipse.milo.opcua.sdk.client.api.subscriptions.UaSubscriptionManager.SubscriptionListener() {
                        @Override
                        public void onSubscriptionTransferFailed(UaSubscription subscription, StatusCode statusCode) {
                            Stack.sharedExecutor().execute(() -> {
                                try {
                                    createItemAndWait(url, finalClient,nodeIds,sf);
                                } catch (InterruptedException | ExecutionException e) {
                                    log.error("Error creating Subscription: {}", e.getMessage(), e);
                                }
                            });
                        }
                    });
            createItemAndWait(url,client,nodeIds,sf);
        } catch (InterruptedException | ExecutionException e) {
            log.info("{}订阅点位时发生了错误", url, e);
            throw new RuntimeException(url+"订阅点位时发生了错误");
        }
    }*/

    /*private static void createItemAndWait(
            String url,
            OpcUaClient client,
            List<NodeId> nodeIds,
            double sf) throws InterruptedException, ExecutionException {
        client.getSubscriptionManager().clearSubscriptions();
        //创建发布间隔sf的订阅对象
        UaSubscription subscription = client.getSubscriptionManager().createSubscription(sf).get();
        List<MonitoredItemCreateRequest> requests = new ArrayList<>();
        for (NodeId nodeId : nodeIds) {
            ReadValueId readValueId = new ReadValueId(
                    nodeId, AttributeId.Value.uid(), null, QualifiedName.NULL_VALUE
            );
            UInteger clientHandle = subscription.nextClientHandle();
            //	创建监控的参数
            MonitoringParameters parameters = new MonitoringParameters(
                    clientHandle, sf, null, UInteger.valueOf(10), true
            );
            //	创建监控项请求
            //	该请求最后用于创建订阅。
            MonitoredItemCreateRequest request = new MonitoredItemCreateRequest(
                    readValueId, MonitoringMode.Reporting, parameters
            );
            requests.add(request);
        }
        //	创建监控项，并且注册变量值改变时候的回调函数。
        subscription.createMonitoredItems(
                TimestampsToReturn.Both,
                requests,
                (item,id)-> item.setValueConsumer((item1, value)->{
                    try {
                        NodeId nodeId = item1.getReadValueId().getNodeId();
                        Variant  variant = value.getValue();
                        Map<String, SyncOpcNode> node = new HashMap<>();
                        if(OpcUaCache.nodes!=null && OpcUaCache.nodes.containsKey(url)) {
                            node = OpcUaCache.nodes.get(url) ;
                        }
                        if(node.containsKey(String.valueOf(nodeId.getIdentifier()))){
                            OpcUaCache.data.put(node.get(String.valueOf(nodeId.getIdentifier())).getId(), variant.getValue());
                        }
                    } catch (Exception e) {
                        log.error("subscription is error {}", e.getMessage());
                    }
                })).get();
    }*/
}

