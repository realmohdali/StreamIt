package it.stream.streamit;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.media.MediaPlayer;
import android.os.IBinder;
import android.support.annotation.NonNull;
import android.support.v7.widget.CardView;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.google.gson.Gson;

import java.util.List;

class RecentAdapter extends RecyclerView.Adapter<RecentAdapter.ViewHolder> {

    public static final String Broadcast_PLAY_NEW_AUDIO = "it.stream.streamit.PlayNewAudio";
    private Context mContext;
    private List<ListItem> mListItems;

    private MediaPlayerService mediaPlayer;
    private boolean serviceBound = false;

    public RecentAdapter(Context mContext, List<ListItem> mListItems) {
        this.mContext = mContext;
        this.mListItems = mListItems;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int i) {
        View v = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.album_item, viewGroup, false);
        ViewHolder viewHolder = new ViewHolder(v);
        return viewHolder;
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

    @Override
    public void onBindViewHolder(@NonNull final ViewHolder viewHolder, final int i) {

        viewHolder.a.setText(mListItems.get(i).getArtist());
        viewHolder.t.setText(mListItems.get(i).getTitle());
        viewHolder.y.setText(mListItems.get(i).getYear());

        Glide.with(mContext)
                .asBitmap()
                .load(mListItems.get(i).getImageUrl())
                .into(viewHolder.iv);

        viewHolder.cv.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String URL = mListItems.get(i).getURL();
                String title = mListItems.get(i).getTitle();
                String sub = mListItems.get(i).getArtist();
                sub += " | ";
                sub += mListItems.get(i).getYear();
                String img = mListItems.get(i).getImageUrl();

                Gson gson = new Gson();
                String json = gson.toJson(mListItems);
                int size = mListItems.size();

                if (!serviceBound) {
                    Intent intent = new Intent(mContext, MediaPlayerService.class);
                    intent.putExtra("media", URL);
                    intent.putExtra("title", title);
                    intent.putExtra("sub", sub);
                    intent.putExtra("img", img);
                    intent.putExtra("size", size);
                    intent.putExtra("position", i);
                    intent.putExtra("playlist", json);
                    mContext.startService(intent);
                    mContext.bindService(intent, serviceConnection, Context.BIND_ABOVE_CLIENT);
                } else {
                    Intent broadcastIntent = new Intent(Broadcast_PLAY_NEW_AUDIO);
                    broadcastIntent.putExtra("media", URL);
                    broadcastIntent.putExtra("title", title);
                    broadcastIntent.putExtra("sub", sub);
                    broadcastIntent.putExtra("img", img);
                    broadcastIntent.putExtra("playlist", json);
                    broadcastIntent.putExtra("size", size);
                    broadcastIntent.putExtra("position", i);
                    mContext.sendBroadcast(broadcastIntent);
                }
            }
        });

    }

    @Override
    public int getItemCount() {
        return mListItems.size();
    }


    public class ViewHolder extends RecyclerView.ViewHolder {

        ImageView iv;
        TextView t, a, y;
        CardView cv;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            iv = itemView.findViewById(R.id.image);
            t = itemView.findViewById(R.id.title);
            a = itemView.findViewById(R.id.artist);
            y = itemView.findViewById(R.id.year);
            cv = itemView.findViewById(R.id.cardView);
        }
    }
}
