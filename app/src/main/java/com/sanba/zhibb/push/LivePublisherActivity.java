package com.sanba.zhibb.push;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Service;
import android.content.ContentResolver;
import android.content.DialogInterface;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.database.ContentObserver;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.util.TypedValue;
import android.view.Surface;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.sanba.zhibb.R;
import com.sanba.zhibb.push.widge.BeautySettingPannel;
import com.tencent.rtmp.ITXLivePushListener;
import com.tencent.rtmp.TXLiveConstants;
import com.tencent.rtmp.TXLivePushConfig;
import com.tencent.rtmp.TXLivePusher;
import com.tencent.rtmp.ui.TXCloudVideoView;

import java.text.SimpleDateFormat;

/**
 * 推流界面
 * <p>
 * 录制视频 推到流媒体服务区  播放
 * 1 添加界面元素
 * 2 创建Pusher对象
 * 不过在创建 LivePush 对象之前，还需要您指定一个LivePushConfig对象，该对象的用途是决定 LivePush 推流时各个环节的配置参数，
 * 比如推流用多大的分辨率、每秒钟要多少帧画面（FPS）以及Gop（表示多少秒一个I帧）等等。
 * TXLivePusher mLivePusher = new TXLivePusher(getActivity());
 * mLivePushConfig = new TXLivePushConfig();
 * mLivePusher.setConfig(mLivePushConfig);
 * 3 启动推流 经过step1 和 step2 的准备之后，用下面这段代码就可以启动推流了：
 * String rtmpUrl = "rtmp://2157.livepush.myqcloud.com/live/xxxxxx";
 * mLivePusher.startPusher(rtmpUrl);
 * TXCloudVideoView mCaptureView = (TXCloudVideoView) view.findViewById(R.id.video_view);
 * mLivePusher.startCameraPreview(mCaptureView);
 * 4 设定清晰度 setVideoQuality()
 * 高清	VIDEO_QUALITY_HIGH_DEFINITION	540P	推荐选择该档位，能确保绝大多数主流手机都能推出很清晰的画面。
 * 标清	VIDEO_QUALITY_STANDARD_DEFINITION	360P	如果您比较关注带宽成本，推荐选择该档位，
 * 画质一般，但带宽成本较高清档要低 60%。
 * 动态	VIDEO_QUALITY_QOS_DEFINITION	动态	分辨率会根据网络情况从 192 * 336 - 540 * 960 动态调整，从而更好地适应网络波动，比较适合海外直播这类网络环境差异大的场景。
 * 特别提醒：并非所有播放器都能兼容这种视频流。
 * 超清	VIDEO_QUALITY_SUPER_DEFINITION	720P	场景提醒：如果您的场景多是小屏观看不推荐使用。
 * 如果是大屏幕观看，且主播网络质量很好可以考虑。
 * `5  美颜滤镜   美颜 mLivePusher.setBeautyFilter(7, 3);
 * ]            滤镜   mLivePusher.setFilter(bmp);
 * 曝光  setExposureCompensation 的参数为 -1 到 1 的浮点数： 0 表示不调整， -1 是将曝光降到最低， 1 表示是将曝光加强到最高。
 * 6: 控制摄像头  // 默认是前置摄像头  mLivePusher.switchCamera();
 * 切换前置或后置摄像头
 * 打开或关闭闪光灯
 * 摄像头自动或手动对焦
 * 7 设置Logo水印
 * //设置视频水印
 * mLivePushConfig.setWatermark(BitmapFactory.decodeResource(getResources(),R.drawable.watermark), 10, 10);
 * mLivePusher.setConfig(mLivePushConfig);
 * 8 : 硬件编码
 * if (!HWSupportList.isHWVideoEncodeSupport()){
 * Toast.makeText(getActivity().getApplicationContext(),
 * "当前手机型号未加入白名单或API级别过低（最低18）,请慎重开启硬件编码！",
 * Toast.LENGTH_SHORT).show();
 * }
 * mLivePushConfig.setHardwareAcceleration(mHWVideoEncode);
 * mLivePusher.setConfig(mLivePushConfig);
 * 兼容性评估  如果您使用硬件编码失败，SDK 内部会自动切换为软件编码。
 * 效果差异
 * 推荐的设计
 * 9 后台推流
 * .
 * .
 * .
 * .
 * 13  结束推流
 * //结束推流，注意做好清理工作
 * public void stopRtmpPublish() {
 * mLivePusher.stopCameraPreview(true); //停止摄像头预览
 * mLivePusher.stopPusher();            //停止推流
 * mLivePusher.setPushListener(null);   //解绑 listener
 * }
 */
public class LivePublisherActivity extends Activity implements View.OnClickListener, ITXLivePushListener, BeautySettingPannel.IOnBeautyParamsChangeListener/*, ImageReader.OnImageAvailableListener*/ {
    private static final String TAG = LivePublisherActivity.class.getSimpleName();

    private TXLivePushConfig mLivePushConfig;  // 推流端相关配置
    private TXLivePusher mLivePusher;               //推流控制器
    private TXCloudVideoView mCaptureView;          //推流视频容器

