/*
 * Copyright 2017 R3BL LLC. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.r3bl.samples.simplemediaplayer;

import android.media.MediaPlayer;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.Button;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

/**
 * Creates a UI to allow the user to play, pause, and reset playback of a single
 * {@link MediaPlayer} (that is managed by {@link MediaPlayerHolder}.
 *
 * <p>All communications between the activity and the {@link MediaPlayerHolder} occur via
 * {@link EventBus} events.
 */
public class MainActivity extends AppCompatActivity {

    public static final String TAG = "MainActivity";
    public static final int MEDIA_RESOURCE_ID = R.raw.formula_1_daniel_simon;

    private MediaPlayerHolder mMediaPlayerHolder;
    private boolean isUserSeeking;

    @BindView(R.id.text_debug)
    TextView mTextDebug;
    @BindView(R.id.button_play)
    Button mPlayButton;
    @BindView(R.id.button_pause)
    Button mPauseButton;
    @BindView(R.id.button_reset)
    Button mResetButton;
    @BindView(R.id.seekbar_audio)
    SeekBar mSeekbarAudio;
    @BindView(R.id.scroll_container)
    ScrollView mScrollContainer;

    // Activity lifecycle.

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);
        EventBus.getDefault().register(this);
        mMediaPlayerHolder = new MediaPlayerHolder(this);
        setupSeekbar();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        EventBus.getDefault().unregister(this);
    }

    @Override
    protected void onStop() {
        super.onStop();
        mMediaPlayerHolder.release();
    }

    @Override
    protected void onStart() {
        super.onStart();
        mMediaPlayerHolder.load(MEDIA_RESOURCE_ID);
    }

    // Handle user input for Seekbar changes.

    public void setupSeekbar() {
        mSeekbarAudio.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            // This holds the progress value for onStopTrackingTouch.
            int userSelectedPosition = 0;

            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                // Only fire seekTo() calls when user stops the touch event.
                if (fromUser) {
                    userSelectedPosition = progress;
                    isUserSeeking = true;
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                isUserSeeking = false;
                EventBus.getDefault().post(new LocalEventFromMainActivity.SeekTo(
                        userSelectedPosition));
            }
        });
    }

    // Handle user input for button presses.

    @OnClick(R.id.button_pause)
    void pause() {
        EventBus.getDefault().post(new LocalEventFromMainActivity.PausePlayback());
    }

    @OnClick(R.id.button_play)
    void play() {
        EventBus.getDefault().post(new LocalEventFromMainActivity.StartPlayback());
    }

    @OnClick(R.id.button_reset)
    void reset() {
        EventBus.getDefault().post(new LocalEventFromMainActivity.ResetPlayback());
    }

    // Display log messges to the UI.

    public void log(StringBuffer formattedMessage) {
        if (mTextDebug != null) {
            mTextDebug.setText(formattedMessage);
            // Move the mScrollContainer focus to the end.
            mScrollContainer.post(new Runnable() {
                @Override
                public void run() {
                    mScrollContainer.fullScroll(ScrollView.FOCUS_DOWN);
                }
            });
        } else {
            Log.d(TAG, String.format("log: %s", formattedMessage));
        }
    }

    // Event subscribers.
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onMessageEvent(LocalEventFromMediaPlayerHolder.UpdateLog event) {
        log(event.formattedMessage);
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onMessageEvent(LocalEventFromMediaPlayerHolder.PlaybackDuration event) {
        mSeekbarAudio.setMax(event.duration);
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onMessageEvent(LocalEventFromMediaPlayerHolder.PlaybackPosition event) {
        if (!isUserSeeking) {
            mSeekbarAudio.setProgress(event.position, true);
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onMessageEvent(LocalEventFromMediaPlayerHolder.StateChanged event) {
        Toast.makeText(this, String.format("State changed to:%s", event.currentState),
                       Toast.LENGTH_SHORT).show();
    }

}
