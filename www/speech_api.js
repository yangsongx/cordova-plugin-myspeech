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

module.exports = myspeechApis;
