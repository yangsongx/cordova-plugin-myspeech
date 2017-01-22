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

import java.util.HashMap;
import java.util.LinkedHashMap;

public class MySpeech extends CordovaPlugin {
    private static final String TAG = "MySpeech";
    //
    // the APPID got from the web
    //private static final String YOUR_APP_ID = "5631ca67";
    private static final String YOUR_APP_ID = "576206f0";

    // Totally - 8
    private static final String [] mVoiceName = {
        "xiaoyan", // lady, madrin
        "xiaoyu",
        "xiaorong", // SiChuan
        "xiaomei", // Guang Dong
        "xiaokun", // HeNan
        "xiaoqiang", // HuNan
        "xiaoqian", // Dong Bei
        "xiaolin" // TaiWan
    };

    private CallbackContext mCb;

    // the speaker
    private SpeechSynthesizer mSynth = null;

    // the listener
    private SpeechRecognizer  mListen = null;
    private HashMap<String, String> mListenResults = new LinkedHashMap<String, String>();
    private StringBuffer mFinalResult = new StringBuffer();

    private InitListener initListener = new InitListener() {
        @Override
        public void onInit(int code) {
            android.util.Log.e(TAG, "onInit code:" + code);
        }
    };

    private RecognizerListener mRecognizerListener = new RecognizerListener() {
        @Override
        public void onVolumeChanged(int vol, byte [] data) {
        }
        @Override
        public void onEvent(int eventType, int arg1, int arg2, android.os.Bundle obj) {
        }
        @Override
        public void onBeginOfSpeech() {
            android.util.Log.e(TAG, ">start of speech");
        }
        @Override
        public void onError(SpeechError error) {
        }
        @Override
        public void onEndOfSpeech() {
            android.util.Log.e(TAG, "end of speech");
        }

        @Override
        public void onResult(RecognizerResult results, boolean isLast) {
            android.util.Log.e(TAG, "+got the result");
            String resultText = parseListenResult(results.getResultString());
            String sn = null;
            try {
                JSONObject resultJson = new JSONObject(resultText);
                sn = resultJson.optString("sn");
            }catch (JSONException e) {
            }

            mListenResults.put(sn, resultText);

            StringBuffer resultBuffer = new StringBuffer();
               for (String key : mListenResults.keySet()) {
                   resultBuffer.append(mListenResults.get(key));
            }

            mFinalResult.append(resultBuffer.toString());
            android.util.Log.e(TAG, "the text are : " + resultBuffer.toString());

            if(isLast) {
                PluginResult r = new PluginResult(
                        PluginResult.Status.OK,
                    mFinalResult.toString());
                r.setKeepCallback(true);
                mCb.sendPluginResult(r);
            }


        }
    };

    @Override
    public boolean execute(String action, JSONArray args, CallbackContext cb) throws JSONException {
        boolean ret = true;

        mCb = cb;

        android.util.Log.e(TAG, "coming with " + action + " action");
        if (action.equals("init")) {

            // Init ...
            //
            android.util.Log.e(TAG, "init the engine...");
            SpeechUtility.createUtility(cordova.getActivity(),
                    SpeechConstant.APPID + "=" + YOUR_APP_ID);
            mListen = SpeechRecognizer.createRecognizer(
                    this.cordova.getActivity(),
                    initListener/* use null listen */);
            if(mListen == null){
                android.util.Log.e(TAG, "listener is a null obj...");
                cb.error("XunFei plugin init with null result[Failed]");
            } else {
                android.util.Log.i(TAG, "got a listener, cool.");
                cb.success("init XunFei plugin [OK]");
            }

        } else if (action.equals("speak")) {

            // Speaking...
            //
            android.util.Log.e(TAG, "speak...");

            int vn = mapTheVoiceName(args.getInt(0));
            int speed = args.getInt(1);
            String val = args.getString(2);
            android.util.Log.e(TAG, "the voice name are : " + mVoiceName[vn]);
            android.util.Log.e(TAG, "the voice speed is: " + speed);

            SpeechSynthesizer syn = getSynthesizer();
            syn.setParameter(SpeechConstant.VOICE_NAME, mVoiceName[vn]);
            syn.setParameter(SpeechConstant.SPEED, String.valueOf(speed));
            syn.startSpeaking(val,
                    null // currently use null listener
                    );
        } else if (action.equals("listen")) {

            // Listen ...
            //
            android.util.Log.e(TAG, "listening...");
            mListen.setParameter(SpeechConstant.PARAMS, null);
            mListen.setParameter(SpeechConstant.ENGINE_TYPE,SpeechConstant.TYPE_CLOUD);
            mListen.setParameter(SpeechConstant.LANGUAGE, "zh_cn");
            mListen.setParameter(SpeechConstant.ACCENT, "mandarin ");
            mListen.setParameter(SpeechConstant.RESULT_TYPE, "json");
            mListen.setParameter(SpeechConstant.VAD_BOS, "4000"); // end timeout setting
            mListen.setParameter(SpeechConstant.VAD_EOS, "1000");
            mListen.setParameter(SpeechConstant.ASR_PTT, "1");
            mListen.setParameter(SpeechConstant.AUDIO_FORMAT,"wav");
            mListen.setParameter(SpeechConstant.ASR_AUDIO_PATH, android.os.Environment.getExternalStorageDirectory()+"/msc/iat.wav");
            mFinalResult = new StringBuffer();
            if(mListen.startListening(mRecognizerListener) != ErrorCode.SUCCESS) {
                android.util.Log.e(TAG, "Failed listen to you!");
            }

        } else {
            ret = false;
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

    private int mapTheVoiceName(int index) {
        int ret = 0;

        if(index < 8 ) {
            return index;
        } else {
            // User choose randomly voice name,
            // so try a random
            ret = (int) (Math.random() * 7);
            if (ret > 7) {
                ret = 7; // force value within 0~7
            }

            return ret;
        }
    }

	private String parseListenResult(String json) {
		StringBuffer ret = new StringBuffer();

		try {
			JSONTokener tokener = new JSONTokener(json);
			JSONObject joResult = new JSONObject(tokener);

			JSONArray words = joResult.getJSONArray("ws");
			for (int i = 0; i < words.length(); i++) {
				// 转写结果词，默认使用第一个结果
				JSONArray items = words.getJSONObject(i).getJSONArray("cw");
				JSONObject obj = items.getJSONObject(0);
				ret.append(obj.getString("w"));
//				如果需要多候选结果，解析数组其他字段
//				for(int j = 0; j < items.length(); j++)
//				{
//					JSONObject obj = items.getJSONObject(j);
//					ret.append(obj.getString("w"));
//				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		} 
		return ret.toString();
	}
}
