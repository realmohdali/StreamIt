package it.stream.streamit.adapters;

import android.content.Context;
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
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.google.gson.Gson;

import java.util.List;

import it.stream.streamit.database.ConnectionCheck;
import it.stream.streamit.database.FavoriteManagement;
import it.stream.streamit.dataList.ListItem;
import it.stream.streamit.R;
import it.stream.streamit.backgroundService.MediaService;
import jp.wasabeef.glide.transformations.RoundedCornersTransformation;

import static com.bumptech.glide.request.RequestOptions.bitmapTransform;
import static it.stream.streamit.backgroundService.MediaPlayerControllerConstants.ADD_TO_PLAYLIST;

public class FavAdapter extends RecyclerView.Adapter<FavAdapter.ViewHolder> {

    public static final String Broadcast_PLAY_NEW_AUDIO = "it.stream.streamit.PlayNewAudio";
    private Context mContext;
    private List<ListItem> mList;

    private boolean serviceRunning;
    private String title, url, sub, img, artist, year;

    private int len;

    public FavAdapter(Context mContext, List<ListItem> mList) {
        this.mContext = mContext;
        this.mList = mList;
        len = mList.size();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int i) {
        View v = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.fav_item, viewGroup, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull final ViewHolder viewHolder, final int i) {

        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(mContext);
        final boolean serviceRunning = sp.getBoolean("serviceRunning", false);

        viewHolder.a.setText(mList.get(i).getArtist());
        viewHolder.t.setText(mList.get(i).getTitle());
        viewHolder.y.setText(mList.get(i).getYear());

        Glide.with(mContext)
                .asBitmap()
                .load(mList.get(i).getImageUrl())
                .apply(bitmapTransform(new RoundedCornersTransformation(5, 0, RoundedCornersTransformation.CornerType.ALL)))
                .into(viewHolder.iv);

        viewHolder.foreground.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (ConnectionCheck.isConnected(mContext)) {
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
                } else {
                    Toast.makeText(mContext, "You are not connected to Internet", Toast.LENGTH_SHORT).show();
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
        RelativeLayout background;
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

    public void removeItem(int i, SQLiteDatabase database) {
        String trackTitle, trackUrl, trackImg, trackSub;
        trackTitle = mList.get(i).getTitle();
        trackImg = mList.get(i).getImageUrl();
        trackSub = mList.get(i).getArtist();
        trackUrl = mList.get(i).getURL();
        FavoriteManagement favoriteManagement = new FavoriteManagement(trackTitle, trackUrl, trackImg, trackSub, database, mContext);
        switch (favoriteManagement.removeFav()) {
            case FavoriteManagement.SUCCESS:
                Toast.makeText(mContext, "Removed from favorite", Toast.LENGTH_SHORT).show();
                break;
            case FavoriteManagement.ALREADY_EXISTS_OR_REMOVED:
                Toast.makeText(mContext, "This track does not exist in favorite", Toast.LENGTH_SHORT).show();
                break;
            case FavoriteManagement.ERROR:
                Toast.makeText(mContext, "Error in removing from favorite", Toast.LENGTH_SHORT).show();
                break;
        }
        mList.remove(i);
        // notify the item removed by position
        // to perform recycler view delete animations
        // NOTE: don't call notifyDataSetChanged()
        notifyItemRemoved(i);
        len = mList.size();
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
