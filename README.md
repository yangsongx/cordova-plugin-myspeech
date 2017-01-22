# cordova-plugin-myspeech

This is a demo cordova Plugin, to show how to write a
workable plugin module.

It can be usable in Ionic Framework APK.

## Usage

> $ ionic plugin add https://github.com/yangsongx/cordova-plugin-myspeech

Now, add code like this(in your JS script, usually):

> navigator.myspeechApis.tryInitEngine(
>         function(goodData) {
>         },
>         function(badData) {
>         },
>         []);
