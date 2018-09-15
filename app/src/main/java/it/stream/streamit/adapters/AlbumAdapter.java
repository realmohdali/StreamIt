package it.stream.streamit.adapters;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.sqlite.SQLiteDatabase;
import android.os.AsyncTask;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;

import java.io.File;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;

import it.stream.streamit.database.ConnectionCheck;
import it.stream.streamit.dataList.ListItem;
import it.stream.streamit.R;
import it.stream.streamit.backgroundService.MediaService;
import it.stream.streamit.database.FavoriteManagement;
import it.stream.streamit.download.DownloadManagement;
import jp.wasabeef.glide.transformations.RoundedCornersTransformation;

import static com.bumptech.glide.request.RequestOptions.bitmapTransform;
import static it.stream.streamit.backgroundService.MediaPlayerControllerConstants.ADD_TO_PLAYLIST;

public class AlbumAdapter extends RecyclerView.Adapter<AlbumAdapter.ViewHolder> {

    public static final String Broadcast_PLAY_NEW_AUDIO = "it.stream.streamit.PlayNewAudio";
    private Context mContext;
    private List<ListItem> mList;
    private boolean serviceRunning;
    private SQLiteDatabase database;
    private String title, url, sub, img, artist, year;
    private DownloadManagement downloadManagement;
    private int fileSize;


