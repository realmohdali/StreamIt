package it.stream.streamit;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.database.sqlite.SQLiteDatabase;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.google.gson.Gson;

import java.util.List;

import jp.wasabeef.glide.transformations.RoundedCornersTransformation;

import static com.bumptech.glide.request.RequestOptions.bitmapTransform;

public class FavAdapter extends RecyclerView.Adapter<FavAdapter.ViewHolder> {

    public static final String Broadcast_PLAY_NEW_AUDIO = "it.stream.streamit.PlayNewAudio";
    private Context mContext;
    private List<ListItem> mList;

    private MediaPlayerService mediaPlayer;
    private boolean serviceBound = false;

    private int len;

    public FavAdapter(Context mContext, List<ListItem> mList) {
        this.mContext = mContext;
        this.mList = mList;
        len = mList.size();
    }

    ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
            MediaPlayerService.LocalBinder binder = (MediaPlayerService.LocalBinder) iBinder;
            mediaPlayer = binder.getService();
            serviceBound = true;
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {

        }
    };


    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int i) {
        View v = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.fav_item, viewGroup, false);
        FavAdapter.ViewHolder viewHolder = new FavAdapter.ViewHolder(v);
        return viewHolder;
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder viewHolder, final int i) {

        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(mContext);
        serviceBound = sp.getBoolean("bound", false);

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
                    final String url = mList.get(i).getURL();
                    String title = mList.get(i).getTitle();
                    String sub = mList.get(i).getArtist();
                    sub += " | ";
                    sub += mList.get(i).getYear();
                    String img = mList.get(i).getImageUrl();

                    Gson gson = new Gson();
                    String json = gson.toJson(mList);
                    int size = mList.size();

                    if (!serviceBound) {
                        Intent intent = new Intent(mContext, MediaPlayerService.class);
                        intent.putExtra("media", url);
                        intent.putExtra("title", title);
                        intent.putExtra("sub", sub);
                        intent.putExtra("img", img);
                        intent.putExtra("playlist", json);
                        intent.putExtra("size", size);
                        intent.putExtra("position", i);
                        mContext.startService(intent);
                        mContext.bindService(intent, serviceConnection, Context.BIND_ABOVE_CLIENT);
                    } else {
                        Intent intent = new Intent(Broadcast_PLAY_NEW_AUDIO);
                        intent.putExtra("media", url);
                        intent.putExtra("title", title);
                        intent.putExtra("sub", sub);
                        intent.putExtra("img", img);
                        intent.putExtra("playlist", json);
                        intent.putExtra("size", size);
                        intent.putExtra("position", i);
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
        return len;
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        RelativeLayout background;
        LinearLayout foreground;
        ImageView iv;
        TextView t, a, y, bt;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            iv = itemView.findViewById(R.id.image);
            t = itemView.findViewById(R.id.title);
            a = itemView.findViewById(R.id.artist);
            y = itemView.findViewById(R.id.year);
            foreground = itemView.findViewById(R.id.foreground);
            bt = itemView.findViewById(R.id.backText);
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
}
