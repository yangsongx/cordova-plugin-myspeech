/**
 * A plugin for XunFei voice feature.
 *
 * [2017-02-06] Try support voice wakeup.
 * [2017-01-22] Avoid blocking the main thread
 */
package org.ioniconline;

import android.os.Bundle;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.PluginResult;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

import com.iflytek.cloud.WakeuperResult;
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

import com.iflytek.cloud.WakeuperListener;
import com.iflytek.cloud.VoiceWakeuper;
import com.iflytek.cloud.util.ResourceUtil;
import com.iflytek.cloud.util.ResourceUtil.RESOURCE_TYPE;

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

    private boolean mFirstCall = false;

    // the wakeup obj
    private VoiceWakeuper mWakeup = null;

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
            android.util.Log.e(TAG, "onError meet:" + error);
            if(error.getErrorCode() == 10118) {
                android.util.Log.e(TAG, "You said nothing at all!");
                mCb.error("Say Nothing!");
            }
        }
        @Override
        public void onEndOfSpeech() {
            android.util.Log.e(TAG, "<end of speech");
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
    public boolean execute(String action, JSONArray args, final CallbackContext cb) throws JSONException {
        boolean ret = true;

        mCb = cb;

        android.util.Log.e(TAG, "coming with " + action + " action");

        if(mFirstCall == false) {
            android.util.Log.i(TAG, "call speech util only once.");
            SpeechUtility.createUtility(
                    cordova.getActivity(),
                    SpeechConstant.APPID + "=" + YOUR_APP_ID);

            mFirstCall = true; // DO NOT Call it twice
        }

        if (action.equals("init")) {

            // Init ...
            //
            android.util.Log.e(TAG, "init the engine in background...");

            cordova.getThreadPool().execute(new Runnable(){
                        @Override
                        public void run(){
                            mListen = SpeechRecognizer.createRecognizer(
                                cordova.getActivity(),
                                initListener/* use null listen */);
                            if(mListen == null){
                                android.util.Log.e(TAG, "listener is a null obj...");
                                cb.error("XunFei plugin init with null result[Failed]");
                            } else {
                                android.util.Log.i(TAG, "got a listener, cool.");
                                cb.success("init XunFei plugin [OK]");
                            }

                        }
                    });






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
            android.util.Log.i(TAG, "listening...");

            if(mListen == null){
                android.util.Log.e(TAG, "null listen obj, probably not inited engine.");
                cb.error("engine not inited");
                return false;
            }

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

        } else if (action.equals("initWakeup")) {
            android.util.Log.i(TAG, "Try Init Wakeup");

            cordova.getThreadPool().execute(new Runnable(){
                @Override
                public void run(){
                    mWakeup = VoiceWakeuper.createWakeuper(
                        cordova.getActivity(), null);
                    if(mWakeup == null) {
                        cb.error("wakeuper creation got a null obj");
                    } else {
                        android.util.Log.i(TAG, "wakuper obj creation[OK]");
                        cb.success("init wakeup good");
                    }

                    //initWakeupFeature();
                }
            });

        } else if (action.equals("startWakeup")){
            if(mWakeup != null) {
                /* listener will send back result later */
                mWakeup.startListening(mWakeuperListener);
            } else {
                android.util.Log.e(TAG, "null wakeup obj, do nohting");
                cb.error("error in start wakeup");
            }
        } else if(action.equals("stopWakeup")) {
            android.util.Log.i(TAG, "Try Stop Wakeup");

            if(mWakeup != null) {
                mWakeup.stopListening();
            } else {
                android.util.Log.e(TAG, "null wakeup obj, do nohting");
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

    private int initWakeupFeature(){

        StringBuffer param =new StringBuffer();
        String resPath = ResourceUtil.generateResourcePath(
                cordova.getActivity(),
                RESOURCE_TYPE.assets,
                "ivw/576206f0.jet");
        param.append(","+ResourceUtil.ENGINE_START+"="+SpeechConstant.ENG_IVW);
        SpeechUtility.getUtility().setParameter(
                ResourceUtil.ENGINE_START,
                param.toString());

        mWakeup.setParameter(SpeechConstant.IVW_THRESHOLD,
                "0:"+10/* configable */);
        mWakeup.setParameter(SpeechConstant.IVW_SST,
                "wakeup");

        /* FIXME and TODO - 1 means keep alive,
         * if set with 0, the wakup action will auto-closed
         * after a successful wakup
         *
         * Consider set this properly in the future.
         */
        mWakeup.setParameter(SpeechConstant.KEEP_ALIVE,"1");

        return 0;
    }

    private WakeuperListener mWakeuperListener = new WakeuperListener() {
        @Override
        public void onResult(WakeuperResult result) {
            try{
                String text = result.getResultString();
                android.util.Log.e(TAG, "wakeup listen onResult:" + text);

                JSONObject jsobj = new JSONObject(text);

                android.util.Log.i(TAG, "The wakeup word id:" + jsobj.optString("id"));
                

                mCb.success("COOL");

            } catch (JSONException e) {
                android.util.Log.e(TAG, "Exception meet on wakup listen");
            }
        }

        @Override
	public void onVolumeChanged(int volume) {
        }

        @Override
        public void onError(SpeechError error) {
        }

        @Override
        public void onBeginOfSpeech() {
        }

        @Override
        public void onEvent(int eventType, int arg1, int arg2, Bundle obj) {
            if (com.iflytek.cloud.SpeechEvent.EVENT_IVW_RESULT == eventType) {
                RecognizerResult reslut =
                    ((RecognizerResult)obj.get(com.iflytek.cloud.SpeechEvent.KEY_EVENT_IVW_RESULT));
            }
        }
    };
}
