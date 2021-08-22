package com.hyphenate.mqttdemo;

import android.content.Context;
import android.util.Log;

import org.eclipse.paho.android.service.MqttAndroidClient;
import org.eclipse.paho.client.mqttv3.IMqttActionListener;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;

public class MqttClient {
    private static MqttClient instance;
    private Context context;
    private MqttListener mqttListener;

    //单例模式
    public static MqttClient getInstance(Context context) {
        if (null == instance) {
            synchronized (MqttClient.class) {
                instance = new MqttClient(context);
            }
        }
        return instance;
    }

    private MqttClient(Context context) {
        this.context = context.getApplicationContext();
    }

    //声明一个MQTT客户端对象
    private MqttAndroidClient mMqttClient;
    private static final String TAG = "MqttClient";

    //连接到服务器
    public void connectMQTT(String appId, String mqttUri, String mqttPort,  String userName, String token, IMqttActionListener callBack) {
        //连接时使用的clientId, 必须唯一, 一般加时间戳
        String clientId = String.format("%s@%s", userName, appId);
        mMqttClient = new MqttAndroidClient(context, String.format("tcp://%s:%s", mqttUri, mqttPort), clientId);
        //连接参数
        MqttConnectOptions options;
        options = new MqttConnectOptions();
        //设置自动重连
        options.setAutomaticReconnect(true);
        // 缓存,
        options.setCleanSession(true);
        // 设置超时时间，单位：秒
        options.setConnectionTimeout(15);
        // 心跳包发送间隔，单位：秒
        options.setKeepAliveInterval(15);
        // 用户名
        options.setUserName(userName);
        // 密码
        options.setPassword(token.toCharArray());
        options.setMqttVersion(MqttConnectOptions.MQTT_VERSION_3_1_1);
        // 设置MQTT监听
        mMqttClient.setCallback(new MqttCallback() {
            @Override
            public void connectionLost(Throwable cause) {
                Log.d(TAG, "connectionLost: 连接断开");
                mqttListener.onConnectionLost(cause);
            }

            @Override
            public void messageArrived(String topic, MqttMessage message) throws Exception {
                Log.d(TAG, "收到消息:"+message.toString());
                mqttListener.onMessageArrived(topic, message);
            }

            @Override
            public void deliveryComplete(IMqttDeliveryToken token) {
                mqttListener.onDeliveryComplete(token);
            }
        });
        try {
            //进行连接
            mMqttClient.connect(options, null, callBack);
        } catch (MqttException e) {
            e.printStackTrace();
        }

    }

    public void subscribeMQTT(String topic, int qos, IMqttActionListener callBack){
        try {
            //连接成功后订阅主题
            mMqttClient.subscribe(topic, qos, null, callBack);
        } catch (MqttException e) {
            e.printStackTrace();
        }

    }

    public void sendMsg(String topic, String content, int qos) throws MqttException {

        MqttMessage msg=new MqttMessage();
        msg.setPayload(content.getBytes());//设置消息内容
        msg.setQos(qos);//设置消息发送质量，可为0,1,2.
        //设置消息的topic，并发送。
        mMqttClient.publish(topic, msg, null, new IMqttActionListener() {
            @Override
            public void onSuccess(IMqttToken asyncActionToken) {
                Log.d(TAG, "onSuccess: 发送成功");
                mqttListener.onSendMsgSuccess(asyncActionToken);
            }

            @Override
            public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                Log.d(TAG, "onFailure: 发送失败="+ exception.getMessage());
                mqttListener.onSendMsgFailure(asyncActionToken, exception);
            }
        });
    }

    public void addMqttListener(MqttListener mqttListener){
        this.mqttListener = mqttListener;
    }

    interface MqttListener{
        void onSendMsgSuccess(IMqttToken asyncActionToken);
        void onSendMsgFailure(IMqttToken asyncActionToken, Throwable exception);
        void onConnectionLost(Throwable cause);
        void onMessageArrived(String topic, MqttMessage message);
        void onDeliveryComplete(IMqttDeliveryToken token);
    }
}
