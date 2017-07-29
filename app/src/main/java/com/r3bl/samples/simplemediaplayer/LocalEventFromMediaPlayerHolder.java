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

/**
 * Holds all the local event types that are fired using from the {@link MediaPlayerHolder} to the
 * {@link MainActivity} via the EventBus.
 */
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
