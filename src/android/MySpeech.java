/**
 * A plugin for XunFei voice feature.
 *
 * [2017-02-14] Add camera input method
 * [2017-02-06] Try support voice wakeup.
 * [2017-01-22] Avoid blocking the main thread
 */
package org.ioniconline;

import android.graphics.ImageFormat;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.os.Bundle;
import android.hardware.Camera;
import android.util.Base64;

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

import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

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

    private CallbackContext mCamCb;

    private Camera.PreviewCallback mCamCallback =
        new Camera.PreviewCallback() {
            @Override
            public void onPreviewFrame(byte[] arg0, Camera arg1) {
                android.util.Log.e(TAG, "Wow, incoming with "
                        + arg0.length + " Bytes data");
                if (arg1.getParameters().getPreviewFormat() != ImageFormat.NV21){
                    android.util.Log.e(TAG, "probably not correct format, check hardware");

                    // FIXME - currently, we just need one frame at one time
                    stopCameraPreview();
                    mCamCb.error("Check your hardware!");
                    return;
                }

                String imgB64Val = "";
                YuvImage image = new YuvImage(arg0, ImageFormat.NV21,
                        mCamPrevSize.width,
                        mCamPrevSize.height,
                        null);

                File pic = new File(mCamDefaultImgName);
                FileOutputStream outputStream;
                try{
                    outputStream = new FileOutputStream(pic);
                    image.compressToJpeg(new Rect(0, 0, image.getWidth(),
                                image.getHeight()),
                            70,  outputStream);

                    outputStream.close();

                    //imgB64Val = imageBase64Format(mCamDefaultImgName);

                    //mCamCb.success(imgB64Val);

                    // FIXME - currently, we just need one frame at one time
                    stopCameraPreview();

                } catch (FileNotFoundException e) {
                    android.util.Log.e(TAG, "exception");
                    e.printStackTrace();
                    // FIXME - need send to JS for such case?
                } catch (IOException ex) {
                    android.util.Log.e(TAG, "exception in IO.");
                    ex.printStackTrace();
                }

                // try detect
                MyFaceDetect mfd = new MyFaceDetect(mCamDefaultImgName);
                android.util.Log.i(TAG, "face detected begin...");
                // JSON data return to JS side...
                mCamCb.success(mfd.faceData());
            }
        };

    private Camera.Size mCamPrevSize;
    // TODO as we only do this via oneshot, below variable
    // will not be used in the future...
    private byte [] mCamPrevBuf;

    private int mPreviewFlag = 0; // 1 means keep previewing, otherwise is one-shot

    // FIXME - a temp debug code...
    private String mCamDefaultImgName = "/sdcard/my.jpg";

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

    private Camera mCamera;

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
            if(error.getErrorCode() == 10118
               || ErrorCode.MSP_ERROR_NO_DATA == error.getErrorCode()) {
                android.util.Log.e(TAG, "You said nothing at all!");
                mCb.error("Say Nothing!");
            } else if (error.getErrorCode() == 10114
               || ErrorCode.MSP_ERROR_TIME_OUT == error.getErrorCode()) {
                android.util.Log.e(TAG, "Time out !");
                mCb.error("MSP_ERROR_TIME_OUT");
            } else if (error.getErrorCode() == 20001
               || ErrorCode.ERROR_NO_NETWORK == error.getErrorCode()) {
                android.util.Log.e(TAG, "no network !");
                mCb.error("ERROR_NO_NETWORK");
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

    /* FIXME - call this in two thread Pool won't cause race-condition? */
    private int initSafe() {
        if(mFirstCall == false) {
            android.util.Log.i(TAG, "call speech util only once.");
            SpeechUtility.createUtility(
                    cordova.getActivity(),
                    SpeechConstant.APPID + "=" + YOUR_APP_ID);

            mFirstCall = true; // DO NOT Call it twice
        }
        return 0;
    }

    @Override
    public boolean execute(String action, JSONArray args, final CallbackContext cb) throws JSONException {
        boolean ret = true;

        mCb = cb;

        android.util.Log.e(TAG, "coming with " + action + " action");

        if (action.equals("init")) {

            // Init ...
            //
            android.util.Log.e(TAG, "init the engine in background...");

            cordova.getThreadPool().execute(new Runnable(){
                        @Override
                        public void run(){
                            initSafe();
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

        } else if (action.equals("initCamera")) {

            initAndroidCamera(args, cb);

        } else if (action.equals("cleanCamera")) {

            cleanAndroidCamera(cb);

        } else if (action.equals("startCameraPreview")) {

            mCamCb = cb;
            startCameraPreview(args, cb);

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
                    initSafe();
                    cb.success("init my wakeup good");

                }
            });

        } else if (action.equals("startWakeup")){
            StringBuffer param =new StringBuffer();
            String resPath = ResourceUtil.generateResourcePath(
                    cordova.getActivity(),
                    RESOURCE_TYPE.assets,
                    "ivw/576206f0.jet");
            param.append(ResourceUtil.IVW_RES_PATH+"="+resPath);
            param.append(","+ResourceUtil.ENGINE_START+"="+SpeechConstant.ENG_IVW);

            SpeechUtility.getUtility().setParameter(
                    ResourceUtil.ENGINE_START,
                    param.toString());

            android.util.Log.e(TAG, "p:" + param.toString());

            mWakeup = VoiceWakeuper.createWakeuper(
                cordova.getActivity(), null);
            if(mWakeup == null) {
                cb.error("wakeuper creation got a null obj");
            } else {
                android.util.Log.i(TAG, "wakuper obj creation[OK]");
                //DO NOT CALL this at init phase...
                //initWakeupFeature();
                // cb.success("createWakeuper good");
            }
            if(mWakeup != null) {
                initWakeupFeature();
                /* listener will send back result later */
                android.util.Log.i(TAG, "set the listeners...");
                mWakeup.startListening(mWakeuperListener);
            } else {
                android.util.Log.e(TAG, "null wakeup obj, do nohting");
                cb.error("error in start wakeup");
            }
        } else if(action.equals("stopWakeup")) {
            android.util.Log.i(TAG, "Try Stop Wakeup");
            if (mWakeup == null) {
                mWakeup = VoiceWakeuper.getWakeuper();
            }
            if(mWakeup != null) {
                mWakeup.stopListening();
                mWakeup.destroy();
                mWakeup = null;
                cb.success("success stop wakeup");
            } else {
                android.util.Log.e(TAG, "null wakeup obj, do nohting");
                cb.success("success stop null wakeup");
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

        mWakeup.setParameter(
                SpeechConstant.IVW_THRESHOLD,
                "0:20;1:20");

        mWakeup.setParameter(
                SpeechConstant.IVW_SST,
                "wakeup");

        /* FIXME and TODO - 1 means keep alive,
         * if set with 0, the wakup action will auto-closed
         * after a successful wakup
         *
         * Consider set this properly in the future.
         */
        mWakeup.setParameter(SpeechConstant.KEEP_ALIVE,"0");

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
            android.util.Log.e(TAG, "onVolumeChanged");
        }

        @Override
        public void onError(SpeechError error) {
            /* NOTE
             * 10000~20000 are ranges in C/C++ Layer
             * 20000~      are ranges in Java/Jar Layer
             */
            android.util.Log.e(TAG, "wakeup error:" +
                    error.getErrorCode() + ", " +
                    error.getErrorDescription());
        }

        @Override
        public void onBeginOfSpeech() {
            android.util.Log.e(TAG, "onBeginOfSpeech");
        }

        @Override
        public void onEvent(int eventType, int arg1, int arg2, Bundle obj) {
            android.util.Log.e(TAG, "onEvent");
            if (com.iflytek.cloud.SpeechEvent.EVENT_IVW_RESULT == eventType) {
                RecognizerResult reslut =
                    ((RecognizerResult)obj.get(com.iflytek.cloud.SpeechEvent.KEY_EVENT_IVW_RESULT));
            }
        }
    };

    private int initAndroidCamera(JSONArray args, final CallbackContext cb) {
        if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.LOLLIPOP) {
            // Camera legacy interface
        } else {
            // camera2 new interface
        }

        /* FIXME - currently, still use legacy interface, in
         * the future, SHOULD use newer camera2 interface
         */

        String model = new android.os.Build().MODEL;
        if(model.equals("MSM8916 for arm64") == false) {
            /* FIXME - is it correct for QLove device? */
            android.util.Log.e(TAG, "Sorry, only QLOVE DEVICE can do this.");
            cb.error("Non-Qlove device");
            return -1;
        }

        try{
            int idx = args.getInt(0);
            mPreviewFlag = args.getInt(1);
            android.util.Log.i(TAG, "Try init [" + idx + "] camera, with " + mPreviewFlag + " preview flag");

            mCamera = Camera.open(idx);
            if(mCamera == null) {
                android.util.Log.e(TAG, "null obj for opening camera");
                cb.error("camera open got null obj");
            } else {
                cb.success("camera open OK");
            }
        } catch (JSONException ex) {
            android.util.Log.e(TAG, "Exception in init");
            cb.error("exception");
        }

        return 0;
    }

    private int cleanAndroidCamera(final CallbackContext cb) {
        if(mCamera != null) {
            mCamera.stopPreview(); // is it necessary?
            mCamera.release();
            mCamera = null;
        }

        cb.success("cleaned OK");
        return 0;
    }

    private int startCameraPreview(JSONArray args, final CallbackContext cb) {
        if(mCamera == null) {
            android.util.Log.e(TAG, "null camera, do nothing");
            cb.error("null camera obj");
            return -1;
        }

        Camera.Parameters camParam = mCamera.getParameters();

        /* below code suggested by Holly */
        camParam.setPreviewSize(640, 480);
        camParam.setPreviewFpsRange(20000, 20000);

        mCamera.setParameters(camParam);

        mCamera.setDisplayOrientation(90);
        /* FIXME, seems we only can do this via oneshot way */
        mCamera.setOneShotPreviewCallback(mCamCallback);

        mCamPrevSize = mCamera.getParameters().getPreviewSize();

        android.util.Log.e(TAG, "the preview w:"
                + mCamPrevSize.width + ", h:" + mCamPrevSize.height);


        //let's do it!
        mCamera.startPreview();

        return 0;
    }

    private int stopCameraPreview() {
        if(mCamera == null) {
            android.util.Log.e(TAG, "already null camera obj");
            return -1;
        }

        mCamera.stopPreview();

        return 0;
    }

    private String imageBase64Format(String fn) throws FileNotFoundException, IOException {
        String b64str = "";

        File file = new File(fn);
        int fileSize = (int)file.length();
        byte[] binBuf = new byte[fileSize];
        FileInputStream in = new FileInputStream(file);
        DataInputStream ds = new DataInputStream(in);
        ds.read(binBuf, 0, fileSize);
        ds.close();
        in.close();
        b64str = Base64.encodeToString(binBuf, Base64.DEFAULT);

        return b64str;
    }
}
