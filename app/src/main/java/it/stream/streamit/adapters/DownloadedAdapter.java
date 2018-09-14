package it.stream.streamit.adapters;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.sqlite.SQLiteDatabase;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.google.gson.Gson;

import java.io.File;
import java.util.List;

import it.stream.streamit.database.ConnectionCheck;
import it.stream.streamit.dataList.ListItem;
import it.stream.streamit.R;
import it.stream.streamit.backgroundService.MediaService;
import it.stream.streamit.download.DownloadManagement;
import jp.wasabeef.glide.transformations.RoundedCornersTransformation;

import static com.bumptech.glide.request.RequestOptions.bitmapTransform;
import static it.stream.streamit.backgroundService.MediaPlayerControllerConstants.ADD_TO_PLAYLIST;

public class DownloadedAdapter extends RecyclerView.Adapter<DownloadedAdapter.ViewHolder> {

    private static final String Broadcast_PLAY_NEW_AUDIO = "it.stream.streamit.PlayNewAudio";
    private Context mContext;
    private List<ListItem> mList;
    private SQLiteDatabase database;

    private boolean serviceRunning;
    private String title, url, sub, img, artist, year;

    private int len;

    public DownloadedAdapter(Context mContext, List<ListItem> mList, SQLiteDatabase database) {
        this.mContext = mContext;
        this.mList = mList;
        len = mList.size();
        this.database = database;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int i) {
        View v = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.fav_item, viewGroup, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull final ViewHolder viewHolder, int i) {
        viewHolder.a.setText(mList.get(i).getArtist());
        viewHolder.t.setText(mList.get(i).getTitle());
        viewHolder.y.setText(mList.get(i).getYear());

        String file_url, image_url, file_title, file_artist, file_year;
        file_url = mList.get(i).getURL();
        image_url = mList.get(i).getImageUrl();
        file_title = mList.get(i).getTitle();
        file_artist = mList.get(i).getArtist();
        file_year = mList.get(i).getYear();

        DownloadManagement downloadManagement = new DownloadManagement(file_url, database, mContext, (file_title + ".mp3"), file_title, image_url, file_artist, file_year);
        final boolean exists = downloadManagement.fileExists();

        if (exists) {
            String fileName = file_title + ".mp3";
            File file = mContext.getFileStreamPath(fileName);
            String newUrl = file.getAbsolutePath();

            ListItem item = new ListItem(file_title, file_artist, image_url, newUrl, file_year);
            mList.set(i, item);
        }

        Glide.with(mContext)
                .asBitmap()
                .load(mList.get(i).getImageUrl())
                .apply(bitmapTransform(new RoundedCornersTransformation(5, 0, RoundedCornersTransformation.CornerType.ALL)))
                .into(viewHolder.iv);

        viewHolder.foreground.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
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

                Gson gson = new Gson();
                String json = gson.toJson(mList);

                if (!serviceRunning) {
                    Intent intent = new Intent(mContext, MediaService.class);
                    intent.putExtra("playlist", json);
                    intent.putExtra("pos", i);
                    intent.putExtra("isPlayList", true);
                    mContext.startService(intent);
                } else {
                    Intent intent = new Intent(Broadcast_PLAY_NEW_AUDIO);
                    intent.putExtra("playlist", json);
                    intent.putExtra("pos", i);
                    intent.putExtra("isPlayList", true);
                    mContext.sendBroadcast(intent);
                }
            }
        });

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


    }

    @Override
    public int getItemCount() {
        return len;
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        public LinearLayout foreground;
        ImageView iv;
        TextView t, a, y, bt;
        ImageButton addToQ;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            iv = itemView.findViewById(R.id.image);
            t = itemView.findViewById(R.id.title);
            a = itemView.findViewById(R.id.artist);
            y = itemView.findViewById(R.id.year);
            foreground = itemView.findViewById(R.id.foreground);
            bt = itemView.findViewById(R.id.backText);
            addToQ = itemView.findViewById(R.id.addToQueue);
        }
    }

    public void removeItem(int i, final SQLiteDatabase database) {

        final String trackTitle, trackUrl, trackImg, trackArtist, trackYear;
        trackTitle = mList.get(i).getTitle();
        trackImg = mList.get(i).getImageUrl();
        trackArtist = mList.get(i).getArtist();
        trackYear = mList.get(i).getYear();
        trackUrl = mList.get(i).getURL();

        final int j = i;

        AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
        builder.setTitle("Delete File?");
        builder.setMessage(trackTitle + ".mp3");
        builder.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                DownloadManagement downloadManagement = new DownloadManagement(trackUrl, database, mContext, (trackTitle + ".mp3"), trackTitle, trackImg, trackArtist, trackYear);
                boolean deleted = downloadManagement.deleteFile();

                if (deleted) {
                    mList.remove(j);
                    notifyItemRemoved(j);
                    len = mList.size();
                } else {
                    Toast.makeText(mContext, "Unable to delte file", Toast.LENGTH_SHORT).show();
                    notifyItemChanged(j);
                }
            }
        });
        builder.setNegativeButton("No", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                notifyItemChanged(j);
            }
        });

        AlertDialog dialog = builder.create();
        dialog.setCancelable(false);
        dialog.show();
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
}
