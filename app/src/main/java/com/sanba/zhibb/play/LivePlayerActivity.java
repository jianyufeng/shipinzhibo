package com.sanba.zhibb.play;

import android.app.Activity;
import android.app.Service;
import android.graphics.Color;
import android.graphics.drawable.AnimationDrawable;
import android.os.Bundle;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.sanba.zhibb.R;
import com.tencent.rtmp.ITXLivePlayListener;
import com.tencent.rtmp.TXLiveConstants;
import com.tencent.rtmp.TXLivePlayConfig;
import com.tencent.rtmp.TXLivePlayer;
import com.tencent.rtmp.ui.TXCloudVideoView;

import java.text.SimpleDateFormat;

/**
 * 直播客户端  播放视频
 * 播放步骤：
 * 1 添加step 1: 添加View为了能够展示播放器的视频画面
 * TXCloudVideoView 其本身拥有生命周期方法；
 * 2 创建Player  接下来创建一个TXLivePlayer的对象，并使用 setPlayerView 接口将这它与我们刚刚添加到界面上的video_view控件进行关联。
 * 3: 启动播放器 mLivePlayer.startPlay(flvUrl, TXLivePlayer.PLAY_TYPE_LIVE_FLV); //推荐FLV
 * 4: 画面调整
 * view：大小和位置
 * 如需修改画面的大小及位置，直接调整 step1中添加的 “video_view” 控件的大小和位置即可。
 * 5: 进度调整
 * 6: 停播放
 * 7 结束播放
 * 8: 硬件加速
 * mLivePlayer.stopPlay(true);
 * mLivePlayer.enableHardwareDecode(true);
 * mLivePlayer.startPlay(flvUrl, type);
 * 9: 截流录制（仅直播）
 * 10 :视频截图
 *                          //截图
 //                    mLivePlayer.snapshot(new TXLivePlayer.ITXSnapshotListener() {
 //                        @Override
 //                        public void onSnapshot(Bitmap bitmap) {
 //                            Log.e("","ss");
 //                        }
 //                    });
 *
 */
public class LivePlayerActivity extends Activity implements ITXLivePlayListener, OnClickListener {
    private static final String TAG = LivePlayerActivity.class.getSimpleName();//设置log标记

    private TXLivePlayer mLivePlayer = null;  //直播控制类
    private boolean mVideoPlay;        //直播状态
    private TXCloudVideoView mPlayerView;      //显示摄像头影像的专用控件：
    private ImageView mLoadingView;         //加载时的动画
    private boolean mHWDecode = false;   //硬件解码
    private LinearLayout mRootView;             //根部局

    private Button mBtnLog;               //切换控制日志
    private Button mBtnPlay;              //切换播放停止
    private Button mBtnRenderRotation;      //横竖屏看切换
    private Button mBtnRenderMode;           //设置图像平铺模式.
    private Button mBtnHWDecode;             //切换硬件解码
    private ScrollView mScrollView;           //日志的容器
    private SeekBar mSeekBar;                  //点播的快捷按钮
    private TextView mTextDuration;             //点播总播放时长
    private TextView mTextStart;                 //时长起点过程

    private static final int CACHE_STRATEGY_FAST = 1;  //极速
    private static final int CACHE_STRATEGY_SMOOTH = 2;  //流畅
    private static final int CACHE_STRATEGY_AUTO = 3;  //自动

    private static final float CACHE_TIME_FAST = 1.0f;  //极速下最大缓存 1s
    private static final float CACHE_TIME_SMOOTH = 5.0f; // 流畅下 最大缓存5s

    private static final float CACHE_TIME_AUTO_MIN = 5.0f;
    private static final float CACHE_TIME_AUTO_MAX = 10.0f;

    public static final int ACTIVITY_TYPE_PUBLISH = 1;     //推流
    public static final int ACTIVITY_TYPE_LIVE_PLAY = 2;  //直播
    public static final int ACTIVITY_TYPE_VOD_PLAY = 3;   //点播
    public static final int ACTIVITY_TYPE_LINK_MIC = 4;    // 连麦

