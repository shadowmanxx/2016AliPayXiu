package com.codeboy.qianghongbao.job;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.Notification;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;

import com.codeboy.qianghongbao.BuildConfig;
import com.codeboy.qianghongbao.Config;
import com.codeboy.qianghongbao.QiangHongBaoService;

import java.util.List;

/**
 * <p>Created 16/1/16 上午12:40.</p>
 * <p><a href="mailto:codeboy2013@gmail.com">Email:codeboy2013@gmail.com</a></p>
 * <p><a href="http://www.happycodeboy.com">LeonLee Blog</a></p>
 *
 * @author LeonLee
 */
public class WechatAccessbilityJob extends BaseAccessbilityJob {

    private static final String TAG = "WechatAccessbilityJob";

    /** 微信的包名*/
    private static final String WECHAT_PACKAGENAME = "com.eg.android.AlipayGphone";

    /** 红包消息的关键字*/
    private static final String HONGBAO_TEXT_KEY = "[微信红包]";

    /** 不能再使用文字匹配的最小版本号 */
    private static final int USE_ID_MIN_VERSION = 700;// 6.3.8 对应code为680,6.3.9对应code为700

    private boolean isFirstChecked ;
    private PackageInfo mWechatPackageInfo = null;
    private Handler mHandler = null;

    private BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            //更新安装包信息
            updatePackageInfo();
        }
    };

    @Override
    public void onCreateJob(QiangHongBaoService service) {
        super.onCreateJob(service);

        updatePackageInfo();

        IntentFilter filter = new IntentFilter();
        filter.addDataScheme("package");
        filter.addAction("android.intent.action.PACKAGE_ADDED");
        filter.addAction("android.intent.action.PACKAGE_REPLACED");
        filter.addAction("android.intent.action.PACKAGE_REMOVED");

        getContext().registerReceiver(broadcastReceiver, filter);
    }

    @Override
    public void onStopJob() {
        try {
            getContext().unregisterReceiver(broadcastReceiver);
        } catch (Exception e) {}
    }

    @Override
    public boolean isEnable() {
        return getConfig().isEnableWechat();
    }

    @Override
    public String getTargetPackageName() {
        return WECHAT_PACKAGENAME;
    }

    @Override
    public void onReceiveJob(AccessibilityEvent event) {
        final int eventType = event.getEventType();
        
       if(eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            openHongBao(event);
        }
    }

    /** 打开通知栏消息*/
    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    private void openNotify(AccessibilityEvent event) {
        if(event.getParcelableData() == null || !(event.getParcelableData() instanceof Notification)) {
            return;
        }

        //以下是精华，将微信的通知栏消息打开
        Notification notification = (Notification) event.getParcelableData();
        PendingIntent pendingIntent = notification.contentIntent;

        isFirstChecked = true;
        try {
            pendingIntent.send();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    private void openHongBao(AccessibilityEvent event) {
    	Log.w(TAG, "EventClass:  "+event.getClassName());
    	
        if("com.alipay.android.wallet.newyear.activity.MonkeyYearActivity".equals(event.getClassName())) {
            //点中了红包，下一步就是去拆红包
            handleLuckyMoneyReceive();
        } else if("com.tencent.mm.plugin.luckymoney.ui.LuckyMoneyDetailUI".equals(event.getClassName())) {
            //拆完红包后看详细的纪录界面
            //nonething
        } else if("com.tencent.mm.ui.LauncherUI".equals(event.getClassName())) {
            //在聊天界面,去点中红包
            handleChatListHongBao();
        }
    }

    /**
     * 点击聊天里的红包后，显示的界面
     * */
    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)    
    private void handleLuckyMoneyReceive() {
        AccessibilityNodeInfo nodeInfo = getService().getRootInActiveWindow();
        if(nodeInfo == null) {
            Log.w(TAG, "rootWindow为空");
            return;
        }
        
        AccessibilityNodeInfo targetNode = null;

        List<AccessibilityNodeInfo> list = null;
        int event = getConfig().getWechatAfterOpenHongBaoEvent();       
        recycle(nodeInfo);

       

       
    }
    
    @SuppressLint("NewApi")
	public void recycle(AccessibilityNodeInfo info) {  
    	Log.w(TAG, "TotalChild: "+info.getChildCount());
        if (info.getChildCount() == 0) {   
            if(info.getText() != null){  
                if("领取红包".equals(info.getText().toString())){  
                    //这里有一个问题需要注意，就是需要找到一个可以点击的View  
                    Log.i("demo", "Click"+",isClick:"+info.isClickable());  
                    info.performAction(AccessibilityNodeInfo.ACTION_CLICK);  
                    AccessibilityNodeInfo parent = info.getParent();  
                    while(parent != null){  
                        Log.i("demo", "parent isClick:"+parent.isClickable());  
                        if(parent.isClickable()){  
                            parent.performAction(AccessibilityNodeInfo.ACTION_CLICK);  
                            break;  
                        }  
                        parent = parent.getParent();  
                    }  
                      
                }  
            }  
              
        } else {    
            for (int i = 0; i < info.getChildCount(); i++) {    
                if(info.getChild(i)!=null){ 
                	 Log.w(TAG, "Childinfo_"+i+": "+info.describeContents());
                    recycle(info.getChild(i));    
                }    
            }    
        }    
    }            
    /**
     * 收到聊天里的红包
     * */
    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    private void handleChatListHongBao() {
        AccessibilityNodeInfo nodeInfo = getService().getRootInActiveWindow();
        if(nodeInfo == null) {
            Log.w(TAG, "rootWindow为空");
            return;
        }

        List<AccessibilityNodeInfo> list = nodeInfo.findAccessibilityNodeInfosByText("领取红包");

        if(list != null && list.isEmpty()) {
            // 从消息列表查找红包
            list = nodeInfo.findAccessibilityNodeInfosByText("[微信红包]");

            if(list == null || list.isEmpty()) {
                return;
            }

            for(AccessibilityNodeInfo n : list) {
                if(BuildConfig.DEBUG) {
                    Log.i(TAG, "-->微信红包:" + n);
                }
                n.performAction(AccessibilityNodeInfo.ACTION_CLICK);
                break;
            }
        } else if(list != null) {
            //最新的红包领起
            for(int i = list.size() - 1; i >= 0; i --) {
                AccessibilityNodeInfo parent = list.get(i).getParent();
                if(BuildConfig.DEBUG) {
                    Log.i(TAG, "-->领取红包:" + parent);
                }
                if(parent != null) {
                    if (isFirstChecked){
                        parent.performAction(AccessibilityNodeInfo.ACTION_CLICK);
                        isFirstChecked = false;
                    }
                    break;
                }
            }
        }
    }

    private Handler getHandler() {
        if(mHandler == null) {
            mHandler = new Handler(Looper.getMainLooper());
        }
        return mHandler;
    }

    /** 获取微信的版本*/
    private int getWechatVersion() {
        if(mWechatPackageInfo == null) {
            return 0;
        }
        return mWechatPackageInfo.versionCode;
    }

    /** 更新微信包信息*/
    private void updatePackageInfo() {
        try {
            mWechatPackageInfo = getContext().getPackageManager().getPackageInfo(WECHAT_PACKAGENAME, 0);
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
    }
}
