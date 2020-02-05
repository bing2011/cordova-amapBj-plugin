var exec = require('cordova/exec');

var camap = {
    getCurrentPosition:function (successFn,errorFn) {
        exec(successFn, errorFn, 'CustomAmap', 'getCurrentPosition', []);
    },
	stopGetCurrentPosition:function (successFn,errorFn) {
        exec(successFn, errorFn, 'CustomAmap', 'stopGetCurrentPosition', []);
    }
};

module.exports = camap;