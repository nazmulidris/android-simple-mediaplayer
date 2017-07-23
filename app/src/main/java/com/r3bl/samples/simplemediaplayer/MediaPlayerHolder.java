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

import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.media.MediaPlayer;
import android.widget.Toast;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.ArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Provides access to a MediaPlayer object which is used to play a single MP3 file from the
 * <code>res/raw</code> folder.
 */
public class MediaPlayerHolder {

    public static final int SEEKBAR_REFRESH_INTERVAL_MS = 100;

    private int mResourceId;
    private MediaPlayer mMediaPlayer = null;
    private Context mContext;
    private ArrayList<String> mLogMessages = new ArrayList<>();
    private ScheduledExecutorService mExecutor;
    private Runnable mSeekbarProgressUpdateTask;

    public MediaPlayerHolder(Context context, int resourceId) {
        mContext = context;
        mResourceId = resourceId;
    }

    // MediaPlayer orchestration.

    public void create() {
        EventBus.getDefault().register(this);
        mMediaPlayer = new MediaPlayer();
        mMediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mediaPlayer) {
                stopUpdatingSeekbarWithPlaybackProgress(true);
                logToUI("MediaPlayer playback completed");
                EventBus.getDefault().post(new LocalEventFromMediaPlayerHolder.PlaybackCompleted());
            }
        });
        load();
        initSeekbar();
        logToUI("mMediaPlayer = new MediaPlayer()");
    }

    public void release() {
        logToUI("release() and mMediaPlayer = null");
        mMediaPlayer.release();
        mMediaPlayer = null;
        EventBus.getDefault().unregister(this);
    }

    public void play() {
        if (mMediaPlayer != null && !mMediaPlayer.isPlaying()) {
            logToUI(String.format("start() %s",
                                  mContext.getResources().getResourceEntryName(mResourceId)));
            mMediaPlayer.start();
            startUpdatingSeekbarWithPlaybackProgress();
        }
    }

    public void pause() {
        if (mMediaPlayer.isPlaying()) {
            mMediaPlayer.pause();
        } else {
            Toast.makeText(mContext,
                           "Can't pause if not playing",
                           Toast.LENGTH_SHORT)
                    .show();
        }
        logToUI("pause()");
    }

    public void reset() {
        logToUI("reset()");
        mMediaPlayer.reset();
        load();
        stopUpdatingSeekbarWithPlaybackProgress(true);
    }

    public void load() {
        AssetFileDescriptor assetFileDescriptor =
                mContext.getResources().openRawResourceFd(mResourceId);
        try {
            logToUI("load() {1. setDataSource}");
            mMediaPlayer.setDataSource(assetFileDescriptor);
        } catch (Exception e) {
            logToUI(e.toString());
        }

        try {
            logToUI("load() {2. prepare}");
            mMediaPlayer.prepare();
        } catch (Exception e) {
            logToUI(e.toString());
        }
    }

    public void seekTo(int duration) {
        logToUI(String.format("seekTo() %d ms", duration));
        mMediaPlayer.seekTo(duration);
    }

    // Reporting media playback position to Seekbar in MainActivity.

    private void stopUpdatingSeekbarWithPlaybackProgress(boolean resetUIPlaybackPosition) {
        mExecutor.shutdownNow();
        mExecutor = null;
        mSeekbarProgressUpdateTask = null;
        if (resetUIPlaybackPosition) {
            EventBus.getDefault().post(new LocalEventFromMediaPlayerHolder.PlaybackPosition(0));
        }
    }

    private void startUpdatingSeekbarWithPlaybackProgress() {
        // Setup a recurring task to sync the mMediaPlayer position with the Seekbar.

        if (mExecutor == null) {
            mExecutor = Executors.newSingleThreadScheduledExecutor();
        }
        if (mSeekbarProgressUpdateTask == null) {
            mSeekbarProgressUpdateTask = new Runnable() {
                @Override
                public void run() {
                    if (mMediaPlayer != null && mMediaPlayer.isPlaying()) {
                        int currentPosition = mMediaPlayer.getCurrentPosition();
                        EventBus.getDefault().post(
                                new LocalEventFromMediaPlayerHolder.PlaybackPosition(
                                        currentPosition));
                    }
                }
            };
        }

        mExecutor.scheduleAtFixedRate(
                mSeekbarProgressUpdateTask,
                0,
                SEEKBAR_REFRESH_INTERVAL_MS,
                TimeUnit.MILLISECONDS
        );
    }

    public void initSeekbar() {
        // Set the duration.
        final int duration = mMediaPlayer.getDuration();

        EventBus.getDefault().post(
                new LocalEventFromMediaPlayerHolder.PlaybackDuration(duration));
        logToUI(String.format("setting seekbar max %d sec",
                              TimeUnit.MILLISECONDS.toSeconds(duration)));
    }

    @Subscribe(threadMode = ThreadMode.BACKGROUND)
    public void onMessageEvent(LocalEventFromMainActivity.SeekTo event) {
        seekTo(event.position);
    }

    @Subscribe(threadMode = ThreadMode.BACKGROUND)
    public void onMessageEvent(
            LocalEventFromMainActivity.StopUpdatingSeekbarWithMediaPosition event) {
        stopUpdatingSeekbarWithPlaybackProgress(false);
    }

    @Subscribe(threadMode = ThreadMode.BACKGROUND)
    public void onMessageEvent(
            LocalEventFromMainActivity.StartUpdatingSeekbarWithPlaybackPosition event) {
        startUpdatingSeekbarWithPlaybackProgress();
    }

    // Logging to UI methods.

    public void logToUI(String msg) {
        mLogMessages.add(msg);
        fireLogUpdate();
    }

    /**
     * update the MainActivity's UI with the debug log messages
     */
    public void fireLogUpdate() {
        StringBuffer formattedLogMessages = new StringBuffer();
        for (int i = 0; i < mLogMessages.size(); i++) {
            formattedLogMessages.append(i)
                    .append(" - ")
                    .append(mLogMessages.get(i));
            if (i != mLogMessages.size() - 1) {
                formattedLogMessages.append("\n");
            }
        }
        EventBus.getDefault().post(
                new LocalEventFromMediaPlayerHolder.UpdateLog(formattedLogMessages));
    }

    // Respond to playback localevents.

    @Subscribe(threadMode = ThreadMode.BACKGROUND)
    public void onMessageEvent(LocalEventFromMainActivity.PausePlayback event) {
        pause();
    }

    @Subscribe(threadMode = ThreadMode.BACKGROUND)
    public void onMessageEvent(LocalEventFromMainActivity.StartPlayback event) {
        play();
    }

    @Subscribe(threadMode = ThreadMode.BACKGROUND)
    public void onMessageEvent(LocalEventFromMainActivity.ResetPlayback event) {
        reset();
    }

}
