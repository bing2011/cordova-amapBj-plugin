package com.zyb.amap;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;

import com.amap.api.location.AMapLocation;
import com.amap.api.location.AMapLocationClient;
import com.amap.api.location.AMapLocationClientOption;
import com.amap.api.location.AMapLocationListener;

import java.lang.ref.WeakReference;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

public class AmapLocationService extends Service {
  private Context mcontext;
  private WeakReference<Context> mContext;
  private String requestId;
  private Set<String> locationSet;
  //声明AMapLocationClient类对象
  public AMapLocationClient mLocationClient = null;
  //声明AMapLocationClientOption对象
  public AMapLocationClientOption mLocationOption = null;
  @SuppressLint("SimpleDateFormat")
  private SimpleDateFormat dateFormat19 = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

  private static int errorNum = 0;
  //声明定位回调监听器
  public AMapLocationListener mLocationListener = new AMapLocationListener() {
    @Override
    public void onLocationChanged(AMapLocation aMapLocation) {
      if (aMapLocation != null) {
        if (aMapLocation.getErrorCode() == 0) {
          errorNum = 0;
          //获取定位时间
          Date cureDate = new Date();
          Date date = new Date(aMapLocation.getTime());
          String message = "******" + dateFormat19.format(cureDate) +
            "\n定位时间： " + dateFormat19.format(date) +
            "\n经度:" + aMapLocation.getLongitude() +
            "  纬度:" + aMapLocation.getLatitude() +
            "  精度:" + aMapLocation.getAccuracy() +
            "\n地址:" + aMapLocation.getAddress();
          //日志
          Log.i("获取定位：", message);
          String key = aMapLocation.getLongitude() + "|" + aMapLocation.getLatitude();
          LocationModel location = new LocationModel();
//                    if(!locationSet.contains(key)){
//                        locationSet.add(key);
          location.setLongitude(aMapLocation.getLongitude());
          location.setLatitude(aMapLocation.getLatitude());
          location.setAccuracy(aMapLocation.getAccuracy());
          location.setTime(dateFormat19.format(date));
          location.setTimestamp(aMapLocation.getTime());
          // }
          AmapLocation.returnLocation(location);
        } else {
          //定位失败时，可通过ErrCode（错误码）信息来确定失败的原因，errInfo是错误信息，详见错误码表。
          String message = "location Error, ErrCode:" + aMapLocation.getErrorCode() + ", errInfo:" + aMapLocation.getErrorInfo();
          errorNum++;
          if(errorNum>3){
            AmapLocation.returnError(aMapLocation);
          }
        }
      }
    }
  };

  public AmapLocationService(Context context) {
    this.mcontext = context;
  }

  @Override
  public void onCreate() {
    //如果需要判断权限，在此处理
    super.onCreate();
    locationSet = new HashSet<String>();
    errorNum = 0;
    initLocation();
    //启动后台定位，第一个参数为通知栏ID，建议整个APP使用一个
    mLocationClient.enableBackgroundLocation(20011, buildNotification());
    mLocationClient.startLocation();
  }

  @Override
  public IBinder onBind(Intent intent) {
    return mBinder;
  }

  @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
  @Override
  public int onStartCommand(Intent intent, int flags, int startId) {

    return super.onStartCommand(intent, flags, startId);
  }

  @Override
  public void onDestroy() {
    if (mLocationClient != null) {
      mLocationClient.disableBackgroundLocation(true);
      mLocationClient.stopLocation();
      mLocationClient.onDestroy();
    }
    super.onDestroy();
  }

  private final IBinder mBinder = new LocalBinder();

  public class LocalBinder extends Binder {
    AmapLocationService getService() {
      return AmapLocationService.this;
    }
  }

  //通知栏
  private static final String NOTIFICATION_CHANNEL_NAME = "BackgroundLocation";
  private NotificationManager notificationManager = null;
  boolean isCreateChannel = false;

  @SuppressLint("NewApi")
  private Notification buildNotification() {

    Notification.Builder builder = null;
    Notification notification = null;
    if (android.os.Build.VERSION.SDK_INT >= 26) {
      //Android O上对Notification进行了修改，如果设置的targetSDKVersion>=26建议使用此种方式创建通知栏
      if (null == notificationManager) {
        this.mContext = new WeakReference<Context>(mcontext);
        notificationManager = (NotificationManager) (mContext.get().getSystemService(Context.NOTIFICATION_SERVICE));
      }
      String channelId = mContext.get().getPackageName();
      if (!isCreateChannel) {
        NotificationChannel notificationChannel = new NotificationChannel(channelId,
          NOTIFICATION_CHANNEL_NAME, NotificationManager.IMPORTANCE_DEFAULT);
        notificationChannel.enableLights(true);//是否在桌面icon右上角展示小圆点
        notificationChannel.setLightColor(Color.BLUE); //小圆点颜色
        notificationChannel.setShowBadge(true); //是否在久按桌面图标时显示此渠道的通知
        notificationManager.createNotificationChannel(notificationChannel);
        isCreateChannel = true;
      }
      builder = new Notification.Builder(mcontext, channelId);
    } else {
      builder = new Notification.Builder(mcontext);
    }
    builder.setContentTitle("养护通")
      .setContentText("正在后台运行")
      .setWhen(System.currentTimeMillis());

    if (android.os.Build.VERSION.SDK_INT >= 16) {
      notification = builder.build();
    } else {
      return builder.getNotification();
    }
    return notification;
  }

  /**
   * 初始化定位
   */
  private void initLocation() {
    //初始化client
    mLocationClient = new AMapLocationClient(this.mcontext);
    mLocationOption = getDefaultOption();
    //设置定位参数
    mLocationClient.setLocationOption(mLocationOption);
    // 设置定位监听
    mLocationClient.setLocationListener(mLocationListener);
  }

  /**
   * 默认定位参数
   *
   * @return
   */
  private AMapLocationClientOption getDefaultOption() {
    AMapLocationClientOption mOption = new AMapLocationClientOption();
    mOption.setLocationMode(AMapLocationClientOption.AMapLocationMode.Hight_Accuracy);//可选，设置定位模式，可选的模式有高精度、仅设备、仅网络。默认为高精度模式
    mOption.setGpsFirst(false);//可选，设置是否gps优先，只在高精度模式下有效。默认关闭
    mOption.setHttpTimeOut(30000);//可选，设置网络请求超时时间。默认为30秒。在仅设备模式下无效
    mOption.setInterval(2000);//可选，设置定位间隔。默认为2秒
    mOption.setNeedAddress(true);//可选，设置是否返回逆地理地址信息。默认是true
    mOption.setOnceLocation(false);//可选，设置是否单次定位。默认是false
    mOption.setOnceLocationLatest(false);//可选，设置是否等待wifi刷新，默认为false.如果设置为true,会自动变为单次定位，持续定位时不要使用
    AMapLocationClientOption.setLocationProtocol(AMapLocationClientOption.AMapLocationProtocol.HTTP);//可选， 设置网络请求的协议。可选HTTP或者HTTPS。默认为HTTP
    mOption.setSensorEnable(false);//可选，设置是否使用传感器。默认是false
    mOption.setWifiScan(true); //可选，设置是否开启wifi扫描。默认为true，如果设置为false会同时停止主动刷新，停止以后完全依赖于系统刷新，定位位置可能存在误差
    mOption.setLocationCacheEnable(true); //可选，设置是否使用缓存定位，默认为true
    return mOption;
  }
}
