package in.xnnyygn.android.musicplayer;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

public class MusicPlayerActivity extends Activity implements
    View.OnClickListener, SeekBar.OnSeekBarChangeListener {

  public static final String EXTRA_MUSIC = "music";
  public static final String EXTRA_SOURCE = "source";
  public static final int SOURCE_MUSIC_LIST = 0;
  public static final int SOURCE_NOTIFICATION = 1;
  private static final String LOG_TAG = "activity.musicplayer";
  private static final long INTERVAL_UPDATE_PROGRESS = 500;

  private boolean mLooping;

  private Button mPlayOrPauseButton;
  private ImageButton mLoopingButton;
  private TextView mMusicTitleTextView;
  private SeekBar mMusicPositionSeekBar;
  private Handler mHandler = new Handler();

  private Runnable mUpdateProgressTask = new Runnable() {

    @Override
    public void run() {
      Log.d(LOG_TAG, "update seekbar from task");
      updateMusicPositionSeekBar();
    }
  };

  private MusicPlayerService mMusicPlayerService;
  private ServiceConnection mConnection = new ServiceConnection() {

    @Override
    public void onServiceDisconnected(ComponentName name) {
      Log.d(LOG_TAG, "onServiceDisconnectd called");
      mMusicPlayerService = null;
    }

    @Override
    public void onServiceConnected(ComponentName name, IBinder service) {
      Log.d(LOG_TAG, "onServiceConnected called");
      mMusicPlayerService =
          ((MusicPlayerService.LocalBinder) service).getService();
      MusicPlayerActivity.this.onServiceConnected();
    }
  };

  private BroadcastReceiver mPlayerStatusReceiver = new BroadcastReceiver() {

    @Override
    public void onReceive(Context context, Intent intent) {
      int playerStatus =
          intent.getExtras().getInt(MusicPlayerService.EXTRA_STATUS, -1);
      Log.d(LOG_TAG, "receive player status " + playerStatus);
      onPlayerStatusUpdated(playerStatus);
    }
  };

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_music_player);

    mPlayOrPauseButton = (Button) findViewById(R.id.btnPlayOrPause);
    mPlayOrPauseButton.setOnClickListener(this);

    mLoopingButton = (ImageButton) findViewById(R.id.btnLooping);
    mLoopingButton.setOnClickListener(this);

    findViewById(R.id.btnBackward).setOnClickListener(this);
    findViewById(R.id.btnForward).setOnClickListener(this);

    mMusicPositionSeekBar = (SeekBar) findViewById(R.id.sbMusicPosition);
    mMusicPositionSeekBar.setOnSeekBarChangeListener(this);

    mMusicTitleTextView = (TextView) findViewById(R.id.tvMusicTitle);
  }

  @Override
  protected void onStart() {
    super.onStart();

    registerReceiver(mPlayerStatusReceiver, new IntentFilter(
        MusicPlayerService.ACTION_STATUS_UPDATE));

    Intent intent = new Intent(this, MusicPlayerService.class);
    startService(intent);

    boolean bound = bindService(intent, mConnection, BIND_AUTO_CREATE);
    Log.d(LOG_TAG, "bind service, result " + bound);
  }

  @Override
  protected void onStop() {
    super.onStop();
    // stop updating when activity is hidden
    mHandler.removeCallbacks(mUpdateProgressTask);

    unregisterReceiver(mPlayerStatusReceiver);

    Log.d(LOG_TAG, "unbind service");
    unbindService(mConnection);
  }

  @Override
  public void onClick(View v) {
    switch (v.getId()) {
      case R.id.btnPlayOrPause:
        playOrPause();
        break;
      case R.id.btnLooping:
        flipLoopingStatus();
        break;
      case R.id.btnBackward:
        step(-5);
        break;
      case R.id.btnForward:
        step(+5);
        break;
    }
  }

  private void step(int delta) {
    int duration = mMusicPlayerService.getDuration();
    int position =
        (mMusicPositionSeekBar.getProgress() + delta) * duration
            / mMusicPositionSeekBar.getMax();
    if (position < 0) position = 0;
    if (position > duration) position = duration;

    mMusicPlayerService.seekTo(position);
    Log.d(LOG_TAG, "update seekbar from button");
    doUpdateMusicPositionSeekBar();
  }

  /**
   * Set looping or not.
   */
  private void flipLoopingStatus() {
    mLooping = !mLooping;
    mMusicPlayerService.setPlayMode(mLooping
        ? MusicPlayerService.PLAY_MODE_LOOPING
        : MusicPlayerService.PLAY_MODE_SINGLE);
    updateLoopingButtonBackground();
  }

  private void updateLoopingButtonBackground() {
    mLoopingButton.setImageResource(mLooping ? R.drawable.btn_repeat_pressed
        : R.drawable.btn_repeat);
  }

  /**
   * Play or pause.
   * 
   * @see MusicPlayerService#pause()
   * @see MusicPlayerService#resume()
   */
  private void playOrPause() {
    switch (mMusicPlayerService.getCurrentStatus()) {
      case MusicPlayerService.STATUS_PAUSED:
      case MusicPlayerService.STATUS_INIT:
        mMusicPlayerService.resume();
        break;
      case MusicPlayerService.STATUS_PLAYING:
        mMusicPlayerService.pause();
        break;
    }
  }

  @Override
  public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
  }

  @Override
  public void onStartTrackingTouch(SeekBar seekBar) {
    // stop updating when tracking
    mHandler.removeCallbacks(mUpdateProgressTask);
  }

  @Override
  public void onStopTrackingTouch(SeekBar seekBar) {
    int position =
        seekBar.getProgress() * mMusicPlayerService.getDuration()
            / seekBar.getMax();
    mMusicPlayerService.seekTo(position);

    Log.d(LOG_TAG, "update seekbar from tracking");
    updateMusicPositionSeekBar();
  }

  /**
   * On music player music connected.
   */
  private void onServiceConnected() {
    Bundle bundle = getIntent().getExtras();
    int source = bundle.getInt(EXTRA_SOURCE);
    Music currentMusic = mMusicPlayerService.getCurrentMusic();

    if (source == SOURCE_MUSIC_LIST) {
      Music musicToPlay = (Music) bundle.getSerializable(EXTRA_MUSIC);
      if (currentMusic == null || !currentMusic.equals(musicToPlay)) {
        playMusic(musicToPlay);
      } else {
        updateView(musicToPlay);
      }
    } else {
      updateView(currentMusic);
    }
  }

  private void playMusic(Music musicToPlay) {
    updateStaticView(musicToPlay);
    if (!mMusicPlayerService.play(musicToPlay)) {
      // failed to play
      Toast.makeText(this,
          "failed to play music [" + musicToPlay.getTitle() + "]",
          Toast.LENGTH_SHORT).show();
      finish();
    }
  }

  /**
   * Update view.
   * 
   * @param music music to play
   */
  private void updateView(Music music) {
    updateStaticView(music);

    onPlayerStatusUpdated(mMusicPlayerService.getCurrentStatus());

    // update looping button
    mLooping =
        mMusicPlayerService.getPlayMode() == MusicPlayerService.PLAY_MODE_LOOPING;
    updateLoopingButtonBackground();
  }

  /**
   * Update static view.
   * 
   * @param music
   */
  private void updateStaticView(Music music) {
    mMusicTitleTextView.setText(music.getTitle());
  }

  /**
   * Update music position seek bar.
   */
  private void updateMusicPositionSeekBar() {
    switch (mMusicPlayerService.getCurrentStatus()) {
      case MusicPlayerService.STATUS_INIT:
        Log.i(LOG_TAG, "player status INIT, set progress to 0");
        mMusicPositionSeekBar.setProgress(0);
        break;
      case MusicPlayerService.STATUS_PAUSED:
        Log.i(LOG_TAG, "player pasued, just update progress");
        doUpdateMusicPositionSeekBar();
        break;
      case MusicPlayerService.STATUS_PLAYING:
        Log.d(LOG_TAG, "updating progress");
        doUpdateMusicPositionSeekBar();
        mHandler.postDelayed(mUpdateProgressTask, INTERVAL_UPDATE_PROGRESS);
        break;
    }
  }

  /**
   * Update music position seek bar.
   */
  private void doUpdateMusicPositionSeekBar() {
    int progress =
        mMusicPlayerService.getCurrentPosition()
            * mMusicPositionSeekBar.getMax()
            / mMusicPlayerService.getDuration();
    mMusicPositionSeekBar.setProgress(progress);
  }

  /**
   * Call when player status updated.
   * 
   * @param playerStatus current player status
   */
  private void onPlayerStatusUpdated(int playerStatus) {
    switch (playerStatus) {
      case MusicPlayerService.STATUS_PLAYING:
        mPlayOrPauseButton.setBackgroundResource(R.drawable.bg_btn_pause);
        break;
      case MusicPlayerService.STATUS_INIT:
      case MusicPlayerService.STATUS_PAUSED:
        mPlayOrPauseButton.setBackgroundResource(R.drawable.bg_btn_play);
        break;
    }

    Log.d(LOG_TAG, "update seekbar from callback");
    mHandler.removeCallbacks(mUpdateProgressTask);
    updateMusicPositionSeekBar();
  }

}
