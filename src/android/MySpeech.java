/**
 *
 */
package org.ioniconline;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.PluginResult;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

import com.iflytek.cloud.ErrorCode;
import com.iflytek.cloud.InitListener;
import com.iflytek.cloud.LexiconListener;
import com.iflytek.cloud.RecognizerListener;
import com.iflytek.cloud.RecognizerResult;
import com.iflytek.cloud.SpeechConstant;
import com.iflytek.cloud.SpeechError;
import com.iflytek.cloud.SpeechRecognizer;
import com.iflytek.cloud.SpeechSynthesizer;
import com.iflytek.cloud.SpeechUtility;

public class MySpeech extends CordovaPlugin {
    private static final String TAG = "MySpeech";
    //
    // the APPID got from the web
    private static final String YOUR_APP_ID = "5631ca67";

    // the speaker
    private SpeechSynthesizer mSynth = null;

    @Override
    public boolean execute(String action, JSONArray args, CallbackContext cb) {
        boolean ret = true;

        if (action.equals("init")) {
            android.util.Log.e(TAG, "init the engine...");
            SpeechUtility.createUtility(cordova.getActivity(),
                    SpeechConstant.APPID + "=" + YOUR_APP_ID);
        } else if (action.equals("speak")) {
            android.util.Log.e(TAG, "speak...");
            // TODO , below stupid init code SHOULD be removed soon..
            SpeechUtility.createUtility(cordova.getActivity(),
                    SpeechConstant.APPID + "=" + YOUR_APP_ID);
            SpeechSynthesizer syn = getSynthesizer();
            syn.startSpeaking("hello world",
                    null // currently use null listener
                    );
        }

        return ret;
    }

    private SpeechSynthesizer getSynthesizer() {
        if (mSynth == null) {
            mSynth = SpeechSynthesizer.createSynthesizer(
                    cordova.getActivity(), null);
        }

        return mSynth;
    }
}
