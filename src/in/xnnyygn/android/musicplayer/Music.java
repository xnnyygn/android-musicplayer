package in.xnnyygn.android.musicplayer;

import java.io.File;
import java.io.Serializable;

public class Music implements Serializable {

  private static final long serialVersionUID = -975085691149061072L;
  private String title;
  private String path;

  public Music(File file) {
    this.title =
        file.getName().substring(0, file.getName().length() - ".mp3".length());
    this.path = file.getAbsolutePath();
  }

  public String getTitle() {
    return title;
  }

  public void setTitle(String title) {
    this.title = title;
  }

  public String getPath() {
    return path;
  }

  public void setPath(String path) {
    this.path = path;
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((path == null) ? 0 : path.hashCode());
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) return true;
    if (obj == null) return false;
    if (getClass() != obj.getClass()) return false;
    Music other = (Music) obj;
    if (path == null) {
      if (other.path != null) return false;
    } else if (!path.equals(other.path)) return false;
    return true;
  }

  @Override
  public String toString() {
    return "Music [title=" + title + ", path=" + path + "]";
  }

}
