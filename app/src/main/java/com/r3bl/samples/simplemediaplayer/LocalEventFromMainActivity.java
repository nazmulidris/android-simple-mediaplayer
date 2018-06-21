/*
 * Copyright 2018 Nazmul Idris. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.r3bl.samples.simplemediaplayer;

/**
 * Holds all the local event types that are fired from the {@link MainActivity} to the
 * {@link MediaPlayerHolder} via the EventBus.
 */
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
