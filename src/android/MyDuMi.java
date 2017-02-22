/**
 * A Wrapper of DuMi
 *
 * [2017-02-22] Creation
 */
package org.ioniconline;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Random;
import javax.net.ssl.SSLException;

import android.app.Application;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.PluginResult;
import org.json.JSONObject;

import com.baidu.duersdk.DuerSDK;
import com.baidu.duersdk.DuerSDKFactory;
import com.baidu.duersdk.sdkconfig.SdkConfigInterface;
import com.baidu.duersdk.sdkverify.SdkVerifyManager;
import com.baidu.duersdk.utils.FileUtil;

import com.baidu.duersdk.DuerSDKFactory;
import com.baidu.duersdk.datas.DuerMessage;
import com.baidu.duersdk.message.IReceiveMessageListener;
import com.baidu.duersdk.message.ISendMessageFinishListener;
import com.baidu.duersdk.message.SendMessageData;
import com.baidu.duersdk.utils.AppLogger;


public class MyDuMi{
    private static final String TAG = "MyDuMi";

    public static final String APP_KEY = "2F4B662AF2064323A16122D702160F15";
    public static final String APP_ID = "650DEBC2B99A4dA4";

    private DuerSDK  mDmSdk;

    final IReceiveMessageListener messageListener = new IReceiveMessageListener() {
        @Override
        public void messageReceive(String megSourceString) {
            try {
                JSONObject duerMessageJson = new JSONObject(megSourceString);
                android.util.Log.e(TAG, "the json:" + duerMessageJson.toString(4));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    };

    public MyDuMi(Application act) {
        mDmSdk = DuerSDKFactory.getDuerSDK();
        mDmSdk.initSDK(act, APP_ID, APP_KEY);
        android.util.Log.i(TAG, "DuMi inited");
    }

    public String sendTextData(String req) {
        return "OK";
    }
}