    private int mCacheStrategy = 0;       //保存当前 缓存模式
    private Button mBtnCacheStrategy;        //缓存模式的切换显示按钮
    private Button mRatioFast;                //快速 按钮
    private Button mRatioSmooth;              //流畅
    private Button mRatioAuto;                //自动
    private Button mBtnStop;                    //停止播放
    private LinearLayout mLayoutCacheStrategy;      //缓冲模式的父容器
    public TextView mLogViewStatus;            // 状态记录
    public TextView mLogViewEvent;             //状态记录
    protected StringBuffer mLogMsg = new StringBuffer("");  // 状态详情
    private final int mLogMsgLenLimit = 3000;            //状态相亲长度科举

    private int mCurrentRenderMode;            //图像平铺模式. 或图片当前着色模式
    private int mCurrentRenderRotation;            //当前图像的旋转角

    private long mTrackingTouchTS = 0;         // 点播 拖动的时间点
    private boolean mStartSeek = false;         // 标志 开始拖动 点播的 进度
    private boolean mVideoPause = false;            // 直播时  暂停拉流
    private int mPlayType = TXLivePlayer.PLAY_TYPE_LIVE_RTMP;  //直播媒体类型：默认 rtmp
    private TXLivePlayConfig mPlayConfig;       //播放器的一些配置  如:设置缓存时间 可以在网络缓快调节
    private long mStartPlayTS = 0;      //当前直播开始时间
    protected int mActivityType;    //当前界面类型

