package it.stream.streamit.adapters;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.sqlite.SQLiteDatabase;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.v7.widget.CardView;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;

import java.util.List;

import it.stream.streamit.R;
import it.stream.streamit.backgroundService.MediaService;
import it.stream.streamit.dataList.ListItem;
import it.stream.streamit.database.ConnectionCheck;
import it.stream.streamit.database.FavoriteManagement;

import static it.stream.streamit.backgroundService.MediaPlayerControllerConstants.ADD_TO_PLAYLIST;

public class RecentAdapter extends RecyclerView.Adapter<RecentAdapter.ViewHolder> {

    private Context context;
    private List<ListItem> list;

    private boolean serviceRunning;
    private SQLiteDatabase database;
    private String title, url, sub, img, artist, year;

    public static final String Broadcast_PLAY_NEW_AUDIO = "it.stream.streamit.PlayNewAudio";

    public RecentAdapter(Context context, List<ListItem> list, SQLiteDatabase database) {
        this.context = context;
        this.list = list;
        this.database = database;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int i) {
        View view = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.recent_item, viewGroup, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull final ViewHolder viewHolder, int i) {
        viewHolder.buttons.setVisibility(View.VISIBLE);

        String ttl = list.get(i).getTitle();
        viewHolder.title.setText(ttl);
        Glide.with(context)
                .asBitmap()
                .load(list.get(i).getImageUrl())
                .into(viewHolder.imageView);

        if (isFav(list.get(i).getURL())) {
            viewHolder.addToFav.setImageResource(R.drawable.ic_favorite_green_24dp);
        } else {
            viewHolder.addToFav.setImageResource(R.drawable.ic_favorite_border_white_24dp);
        }

        viewHolder.item.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (ConnectionCheck.isConnected(context)) {
                    readData();

                    int i = viewHolder.getAdapterPosition();

                    url = list.get(i).getURL();
                    title = list.get(i).getTitle();
                    sub = list.get(i).getArtist();
                    sub += " | ";
                    sub += list.get(i).getYear();
                    img = list.get(i).getImageUrl();
                    year = list.get(i).getYear();
                    artist = list.get(i).getArtist();

                    writeData();

                    if (!serviceRunning) {
                        Intent intent = new Intent(context, MediaService.class);
                        intent.putExtra("isPlayList", false);
                        context.startService(intent);
                    } else {
                        Intent intent = new Intent(Broadcast_PLAY_NEW_AUDIO);
                        intent.putExtra("isPlayList", false);
                        context.sendBroadcast(intent);
                    }
                } else {
                    Toast.makeText(context, "You are not connected to Internet", Toast.LENGTH_SHORT).show();
                }
            }
        });

        viewHolder.addToQ.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                readData();
                int i = viewHolder.getAdapterPosition();
                if (!serviceRunning) {
                    Toast.makeText(context, "Media player is not running", Toast.LENGTH_SHORT).show();
                } else {
                    url = list.get(i).getURL();
                    title = list.get(i).getTitle();
                    sub = list.get(i).getArtist();
                    sub += " | ";
                    sub += list.get(i).getYear();
                    img = list.get(i).getImageUrl();
                    year = list.get(i).getYear();
                    artist = list.get(i).getArtist();

                    Intent intent = new Intent(ADD_TO_PLAYLIST);
                    intent.putExtra("url", url);
                    intent.putExtra("title", title);
                    intent.putExtra("sub", sub);
                    intent.putExtra("img", img);
                    intent.putExtra("year", year);
                    intent.putExtra("artist", artist);
                    context.sendBroadcast(intent);

                    Toast.makeText(context, "Added to queue", Toast.LENGTH_SHORT).show();
                }
            }
        });

        viewHolder.addToFav.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                boolean isFav = isFav(list.get(viewHolder.getAdapterPosition()).getURL());

                int i = viewHolder.getAdapterPosition();
                url = list.get(i).getURL();
                title = list.get(i).getTitle();
                sub = list.get(i).getArtist();
                sub += " | ";
                sub += list.get(i).getYear();
                img = list.get(i).getImageUrl();
                year = list.get(i).getYear();
                artist = list.get(i).getArtist();
                if (isFav) {
                    FavoriteManagement favoriteManagement = new FavoriteManagement(title, url, img, artist, year, database, context);
                    switch (favoriteManagement.removeFav()) {
                        case FavoriteManagement.SUCCESS:
                            viewHolder.addToFav.setImageResource(R.drawable.ic_favorite_border_white_24dp);
                            Toast.makeText(context, "Removed from favorite", Toast.LENGTH_SHORT).show();
                            break;
                        case FavoriteManagement.ALREADY_EXISTS_OR_REMOVED:
                            viewHolder.addToFav.setImageResource(R.drawable.ic_favorite_border_white_24dp);
                            Toast.makeText(context, "This track does not exist in favorite", Toast.LENGTH_SHORT).show();
                            break;
                        case FavoriteManagement.ERROR:
                            Toast.makeText(context, "Error in removing from favorite", Toast.LENGTH_SHORT).show();
                            break;
                    }
                } else {
                    FavoriteManagement favoriteManagement = new FavoriteManagement(title, url, img, artist, year, database, context);
                    switch (favoriteManagement.addFav()) {
                        case FavoriteManagement.SUCCESS:
                            viewHolder.addToFav.setImageResource(R.drawable.ic_favorite_green_24dp);
                            Toast.makeText(context, "Added to favorite", Toast.LENGTH_SHORT).show();
                            break;
                        case FavoriteManagement.ALREADY_EXISTS_OR_REMOVED:
                            viewHolder.addToFav.setImageResource(R.drawable.ic_favorite_green_24dp);
                            Toast.makeText(context, "This track is already exist in favorite", Toast.LENGTH_SHORT).show();
                            break;
                        case FavoriteManagement.ERROR:
                            Toast.makeText(context, "Error in adding to favorite", Toast.LENGTH_SHORT).show();
                            break;
                    }
                }
            }
        });
    }

    @Override
    public int getItemCount() {
        return list.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        ImageView imageView, addToFav, addToQ;
        TextView title;
        CardView item;
        LinearLayout buttons;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            imageView = itemView.findViewById(R.id.image);
            title = itemView.findViewById(R.id.title);
            item = itemView.findViewById(R.id.item);
            addToFav = itemView.findViewById(R.id.addToFav);
            addToQ = itemView.findViewById(R.id.addToQueue);
            buttons = itemView.findViewById(R.id.buttons);
        }
    }

    private void writeData() {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
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
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
        serviceRunning = sp.getBoolean("serviceRunning", false);
    }

    private boolean isFav(String url) {
        FavoriteManagement favoriteManagement = new FavoriteManagement(url, database);
        return favoriteManagement.alreadyExists();
    }
}