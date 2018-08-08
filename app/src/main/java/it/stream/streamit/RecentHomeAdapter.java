package it.stream.streamit;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.v7.widget.CardView;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.google.gson.Gson;

import java.util.ArrayList;
import java.util.List;

import jp.wasabeef.glide.transformations.RoundedCornersTransformation;

import static com.bumptech.glide.request.RequestOptions.bitmapTransform;

public class RecentHomeAdapter extends RecyclerView.Adapter<RecentHomeAdapter.ViewHolder> {

    private List<ListItem> mListItems;
    private Context mContext;
    private MediaPlayerService mediaPlayer;
    private boolean serviceBound = false;

    public static final String Broadcast_PLAY_NEW_AUDIO = "it.stream.streamit.PlayNewAudio";

    public RecentHomeAdapter(List<ListItem> mListItems, Context mContext) {
        this.mListItems = new ArrayList<>();
        this.mListItems = mListItems;
        this.mContext = mContext;
    }

    private ServiceConnection serviceConnection = new ServiceConnection() {
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
    public RecentHomeAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int i) {
        View v = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.list_item, viewGroup, false);
        ViewHolder mViewHolder = new ViewHolder(v);
        return mViewHolder;
    }

    @Override
    public void onBindViewHolder(@NonNull RecentHomeAdapter.ViewHolder viewHolder, final int i) {

        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(mContext);
        serviceBound = sp.getBoolean("bound", false);

        Glide.with(mContext)
                .asBitmap()
                .load(mListItems.get(i).getImageUrl())
                .apply(bitmapTransform(new RoundedCornersTransformation(5, 0, RoundedCornersTransformation.CornerType.ALL)))
                .into(viewHolder.iv);
        viewHolder.t.setText(mListItems.get(i).getTitle());

        viewHolder.cv.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (ConnectionCheck.isConnected(mContext)) {
                    String URL = mListItems.get(i).getURL();
                    String title = mListItems.get(i).getTitle();
                    String sub = mListItems.get(i).getArtist();
                    sub += " | ";
                    sub += mListItems.get(i).getYear();
                    String img = mListItems.get(i).getImageUrl();
                    Gson gson = new Gson();
                    String json = gson.toJson(mListItems);
                    int size = mListItems.size();
                    int pos = i;

                    if (!serviceBound) {
                        Intent playerIntent = new Intent(mContext, MediaPlayerService.class);
                        playerIntent.putExtra("media", URL);
                        playerIntent.putExtra("title", title);
                        playerIntent.putExtra("sub", sub);
                        playerIntent.putExtra("img", img);
                        playerIntent.putExtra("playlist", json);
                        playerIntent.putExtra("size", size);
                        playerIntent.putExtra("position", pos);
                        mContext.startService(playerIntent);
                        mContext.bindService(playerIntent, serviceConnection, Context.BIND_ABOVE_CLIENT);
                    } else {
                        Intent broadcastIntent = new Intent(Broadcast_PLAY_NEW_AUDIO);
                        broadcastIntent.putExtra("media", URL);
                        broadcastIntent.putExtra("title", title);
                        broadcastIntent.putExtra("sub", sub);
                        broadcastIntent.putExtra("img", img);
                        broadcastIntent.putExtra("playlist", json);
                        broadcastIntent.putExtra("size", size);
                        broadcastIntent.putExtra("position", pos);
                        mContext.sendBroadcast(broadcastIntent);
                    }
                } else {
                    Toast.makeText(mContext, R.string.offline, Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    @Override
    public int getItemCount() {
        return mListItems.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        //TextView mTextViewHead;
        //TextView mTextViewDes;

        ImageView iv;
        TextView t;
        CardView cv;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            iv = itemView.findViewById(R.id.image);
            t = itemView.findViewById(R.id.title);
            cv = itemView.findViewById(R.id.cardView);
        }
    }
}