    private LinearLayout mBitrateLayout;                //清晰度选择界面
    private BeautySettingPannel mBeautyPannelView;   //美图选择界面
    private ScrollView mScrollView;                 //打印log的容器
    private RadioGroup mRadioGroupBitrate;              //分辨率的容器
    private Button mBtnBitrate;                         //分辨率界面显示开关
    private Button mBtnPlay;                    //开始录屏或暂停录屏
    private Button mBtnFaceBeauty;              //
    private Button mBtnFlashLight;
    private Button mBtnTouchFocus;
    private Button mBtnHWEncode;
    private Button mBtnOrientation;
    public TextView mLogViewStatus;
    public TextView mLogViewEvent;
    protected int mActivityType;
    private boolean mPortrait = true;         //手动切换，横竖屏推流

    private boolean mVideoPublish;  //是在推送视频流
    private boolean mFrontCamera = true; // 是否使用前置摄像头
    private boolean mHWVideoEncode = false;  //默认禁用硬件加速
    private boolean mFlashTurnOn = false;
    private boolean mTouchFocus = true;
    private boolean mHWListConfirmDialogResult = false;
    private int mBeautyLevel = 5;
    private int mWhiteningLevel = 3;

    private Handler mHandler = new Handler();

    private Bitmap mBitmap;

    protected StringBuffer mLogMsg = new StringBuffer("");
    private final int mLogMsgLenLimit = 3000;
    private int mNetBusyCount = 0;
    private Handler mNetBusyHandler;
    private TextView mNetBusyTips;

    public static final int ACTIVITY_TYPE_PUBLISH = 1;
    public static final int ACTIVITY_TYPE_LIVE_PLAY = 2;
    public static final int ACTIVITY_TYPE_VOD_PLAY = 3;
    public static final int ACTIVITY_TYPE_LINK_MIC = 4;

    private String inputUrl;
//    private final int           REQUEST_CODE_CS = 10001;

//    private MediaProjectionManager      mProjectionManager;
//    private MediaProjection             mMediaProjection;
//    private VirtualDisplay mVirtualDisplay         = null;
//    private ImageReader mImageReader            = null;

