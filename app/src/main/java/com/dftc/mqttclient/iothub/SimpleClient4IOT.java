/**
 * aliyun.com Inc.
 * Copyright (c) 2004-2017 All Rights Reserved.
 */
package com.dftc.mqttclient.iothub;

import android.content.Context;

import com.dftc.mqttclient.util.LogUtil;
import com.dftc.mqttclient.util.SignUtil;

import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;

import java.net.InetAddress;
import java.util.HashMap;
import java.util.Map;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;

/**
 * IoT套件JAVA版设备接入demo
 */
public class SimpleClient4IOT {

    /******这里是客户端需要的参数*******/
    public static String deviceName = "dftc002";
    public static String productKey = "qLhKrlpINTO";
    public static String secret = "fKxsI2gDjaa27x2fpV7DHyrtvcnG0UYl";

    //用于测试的topic
    public static String subTopic = "/" + productKey + "/" + deviceName + "/get";
    private static String pubTopic = "/" + productKey + "/" + deviceName + "/update";

    private static MqttClient sampleClient;

    public static void startConnect(Context context,
                                    MqttCallback callback) throws Exception {
        SimpleClient4IOT.subTopic = "/" + productKey + "/" + deviceName + "/subUpdateStatus";
        //客户端设备自己的一个标记，建议是MAC或SN，不能为空，32字符内
        String clientId = InetAddress.getLocalHost().getHostAddress();

        //设备认证
        Map<String, String> params = new HashMap<String, String>();
        params.put("productKey", productKey); //这个是对应用户在控制台注册的 设备productkey
        params.put("deviceName", deviceName); //这个是对应用户在控制台注册的 设备name
        params.put("clientId", clientId);
        String t = System.currentTimeMillis() + "";
        params.put("timestamp", t);

        //MQTT服务器地址，TLS连接使用ssl开头
        //tcp://iot.eclipse.org:1883";//
        String targetServer = "ssl://" + productKey + ".iot-as-mqtt.cn-shanghai.aliyuncs.com:1883";

        //客户端ID格式，两个||之间的内容为设备端自定义的标记，字符范围[0-9][a-z][A-Z]
        String mqttclientId = clientId + "|securemode=2,signmethod=hmacsha1,timestamp=" + t + "|";
        String mqttUsername = deviceName + "&" + productKey; //mqtt用户名格式
        String mqttPassword = SignUtil.sign(params, secret, "hmacsha1"); //签名

        System.err.println("mqttclientId=" + mqttclientId);

        connectMqtt(context, targetServer, mqttclientId, mqttUsername, mqttPassword, deviceName, callback);
    }

    private static void connectMqtt(Context context, String url, String clientId, String mqttUsername,
                                    String mqttPassword, final String deviceName,
                                    MqttCallback callback) throws Exception {
        if (sampleClient != null)
            sampleClient.disconnect();
        MemoryPersistence persistence = new MemoryPersistence();
        SSLSocketFactory socketFactory = createSSLSocket(context);
        sampleClient = new MqttClient(url, clientId, persistence);
        MqttConnectOptions connOpts = new MqttConnectOptions();
        connOpts.setMqttVersion(4); // MQTT 3.1.1
        connOpts.setSocketFactory(socketFactory);

        //设置是否自动重连
        connOpts.setAutomaticReconnect(true);

        //如果是true，那么清理所有离线消息，即QoS1或者2的所有未接收内容
        connOpts.setCleanSession(false);

        connOpts.setUserName(mqttUsername);
        connOpts.setPassword(mqttPassword.toCharArray());
        connOpts.setKeepAliveInterval(65);

        LogUtil.print(clientId + "进行连接, 目的地: " + url);
        sampleClient.setCallback(callback);
        sampleClient.connect(connOpts);
        LogUtil.print("连接成功:---");

    }

    private static SSLSocketFactory createSSLSocket(Context con) throws Exception {
        SSLContext context = SSLContext.getInstance("TLSV1.2");
        context.init(null, new TrustManager[]{new ALiyunIotX509TrustManager(con)}, null);
        return context.getSocketFactory();
    }

    public static void subscribe(String topic) throws Exception {
        //一次订阅永久生效
        //这个是第一种订阅topic方式，回调到统一的callback
        sampleClient.subscribe(topic);
        //这个是第二种订阅方式, 订阅某个topic，有独立的callback
        //sampleClient.subscribe(subTopic, new IMqttMessageListener() {
        //    @Override
        //    public void messageArrived(String topic, MqttMessage message) throws Exception {
        //
        //        LogUtil.print("收到消息：" + message + ",topic=" + topic);
        //    }
        //});

//        //回复RRPC响应
//        final ExecutorService executorService = new ThreadPoolExecutor(2,
//                4, 600, TimeUnit.SECONDS,
//                new ArrayBlockingQueue<Runnable>(100), new CallerRunsPolicy());
//
//        String reqTopic = "/sys/" + productKey + "/" + deviceName + "/rrpc/request/+";
//        sampleClient.subscribe(reqTopic, new IMqttMessageListener() {
//            @Override
//            public void messageArrived(String topic, MqttMessage message) throws Exception {
//                LogUtil.print("收到请求：" + message + ", topic=" + topic);
//                String messageId = topic.substring(topic.lastIndexOf('/') + 1);
//                final String respTopic = "/sys/" + productKey + "/" + deviceName + "/rrpc/response/" + messageId;
//                String content = "hello world";
//                final MqttMessage response = new MqttMessage(content.getBytes());
//                response.setQos(0); //RRPC只支持QoS0
//                //不能在回调线程中调用publish，会阻塞线程，所以使用线程池
//                executorService.submit(new Runnable() {
//                    @Override
//                    public void run() {
//                        try {
//                            sampleClient.publish(respTopic, response);
//                            LogUtil.print("回复响应成功，topic=" + respTopic);
//                        } catch (Exception e) {
//                            e.printStackTrace();
//                        }
//                    }
//                });
//            }
//        });
    }

    public static void publish(String topic, String content) throws Exception {
        //这里测试发送一条消息
//        String content = "{'content':'msg from :" + mClientId + "," + System.currentTimeMillis() + "'}";

        MqttMessage message = new MqttMessage(content.getBytes("utf-8"));
        message.setQos(0);
        //System.out.println(System.currentTimeMillis() + "消息发布:---");
        sampleClient.publish(topic, message);
    }

    public static void disconnect() throws Exception {
        if (sampleClient != null)
            sampleClient.disconnect();
    }
}