    private String playUrl;     //直播的路径

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //图像平铺模式
        mCurrentRenderMode = TXLiveConstants.RENDER_MODE_ADJUST_RESOLUTION;
        //图像渲染角度
        mCurrentRenderRotation = TXLiveConstants.RENDER_ROTATION_PORTRAIT;
        //推流  直播  点播
        mActivityType = getIntent().getIntExtra("PLAY_TYPE", ACTIVITY_TYPE_LIVE_PLAY);//默认直播
        //播放一些配置的设置
        mPlayConfig = new TXLivePlayConfig();
        playUrl = getIntent().getStringExtra("PLAY_URL");
        //电话管理类
        TelephonyManager tm = (TelephonyManager) getApplicationContext().getSystemService(Service.TELEPHONY_SERVICE);
        //监听来电
        tm.listen(listener, PhoneStateListener.LISTEN_CALL_STATE);
        setContentView();
        //标题 返回等
        LinearLayout backLL = (LinearLayout) findViewById(R.id.back_ll);
        backLL.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
        TextView titleTV = (TextView) findViewById(R.id.title_tv);
        titleTV.setText(getIntent().getStringExtra("TITLE"));
    }

    void initView() {
        //监听 网速等 状态 容器
        mLogViewEvent = (TextView) findViewById(R.id.logViewEvent);
        mLogViewStatus = (TextView) findViewById(R.id.logViewStatus);

    }


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
        super.setContentView(R.layout.activity_play);
        initView();
        //根部局
        mRootView = (LinearLayout) findViewById(R.id.root);
        if (mLivePlayer == null) {   //创建播放控制器
            //创建player对象
            mLivePlayer = new TXLivePlayer(this);
        }
        //mPlayerView即step1中添加的界面view
        mPlayerView = (TXCloudVideoView) findViewById(R.id.video_view);
        mPlayerView.disableLog(true);
        mLoadingView = (ImageView) findViewById(R.id.loadingImageView);

        mVideoPlay = false;  //初始化播放状态
        mLogViewStatus.setVisibility(View.GONE);  //将log视图隐藏
        mLogViewStatus.setMovementMethod(new ScrollingMovementMethod());
        mLogViewEvent.setMovementMethod(new ScrollingMovementMethod());
        mScrollView = (ScrollView) findViewById(R.id.scrollview);
        mScrollView.setVisibility(View.GONE);

        mBtnPlay = (Button) findViewById(R.id.btnPlay);  //开始播放按钮
        mBtnPlay.setOnClickListener(new OnClickListener() {  //点击开始 可以和暂停切换
            @Override
            public void onClick(View v) {
                Log.d(TAG, "click playbtn isplay:" + mVideoPlay + " ispause:" + mVideoPause + " playtype:" + mPlayType);
                playOrPause();
            }
        });
        //初始化进去直接开始直播
        playOrPause();
        //停止按钮
        mBtnStop = (Button) findViewById(R.id.btnStop);
        mBtnStop.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mActivityType == ACTIVITY_TYPE_LIVE_PLAY) {
                    //是直播
                    finish();
                    return;
                }
                stopPlayRtmp();
                mVideoPlay = false;
                mVideoPause = false;
                if (mTextStart != null) {
                    mTextStart.setText("00:00");
                }
                if (mSeekBar != null) {
                    mSeekBar.setProgress(0);
                }
            }
        });
        //日志  按钮
        mBtnLog = (Button) findViewById(R.id.btnLog);
        mBtnLog.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mLogViewStatus.getVisibility() == View.GONE) {
                    mLogViewStatus.setVisibility(View.VISIBLE);
                    mScrollView.setVisibility(View.VISIBLE);
                    mLogViewEvent.setText(mLogMsg);
                    scroll2Bottom(mScrollView, mLogViewEvent);
                    mBtnLog.setBackgroundResource(R.mipmap.log_hidden);
                } else {
                    mLogViewStatus.setVisibility(View.GONE);
                    mScrollView.setVisibility(View.GONE);
                    mBtnLog.setBackgroundResource(R.mipmap.log_show);
                }
            }
        });

        //横屏|竖屏
        mBtnRenderRotation = (Button) findViewById(R.id.btnOrientation);
        mBtnRenderRotation.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mLivePlayer == null) {
                    return;
                }

                if (mCurrentRenderRotation == TXLiveConstants.RENDER_ROTATION_PORTRAIT) {
                    mBtnRenderRotation.setBackgroundResource(R.mipmap.portrait);
                    mCurrentRenderRotation = TXLiveConstants.RENDER_ROTATION_LANDSCAPE;
                } else if (mCurrentRenderRotation == TXLiveConstants.RENDER_ROTATION_LANDSCAPE) {
                    mBtnRenderRotation.setBackgroundResource(R.mipmap.landscape);
                    mCurrentRenderRotation = TXLiveConstants.RENDER_ROTATION_PORTRAIT;
                }

                mLivePlayer.setRenderRotation(mCurrentRenderRotation);
            }
        });

        //平铺模式
        mBtnRenderMode = (Button) findViewById(R.id.btnRenderMode);
        mBtnRenderMode.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mLivePlayer == null) {
                    return;
                }

                if (mCurrentRenderMode == TXLiveConstants.RENDER_MODE_FULL_FILL_SCREEN) {
                    mLivePlayer.setRenderMode(TXLiveConstants.RENDER_MODE_ADJUST_RESOLUTION);
                    mBtnRenderMode.setBackgroundResource(R.mipmap.fill_mode);
                    mCurrentRenderMode = TXLiveConstants.RENDER_MODE_ADJUST_RESOLUTION;
                } else if (mCurrentRenderMode == TXLiveConstants.RENDER_MODE_ADJUST_RESOLUTION) {
                    mLivePlayer.setRenderMode(TXLiveConstants.RENDER_MODE_FULL_FILL_SCREEN);
                    mBtnRenderMode.setBackgroundResource(R.mipmap.adjust_mode);
                    mCurrentRenderMode = TXLiveConstants.RENDER_MODE_FULL_FILL_SCREEN;
                }
            }
        });

        //硬件解码
        mBtnHWDecode = (Button) findViewById(R.id.btnHWDecode);
        mBtnHWDecode.getBackground().setAlpha(mHWDecode ? 255 : 100);
        mBtnHWDecode.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                mHWDecode = !mHWDecode;
                mBtnHWDecode.getBackground().setAlpha(mHWDecode ? 255 : 100);

                if (mHWDecode) {
                    Toast.makeText(getApplicationContext(), "已开启硬件解码加速，切换会重启播放流程!", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(getApplicationContext(), "已关闭硬件解码加速，切换会重启播放流程!", Toast.LENGTH_SHORT).show();
                }

                if (mVideoPlay) {
                    stopPlayRtmp();
                    startPlayRtmp();
                    if (mVideoPause) {
                        if (mPlayerView != null) {
                            mPlayerView.onResume();
                        }
                        mVideoPause = false;
                    }
                }
            }
        });

        mSeekBar = (SeekBar) findViewById(R.id.seekbar);
        //点播进度条
        mSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean bFromUser) {
                mTextStart.setText(String.format("%02d:%02d", progress / 60, progress % 60));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                mStartSeek = true;
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                if (mLivePlayer != null) {
                    //点播时,跳转到视频流指定时间点.
                    mLivePlayer.seek(seekBar.getProgress());
                }
                mTrackingTouchTS = System.currentTimeMillis();
                mStartSeek = false;
            }
        });

        //点播时 的
        mTextDuration = (TextView) findViewById(R.id.duration);
        mTextStart = (TextView) findViewById(R.id.play_start);
        mTextDuration.setTextColor(Color.rgb(255, 255, 255));
        mTextStart.setTextColor(Color.rgb(255, 255, 255));
        //缓存策略
        mBtnCacheStrategy = (Button) findViewById(R.id.btnCacheStrategy);
        mLayoutCacheStrategy = (LinearLayout) findViewById(R.id.layoutCacheStrategy);
        mBtnCacheStrategy.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                mLayoutCacheStrategy.setVisibility(mLayoutCacheStrategy.getVisibility() == View.VISIBLE ? View.GONE : View.VISIBLE);
            }
        });
        //设置为自动的缓存模式
        this.setCacheStrategy(CACHE_STRATEGY_AUTO);

        mRatioFast = (RadioButton) findViewById(R.id.radio_btn_fast);
        mRatioFast.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                LivePlayerActivity.this.setCacheStrategy(CACHE_STRATEGY_FAST);
                mLayoutCacheStrategy.setVisibility(View.GONE);
            }
        });

        mRatioSmooth = (RadioButton) findViewById(R.id.radio_btn_smooth);
        mRatioSmooth.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                LivePlayerActivity.this.setCacheStrategy(CACHE_STRATEGY_SMOOTH);
                mLayoutCacheStrategy.setVisibility(View.GONE);
            }
        });

        mRatioAuto = (RadioButton) findViewById(R.id.radio_btn_auto);
        mRatioAuto.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                LivePlayerActivity.this.setCacheStrategy(CACHE_STRATEGY_AUTO);
                mLayoutCacheStrategy.setVisibility(View.GONE);
            }
        });

        View progressGroup = findViewById(R.id.play_progress);

        // 直播不需要进度条和停止按钮，点播不需要缓存策略
        if (mActivityType == ACTIVITY_TYPE_LIVE_PLAY) {
            progressGroup.setVisibility(View.GONE);
            mBtnStop.setVisibility(View.VISIBLE);
        } else if (mActivityType == ACTIVITY_TYPE_VOD_PLAY) {
            mBtnCacheStrategy.setVisibility(View.GONE);
        }

        View view = mPlayerView.getRootView();
        view.setOnClickListener(this);
    }

    //播放直播   分类播放类型  只是对rtmp直播进行了处理
    private void playOrPause() {
        if (mVideoPlay) {
            //在直播
            if (mPlayType == TXLivePlayer.PLAY_TYPE_VOD_FLV || mPlayType == TXLivePlayer.PLAY_TYPE_VOD_HLS || mPlayType == TXLivePlayer.PLAY_TYPE_VOD_MP4 || mPlayType == TXLivePlayer.PLAY_TYPE_LOCAL_VIDEO) {
                if (mVideoPause) {
                    //在直播频道  但未拉流
                    mLivePlayer.resume();    //恢复播放,重新获取流数据.
                    mBtnPlay.setBackgroundResource(R.mipmap.play_pause);
                    mRootView.setBackgroundColor(0xff000000);
                } else {
                    mLivePlayer.pause(); //暂停播放,停止获取流数据,保留最后一帧画面.
                    mBtnPlay.setBackgroundResource(R.mipmap.play_start);
                }
                mVideoPause = !mVideoPause;

            } else {
                //停止播放前， 停止拉流
                stopPlayRtmp();
                mVideoPlay = !mVideoPlay; ///改变状态
            }

        } else {
            //当前没有在直播  或正暂停直播
            if (startPlayRtmp()) {
                mVideoPlay = !mVideoPlay;
            }
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mLivePlayer != null) {
            mLivePlayer.stopPlay(true);
            mLivePlayer = null;
        }
        if (mPlayerView != null) {
            mPlayerView.onDestroy();
            mPlayerView = null;
        }
        mPlayConfig = null;
        Log.d(TAG, "vrender onDestroy");
    }

    @Override
    public void onPause() {
        super.onPause();
    }

    @Override
    public void onStop() {
        super.onStop();

        if (mPlayType == TXLivePlayer.PLAY_TYPE_VOD_FLV || mPlayType == TXLivePlayer.PLAY_TYPE_VOD_HLS || mPlayType == TXLivePlayer.PLAY_TYPE_VOD_MP4 || mPlayType == TXLivePlayer.PLAY_TYPE_LOCAL_VIDEO) {
            if (mLivePlayer != null) {
                mLivePlayer.pause();
            }
        } else {
//            stopPlayRtmp();
            mLivePlayer.pause();
        }

        if (mPlayerView != null) {
            mPlayerView.onPause();
        }
    }

    @Override
    public void onResume() {
        super.onResume();

        if (mVideoPlay && !mVideoPause) {
            if (mPlayType == TXLivePlayer.PLAY_TYPE_VOD_FLV || mPlayType == TXLivePlayer.PLAY_TYPE_VOD_HLS || mPlayType == TXLivePlayer.PLAY_TYPE_VOD_MP4 || mPlayType == TXLivePlayer.PLAY_TYPE_LOCAL_VIDEO) {
                if (mLivePlayer != null) {
                    mLivePlayer.resume();
                }
            } else {
//                startPlayRtmp();
                mLivePlayer.resume();
            }
        }

        if (mPlayerView != null) {
            mPlayerView.onResume();
        }
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            default:
                mLayoutCacheStrategy.setVisibility(View.GONE);
        }
    }

    private boolean checkPlayUrl(final String playUrl) {
        if (TextUtils.isEmpty(playUrl) || (!playUrl.startsWith("http://") && !playUrl.startsWith("https://") && !playUrl.startsWith("rtmp://") && !playUrl.startsWith("/"))) {
            Toast.makeText(getApplicationContext(), "播放地址不合法，目前仅支持rtmp,flv,hls,mp4播放方式和本地播放方式（绝对路径，如\"/sdcard/test.mp4\"）!", Toast.LENGTH_SHORT).show();
            return false;
        }

        switch (mActivityType) {
            case ACTIVITY_TYPE_LIVE_PLAY: {
                if (playUrl.startsWith("rtmp://")) {
                    mPlayType = TXLivePlayer.PLAY_TYPE_LIVE_RTMP;
                } else if ((playUrl.startsWith("http://") || playUrl.startsWith("https://")) && playUrl.contains(".flv")) {
                    mPlayType = TXLivePlayer.PLAY_TYPE_LIVE_FLV;
                } else {
                    Toast.makeText(getApplicationContext(), "播放地址不合法，直播目前仅支持rtmp,flv播放方式!", Toast.LENGTH_SHORT).show();
                    return false;
                }
            }
            break;
            case ACTIVITY_TYPE_VOD_PLAY: {
                if (playUrl.startsWith("http://") || playUrl.startsWith("https://")) {
                    if (playUrl.contains(".flv")) {
                        mPlayType = TXLivePlayer.PLAY_TYPE_VOD_FLV;
                    } else if (playUrl.contains(".m3u8")) {
                        mPlayType = TXLivePlayer.PLAY_TYPE_VOD_HLS;
                    } else if (playUrl.toLowerCase().contains(".mp4")) {
                        mPlayType = TXLivePlayer.PLAY_TYPE_VOD_MP4;
                    } else {
                        Toast.makeText(getApplicationContext(), "播放地址不合法，点播目前仅支持flv,hls,mp4播放方式!", Toast.LENGTH_SHORT).show();
                        return false;
                    }
                } else if (playUrl.startsWith("/")) {
                    if (playUrl.contains(".mp4") || playUrl.contains(".flv")) {
                        mPlayType = TXLivePlayer.PLAY_TYPE_LOCAL_VIDEO;
                    } else {
                        Toast.makeText(getApplicationContext(), "播放地址不合法，目前本地播放器仅支持播放mp4，flv格式文件", Toast.LENGTH_SHORT).show();
                        return false;
                    }
                } else {
                    Toast.makeText(getApplicationContext(), "播放地址不合法，点播目前仅支持flv,hls,mp4播放方式!", Toast.LENGTH_SHORT).show();
                    return false;
                }
            }
            break;
            default:
                Toast.makeText(getApplicationContext(), "播放地址不合法，目前仅支持rtmp,flv,hls,mp4播放方式!", Toast.LENGTH_SHORT).show();
                return false;
        }
        return true;
    }

    protected void clearLog() {
        mLogMsg.setLength(0);
        mLogViewEvent.setText("");
        mLogViewStatus.setText("");
    }

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


    private boolean startPlayRtmp() {


//        //由于iOS AppStore要求新上架的app必须使用https,所以后续腾讯云的视频连接会支持https,但https会有一定的性能损耗,所以android将统一替换会http
//        if (playUrl.startsWith("https://")) {
//            playUrl = "http://" + playUrl.substring(8);
//        }

        if (!checkPlayUrl(playUrl)) {
            return false;
        }

        clearLog();

        int[] ver = TXLivePlayer.getSDKVersion();
        if (ver != null && ver.length >= 4) {
            mLogMsg.append(String.format("rtmp sdk version:%d.%d.%d.%d ", ver[0], ver[1], ver[2], ver[3]));
            mLogViewEvent.setText(mLogMsg);
        }
        mBtnPlay.setBackgroundResource(R.mipmap.play_pause);
        mRootView.setBackgroundColor(0xff000000);
        //设置播放器的视频渲染View.+仅仅在启动播放之前设置有效.
        mLivePlayer.setPlayerView(mPlayerView);
        mLivePlayer.setPlayListener(this);  //设置播放器的回调.

        // 硬件加速在1080p解码场景下效果显著，但细节之处并不如想象的那么美好：
        // (1) 只有 4.3 以上android系统才支持
        // (2) 兼容性我们目前还仅过了小米华为等常见机型，故这里的返回值您先不要太当真
        mLivePlayer.enableHardwareDecode(mHWDecode);
        mLivePlayer.setRenderRotation(mCurrentRenderRotation);
        mLivePlayer.setRenderMode(mCurrentRenderMode);
        //设置播放器缓存策略
        //这里将播放器的策略设置为自动调整，调整的范围设定为1到4s，您也可以通过setCacheTime将播放器策略设置为采用
        //固定缓存时间。如果您什么都不调用，播放器将采用默认的策略（默认策略为自动调整，调整范围为1到4s）
        //mLivePlayer.setCacheTime(5);-
        mLivePlayer.setConfig(mPlayConfig);
        //关键player对象与界面view
        int result = mLivePlayer.startPlay(playUrl, mPlayType); // result返回值：0 success;  -1 empty url; -2 invalid url; -3 invalid playType;

        if (result != 0) {
            mBtnPlay.setBackgroundResource(R.mipmap.play_start);
            mRootView.setBackgroundResource(R.mipmap.main_bkg);
            return false;
        }

        appendEventLog(0, "点击播放按钮！播放类型：" + mPlayType);

        startLoadingAnimation();


        mStartPlayTS = System.currentTimeMillis();
        return true;
    }

    private void stopPlayRtmp() {
        mBtnPlay.setBackgroundResource(R.mipmap.play_start);
        mRootView.setBackgroundResource(R.mipmap.main_bkg);
        stopLoadingAnimation();
        if (mLivePlayer != null) {
            mLivePlayer.setPlayListener(null);
            mLivePlayer.stopPlay(true);
        }
    }

    /**
     * setPlayListener(ITXLivePlayListener listener)     设置播放器的回调.ITXLivePlayListener
     * onPlayEvent 播放事件通知
     *
     * @param event - 事件id.id类型请参考 播放事件列表.
     * @param param 事件相关的参数.(key,value)格式,其中key请参考 事件参数.
     */
    @Override
    public void onPlayEvent(int event, Bundle param) {
        if (event == TXLiveConstants.PLAY_EVT_PLAY_BEGIN) {//视频开始播放
            //开始播放
            stopLoadingAnimation();
            Log.d("AutoMonitor", "PlayFirstRender,cost=" + (System.currentTimeMillis() - mStartPlayTS));
        } else if (event == TXLiveConstants.PLAY_EVT_PLAY_PROGRESS) {//视频播放进度，会通知当前进度和总体进度，仅在点播时有效
            //拖动播放进度    之后要再哪里开始
            if (mStartSeek) {
                return;
            }
            int progress = param.getInt(TXLiveConstants.EVT_PLAY_PROGRESS);
            int duration = param.getInt(TXLiveConstants.EVT_PLAY_DURATION);
            long curTS = System.currentTimeMillis();

            // 避免滑动进度条松开的瞬间可能出现滑动条瞬间跳到上一个位置
            if (Math.abs(curTS - mTrackingTouchTS) < 500) {
                return;
            }
            mTrackingTouchTS = curTS;

            if (mSeekBar != null) {
                mSeekBar.setProgress(progress);
            }
            if (mTextStart != null) {
                mTextStart.setText(String.format("%02d:%02d", progress / 60, progress % 60));
            }
            if (mTextDuration != null) {
                mTextDuration.setText(String.format("%02d:%02d", duration / 60, duration % 60));
            }
            if (mSeekBar != null) {
                mSeekBar.setMax(duration);
            }
            return;
        } else if (event == TXLiveConstants.PLAY_ERR_NET_DISCONNECT || event == TXLiveConstants.PLAY_EVT_PLAY_END) {
            //播放结束  或者 播放断开
            stopPlayRtmp();
            mVideoPlay = false;
            mVideoPause = false;
            if (mTextStart != null) {
                mTextStart.setText("00:00");
            }
            if (mSeekBar != null) {
                mSeekBar.setProgress(0);
            }
        } else if (event == TXLiveConstants.PLAY_EVT_PLAY_LOADING) {
            // 加载中  开启加载中的动画
            startLoadingAnimation();
        }

        String msg = param.getString(TXLiveConstants.EVT_DESCRIPTION);
        appendEventLog(event, msg);
        if (mScrollView.getVisibility() == View.VISIBLE) {
            mLogViewEvent.setText(mLogMsg);
            scroll2Bottom(mScrollView, mLogViewEvent);
        }
//        if(mLivePlayer != null){
//            mLivePlayer.onLogRecord("[event:"+event+"]"+msg+"\n");
//        }
        if (event < 0) {
            Toast.makeText(getApplicationContext(), param.getString(TXLiveConstants.EVT_DESCRIPTION), Toast.LENGTH_SHORT).show();
        } else if (event == TXLiveConstants.PLAY_EVT_PLAY_BEGIN) {
            stopLoadingAnimation();
        }
    }

    /**
     * setPlayListener(ITXLivePlayListener listener)     设置播放器的回调.ITXLivePlayListener
     * onPlayEvent 播放事件通知
     * 网络状态通知.
     *
     * @param status
     */
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
        //Log.d(TAG, "Current status: " + status.toString());
