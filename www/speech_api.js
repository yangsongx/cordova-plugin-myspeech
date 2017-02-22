var myspeechApis = {};

myspeechApis.tryInitEngine = function(okCb, failCb) {
    console.error("plugin JS called");
    cordova.exec(okCb, failCb,
            "MySpeech", // service
            "init", // action name
            [] // a blank option
            );
    console.error("JS call ended");
};

myspeechApis.tryListening = function(okCb, failCb, opt) {
    cordova.exec(okCb, failCb,
            "MySpeech", // service
            "listen", // action name
            opt
            );
};

myspeechApis.trySpeak = function(okCb, failCb, opt) {
    console.error("in speak JS code...");
    cordova.exec(okCb, failCb,
            "MySpeech", // service
            "speak", // action name
            opt
            );
};

myspeechApis.initWakeup = function(okCb, failCb, opt) {
    console.error("in wakeup JS code...");
    cordova.exec(okCb, failCb,
            "MySpeech", // service
            "initWakeup", // action name
            opt
            );
};

myspeechApis.startWakeup = function(okCb, failCb, opt) {
    console.error("in start wakeup JS code...");
    cordova.exec(okCb, failCb,
            "MySpeech", // service
            "startWakeup", // action name
            opt
            );
};

myspeechApis.stopWakeup = function(okCb, failCb, opt) {
    console.error("in stop wakeup JS code...");
    cordova.exec(okCb, failCb,
            "MySpeech", // service
            "stopWakeup", // action name
            opt
            );
};

myspeechApis.tryInitCamera = function(okCb, failCb, opt) {
    cordova.exec(okCb, failCb,
            "MySpeech", // service
            "initCamera", // action name
            opt
            );
};

myspeechApis.cleanCamera = function(okCb, failCb, opt) {
    cordova.exec(okCb, failCb,
            "MySpeech", // service
            "cleanCamera", // action name
            opt
            );
};

myspeechApis.startCameraPreview = function(okCb, failCb, opt) {
    cordova.exec(okCb, failCb,
            "MySpeech", // service
            "startCameraPreview", // action name
            opt
            );
};

myspeechApis.initDuMi = function(okCb, failCb) {
    cordova.exec(okCb, failCb,
            "MySpeech", // service
            "initDuMi", // action name
            [] // a blank option
            );
};

module.exports = myspeechApis;