    // 关注系统设置项“自动旋转”的状态切换
    private RotationObserver mRotationObserver = null;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //直播推流器  代称
        mLivePusher = new TXLivePusher(this); //完成推流工作
        //  推流的一些设置
        mLivePushConfig = new TXLivePushConfig();
        //设置推流器配置信息.推荐在启动推流前设置配置信息.
        mLivePusher.setConfig(mLivePushConfig);
        // 资源水印图 转换为 bitmap 图
        mBitmap = decodeResource(getResources(), R.mipmap.ic_launcher);
        // 屏幕监测
        mRotationObserver = new RotationObserver(new Handler());
        //注册观察
        mRotationObserver.startObserver();
        //电话管理
        TelephonyManager tm = (TelephonyManager) getSystemService(Service.TELEPHONY_SERVICE);
        // 监听来电时
        tm.listen(listener, PhoneStateListener.LISTEN_CALL_STATE);
        //设置布局
        setContentView();
        LinearLayout backLL = (LinearLayout) findViewById(R.id.back_ll);
        backLL.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
        TextView titleTV = (TextView) findViewById(R.id.title_tv);
        titleTV.setText(getIntent().getStringExtra("TITLE"));
        inputUrl = getIntent().getStringExtra("PLAY_URL");
        startPublishRtmp();//进人界面后 开始就推流
    }

    @Override
    public void onResume() {
        super.onResume();

        if (mCaptureView != null) {
            mCaptureView.onResume();
        }

        if (mVideoPublish && mLivePusher != null) {
            //恢复推流
            mLivePusher.resumePusher();
        }
    }

    @Override
    public void onPause() {
        super.onPause();
    }

    @Override
    public void onStop() {
        super.onStop();
        if (mCaptureView != null) {
            mCaptureView.onPause();
        }

        if (mVideoPublish && mLivePusher != null) {
            //暂停推流.
            mLivePusher.pausePusher();
        }

    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        //停止推流
        stopPublishRtmp();
        if (mCaptureView != null) {
            mCaptureView.onDestroy();
        }
        //取消观察
        mRotationObserver.stopObserver();
    }

    private Bitmap decodeResource(Resources resources, int id) {
        TypedValue value = new TypedValue();
        resources.openRawResource(id, value);
        BitmapFactory.Options opts = new BitmapFactory.Options();
        opts.inTargetDensity = value.density;
        return BitmapFactory.decodeResource(resources, id, opts);
    }

    final PhoneStateListener listener = new PhoneStateListener() {
        @Override
        public void onCallStateChanged(int state, String incomingNumber) {
            super.onCallStateChanged(state, incomingNumber);
            switch (state) {
                //电话等待接听
                case TelephonyManager.CALL_STATE_RINGING:
                    if (mLivePusher != null) mLivePusher.pausePusher();
                    break;
                //电话接听
                case TelephonyManager.CALL_STATE_OFFHOOK:
                    if (mLivePusher != null) mLivePusher.pausePusher();
                    break;
                //电话挂机
                case TelephonyManager.CALL_STATE_IDLE:
                    if (mLivePusher != null) mLivePusher.resumePusher();
                    break;
            }
        }
    };

    protected void initView() {

        //相关状态
        mLogViewEvent = (TextView) findViewById(R.id.logViewEvent);
        //相关状态
        mLogViewStatus = (TextView) findViewById(R.id.logViewStatus);

    }

    //滑动到底部
    public static void scroll2Bottom(final ScrollView scroll, final View inner) {
        if (scroll == null || inner == null) {
            return;
        }
        int offset = inner.getMeasuredHeight() - scroll.getMeasuredHeight();
        if (offset < 0) {
            offset = 0;
        }
        scroll.scrollTo(0, offset);
    }

    public void setContentView() {
        //设置布局
        super.setContentView(R.layout.activity_publish);

        initView();

        mCaptureView = (TXCloudVideoView) findViewById(R.id.video_view);
        mCaptureView.disableLog(true);

        //网路不好的提醒
        mNetBusyTips = (TextView) findViewById(R.id.netbusy_tv);
        mVideoPublish = true;  //默认开始录制
        mLogViewStatus.setVisibility(View.GONE);
        //设置mLogViewStatus设置  文本滚动
        mLogViewStatus.setMovementMethod(new ScrollingMovementMethod());
        //设置mLogViewStatus设置  文本滚动
        mLogViewEvent.setMovementMethod(new ScrollingMovementMethod());
        //状态父容器
        mScrollView = (ScrollView) findViewById(R.id.scrollview);
        mScrollView.setVisibility(View.GONE);


        //美颜p图部分
        mBeautyPannelView = (BeautySettingPannel) findViewById(R.id.layoutFaceBeauty);
        //选择不同的美颜效果 改变时
        mBeautyPannelView.setBeautyParamsChangeListener(this);
        //效果按钮
        mBtnFaceBeauty = (Button) findViewById(R.id.btnFaceBeauty);
        mBtnFaceBeauty.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //显示 隐藏 不同的 效果操作按钮
                mBeautyPannelView.setVisibility(mBeautyPannelView.getVisibility() == View.VISIBLE ? View.GONE : View.VISIBLE);
            }
        });

        //播放部分
        mBtnPlay = (Button) findViewById(R.id.btnPlay);
        mBtnPlay.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if (mVideoPublish) {
                    //停止推送
                    stopPublishRtmp();
                } else {
                    FixOrAdjustBitrate();  //根据设置确定是“固定”还是“自动”码率
                    //启动摄像头 并且开始推送视频流
                    mVideoPublish = startPublishRtmp();
//                    StartScreenCapture();
                }
            }
        });


        //log部分
        final Button btnLog = (Button) findViewById(R.id.btnLog);
        btnLog.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mLogViewStatus.getVisibility() == View.GONE) {
                    mLogViewStatus.setVisibility(View.VISIBLE);
                    mScrollView.setVisibility(View.VISIBLE);
                    mLogViewEvent.setText(mLogMsg);
                    scroll2Bottom(mScrollView, mLogViewEvent);
                    btnLog.setBackgroundResource(R.mipmap.log_hidden);
                } else {
                    mLogViewStatus.setVisibility(View.GONE);
                    mScrollView.setVisibility(View.GONE);
                    btnLog.setBackgroundResource(R.mipmap.log_show);
                }
            }
        });

        //切换前置后置摄像头
        final Button btnChangeCam = (Button) findViewById(R.id.btnCameraChange);
        btnChangeCam.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mFrontCamera = !mFrontCamera;
                //是否正在推流
                if (mLivePusher.isPushing()) {
                    //切换前置或后置摄像头.
                    mLivePusher.switchCamera();
                } else {
                    //是否使用前置摄像头. true:使用前置摄像头. false:不使用前置摄像头,使用后置摄像头.默认使用前置摄像头.
//                    mLivePushConfig.setFrontCamera(mFrontCamera);
                    mLivePusher.switchCamera();
                }
                btnChangeCam.setBackgroundResource(mFrontCamera ? R.mipmap.camera_change : R.mipmap.camera_change2);
            }
        });

        //开启硬件加速  设置默认值 false 未开启硬件加速
        mBtnHWEncode = (Button) findViewById(R.id.btnHWEncode);
        mBtnHWEncode.getBackground().setAlpha(mHWVideoEncode ? 255 : 100);
        mBtnHWEncode.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                boolean HWVideoEncode = mHWVideoEncode;
                mHWVideoEncode = !mHWVideoEncode;
                mBtnHWEncode.getBackground().setAlpha(mHWVideoEncode ? 255 : 100);

                if (mHWVideoEncode) {  //如果想要开启  需要条件
                    if (mLivePushConfig != null) {
                        if (Build.VERSION.SDK_INT < 18) {
                            Toast.makeText(getApplicationContext(), "硬件加速失败，当前手机API级别过低（最低18）", Toast.LENGTH_SHORT).show();
                            mHWVideoEncode = false;  //结果是没有开启
                        }
                    }
                }
                if (HWVideoEncode != mHWVideoEncode) {
                    //启用或禁用硬件加速.
                    //encodeOpt - 硬件加速选项. TXLiveConstants.ENCODE_VIDEO_HARDWARE:开启硬件加速.
                    // TXLiveConstants.ENCODE_VIDEO_SOFTWARE:禁用硬件加速.默认禁用硬件加速.
                    // TXLiveConstants.ENCODE_VIDEO_AUTO:自动选择是否启用硬件加速
                    mLivePushConfig.setHardwareAcceleration(mHWVideoEncode ? TXLiveConstants.ENCODE_VIDEO_HARDWARE : TXLiveConstants.ENCODE_VIDEO_SOFTWARE);
                    if (mHWVideoEncode == false) {
                        Toast.makeText(getApplicationContext(), "取消硬件加速", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(getApplicationContext(), "开启硬件加速", Toast.LENGTH_SHORT).show();
                    }
                }
                //设置推流器配置信息.推荐在启动推流前设置配置信息.
                if (mLivePusher != null) {
                    mLivePusher.setConfig(mLivePushConfig);
                }
            }
        });

        //码率自适应部分
        mBtnBitrate = (Button) findViewById(R.id.btnBitrate);
        mBitrateLayout = (LinearLayout) findViewById(R.id.layoutBitrate);
        mBtnBitrate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mBitrateLayout.setVisibility(mBitrateLayout.getVisibility() == View.VISIBLE ? View.GONE : View.VISIBLE);
            }
        });
        //不同分辩率  码率不同 720p
        mRadioGroupBitrate = (RadioGroup) findViewById(R.id.resolutionRadioGroup);
        //点击不同码率的按钮  选择不同的码率
        mRadioGroupBitrate.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {

            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                //设置码率相关的动作
                FixOrAdjustBitrate();
                mBitrateLayout.setVisibility(View.GONE);
            }
        });

        //闪光灯
        mBtnFlashLight = (Button) findViewById(R.id.btnFlash);
        mBtnFlashLight.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mLivePusher == null) {
                    return;
                }

                mFlashTurnOn = !mFlashTurnOn;
                /** //打开或关闭闪光灯.  true:打开成功. false:打开失败.
                 * Parameters:
                 enable - 是否打开闪光灯. true:打开闪光灯. false:关闭闪光灯.
                 Returns:
                 是否成功打开闪光灯. true:打开成功. false:打开失败.
                 */

                if (!mLivePusher.turnOnFlashLight(mFlashTurnOn)) {
                    Toast.makeText(getApplicationContext(),
                            "打开闪光灯失败（1）大部分前置摄像头并不支持闪光灯（2）该接口需要在启动预览之后调用", Toast.LENGTH_SHORT).show();
                }

                mBtnFlashLight.setBackgroundResource(mFlashTurnOn ? R.mipmap.flash_off : R.mipmap.flash_on);
            }
        });

        //手动对焦/自动对焦 聚焦方式 默然是自动聚焦  点击切换聚焦方式   必须是后置摄像头
        mBtnTouchFocus = (Button) findViewById(R.id.btnTouchFoucs);
        mBtnTouchFocus.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mFrontCamera) {
                    return;
                }

                mTouchFocus = !mTouchFocus;
                //设置是否开启手动对焦. enable - 是否开启手动对焦. true:开启手动对焦. false:不开启手动对焦.默认开启手动对焦.
                mLivePushConfig.setTouchFocus(mTouchFocus);
                v.setBackgroundResource(mTouchFocus ? R.mipmap.automatic : R.mipmap.manual);
                if (mLivePusher.isPushing()) { //是否正在推流.
                    mLivePusher.stopCameraPreview(false); //停止摄像头预览.
                    mLivePusher.startCameraPreview(mCaptureView); //启动摄像头预览.
                }

                Toast.makeText(LivePublisherActivity.this, mTouchFocus ? "已开启手动对焦" : "已开启自动对焦", Toast.LENGTH_SHORT).show();
            }
        });

        //锁定Activity不旋转的情况下，才能进行横屏|竖屏推流切换
        mBtnOrientation = (Button) findViewById(R.id.btnPushOrientation);
        if (isActivityCanRotation()) {
            mBtnOrientation.setVisibility(View.GONE);
        }
        //点击方向键
        mBtnOrientation.setOnClickListener(new View.OnClickListener() {
            @Override

            public void onClick(View view) {
                mPortrait = !mPortrait;
                int renderRotation = 0;
                if (mPortrait) {
                    mLivePushConfig.setHomeOrientation(TXLiveConstants.VIDEO_ANGLE_HOME_DOWN);
                    mBtnOrientation.setBackgroundResource(R.mipmap.landscape);
                    renderRotation = 0;
                } else {
                    mLivePushConfig.setHomeOrientation(TXLiveConstants.VIDEO_ANGLE_HOME_RIGHT);
                    mBtnOrientation.setBackgroundResource(R.mipmap.portrait);
                    renderRotation = 270;
                }
                mLivePusher.setRenderRotation(renderRotation);
                mLivePusher.setConfig(mLivePushConfig);
            }
        });

        View view = findViewById(android.R.id.content);
        view.setOnClickListener(this);
        mLogViewStatus.setText("Log File Path:" + Environment.getExternalStorageDirectory().getAbsolutePath() + "/txRtmpLog");
    }

    protected void HWListConfirmDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(LivePublisherActivity.this);
        builder.setMessage("警告：当前机型不在白名单中,是否继续尝试硬编码？");
        builder.setPositiveButton("确认", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
                mHWListConfirmDialogResult = true;
                throw new RuntimeException();
            }
        });
        builder.setNegativeButton("取消", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
                mHWListConfirmDialogResult = false;
                throw new RuntimeException();
            }
        });
        builder.setCancelable(false);
        builder.create().show();
        try {
            Looper.loop();
        } catch (Exception e) {
        }
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            default:
                mBeautyPannelView.setVisibility(View.GONE); //隐藏
                mBitrateLayout.setVisibility(View.GONE);
        }
    }



    //公用打印辅助函数
    protected void appendEventLog(int event, String message) {
        String str = "receive event: " + event + ", " + message;
        Log.d(TAG, str);
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss.SSS");
        String date = sdf.format(System.currentTimeMillis());
        while (mLogMsg.length() > mLogMsgLenLimit) {
            int idx = mLogMsg.indexOf("\n");
            if (idx == 0)
                idx = 1;
            mLogMsg = mLogMsg.delete(0, idx);
        }
        mLogMsg = mLogMsg.append("\n" + "[" + date + "]" + message);
    }

    //公用打印辅助函数
    protected String getNetStatusString(Bundle status) {
        String str = String.format("%-14s %-14s %-12s\n%-14s %-14s %-12s\n%-14s %-14s %-12s\n%-14s %-12s",
                "CPU:" + status.getString(TXLiveConstants.NET_STATUS_CPU_USAGE),
                "RES:" + status.getInt(TXLiveConstants.NET_STATUS_VIDEO_WIDTH) + "*" + status.getInt(TXLiveConstants.NET_STATUS_VIDEO_HEIGHT),
                "SPD:" + status.getInt(TXLiveConstants.NET_STATUS_NET_SPEED) + "Kbps",
                "JIT:" + status.getInt(TXLiveConstants.NET_STATUS_NET_JITTER),
                "FPS:" + status.getInt(TXLiveConstants.NET_STATUS_VIDEO_FPS),
                "ARA:" + status.getInt(TXLiveConstants.NET_STATUS_AUDIO_BITRATE) + "Kbps",
                "QUE:" + status.getInt(TXLiveConstants.NET_STATUS_CODEC_CACHE) + "|" + status.getInt(TXLiveConstants.NET_STATUS_CACHE_SIZE),
                "DRP:" + status.getInt(TXLiveConstants.NET_STATUS_CODEC_DROP_CNT) + "|" + status.getInt(TXLiveConstants.NET_STATUS_DROP_SIZE),
                "VRA:" + status.getInt(TXLiveConstants.NET_STATUS_VIDEO_BITRATE) + "Kbps",
                "SVR:" + status.getString(TXLiveConstants.NET_STATUS_SERVER_IP),
                "AVRA:" + status.getInt(TXLiveConstants.NET_STATUS_SET_VIDEO_BITRATE));
        return str;
    }

    protected void clearLog() {
        mLogMsg.setLength(0);
        mLogViewEvent.setText("");
        mLogViewStatus.setText("");
    }

    // 开始推送流
    private boolean startPublishRtmp() {
        //预处理 rtmpurl
        String rtmpUrl = "";
        if (!TextUtils.isEmpty(inputUrl)) {
            String url[] = inputUrl.split("###");
            if (url.length > 0) {
                rtmpUrl = url[0];
            }
        }

        if (TextUtils.isEmpty(rtmpUrl) || (!rtmpUrl.trim().toLowerCase().startsWith("rtmp://"))) {
            Toast.makeText(getApplicationContext(), "推流地址不合法，目前支持rtmp推流!", Toast.LENGTH_SHORT).show();
            return false;
        }

        mCaptureView.setVisibility(View.VISIBLE);
        //设置水印图片及水印图片位置.
        mLivePushConfig.setWatermark(mBitmap, 10, 10);

        int customModeType = 0;
        // 音视频采集、预处理类型.请参考自定义音视频采集、预处理类型.
        // 设置自定义音视频采集、预处理类型.
        //  设置自定义音视频采集、预处理类型.  音视频采集、预处理类型.请参考自定义音视频采集、预处理类型.
        mLivePushConfig.setCustomModeType(customModeType);
        /*设置推流暂停时,后台播放暂停图片的方式.
        Parameters:
        time - 后台播放暂停图片的最长持续时间,单位是秒,默认值是300.
         fps - 后台播放暂停图片的帧率,最小值为5,最大值为20,默认是10.*/
        mLivePushConfig.setPauseImg(300, 10);
        Bitmap bitmap = decodeResource(getResources(), R.mipmap.pause_publish);
        mLivePushConfig.setPauseImg(bitmap);
        mLivePushConfig.setPauseFlag(TXLiveConstants.PAUSE_FLAG_PAUSE_VIDEO | TXLiveConstants.PAUSE_FLAG_PAUSE_AUDIO);
        /**
         * 设置美白和美颜效果.
         * Parameters:
         beautyLevel - 美颜等级.美颜等级即 beautyLevel 取值为0-9.取值为0时代表关闭美颜效果.默认值:0,即关闭美颜效果.
         whiteningLevel - 美白等级.美白等级即 whiteningLevel 取值为0-3.取值为0时代表关闭美白效果.默认值:0,即关闭美白效果.
         */
        mLivePushConfig.setBeautyFilter(mBeautyLevel, mWhiteningLevel);
        /**
         * 开启就近选路
         只有在推流启动前设置才会生效，推流过程中设置不会生效。
         Parameters:
         enable - true:开启就近选路 false:关闭就近选路
         */
        mLivePushConfig.enableNearestIP(false);
        //设置推流器配置信息.推荐在启动推流前设置配置信息.
        mLivePusher.setConfig(mLivePushConfig);
        //设置推流器的回调.
        mLivePusher.setPushListener(this);


        //启动推流.  是否成功启动推流. 0:成功. -1:url为空.
        mLivePusher.startPusher(rtmpUrl.trim());

        //启动摄像头预览.
        mLivePusher.startCameraPreview(mCaptureView);

        clearLog();

        int[] ver = TXLivePusher.getSDKVersion();
        if (ver != null && ver.length >= 4) {
            mLogMsg.append(String.format("rtmp sdk version:%d.%d.%d.%d ", ver[0], ver[1], ver[2], ver[3]));
            mLogViewEvent.setText(mLogMsg);
        }

        mBtnPlay.setBackgroundResource(R.mipmap.play_pause);

        appendEventLog(0, "点击推流按钮！");

        return true;
    }

    private void stopPublishRtmp() {

//        StopScreenCapture();

        mVideoPublish = false;
        //是否需要清除最后一帧画面. true:清除最后一帧画面. false:保留最后一帧画面.
        //停止预览
        mLivePusher.stopCameraPreview(true);  //停止摄像头预览
        //结束录屏.
        mLivePusher.stopScreenCapture();
        //停止推流..
        mLivePusher.stopPusher();    //停止推流
        //设置推流器的回调为null
        mLivePusher.setPushListener(null); //解绑 listener

        mCaptureView.setVisibility(View.GONE);


        if (mBtnHWEncode != null) {
            //mHWVideoEncode = true;
            mLivePushConfig.setHardwareAcceleration(mHWVideoEncode ? TXLiveConstants.ENCODE_VIDEO_HARDWARE : TXLiveConstants.ENCODE_VIDEO_SOFTWARE);
            mBtnHWEncode.setBackgroundResource(R.mipmap.quick);
            mBtnHWEncode.getBackground().setAlpha(mHWVideoEncode ? 255 : 100);
        }
        mBtnPlay.setBackgroundResource(R.mipmap.play_start);

        if (mLivePushConfig != null) {
            mLivePushConfig.setPauseImg(null);
        }
    }

    //根据设置确定是“固定”还是“自动”码率
    public void FixOrAdjustBitrate() {
        if (mRadioGroupBitrate == null || mLivePushConfig == null || mLivePusher == null) {
            return;
        }

        RadioButton rb = (RadioButton) findViewById(mRadioGroupBitrate.getCheckedRadioButtonId());
        int mode = Integer.parseInt((String) rb.getTag());

        switch (mode) {
            case 4: /*720p*/
                if (mLivePusher != null) {
//                    mLivePushConfig.setVideoResolution(TXLiveConstants.VIDEO_RESOLUTION_TYPE_720_1280);
//                    mLivePushConfig.setAutoAdjustBitrate(false);
//                    mLivePushConfig.setVideoBitrate(1500);
//                    mLivePusher.setConfig(mLivePushConfig);
                    //设置推流的视频质量
                    mLivePusher.setVideoQuality(TXLiveConstants.VIDEO_QUALITY_SUPER_DEFINITION);
                    //超清默认开启硬件加速
                    if (Build.VERSION.SDK_INT >= 18) {
                        mHWVideoEncode = true;
                    }
                    mBtnHWEncode.getBackground().setAlpha(255);
                }
                mBtnBitrate.setBackgroundResource(R.mipmap.fix_bitrate);
                break;
            case 3: /*540p*/
                if (mLivePusher != null) {
//                    mLivePushConfig.setVideoResolution(TXLiveConstants.VIDEO_RESOLUTION_TYPE_540_960);
//                    mLivePushConfig.setAutoAdjustBitrate(false);
//                    mLivePushConfig.setVideoBitrate(1000);
//                    mLivePusher.setConfig(mLivePushConfig);
                    mLivePusher.setVideoQuality(TXLiveConstants.VIDEO_QUALITY_HIGH_DEFINITION);
                    mHWVideoEncode = false;
                    mBtnHWEncode.getBackground().setAlpha(100);
                }
                mBtnBitrate.setBackgroundResource(R.mipmap.fix_bitrate);
                break;
            case 2: /*360p*/
                if (mLivePusher != null) {
                    mLivePusher.setVideoQuality(TXLiveConstants.VIDEO_QUALITY_STANDARD_DEFINITION);
//                    mLivePushConfig.setVideoResolution(TXLiveConstants.VIDEO_RESOLUTION_TYPE_360_640);
                    //标清默认开启了码率自适应，需要关闭码率自适应
                    mLivePushConfig.setAutoAdjustBitrate(false);
                    mLivePushConfig.setVideoBitrate(700);
                    mLivePusher.setConfig(mLivePushConfig);
                    //标清默认关闭硬件加速
                    mHWVideoEncode = false;
                    mBtnHWEncode.getBackground().setAlpha(100);
                }
                mBtnBitrate.setBackgroundResource(R.mipmap.fix_bitrate);
                break;

            case 1: /*自动*/
                if (mLivePusher != null) {
//                    mLivePushConfig.setVideoResolution(TXLiveConstants.VIDEO_RESOLUTION_TYPE_360_640);
//                    mLivePushConfig.setAutoAdjustBitrate(true);
//                    mLivePushConfig.setAutoAdjustStrategy(TXLiveConstants.AUTO_ADJUST_BITRATE_STRATEGY_1);
//                    mLivePushConfig.setMaxVideoBitrate(1000);
//                    mLivePushConfig.setMinVideoBitrate(400);
//                    mLivePushConfig.setVideoBitrate(700);
//                    mLivePusher.setConfig(mLivePushConfig);
                    mLivePusher.setVideoQuality(TXLiveConstants.VIDEO_QUALITY_QOS_DEFINITION);
                    //标清默认关闭硬件加速
                    mHWVideoEncode = false;
                    mBtnHWEncode.getBackground().setAlpha(100);
                }
                mBtnBitrate.setBackgroundResource(R.mipmap.auto_bitrate);
                break;
            default:
                break;
        }
    }

    private void showNetBusyTips() {
        if (null == mNetBusyHandler) {
            mNetBusyHandler = new Handler(Looper.getMainLooper());
        }
        if (mNetBusyTips.isShown()) {
            return;
        }
        mNetBusyTips.setVisibility(View.VISIBLE);
        mNetBusyHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                mNetBusyTips.setVisibility(View.GONE);
            }
        }, 5000);
    }

    @Override
    public void onPushEvent(int event, Bundle param) {
        String msg = param.getString(TXLiveConstants.EVT_DESCRIPTION);
        appendEventLog(event, msg);
        if (mScrollView.getVisibility() == View.VISIBLE) {
            mLogViewEvent.setText(mLogMsg);
            scroll2Bottom(mScrollView, mLogViewEvent);
        }
//        if (mLivePusher != null) {
//            mLivePusher.onLogRecord("[event:" + event + "]" + msg + "\n");
//        }
        //错误还是要明确的报一下
        if (event < 0) {
            Toast.makeText(getApplicationContext(), param.getString(TXLiveConstants.EVT_DESCRIPTION), Toast.LENGTH_SHORT).show();
            if (event == TXLiveConstants.PUSH_ERR_OPEN_CAMERA_FAIL) {
                stopPublishRtmp();
            }
        }

        if (event == TXLiveConstants.PUSH_ERR_NET_DISCONNECT) {
            stopPublishRtmp();
        } else if (event == TXLiveConstants.PUSH_WARNING_HW_ACCELERATION_FAIL) {
            Toast.makeText(getApplicationContext(), param.getString(TXLiveConstants.EVT_DESCRIPTION), Toast.LENGTH_SHORT).show();
            mLivePushConfig.setHardwareAcceleration(TXLiveConstants.ENCODE_VIDEO_SOFTWARE);
            mBtnHWEncode.setBackgroundResource(R.mipmap.quick2);
            mLivePusher.setConfig(mLivePushConfig);
            mHWVideoEncode = false;
        } else if (event == TXLiveConstants.PUSH_ERR_SCREEN_CAPTURE_UNSURPORT) {
            stopPublishRtmp();
        } else if (event == TXLiveConstants.PUSH_ERR_SCREEN_CAPTURE_START_FAILED) {
            stopPublishRtmp();
        } else if (event == TXLiveConstants.PUSH_EVT_CHANGE_RESOLUTION) {
            Log.d(TAG, "change resolution to " + param.getInt(TXLiveConstants.EVT_PARAM2) + ", bitrate to" + param.getInt(TXLiveConstants.EVT_PARAM1));
        } else if (event == TXLiveConstants.PUSH_EVT_CHANGE_BITRATE) {
            Log.d(TAG, "change bitrate to" + param.getInt(TXLiveConstants.EVT_PARAM1));
        } else if (event == TXLiveConstants.PUSH_WARNING_NET_BUSY) {
            ++mNetBusyCount;
            Log.d(TAG, "net busy. count=" + mNetBusyCount);
            showNetBusyTips();
        } else if (event == TXLiveConstants.PUSH_EVT_START_VIDEO_ENCODER) {
            int encType = param.getInt(TXLiveConstants.EVT_PARAM1);
            mHWVideoEncode = (encType == 1);
            mBtnHWEncode.getBackground().setAlpha(mHWVideoEncode ? 255 : 100);
        }
    }

    @Override
    public void onNetStatus(Bundle status) {
        String str = getNetStatusString(status);
        mLogViewStatus.setText(str);
        Log.d(TAG, "Current status, CPU:" + status.getString(TXLiveConstants.NET_STATUS_CPU_USAGE) +
                ", RES:" + status.getInt(TXLiveConstants.NET_STATUS_VIDEO_WIDTH) + "*" + status.getInt(TXLiveConstants.NET_STATUS_VIDEO_HEIGHT) +
                ", SPD:" + status.getInt(TXLiveConstants.NET_STATUS_NET_SPEED) + "Kbps" +
                ", FPS:" + status.getInt(TXLiveConstants.NET_STATUS_VIDEO_FPS) +
                ", ARA:" + status.getInt(TXLiveConstants.NET_STATUS_AUDIO_BITRATE) + "Kbps" +
                ", VRA:" + status.getInt(TXLiveConstants.NET_STATUS_VIDEO_BITRATE) + "Kbps");

    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        onActivityRotation();
    }

    protected void onActivityRotation() {
        // 自动旋转打开，Activity随手机方向旋转之后，需要改变推流方向
        int mobileRotation = this.getWindowManager().getDefaultDisplay().getRotation();
        int pushRotation = TXLiveConstants.VIDEO_ANGLE_HOME_DOWN;
        switch (mobileRotation) {
            case Surface.ROTATION_0:
                pushRotation = TXLiveConstants.VIDEO_ANGLE_HOME_DOWN;
                break;
            case Surface.ROTATION_90:
                pushRotation = TXLiveConstants.VIDEO_ANGLE_HOME_RIGHT;
                break;
            case Surface.ROTATION_270:
                pushRotation = TXLiveConstants.VIDEO_ANGLE_HOME_LEFT;
                break;
            default:
                break;
        }
        mLivePusher.setRenderRotation(0); //因为activity也旋转了，本地渲染相对正方向的角度为0。
        mLivePushConfig.setHomeOrientation(pushRotation);
        mLivePusher.setConfig(mLivePushConfig);
    }

    /**
     * 判断Activity是否可旋转。只有在满足以下条件的时候，Activity才是可根据重力感应自动旋转的。
     * 系统“自动旋转”设置项打开；
     *
     * @return false---Activity可根据重力感应自动旋转
     */
    protected boolean isActivityCanRotation() {
        // 判断自动旋转是否打开
        int flag = Settings.System.getInt(this.getContentResolver(), Settings.System.ACCELEROMETER_ROTATION, 0);
        if (flag == 0) {
            return false;
        }
        return true;
    }

    @Override
    public void onBeautyParamsChange(BeautySettingPannel.BeautyParams params, int key) {
        switch (key) {
            case BeautySettingPannel.BEAUTYPARAM_EXPOSURE:
                if (mLivePusher != null) {
                    mLivePusher.setExposureCompensation(params.mExposure);
                }
                break;
            case BeautySettingPannel.BEAUTYPARAM_BEAUTY:
                mBeautyLevel = params.mBeautyLevel;
                if (mLivePusher != null) {
                    mLivePusher.setBeautyFilter(mBeautyLevel, mWhiteningLevel);
                }
                break;
            case BeautySettingPannel.BEAUTYPARAM_WHITE:
                mWhiteningLevel = params.mWhiteLevel;
                if (mLivePusher != null) {
                    mLivePusher.setBeautyFilter(mBeautyLevel, mWhiteningLevel);
                }
                break;
            case BeautySettingPannel.BEAUTYPARAM_BIG_EYE:
                if (mLivePusher != null) {
                    mLivePusher.setEyeScaleLevel(params.mBigEyeLevel);
                }
                break;
            case BeautySettingPannel.BEAUTYPARAM_FACE_LIFT:
                if (mLivePusher != null) {
                    mLivePusher.setFaceSlimLevel(params.mFaceSlimLevel);
                }
                break;
            case BeautySettingPannel.BEAUTYPARAM_FILTER:
                if (mLivePusher != null) {
                    mLivePusher.setFilter(params.mFilterBmp);
                }
                break;
            case BeautySettingPannel.BEAUTYPARAM_GREEN:
                if (mLivePusher != null) {
                    mLivePusher.setGreenScreenFile(params.mGreenFile);
                }
                break;
            case BeautySettingPannel.BEAUTYPARAM_MOTION_TMPL:
                if (mLivePusher != null) {
                    mLivePusher.setMotionTmpl(params.mMotionTmplPath);
                }
                break;
        }
    }

    //观察屏幕旋转设置变化，类似于注册动态广播监听变化机制
    private class RotationObserver extends ContentObserver {
        ContentResolver mResolver;

        public RotationObserver(Handler handler) {
            super(handler);
            mResolver = LivePublisherActivity.this.getContentResolver();
        }

        //屏幕旋转设置改变时调用
        @Override
        public void onChange(boolean selfChange) {
            super.onChange(selfChange);
            //更新按钮状态
            if (isActivityCanRotation()) {
                mBtnOrientation.setVisibility(View.GONE);
                onActivityRotation();
            } else {
                mBtnOrientation.setVisibility(View.VISIBLE);
                mPortrait = true;
                mLivePushConfig.setHomeOrientation(TXLiveConstants.VIDEO_ANGLE_HOME_DOWN);
                mBtnOrientation.setBackgroundResource(R.mipmap.landscape);
                mLivePusher.setRenderRotation(0);
                mLivePusher.setConfig(mLivePushConfig);
            }

        }

        public void startObserver() {
            /*注册：利用context.getContentResolover()获得ContentResolove对象，接着调用registerContentObserver()方法去注册内容观察者，
            该注册方法是在onCreate()方法中调用；
            registerContentObserver(Uri uri,boolean notifyForDescendents,ContentObserver observer)，
            给指定的uri注册一个ContentObserver类，当给定的uri发生改变时，就会回调该实例对象进行处理。
            uri：需要观察的uri
            notifyForDescendents：为false表示精确匹配，即只能匹配该uri；为true时表示，可以同时匹配该派生的uri*/
            mResolver.registerContentObserver(Settings.System.getUriFor(Settings.System.ACCELEROMETER_ROTATION), false, this);
        }

        public void stopObserver() {
            //取消观察
            mResolver.unregisterContentObserver(this);
        }
    }

}
