var myspeechApis = {};

myspeechApis.tryInitEngine = function(okCb, failCb, opt) {
    cordova.exec(okCb, failCb,
            "MySpeech", // service
            "init", // action name
            {} // a blank option
            );
};

myspeechApis.tryListening = function(okCb, failCb, opt) {
};

myspeechApis.trySpeak = function(okCb, failCb, opt) {
    cordova.exec(okCb, failCb,
            "MySpeech", // service
            "speak", // action name
            ["a", "b", 1, 2]
            );
};

module.exports = myspeechApis;
