package com.dftc.mqttclient;

import android.content.DialogInterface;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.NavigationView;
import android.support.design.widget.Snackbar;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.dftc.baseview.CustomDialog;
import com.dftc.baseview.CustomEditText;
import com.dftc.mqttclient.iothub.SimpleClient4IOT;
import com.dftc.mqttclient.util.LogUtil;

import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttMessage;

import static com.dftc.mqttclient.iothub.SimpleClient4IOT.deviceName;
import static com.dftc.mqttclient.iothub.SimpleClient4IOT.productKey;
import static com.dftc.mqttclient.iothub.SimpleClient4IOT.secret;
import static com.dftc.mqttclient.iothub.SimpleClient4IOT.subTopic;

public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        LogUtil.init((ScrollView) findViewById(R.id.scrollView));

        final FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "GONE", Snackbar.LENGTH_SHORT)
                        .setAction("Action", null).show();
                fab.setVisibility(View.GONE);
            }
        });

        initView();

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.setDrawerListener(toggle);
        toggle.syncState();

        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);
    }

    private void initView() {
        Button bt_connect = (Button) findViewById(R.id.bt_connect);
        bt_connect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                deviceName = ((EditText) findViewById(R.id.et_deviceName)).getText().toString();
                productKey = ((EditText) findViewById(R.id.et_productKey)).getText().toString();
                secret = ((EditText) findViewById(R.id.et_secret)).getText().toString();
                new Thread() {
                    public void run() {
                        startConnect();
                    }
                }.start();
            }
        });

        Button bt_subscribe = (Button) findViewById(R.id.bt_subscribe);
        bt_subscribe.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                showSubscribeDialog();
            }
        });

        Button bt_publish = (Button) findViewById(R.id.bt_publish);
        bt_publish.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                showPublishDialog();
            }
        });

        Button bt_clear = (Button) findViewById(R.id.bt_clear);
        bt_clear.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                LogUtil.clear();
            }
        });
    }

    private void startConnect() {
        try {
            SimpleClient4IOT.startConnect(MainActivity.this,
                    new MqttCallback() {
                        @Override
                        public void connectionLost(Throwable cause) {
                            LogUtil.print("连接错误，原因:" + cause);
                            cause.printStackTrace();
                            try {
                                SimpleClient4IOT.disconnect();
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                            startConnect();
                        }

                        @Override
                        public void messageArrived(String topic, MqttMessage message) throws Exception {
                            LogUtil.print("接收到消息,来至Topic [" + topic + "] , 内容是:["
                                    + new String(message.getPayload(), "UTF-8") + "],  ");
                            handleMessage(topic, message);
                        }

                        @Override
                        public void deliveryComplete(IMqttDeliveryToken token) {
                            //如果是QoS0的消息，token.resp是没有回复的
                            LogUtil.print("消息发送成功! " + ((token == null || token.getResponse() == null) ? "null"
                                    : token.getResponse().getKey()));
                        }
                    });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void handleMessage(String topic, MqttMessage message) {
        try {
            JSONObject jsonObj = JSON.parseObject(new String(message.getPayload(), "UTF-8"));
            String type = jsonObj.getString("type");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void showSubscribeDialog() {
        CustomDialog.Builder builder = new CustomDialog.Builder(this);

        builder.setTitle("subscribe");

        LinearLayout fragment = (LinearLayout) getLayoutInflater()
                .inflate(R.layout.dialog_subscribe, null);
        builder.setContentView(fragment);

        final CustomEditText et_topic = (CustomEditText) fragment.findViewById(R.id.et_topic);
        et_topic.setText(subTopic);

        builder.setPositiveButton(R.string.ok,
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        subTopic = et_topic.getText().toString();
                        if (TextUtils.isEmpty(subTopic)) {
                            et_topic.showPopupPrompt("Input empty");
                            return;
                        }
                        try {
                            SimpleClient4IOT.subscribe(subTopic);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                        dialog.cancel();

                    }
                });
        builder.setNegativeButton(R.string.cancel, null);
        builder.create().show();
    }

    private void showPublishDialog() {
        CustomDialog.Builder builder = new CustomDialog.Builder(this);

        builder.setTitle("Publish");

        LinearLayout fragment = (LinearLayout) getLayoutInflater()
                .inflate(R.layout.dialog_publish, null);
        builder.setContentView(fragment);

        final CustomEditText et_topic = (CustomEditText) fragment.findViewById(R.id.et_topic);
        final CustomEditText et_content = (CustomEditText) fragment.findViewById(R.id.et_content);
        et_topic.setText(subTopic);

        builder.setPositiveButton(R.string.ok,
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        subTopic = et_topic.getText().toString();
                        String str_content = et_content.getText().toString();
                        if (TextUtils.isEmpty(subTopic)) {
                            et_topic.showPopupPrompt("Input empty");
                            return;
                        }
                        if (TextUtils.isEmpty(str_content)) {
                            et_content.showPopupPrompt("Input empty");
                            return;
                        }
                        try {
                            SimpleClient4IOT.publish(subTopic, str_content);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                        dialog.cancel();

                    }
                });
        builder.setNegativeButton(R.string.cancel, null);
        builder.create().show();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        try {
            SimpleClient4IOT.disconnect();
        } catch (Exception e) {
            e.printStackTrace();
        }
        LogUtil.destroy();
    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();

        if (id == R.id.nav_camera) {
            // Handle the camera action
        } else if (id == R.id.nav_gallery) {

        } else if (id == R.id.nav_slideshow) {

        } else if (id == R.id.nav_manage) {

        } else if (id == R.id.nav_share) {

        } else if (id == R.id.nav_send) {

        }

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }
}
