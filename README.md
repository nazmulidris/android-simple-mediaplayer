# Getting started with MediaPlayer on Android

This article is an introduction on how to use Android MediaPlayer in your apps to playback audio.

In order to playback audio you can use MediaPlayer or ExoPlayer on Android. For this article, we are going to use MediaPlayer. 

You can use one instance of MediaPlayer to play one piece of audio at any given time. However, you can re-use an instance of MediaPlayer to play different audio files.

In the source code used in this example, we are going to load a single MP3 file from the `res/raw` folder in the app. You can also stream audio over a URL, but we are going to keep things really simple for this example and article.

The MediaPlayer holds a lot of heavy resources in the Android platform (such as codecs). These are shared resources and should be relased when the MediaPlayer isn't playing anything. While you can re-use a MediaPlayer object to play more than one audio file, you should release it's resources as often as possible, so that you're not consuming resources while not really playing anything back.

There are more sophisticated use cases involving audio focus and MediaSession which are not going to be covered in this article.

# Source code on Github

You can get the source code for the example app use in this article [at github.com/r3bl-alliance/android-simple-mediaplayer/](https://github.com/r3bl-alliance/android-simple-mediaplayer). 

# Using MediaPlayer

The code example on GitHub shows you a very simple way to use a MediaPlayer object, that just plays one MP3 file that's loaded from the APK itself.

Here are the steps to using a MediaPlayer.

1. Create a MediaPlayer object. You can reuse this instance to play the same MP3 file over and over again, or load new MP3 files into the player object.
2. Once created, you have to load media into it before you can play it. We are not loading audio files from the network, but from the APK directly. You have to prepare this audio before it can be played back. In the example, we use the blocking method `prepare()` but if you were loading a very large file, or streaming over network, then you should use `prepareAsync()`. If you use the async version, then you have to [attach a listener](https://stackoverflow.com/questions/23309857/android-correct-usage-of-prepareasync-in-media-player-activity) that will be called once enough content has been buffered to start playback.
3. You can cycle between `play()`, `pause()`, and `stop()` as many times as you like. 
4. If you call `reset()` then playback stops and the MediaPlayer has to be loaded with content again before playback can start. In the MediaPlayerHolder, `load(int)` has to be called after `reset()` for this reason. 
5. Once you are done with playback, make sure to call `release()` and let go of all the resources the MediaPlayer has been holding. You can start from Step 2, and reuse the same instance of the MediaPlayer to play audio.

## MainActivity

The main classes to look at in the source code example are MainActivity and MediaPlayerHolder. Here are some key elements highlighed from each of these classes.

The MainActivity contains the MediaPlayerHolder object. The holder object is created and released with the activity lifecycle. You can also move the holder into a [bound and started service](https://developerlife.com/2017/07/10/android-o-n-and-below-component-lifecycles-and-background-tasks/) and it would easily map to the service's lifecycle as well.

```java
public class MainActivity extends Activity{
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
}
```

## MediaPlayerHolder

The holder class wraps a MediaPlayer and manages it's creation, release, loading, media preparation, and playback.

```java
public class MediaPlayerHolder{
    public MediaPlayerHolder(Context context) {
        mContext = context;
        EventBus.getDefault().register(this);
        mMediaPlayer = new MediaPlayer();
        mMediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mediaPlayer) {
                stopUpdatingSeekbarWithPlaybackProgress(true);
                logToUI("MediaPlayer playback completed");
                EventBus.getDefault().post(new LocalEventFromMediaPlayerHolder.PlaybackCompleted());
                EventBus.getDefault()
                        .post(new LocalEventFromMediaPlayerHolder.StateChanged(
                                PlayerState.COMPLETED));
            }
        });
        logToUI("mMediaPlayer = new MediaPlayer()");
    }

    // MediaPlayer orchestration.

    public void release() {
        logToUI("release() and mMediaPlayer = null");
        mMediaPlayer.release();
        EventBus.getDefault().unregister(this);
    }

    public void play() {
        if (!mMediaPlayer.isPlaying()) {
            logToUI(String.format("start() %s",
                                  mContext.getResources().getResourceEntryName(mResourceId)));
            mMediaPlayer.start();
            startUpdatingSeekbarWithPlaybackProgress();
            EventBus.getDefault()
                    .post(new LocalEventFromMediaPlayerHolder.StateChanged(PlayerState.PLAYING));
        }
    }

    public void pause() {
        if (mMediaPlayer.isPlaying()) {
            mMediaPlayer.pause();
            logToUI("pause()");
            EventBus.getDefault()
                    .post(new LocalEventFromMediaPlayerHolder.StateChanged(PlayerState.PAUSED));
        }
    }

    public void reset() {
        logToUI("reset()");
        mMediaPlayer.reset();
        load(mResourceId);
        stopUpdatingSeekbarWithPlaybackProgress(true);
        EventBus.getDefault()
                .post(new LocalEventFromMediaPlayerHolder.StateChanged(PlayerState.RESET));
    }

    public void load(int resourceId) {
        mResourceId = resourceId;
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
        initSeekbar();
    }
}
```

## Event Bus

The MainActivity has a Seekbar which has to change based on MediaPlayer playback state. And of course the control of the playback occurs in the MainActivity where the user presses the PLAY, PAUSE, or RESET buttons. You could wire these classes up using callbacks, interfaces, or even directly making method calls, and passing references to the activity to the holder and vice versa. Instead, the code example uses an event bus in order to do this wiring while keeping these components loosely coupled, but strongly coherent.

There are 2 classes that define the events (that are local to the process, since both the Activity and MediaPlayer run in the same process). 

### Events from media player to UI

These events are fired from the MediaPlayerHolder class, and they have listeners in the MainActivity that respond to them. This is how information about playback is sent to the UI (so that the seekbar can be updated).

```java
public class LocalEventFromMediaPlayerHolder {

    public static class UpdateLog {

        public final StringBuffer formattedMessage;

        public UpdateLog(StringBuffer formattedMessage) {
            this.formattedMessage = formattedMessage;
        }
    }

    public static class PlaybackDuration {

        public final int duration;

        public PlaybackDuration(int duration) {
            this.duration = duration;
        }
    }

    public static class PlaybackPosition {

        public final int position;

        public PlaybackPosition(int position) {
            this.position = position;
        }
    }

    public static class PlaybackCompleted {

    }

    public static class StateChanged {

        public final MediaPlayerHolder.PlayerState currentState;

        public StateChanged(MediaPlayerHolder.PlayerState currentState) {
            this.currentState = currentState;
        }
    }

}
```

### Events from UI to media player

These events are fired from the MainActivity to the MediaPlayerHolder. Things like the user pressing on a button to PLAY, PAUSE, RESET, and dragging the seekbar to change progress are fired from the activity and handled in the holder.

```java
public class LocalEventFromMainActivity {

    public static class StartPlayback {

    }

    public static class ResetPlayback {

    }

    public static class PausePlayback {

    }

    public static class StopUpdatingSeekbarWithMediaPosition {

    }

    public static class StartUpdatingSeekbarWithPlaybackPosition {

    }

    public static class SeekTo {

        public final int position;

        public SeekTo(int position) {
            this.position = position;
        }
    }

}
```

# Seekbar integration

There are two parts to seekbar integration. 

1. The MainActivity has a Seekbar that moves as playback occurs. 
2. The user can also drag the seekbar at anytime and move the playback position of the audio to whatever they have selected.

## 1. Updating the seekbar with playback info

The following code lives in MediaPlayerHolder. 

- It starts an Executor that is started when playback starts. 
- This executor simply gets the progress and reports it to the MainActivity via the event bus. 
- This executor is stopped when playback completes, or reset is called.

Note: when the MP3 file is first loaded, it sends its duration to the activity as well, so that the Seekbar's `setMax(int)` method can be called with the duration of the loaded media. 

```java
public class MediaPlayerHolder{
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
}
```

## 2. Dragging the seekbar

The user can drag the Seekbar in the MainActivity at anytime. This requires the Seekbar to already be initialized with the duration of the audio from the MediaPlayerHolder (as described above). 

While the user is dragging the Seekbar, it can no longer report media playback progress. Otherwise, the playback progress info will fight with the user's drag operation. So while the user is dragging the Seekbar, it becomes numb to being updated by playback progress info.

```java
public class MainActivity extends Activity{
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
}    
```