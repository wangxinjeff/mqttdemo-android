package com.hyphenate.mqttdemo;

import androidx.appcompat.app.AppCompatActivity;

import android.nfc.Tag;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.hyphenate.mqttdemo.databinding.ActivityMainBinding;

import org.eclipse.paho.client.mqttv3.IMqttActionListener;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.internal.framed.Header;

public class MainActivity extends AppCompatActivity implements MqttClient.MqttListener {

    private static final String TAG = MainActivity.class.getSimpleName();
    private ActivityMainBinding binding;
    private EditText etUsername;
    private EditText etTopic;
    private EditText etMessage;
    private EditText etQos;
    private LinearLayout conView;
    private LinearLayout subView;
    private String topic;
    private String qos;

    private MqttClient client;

    private String host = "ff6sc0.cn1.mqtt.chat"; //环信MQTT服务器地址 通过console后台[MQTT]->[服务概览]->[服务配置]下[连接地址]获取
    private String port = "1883"; // 协议服务端口 通过console后台[MQTT]->[服务概览]->[服务配置]下[连接端口]获取
    private String appId = "ff6sc0"; // appId 通过console后台[MQTT]->[服务概览]->[服务配置]下[AppID]获取
    private String deviceId = System.currentTimeMillis()+""; // 自定义deviceId
    private String restApi = "https://api.cn1.mqtt.chat/app/ff6sc0"; //环信MQTT REST API地址 通过console后台[MQTT]->[服务概览]->[服务配置]下[REST API地址]获取
    private String appClientId = "YXA67-uKaalmThCOut6Q8uPLSg";//开发者ID 通过console后台[应用概览]->[应用详情]->[开发者ID]下[ Client ID]获取
    private String appClientSecret = "YXA63CFpMQFai4MdTDdGN92BBoG6_6g"; // 开发者密钥 通过console后台[应用概览]->[应用详情]->[开发者ID]下[ ClientSecret]获取
    private String clientId = deviceId + '@' + appId;
    private String userName = "test"; //自定义用户名 长度不超过64位即可


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        initView();
        client = MqttClient.getInstance(MainActivity.this);
        client.addMqttListener(this);
    }

    private void initView() {
        etUsername = binding.userName;
        etTopic = binding.topic;
        etMessage = binding.message;
        etQos = binding.qos;
        conView = binding.viewConnect;
        subView = binding.viewSub;
        subView.setVisibility(View.GONE);

    }

    public void conClick(View view){
        userName = etUsername.getText().toString();
        if (TextUtils.isEmpty(userName)) {
            Toast.makeText(MainActivity.this, "Username is null", Toast.LENGTH_SHORT).show();
            return;
        }

        getAppToken(new CallBack<String>() {
            @Override
            public void onSuccess(String appToken) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        getUserToken(appToken, new CallBack<String>(){
                            @Override
                            public void onSuccess(String userToken) {
                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        client.connectMQTT(host, port, clientId, userName, userToken, new IMqttActionListener() {
                                            @Override
                                            public void onSuccess(IMqttToken asyncActionToken) {
                                                Log.e(TAG, "connect success");
                                                sendToast("connect success");
                                                runOnUiThread(new Runnable() {
                                                    @Override
                                                    public void run() {
                                                        conView.setVisibility(View.GONE);
                                                        subView.setVisibility(View.VISIBLE);
                                                    }
                                                });
                                            }

                                            @Override
                                            public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                                                Log.e(TAG, "connect failure");
                                                sendToast("connect failure");
                                            }
                                        });
                                    }
                                });
                            }

                            @Override
                            public void onFail(String desc) {

                            }
                        });
                    }
                });
            }

            @Override
            public void onFail(String desc) {

            }
        });

    }

    private void getAppToken(CallBack<String> callBack) {
        try {
            JSONObject reqBody = new JSONObject();
            reqBody.put("appClientId", appClientId);
            reqBody.put("appClientSecret", appClientSecret);
            OkHttpClient okHttpClient = new OkHttpClient.Builder().build();
            MediaType mediaType = MediaType.parse("application/json");
            RequestBody requestBody = RequestBody.create(mediaType, reqBody.toString());
            Request request = new Request.Builder()
                    .url(restApi + "/openapi/rm/app/token")
                    .post(requestBody)
                    .build();
            Call call = okHttpClient.newCall(request);
            call.enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    Log.e(TAG, "okhttp_onFailure:" + e.getMessage());
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    String responseBody = response.body().string();
                    if (response.code() == 200 && !responseBody.isEmpty()) {
                        try {
                            JSONObject result = new JSONObject(responseBody);
                            String token = result.optJSONObject("body").optString("access_token");
                            Log.e(TAG, "App Token:" + token);
                            callBack.onSuccess(token);
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    } else {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                Toast.makeText(MainActivity.this, responseBody, Toast.LENGTH_LONG).show();
                            }
                        });
                    }
                }
            });
        }catch (JSONException e){
            e.printStackTrace();
        }
    }

    private void getUserToken(String appToken, CallBack<String> callBack){
        try {
            JSONObject reqBody = new JSONObject();
            reqBody.put("username", userName);
            reqBody.put("expires_in", 86400);//过期时间，单位为秒，默认为3天，如需调整，可提工单调整
            reqBody.put("cid", clientId);
            OkHttpClient okHttpClient = new OkHttpClient.Builder().build();
            MediaType mediaType = MediaType.parse("application/json");
            RequestBody requestBody = RequestBody.create(mediaType, reqBody.toString());
            Request request = new Request.Builder()
                    .url(restApi + "/openapi/rm/user/token")
                    .addHeader("Authorization", appToken)
                    .post(requestBody)
                    .build();
            Call call = okHttpClient.newCall(request);
            call.enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    Log.e(TAG, "okhttp_onFailure:" + e.getMessage());
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    String responseBody = response.body().string();
                    if (response.code() == 200 && !responseBody.isEmpty()) {
                        try {
                            JSONObject result = new JSONObject(responseBody);
                            String token = result.optJSONObject("body").optString("access_token");
                            Log.e(TAG, "User Token:" + token);
                            callBack.onSuccess(token);
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    } else {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                Toast.makeText(MainActivity.this, responseBody, Toast.LENGTH_LONG).show();
                            }
                        });
                    }
                }
            });
        }catch (JSONException e){
            e.printStackTrace();
        }
    }

    public void subClick(View view) {
        topic = etTopic.getText().toString();
        qos = etQos.getText().toString();
        if (TextUtils.isEmpty(topic) || TextUtils.isEmpty(qos)) {
            sendToast("Topic or Qos is null");
            return;
        }else if(Integer.parseInt(qos) > 2 || Integer.parseInt(qos) < 0){
            sendToast("Qos input error");
            return;
        }
        client.subscribeMQTT(topic, Integer.parseInt(qos), new IMqttActionListener() {
            @Override
            public void onSuccess(IMqttToken asyncActionToken) {
                Log.e(TAG, "subscribe success");
                sendToast("subscribe success");
            }

            @Override
            public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                Log.e(TAG, "subscribe failure");
                sendToast("subscribe failure");
            }
        });
    }

    public void unsubClick(View view){
        topic = etTopic.getText().toString();
        if (TextUtils.isEmpty(topic)) {
            sendToast("Topic is null");
            return;
        }

        client.unsubscribeMQTT(topic, new IMqttActionListener(){

            @Override
            public void onSuccess(IMqttToken asyncActionToken) {
                Log.e(TAG, "unsubscribe success");
                sendToast("unsubscribe success");
            }

            @Override
            public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                Log.e(TAG, "unsubscribe failure");
                sendToast("unsubscribe failure");
            }
        });
    }

    public void sendClick(View view) throws MqttException {
        topic = etTopic.getText().toString();
        String message = etMessage.getText().toString();
        qos = etQos.getText().toString();
        if (TextUtils.isEmpty(topic) || TextUtils.isEmpty(message) || TextUtils.isEmpty(qos)) {
            sendToast("Topic, Qos or Message is null");
            return;
        }else if(Integer.parseInt(qos) > 2 || Integer.parseInt(qos) < 0){
            sendToast("Qos input error");
            return;
        }
        client.sendMsg(topic, message, Integer.parseInt(qos));
    }

    @Override
    public void onSendMsgSuccess(IMqttToken asyncActionToken) {
        Log.e(TAG, "onSendMsgSuccess");
        sendToast("onSendMsgSuccess");
    }

    @Override
    public void onSendMsgFailure(IMqttToken asyncActionToken, Throwable exception) {
        Log.e(TAG, "onSendMsgFailure");
        sendToast("onSendMsgFailure");
    }

    @Override
    public void onConnectionLost(Throwable cause) {
        Log.e(TAG, "onConnectionLost");
        sendToast("onConnectionLost");
    }

    @Override
    public void onMessageArrived(String topic, MqttMessage message) {
        Log.e(TAG, "onMessageArrived");
        String msg = binding.viewMsg.getText().toString();
        binding.viewMsg.setText(new StringBuilder(msg).append("topic: ").append(topic).append("  message:").append(message.toString()).append("\n"));
    }

    @Override
    public void onDeliveryComplete(IMqttDeliveryToken token) {
        Log.e(TAG, "onDeliveryComplete");
    }

    private void sendToast(String content){
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(MainActivity.this, content, Toast.LENGTH_SHORT).show();
            }
        });
    }

    interface CallBack<T>{
        void onSuccess(T value);
        void onFail(String desc);
    }
}