    public AlbumAdapter(Context mContext, List<ListItem> mList, SQLiteDatabase database) {
        this.mContext = mContext;
        this.mList = mList;
        this.database = database;
    }


    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int i) {
        View v = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.album_list_view_item, viewGroup, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull final ViewHolder viewHolder, int i) {
        viewHolder.a.setText(mList.get(i).getArtist());
        viewHolder.t.setText(mList.get(i).getTitle());
        viewHolder.y.setText(mList.get(i).getYear());

        viewHolder.loading.setVisibility(View.GONE);
        viewHolder.down.setVisibility(View.VISIBLE);

        final String file_url, image_url, file_title, file_artist, file_year;
        file_url = mList.get(i).getURL();
        image_url = mList.get(i).getImageUrl();
        file_title = mList.get(i).getTitle();
        file_artist = mList.get(i).getArtist();
        file_year = mList.get(i).getYear();

        downloadManagement = new DownloadManagement(file_url, database, mContext, (file_title + ".mp3"), file_title, image_url, file_artist, file_year);
        final boolean exists = downloadManagement.fileExists();

        if (isFav(mList.get(i).getURL())) {
            viewHolder.addToFav.setImageResource(R.drawable.ic_favorite_green_24dp);
        } else {
            viewHolder.addToFav.setImageResource(R.drawable.ic_favorite_border_white_24dp);
        }

        Glide.with(mContext)
                .asBitmap()
                .load(mList.get(i).getImageUrl())
                .apply(bitmapTransform(new RoundedCornersTransformation(5, 0, RoundedCornersTransformation.CornerType.ALL)))
                .into(viewHolder.iv);


        viewHolder.addToQ.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                readData();
                int i = viewHolder.getAdapterPosition();
                if (!serviceRunning) {
                    Toast.makeText(mContext, "Media player is not running", Toast.LENGTH_SHORT).show();
                } else {
                    url = mList.get(i).getURL();
                    title = mList.get(i).getTitle();
                    sub = mList.get(i).getArtist();
                    sub += " | ";
                    sub += mList.get(i).getYear();
                    img = mList.get(i).getImageUrl();
                    year = mList.get(i).getYear();
                    artist = mList.get(i).getArtist();

                    Intent intent = new Intent(ADD_TO_PLAYLIST);
                    intent.putExtra("url", url);
                    intent.putExtra("title", title);
                    intent.putExtra("sub", sub);
                    intent.putExtra("img", img);
                    intent.putExtra("year", year);
                    intent.putExtra("artist", artist);
                    mContext.sendBroadcast(intent);

                    Toast.makeText(mContext, "Added to queue", Toast.LENGTH_SHORT).show();
                }
            }
        });

        viewHolder.addToFav.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                boolean isFav = isFav(mList.get(viewHolder.getAdapterPosition()).getURL());

                int i = viewHolder.getAdapterPosition();
                url = mList.get(i).getURL();
                title = mList.get(i).getTitle();
                sub = mList.get(i).getArtist();
                sub += " | ";
                sub += mList.get(i).getYear();
                img = mList.get(i).getImageUrl();
                year = mList.get(i).getYear();
                artist = mList.get(i).getArtist();
                if (isFav) {
                    FavoriteManagement favoriteManagement = new FavoriteManagement(title, url, img, artist, year, database, mContext);
                    switch (favoriteManagement.removeFav()) {
                        case FavoriteManagement.SUCCESS:
                            viewHolder.addToFav.setImageResource(R.drawable.ic_favorite_border_white_24dp);
                            Toast.makeText(mContext, "Removed from favorite", Toast.LENGTH_SHORT).show();
                            break;
                        case FavoriteManagement.ALREADY_EXISTS_OR_REMOVED:
                            viewHolder.addToFav.setImageResource(R.drawable.ic_favorite_border_white_24dp);
                            Toast.makeText(mContext, "This track does not exist in favorite", Toast.LENGTH_SHORT).show();
                            break;
                        case FavoriteManagement.ERROR:
                            Toast.makeText(mContext, "Error in removing from favorite", Toast.LENGTH_SHORT).show();
                            break;
                    }
                } else {
                    FavoriteManagement favoriteManagement = new FavoriteManagement(title, url, img, artist, year, database, mContext);
                    switch (favoriteManagement.addFav()) {
                        case FavoriteManagement.SUCCESS:
                            viewHolder.addToFav.setImageResource(R.drawable.ic_favorite_green_24dp);
                            Toast.makeText(mContext, "Added to favorite", Toast.LENGTH_SHORT).show();
                            break;
                        case FavoriteManagement.ALREADY_EXISTS_OR_REMOVED:
                            viewHolder.addToFav.setImageResource(R.drawable.ic_favorite_green_24dp);
                            Toast.makeText(mContext, "This track is already exist in favorite", Toast.LENGTH_SHORT).show();
                            break;
                        case FavoriteManagement.ERROR:
                            Toast.makeText(mContext, "Error in adding to favorite", Toast.LENGTH_SHORT).show();
                            break;
                    }
                }
            }
        });

        if (exists) {
            viewHolder.down.setVisibility(View.GONE);
        }
        viewHolder.down.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(final View view) {
                if (downloading()) {
                    Toast.makeText(mContext, "A Download is already in progress", Toast.LENGTH_SHORT).show();
                } else if (gettingSize()) {
                    Toast.makeText(mContext, "Fetching size of another file", Toast.LENGTH_SHORT).show();
                } else {
                    class GetSize extends AsyncTask<Void, Void, Void> {

                        @Override
                        protected void onPreExecute() {
                            super.onPreExecute();
                            viewHolder.down.setVisibility(View.GONE);
                            viewHolder.loading.setVisibility(View.VISIBLE);
                            SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(mContext);
                            SharedPreferences.Editor editor = preferences.edit();
                            editor.putBoolean("gettingSize", true);
                            editor.apply();
                        }

                        @Override
                        protected Void doInBackground(Void... voids) {
                            try {
                                URL url = new URL(file_url);
                                HttpURLConnection connection = (HttpURLConnection) url.openConnection();

                                fileSize = connection.getContentLength();

                                connection.disconnect();

                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                            return null;
                        }

                        @Override
                        protected void onPostExecute(Void aVoid) {
                            super.onPostExecute(aVoid);

                            SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(mContext);
                            SharedPreferences.Editor editor = preferences.edit();
                            editor.putBoolean("gettingSize", false);
                            editor.apply();

                            int i = viewHolder.getAdapterPosition();

                            if (fileSize > 0) {
                                float mbSize = fileSize / (1024 * 1024);
                                String fileSizeMB = String.valueOf(mbSize) + " MB";

                                AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
                                builder.setTitle("Download");
                                builder.setMessage("File : " + mList.get(i).getTitle() + " | Size : " + fileSizeMB);
                                builder.setPositiveButton("Download", new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialogInterface, int i) {
                                        int j = viewHolder.getAdapterPosition();
                                        String file_url, image_url, file_title, file_artist, file_year;
                                        file_url = mList.get(j).getURL();
                                        image_url = mList.get(j).getImageUrl();
                                        file_title = mList.get(j).getTitle();
                                        file_artist = mList.get(j).getArtist();
                                        file_year = mList.get(j).getYear();

                                        downloadManagement = new DownloadManagement(file_url, database, mContext, (file_title + ".mp3"), file_title, image_url, file_artist, file_year);
                                        downloadManagement.initDownload();
                                    }
                                });
                                builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialogInterface, int i) {
                                        viewHolder.loading.setVisibility(View.GONE);
                                        viewHolder.down.setVisibility(View.VISIBLE);
                                        dialogInterface.dismiss();
                                    }
                                });
                                builder.setCancelable(false);

                                builder.show();
                            }
                        }
                    }

                    GetSize getSize = new GetSize();
                    getSize.execute();
                }
            }
        });

        viewHolder.item.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (ConnectionCheck.isConnected(mContext)) {
                    readData();

                    int i = viewHolder.getAdapterPosition();

                    url = mList.get(i).getURL();
                    title = mList.get(i).getTitle();
                    sub = mList.get(i).getArtist();
                    sub += " | ";
                    sub += mList.get(i).getYear();
                    img = mList.get(i).getImageUrl();
                    year = mList.get(i).getYear();
                    artist = mList.get(i).getArtist();

                    writeData();

                    if (!serviceRunning) {
                        if (exists) {
                            String fileName = title + ".mp3";
                            File file = mContext.getFileStreamPath(fileName);
                            url = file.getAbsolutePath();
                            writeData();
                        }
                        Intent intent = new Intent(mContext, MediaService.class);
                        intent.putExtra("isPlayList", false);
                        mContext.startService(intent);
                    } else {
                        if (exists) {
                            String fileName = title + ".mp3";
                            File file = mContext.getFileStreamPath(fileName);
                            url = file.getAbsolutePath();
                            writeData();
                        }
                        Intent intent = new Intent(Broadcast_PLAY_NEW_AUDIO);
                        intent.putExtra("isPlayList", false);
                        mContext.sendBroadcast(intent);
                    }
                } else {
                    Toast.makeText(mContext, "You are not connected to Internet", Toast.LENGTH_SHORT).show();
                }
            }
        });

    }

    @Override
    public int getItemCount() {
        try {
            if (mList.size() > 0) {
                return mList.size();
            }
        } catch (NullPointerException e) {
            return 0;
        }
        return 0;
    }

    public class ViewHolder extends RecyclerView.ViewHolder {

        ImageView iv;
        TextView t, a, y;
        LinearLayout item;
        ImageButton addToQ, addToFav, down;
        ProgressBar loading;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);

            iv = itemView.findViewById(R.id.image);
            t = itemView.findViewById(R.id.title);
            a = itemView.findViewById(R.id.artist);
            y = itemView.findViewById(R.id.year);
            item = itemView.findViewById(R.id.albumItemView);
            addToFav = itemView.findViewById(R.id.addToFav);
            addToQ = itemView.findViewById(R.id.addToQueue);
            down = itemView.findViewById(R.id.download);
            loading = itemView.findViewById(R.id.loadingDownload);
        }
    }

    private void writeData() {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(mContext);
        SharedPreferences.Editor editor = preferences.edit();
        editor.putString("title", title);
        editor.putString("sub", sub);
        editor.putString("img", img);
        editor.putString("artist", artist);
        editor.putString("year", year);
        editor.putString("media", url);
        editor.apply();
    }

    private void readData() {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(mContext);
        serviceRunning = sp.getBoolean("serviceRunning", false);
    }

    private boolean isFav(String url) {
        FavoriteManagement favoriteManagement = new FavoriteManagement(url, database);
        return favoriteManagement.alreadyExists();
    }

    private boolean downloading() {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(mContext);
        return sp.getBoolean("downloadingService", false);
    }

    private boolean gettingSize() {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(mContext);
        return sp.getBoolean("gettingSize", false);
    }
}