//        if (mLivePlayer != null){
//            mLivePlayer.onLogRecord("[net state]:\n"+str+"\n");
//        }
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


    public void setCacheStrategy(int nCacheStrategy) {
        if (mCacheStrategy == nCacheStrategy) return;
        mCacheStrategy = nCacheStrategy;

        /**
         * 设置是否根据网络状况自动调整播放器缓存时间.
         启用自动调整时,SDK将根据网络状况在一个范围内调整缓存时间.
         自动调整的范围可以通过修改MaxAutoAdjustCacheTime和修改MinAutoAdjustCacheTime来调整.
         关闭自动调整时,SDK将使用固定的缓存时间.
         固定的缓存时间可以通过修改cacheTime来调整.
         */
        switch (nCacheStrategy) {
            case CACHE_STRATEGY_FAST:  //极速
                mPlayConfig.setAutoAdjustCacheTime(true);
                //播放器最大缓存时间,单位秒,默认值为 5,取值需要大于0.
                mPlayConfig.setMaxAutoAdjustCacheTime(CACHE_TIME_FAST);
                //放器最小缓存时间,单位秒,默认值为 1,取值需要大于0.
                mPlayConfig.setMinAutoAdjustCacheTime(CACHE_TIME_FAST);
                mLivePlayer.setConfig(mPlayConfig);
                break;

            case CACHE_STRATEGY_SMOOTH: //流畅
                mPlayConfig.setAutoAdjustCacheTime(false);
                mPlayConfig.setCacheTime(CACHE_TIME_SMOOTH);
                mLivePlayer.setConfig(mPlayConfig);
                break;

            case CACHE_STRATEGY_AUTO: //自动
                mPlayConfig.setAutoAdjustCacheTime(true);
                mPlayConfig.setMaxAutoAdjustCacheTime(CACHE_TIME_SMOOTH);
                mPlayConfig.setMinAutoAdjustCacheTime(CACHE_TIME_FAST);
                mLivePlayer.setConfig(mPlayConfig);
                break;

            default:
                break;
        }
    }
    boolean oo = true;
    //等待中 加载的动画
    private void startLoadingAnimation() {
        if (mLoadingView != null) {
            if (!oo){
                return;
            }

            mLoadingView.setVisibility(View.VISIBLE);
            ((AnimationDrawable) mLoadingView.getDrawable()).start();
        }
    }

    //开始播放 停止加载的动画
    private void stopLoadingAnimation() {
        if (mLoadingView != null) {
            mLoadingView.setVisibility(View.GONE);
            ((AnimationDrawable) mLoadingView.getDrawable()).stop();
        }
    }


    final PhoneStateListener listener = new PhoneStateListener() {
        @Override
        public void onCallStateChanged(int state, String incomingNumber) {
            super.onCallStateChanged(state, incomingNumber);
            switch (state) {
                //电话等待接听
                case TelephonyManager.CALL_STATE_RINGING:
                    //设置静音  setMute 设置是否静音播放. - 是否静音播放. true:静音播放. false:不静音播放.
                    if (mLivePlayer != null) mLivePlayer.setMute(true);
                    break;
                //电话接听
                case TelephonyManager.CALL_STATE_OFFHOOK:
                    if (mLivePlayer != null) mLivePlayer.setMute(true);
                    break;
                //电话挂机
                case TelephonyManager.CALL_STATE_IDLE:
                    if (mLivePlayer != null) mLivePlayer.setMute(false);
                    break;
            }
        }
    };
}