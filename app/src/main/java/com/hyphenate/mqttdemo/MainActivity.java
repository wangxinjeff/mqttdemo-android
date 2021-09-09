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

public class MainActivity extends AppCompatActivity implements MqttClient.MqttListener {

    private static final String TAG = MainActivity.class.getSimpleName();
    private ActivityMainBinding binding;
    private EditText etUsername;
    private EditText etPassword;
    private EditText etTopic;
    private EditText etMessage;
    private EditText etQos;
    private LinearLayout conView;
    private LinearLayout subView;
    private String userName;
    private String topic;
    private String qos;

    private MqttClient client;
    private final String appId = "9nd4c0";
    private final String mqttUri= "9nd4c0.cn1.mqtt.chat";
    private final String mqttPort = "1883";
    private String orgName = "0019";
    private String appName = "testtwos";

    private String tokenUrl = "https://a1.easemob.com/%s/%s/token";

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
        etPassword = binding.passWord;
        etTopic = binding.topic;
        etMessage = binding.message;
        etQos = binding.qos;
        conView = binding.viewConnect;
        subView = binding.viewSub;
        subView.setVisibility(View.GONE);

    }

    public void conClick(View view) throws JSONException {
        userName = etUsername.getText().toString();
        String passWord = etPassword.getText().toString();
        if (TextUtils.isEmpty(userName) || TextUtils.isEmpty(passWord)) {
            Toast.makeText(MainActivity.this, "Username or Password is null", Toast.LENGTH_SHORT).show();
            return;
        }

        JSONObject reqBody = new JSONObject();
        reqBody.put("grant_type", "password");
        reqBody.put("username", etUsername.getText().toString());
        reqBody.put("password", etPassword.getText().toString());


        OkHttpClient okHttpClient = new OkHttpClient.Builder().build();
        MediaType mediaType = MediaType.parse("application/json");
        RequestBody requestBody = RequestBody.create(mediaType, reqBody.toString());
        Request request = new Request.Builder()
                .url(String.format(tokenUrl, orgName, appName))
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
                if (response.code() == 200) {
                    try {
                        JSONObject result = new JSONObject(responseBody);
                        String token = result.getString("access_token");
                        client.connectMQTT(appId, mqttUri, mqttPort, userName, token, new IMqttActionListener() {
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
}