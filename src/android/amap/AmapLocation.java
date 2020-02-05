package com.zyb.amap;

import android.Manifest;
import android.util.Log;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.PluginResult;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class AmapLocation extends CordovaPlugin {
    //权限申请码
    private static final int PERMISSION_REQUEST_CODE = 500;

    /**
     * 需要进行检测的权限数组
     */
    protected String[] needPermissions = {
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.READ_PHONE_STATE
    };
    /**
     * JS回调接口对象
     */
    public static CallbackContext cb = null;
    private AmapLocationService amapService;
    @Override
    public boolean execute(String action, JSONArray args, CallbackContext callbackContext) throws JSONException {
        if (action.equals("getCurrentPosition")) {
            cb = callbackContext;
            PluginResult pluginResult = new PluginResult(PluginResult.Status.NO_RESULT);
            pluginResult.setKeepCallback(true);
            cb.sendPluginResult(pluginResult);
            if(this.isNeedCheckPermissions(needPermissions)){
                this.checkPermissions(needPermissions);
            }else{
                // 调用方法将原生代码的执行结果返回给js层并触发相应的JS层回调函数
                amapService =new AmapLocationService(this.webView.getContext());
                amapService.onCreate();
            }

            return true;
        }else if(action.equals("stopGetCurrentPosition")){
            amapService.onDestroy();
            return true;
        }
        return false;
    }

    public static void returnLocation(LocationModel location){
        JSONObject json=new JSONObject();
        JSONObject json2=new JSONObject();
        try {
            json.put("longitude",location.getLongitude());
            json.put("latitude",location.getLatitude());
            json.put("accuracy",location.getAccuracy());
            json2.put("coords",json);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        Log.i("调试输出",json2.toString()+" 时间："+location.getTime());
        PluginResult pluginResult = new PluginResult(PluginResult.Status.OK, json2);
        pluginResult.setKeepCallback(true);
        cb.sendPluginResult(pluginResult);
    }

    /**
     * 判断是否需要检查权限
     *
     * @author zhaoying
     */

    private boolean isNeedCheckPermissions(String... permissions) {
        List<String> needRequestPermissonList = findNeedPermissions(permissions);
        if (null != needRequestPermissonList && needRequestPermissonList.size() > 0) {
            return true;
        } else {
            return false;
        }
    }


    /**
     * 检查权限
     *
     * @author zhaoying
     */
    private void checkPermissions(String... permissions) {
        try {
            List<String> needRequestPermissonList = findNeedPermissions(permissions);
            if (null != needRequestPermissonList && needRequestPermissonList.size() > 0) {
                String[] array = needRequestPermissonList.toArray(new String[needRequestPermissonList.size()]);
                cordova.requestPermissions(this, PERMISSION_REQUEST_CODE, array);
            }
        } catch (Throwable e) {

        }
    }

    /**
     * 获取需要获取权限的集合
     *
     * @author zhaoying
     */
    private List<String> findNeedPermissions(String[] permissions) {
        List<String> needRequestPermissonList = new ArrayList<String>();
        try {
            for (String perm : permissions) {
                if (!cordova.hasPermission(perm)) {
                    needRequestPermissonList.add(perm);
                }
            }
        } catch (Throwable e) {

        }
        return needRequestPermissonList;
    }

    /*
     * 权限检查回调
     *
     * @author zhaoying
     * */
    public void onRequestPermissionResult(int requestCode,
                                          String[] permissions, int[] paramArrayOfInt) {
        if (requestCode == PERMISSION_REQUEST_CODE) {
            // 调用方法将原生代码的执行结果返回给js层并触发相应的JS层回调函数
            amapService =new AmapLocationService(this.webView.getContext());
            amapService.onCreate();
        }
    }
}
