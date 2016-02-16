var argscheck = require('cordova/argscheck'),
  utils = require('cordova/utils'),
  exec = require('cordova/exec');

var PLUGIN_NAME = "CameraViewRecord";

var CameraViewRecord = function() {};

CameraViewRecord.setOnVideoTakenHandler = function(onVideoTaken) {
  exec(onVideoTaken, onVideoTaken, PLUGIN_NAME, "setOnVideoTakenHandler", []);
};

//@param rect {x: 0, y: 0, width: 100, height:100}
//@param defaultCamera "front" | "back"
CameraViewRecord.startCamera = function(rect, defaultCamera, tapEnabled, dragEnabled, toBack, alpha) {
  if (typeof(alpha) === 'undefined') alpha = 1;
  exec(null, null, PLUGIN_NAME, "startCamera", [rect.x, rect.y, rect.width, rect.height]);
};

CameraViewRecord.stopCamera = function() {
  exec(null, null, PLUGIN_NAME, "stopCamera", []);
};

CameraViewRecord.startRecording = function (duration, success, failure) {
  exec(function () {
    success && success();
  }, function () {
    failure && failure();
  }, PLUGIN_NAME, "startRecording", [duration]);
};

CameraViewRecord.stopRecording = function (success, failure) {
  exec(function () {
    success && success();
  }, function () {
    failure && failure();
  }, PLUGIN_NAME, "stopRecording", []);
};

CameraViewRecord.hide = function() {
  exec(null, null, PLUGIN_NAME, "hideCamera", []);
};

CameraViewRecord.show = function() {
  exec(null, null, PLUGIN_NAME, "showCamera", []);
};

CameraViewRecord.disable = function(disable) {
  exec(null, null, PLUGIN_NAME, "disable", [disable]);
};

module.exports = CameraViewRecord;
