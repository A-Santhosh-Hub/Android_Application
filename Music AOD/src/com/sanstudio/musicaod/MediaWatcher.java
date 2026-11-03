package com.sanstudio.musicaod;

import android.content.ComponentName;
import android.content.Context;
import android.graphics.Bitmap;
import android.media.AudioManager;
import android.media.MediaMetadata;
import android.media.session.MediaController;
import android.media.session.MediaSessionManager;
import android.media.session.PlaybackState;
import android.os.Handler;
import android.os.SystemClock;
import android.view.KeyEvent;

import java.util.List;

/**
 * Follows the phone's active media session: song info, album art and its colours, position, and controls.
 * Needs notification access (MediaListener); without it, info stays empty and buttons send media keys.
 */
final class MediaWatcher {
    String title = "";
    String artist = "";
    Bitmap art;
    int[] artColors;
    long duration;

    private final Handler handler;
    private final AudioManager audio;
    private final MediaSessionManager sessions;
    private final ComponentName listener;
    private MediaController controller;
    private PlaybackState state;
    private Bitmap colorsFor;
    private boolean musicActive;

    private final MediaController.Callback callback = new MediaController.Callback() {
        @Override
        public void onMetadataChanged(MediaMetadata metadata) {
            read();
        }

        @Override
        public void onPlaybackStateChanged(PlaybackState s) {
            read();
        }

        @Override
        public void onSessionDestroyed() {
            handler.post(MediaWatcher.this::attach);
        }
    };

    private final MediaSessionManager.OnActiveSessionsChangedListener sessionsChanged = list -> attach();

    MediaWatcher(Context context, Handler handler) {
        this.handler = handler;
        audio = context.getSystemService(AudioManager.class);
        sessions = context.getSystemService(MediaSessionManager.class);
        listener = new ComponentName(context, MediaListener.class);
    }

    void start() {
        try {
            sessions.addOnActiveSessionsChangedListener(sessionsChanged, listener, handler);
        } catch (SecurityException ignored) {
            // No notification access yet.
        }
        poll();
        attach();
    }

    void stop() {
        sessions.removeOnActiveSessionsChangedListener(sessionsChanged);
        if (controller != null) controller.unregisterCallback(callback);
        controller = null;
        state = null;
    }

    /** Refreshes the "is anything playing" fallback; call about once a second, not every frame. */
    void poll() {
        musicActive = audio.isMusicActive();
    }

    boolean hasSong() {
        return !title.isEmpty();
    }

    boolean isPlaying() {
        return state != null ? state.getState() == PlaybackState.STATE_PLAYING : musicActive;
    }

    long position() {
        if (state == null) return 0;
        long pos = state.getPosition();
        if (state.getState() == PlaybackState.STATE_PLAYING) {
            pos += (long) ((SystemClock.elapsedRealtime() - state.getLastPositionUpdateTime()) * state.getPlaybackSpeed());
        }
        return Math.max(0, duration > 0 ? Math.min(pos, duration) : pos);
    }

    void previous() {
        if (controller != null) controller.getTransportControls().skipToPrevious();
        else sendKey(KeyEvent.KEYCODE_MEDIA_PREVIOUS);
    }

    void next() {
        if (controller != null) controller.getTransportControls().skipToNext();
        else sendKey(KeyEvent.KEYCODE_MEDIA_NEXT);
    }

    void playPause() {
        if (controller == null || state == null) sendKey(KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE);
        else if (isPlaying()) controller.getTransportControls().pause();
        else controller.getTransportControls().play();
    }

    private void sendKey(int code) {
        audio.dispatchMediaKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN, code));
        audio.dispatchMediaKeyEvent(new KeyEvent(KeyEvent.ACTION_UP, code));
    }

    private void attach() {
        MediaController next = null;
        try {
            List<MediaController> list = sessions.getActiveSessions(listener);
            for (MediaController mc : list) {
                PlaybackState s = mc.getPlaybackState();
                if (s != null && s.getState() == PlaybackState.STATE_PLAYING) {
                    next = mc;
                    break;
                }
            }
            if (next == null && !list.isEmpty()) next = list.get(0);
        } catch (SecurityException ignored) {
            // No notification access: song info stays hidden and buttons send media keys.
        }
        if (controller != null) controller.unregisterCallback(callback);
        controller = next;
        if (controller != null) controller.registerCallback(callback, handler);
        read();
    }

    private void read() {
        MediaMetadata m = controller != null ? controller.getMetadata() : null;
        state = controller != null ? controller.getPlaybackState() : null;
        title = m != null ? nonNull(m.getString(MediaMetadata.METADATA_KEY_TITLE)) : "";
        artist = m != null ? nonNull(m.getString(MediaMetadata.METADATA_KEY_ARTIST)) : "";
        duration = m != null ? m.getLong(MediaMetadata.METADATA_KEY_DURATION) : 0;

        Bitmap b = null;
        if (m != null) {
            b = m.getBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART);
            if (b == null) b = m.getBitmap(MediaMetadata.METADATA_KEY_ART);
            if (b == null) b = m.getBitmap(MediaMetadata.METADATA_KEY_DISPLAY_ICON);
        }
        art = b;
        if (b != colorsFor) {
            colorsFor = b;
            artColors = b != null ? ArtColors.extract(b) : null;
        }
    }

    private static String nonNull(String s) {
        return s == null ? "" : s;
    }
}
