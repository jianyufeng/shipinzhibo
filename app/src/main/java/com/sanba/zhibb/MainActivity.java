package com.sanba.zhibb;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import com.sanba.zhibb.play.LivePlayerActivity;
import com.sanba.zhibb.push.LivePublisherActivity;
import com.tencent.rtmp.TXLivePlayer;

public class MainActivity extends AppCompatActivity {
    private String playUrl;
    private EditText mRtmpUrlView;
    private String playUrl_1;
    private EditText mRtmpUrlView_1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mRtmpUrlView = (EditText) findViewById(R.id.rtmpUrl);
        mRtmpUrlView_1 = (EditText) findViewById(R.id.rtmpUrl_1);

        findViewById(R.id.btnScan).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                playUrl = mRtmpUrlView.getText().toString();
//                if (!checkPlayUrl(playUrl)) {
                    Intent intent = new Intent(MainActivity.this, LivePlayerActivity.class);
                    intent.putExtra("TITLE", "直播");
                    intent.putExtra("PLAY_TYPE", LivePlayerActivity.ACTIVITY_TYPE_LIVE_PLAY);
                    intent.putExtra("PLAY_URL", playUrl);
                    MainActivity.this.startActivity(intent);
//                }

            }
        });
        findViewById(R.id.btnScan_1).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                playUrl_1 = mRtmpUrlView_1.getText().toString();
//                if (!checkPlayUrl(playUrl)) {
                    Intent intent = new Intent(MainActivity.this, LivePublisherActivity.class);
                    intent.putExtra("TITLE", "直播");
                    intent.putExtra("PLAY_TYPE", LivePlayerActivity.ACTIVITY_TYPE_LIVE_PLAY);
                    intent.putExtra("PLAY_URL", playUrl_1);
                    MainActivity.this.startActivity(intent);
//                }

            }
        });

    }

    private int mPlayType = TXLivePlayer.PLAY_TYPE_LIVE_RTMP;

    private boolean checkPlayUrl(final String playUrl) {
        if (TextUtils.isEmpty(playUrl) || (!playUrl.startsWith("http://") && !playUrl.startsWith("https://") && !playUrl.startsWith("rtmp://") && !playUrl.startsWith("/"))) {
            Toast.makeText(getApplicationContext(), "播放地址不合法，目前仅支持rtmp,flv,hls,mp4播放方式和本地播放方式（绝对路径，如\"/sdcard/test.mp4\"）!", Toast.LENGTH_SHORT).show();
            return false;
        }

        if (playUrl.startsWith("rtmp://")) {
            mPlayType = TXLivePlayer.PLAY_TYPE_LIVE_RTMP;
            return true;
        } else if ((playUrl.startsWith("http://") || playUrl.startsWith("https://")) && playUrl.contains(".flv")) {
            mPlayType = TXLivePlayer.PLAY_TYPE_LIVE_FLV;
            return true;
        } else {
            Toast.makeText(getApplicationContext(), "播放地址不合法，直播目前仅支持rtmp,flv播放方式!", Toast.LENGTH_SHORT).show();
            return false;
        }
    }
}
