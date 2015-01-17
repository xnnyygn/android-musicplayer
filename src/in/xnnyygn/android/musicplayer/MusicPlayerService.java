package in.xnnyygn.android.musicplayer;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.media.MediaPlayer;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;

public class MusicPlayerService extends Service implements
    MediaPlayer.OnPreparedListener, MediaPlayer.OnErrorListener,
    MediaPlayer.OnCompletionListener {

  /**
   * Local binder.
   * 
   * @author xnnyygn
   */
  public class LocalBinder extends Binder {

    /**
     * Get service.
     * 
     * @return music player service
     */
    MusicPlayerService getService() {
      return MusicPlayerService.this;
    }

  }

  public static final String ACTION_STATUS_UPDATE =
      "in.xnnyygn.android.musicplayer.action.STATUS_UPDATE";
  public static final String EXTRA_STATUS = "playerStatus";
  public static final int STATUS_INIT = 0;
  public static final int STATUS_PLAYING = 1;
  public static final int STATUS_PAUSED = 2;
  public static final int PLAY_MODE_SINGLE = 0;
  public static final int PLAY_MODE_LOOPING = 1;

  private static final String LOG_TAG = "service";
  private static final int NOTIFICATION_ID = 955;

  private MediaPlayer mMediaPlayer;
  private IBinder mBinder = new LocalBinder();
  private int mStartId;

  private Music mCurrentMusic;
  private int mCurrentStatus = STATUS_INIT;
  private int mPlayMode = PLAY_MODE_SINGLE;

  @Override
  public int onStartCommand(Intent intent, int flags, int startId) {
    Log.d(LOG_TAG, "onStartCommand called");
    mStartId = startId;

    if (mMediaPlayer == null) {
      Log.i(LOG_TAG, "create media player");
      mMediaPlayer = new MediaPlayer();
      mMediaPlayer.setOnPreparedListener(this);
      mMediaPlayer.setOnErrorListener(this);
      mMediaPlayer.setOnCompletionListener(this);
    }
    return super.onStartCommand(intent, flags, startId); // return STICKY
  }

  @Override
  public IBinder onBind(Intent intent) {
    Log.v(LOG_TAG, "onBind called");
    return mBinder;
  }

  @Override
  public boolean onUnbind(Intent intent) {
    Log.v(LOG_TAG, "onUnbind called");
    return true; // indicate call to rebind other than bind
  }

  @Override
  public void onRebind(Intent intent) {
    Log.v(LOG_TAG, "onRebind called");
    super.onRebind(intent);
  }

  @Override
  public void onDestroy() {
    Log.v(LOG_TAG, "onDestroy called");

    Log.i(LOG_TAG, "stop and release media player");
    if (mMediaPlayer.isPlaying()) {
      mMediaPlayer.stop();
    }
    // set to INIT status to prevent future call
    updateStatus(STATUS_INIT);

    mMediaPlayer.release();

    Log.d(LOG_TAG, "stop foreground and remove notification");
    stopForeground(true); // also remove notification
    super.onDestroy();
  }

  /**
   * Pause player.
   * 
   * @see #STATUS_PLAYING
   * @see MediaPlayer#pause()
   * @see #updateAndBroadcastStatus(int)
   */
  public void pause() {
    if (mCurrentStatus != STATUS_PLAYING) {
      Log.i(LOG_TAG, "not playing, cannot pause");
      return;
    }

    Log.i(LOG_TAG, "player pause");
    mMediaPlayer.pause();
    updateAndBroadcastStatus(STATUS_PAUSED);
  }

  /**
   * Resume player.
   * 
   * @see #STATUS_INIT
   * @see #STATUS_PAUSED
   * @see MediaPlayer#start()
   * @see #updateAndBroadcastStatus(int)
   */
  public void resume() {
    Log.i(LOG_TAG, "player resume");
    mMediaPlayer.start();

    updateAndBroadcastStatus(STATUS_PLAYING);
  }

  /**
   * Update player status and broadcast it.
   * 
   * @param status new status
   * @see #ACTION_STATUS_UPDATE
   * @see #EXTRA_STATUS
   */
  private void updateAndBroadcastStatus(int status) {
    updateStatus(status);

    Log.d(LOG_TAG, "broadcast status " + status);
    Intent intent = new Intent(ACTION_STATUS_UPDATE);
    intent.putExtra(EXTRA_STATUS, status);
    sendBroadcast(intent);
  }

  /**
   * Update status.
   * 
   * @param status
   */
  private void updateStatus(int status) {
    Log.i(LOG_TAG, "update status to " + status);

    mCurrentStatus = status;
  }

  /**
   * Get current status.
   * 
   * @return current status
   */
  public int getCurrentStatus() {
    return mCurrentStatus;
  }

  /**
   * Get music duration.
   * 
   * @return duration
   * @see MediaPlayer#getDuration()
   */
  public int getDuration() {
    return mMediaPlayer.getDuration();
  }

  /**
   * Get current position.
   * 
   * @return current position
   * @see MediaPlayer#getCurrentPosition()
   */
  public int getCurrentPosition() {
    return mMediaPlayer.getCurrentPosition();
  }

  /**
   * Seek to specified position.
   * 
   * @param position position
   * @see MediaPlayer#seekTo(int)
   */
  public void seekTo(int position) {
    Log.i(LOG_TAG, "player seek to " + position);
    mMediaPlayer.seekTo(position);
  }

  /**
   * Set play mode.
   * 
   * @param playMode play mode
   */
  public void setPlayMode(int playMode) {
    Log.i(LOG_TAG, "set play mode to " + playMode);
    mPlayMode = playMode;
  }

  /**
   * Get play mode.
   * 
   * @return
   */
  public int getPlayMode() {
    return mPlayMode;
  }

  /**
   * Play music.
   * 
   * @param music music to play, should not be null
   * @return true if success, otherwise false
   */
  public boolean play(Music music) {
    mCurrentMusic = music;
    mMediaPlayer.reset();
    Log.i(LOG_TAG, "schedule to play music " + music.getPath());
    try {
      mMediaPlayer.setDataSource(music.getPath());
      mMediaPlayer.prepare();
      return true;
    } catch (Exception e) {
      Log.w(LOG_TAG, "failed to play music " + music.getPath()
          + ", nested exception is " + e, e);
      
      // just stop service to prevent future call
      stopSelf(mStartId);
    }
    return false;
  }

  @Override
  public void onPrepared(MediaPlayer mp) {
    Log.v(LOG_TAG, "onPrepared called");
    Log.i(LOG_TAG, "play music " + mCurrentMusic + " now");
    updateStatus(STATUS_INIT);
    resume();
    sendForegroundNotification();
  }

  @SuppressWarnings("deprecation")
  private void sendForegroundNotification() {
    Notification notification =
        new Notification(R.drawable.ic_launcher, mCurrentMusic.getTitle(),
            System.currentTimeMillis());
    Intent intent = new Intent(this, MusicPlayerActivity.class);
    intent.putExtra(MusicPlayerActivity.EXTRA_SOURCE,
        MusicPlayerActivity.SOURCE_NOTIFICATION);
    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK
        | Intent.FLAG_ACTIVITY_CLEAR_TOP);
    PendingIntent pendingIntent =
        PendingIntent.getActivity(this, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT);
    notification.setLatestEventInfo(this, "MusicPlayer", "playing "
        + mCurrentMusic.getTitle(), pendingIntent);
    startForeground(NOTIFICATION_ID, notification);
  }

  /**
   * Get current music.
   * 
   * @return current music
   */
  public Music getCurrentMusic() {
    return mCurrentMusic;
  }

  @Override
  public boolean onError(MediaPlayer mp, int what, int extra) {
    Log.w(LOG_TAG, String.format("error occured, type %d, code %d, stop self",
        what, extra));
    stopSelf(mStartId);
    return true;
  }

  @Override
  public void onCompletion(MediaPlayer mp) {
    Log.v(LOG_TAG, "onCompletion called");

    updateAndBroadcastStatus(STATUS_INIT);
    Log.d(LOG_TAG, "current play mode " + mPlayMode);
    switch (mPlayMode) {
      case PLAY_MODE_LOOPING:
        resume();
        break;
    }
  }

}
