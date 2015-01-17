package in.xnnyygn.android.musicplayer;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;

public class MusicListActivity extends Activity implements
    AdapterView.OnItemClickListener {

  private static final String LOG_TAG = "activity.musiclist";
  private MusicListAdapter mAdapter;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_music_list);

    File musicDir =
        new File(Environment.getExternalStorageDirectory(), "/xy/music/test");
    Log.i(LOG_TAG, "list music from dir " + musicDir);
    if (musicDir.exists()) {
      ListView musicList = (ListView) findViewById(R.id.lvMusicList);
      mAdapter = new MusicListAdapter(listMp3(musicDir));
      musicList.setAdapter(mAdapter);
      musicList.setOnItemClickListener(this);
    }
  }

  private List<File> listMp3(File rootDir) {
    List<File> files = new ArrayList<File>();
    for (File file : rootDir.listFiles()) {
      if (file.isFile()
          && file.getName().toLowerCase(Locale.getDefault()).endsWith(".mp3")) {
        files.add(file);
      }
    }
    return files;
  }

  private class MusicListAdapter extends BaseAdapter {

    private final List<Music> musics;

    public MusicListAdapter(List<File> files) {
      super();

      musics = new ArrayList<Music>();
      for (File f : files) {
        musics.add(new Music(f));
      }
    }

    @Override
    public int getCount() {
      return musics.size();
    }

    @Override
    public Object getItem(int position) {
      return musics.get(position);
    }

    public Music getMusic(int position) {
      return (Music) getItem(position);
    }

    @Override
    public long getItemId(int position) {
      return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
      if (convertView == null) {
        convertView =
            getLayoutInflater().inflate(R.layout.listitem_music, parent, false);
      }

      Music music = getMusic(position);
      TextView musicTitle =
          (TextView) convertView.findViewById(R.id.tvMusicTitle);
      musicTitle.setText(music.getTitle());

      TextView musicPath =
          (TextView) convertView.findViewById(R.id.tvMusicPath);
      musicPath.setText(music.getPath());
      return convertView;
    }

  }

  @Override
  public void onItemClick(AdapterView<?> parent, View view, int position,
      long id) {
    if (mAdapter == null) return; // never happen
    Music music = mAdapter.getMusic(position);
    Log.i(LOG_TAG, "choose music " + music);

    Intent intent = new Intent(this, MusicPlayerActivity.class);
    intent.putExtra(MusicPlayerActivity.EXTRA_MUSIC, music);
    startActivity(intent);
  }

  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
    getMenuInflater().inflate(R.menu.option_menu_activity_music_list, menu);
    return super.onCreateOptionsMenu(menu);
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    switch (item.getItemId()) {
      case R.id.mnuExit:
        stopService(new Intent(this, MusicPlayerService.class));
        finish();
        break;
    }
    return super.onOptionsItemSelected(item);
  }

}
