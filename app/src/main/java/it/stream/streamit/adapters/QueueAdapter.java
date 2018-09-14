package it.stream.streamit.adapters;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
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
import jp.wasabeef.glide.transformations.RoundedCornersTransformation;

import static com.bumptech.glide.request.RequestOptions.bitmapTransform;
import static it.stream.streamit.backgroundService.MediaPlayerControllerConstants.REMOVE_ITEM;

public class QueueAdapter extends RecyclerView.Adapter<QueueAdapter.ViewHolder> {

    private static final String Broadcast_PLAY_NEW_AUDIO = "it.stream.streamit.PlayNewAudio";
    private Context mContext;
    private List<ListItem> mList;
    private String status;
    private int change;

    private String title, url, sub, img, artist, year;

    public QueueAdapter(Context mContext, List<ListItem> mList) {
        this.mContext = mContext;
        this.mList = mList;
        status = "normal";
        change = 0;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int i) {
        View v = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.queue_item, viewGroup, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull final ViewHolder viewHolder, int i) {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(mContext);
        final boolean serviceRunning = sp.getBoolean("serviceRunning", false);

        if (change == viewHolder.getAdapterPosition()) {
            switch (status) {
                case "normal":
                    viewHolder.loading.setVisibility(View.GONE);
                    viewHolder.playing.setVisibility(View.GONE);
                    break;
                case "loading":
                    viewHolder.playing.setVisibility(View.GONE);
                    viewHolder.loading.setVisibility(View.VISIBLE);
                    break;
                case "playing":
                    viewHolder.loading.setVisibility(View.GONE);
                    viewHolder.playing.setVisibility(View.VISIBLE);
                    break;
            }
        } else {
            viewHolder.loading.setVisibility(View.GONE);
            viewHolder.playing.setVisibility(View.GONE);
        }

        viewHolder.a.setText(mList.get(i).getArtist());
        viewHolder.t.setText(mList.get(i).getTitle());
        viewHolder.y.setText(mList.get(i).getYear());

        viewHolder.ap.setText(mList.get(i).getArtist());
        viewHolder.tp.setText(mList.get(i).getTitle());
        viewHolder.yp.setText(mList.get(i).getYear());

        Glide.with(mContext)
                .asBitmap()
                .load(mList.get(i).getImageUrl())
                .apply(bitmapTransform(new RoundedCornersTransformation(5, 0, RoundedCornersTransformation.CornerType.ALL)))
                .into(viewHolder.iv);

        Glide.with(mContext)
                .asBitmap()
                .load(mList.get(i).getImageUrl())
                .apply(bitmapTransform(new RoundedCornersTransformation(5, 0, RoundedCornersTransformation.CornerType.ALL)))
                .into(viewHolder.ivPlaying);

        viewHolder.foreground.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                final int i = viewHolder.getAdapterPosition();
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

                    if (!serviceRunning) {
                        Intent intent = new Intent(mContext, MediaService.class);
                        intent.putExtra("isPlayList", false);
                        mContext.startService(intent);
                    } else {
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
        if (mList != null) {
            return mList.size();
        } else {
            return 0;
        }
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        ImageView iv, ivPlaying;
        TextView t, a, y, tp, ap, yp;
        LinearLayout foreground, loading, playing;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            iv = itemView.findViewById(R.id.image);
            ivPlaying = itemView.findViewById(R.id.imagePlaying);
            t = itemView.findViewById(R.id.title);
            tp = itemView.findViewById(R.id.titlePlaying);
            a = itemView.findViewById(R.id.artist);
            ap = itemView.findViewById(R.id.artistPlaying);
            y = itemView.findViewById(R.id.year);
            yp = itemView.findViewById(R.id.yearPlaying);
            foreground = itemView.findViewById(R.id.foreground);
            playing = itemView.findViewById(R.id.playing);
            loading = itemView.findViewById(R.id.loading);
        }
    }

    public void removeItem(int i) {
        mList.remove(i);
        Intent intent = new Intent(REMOVE_ITEM);
        intent.putExtra("i", i);
        mContext.sendBroadcast(intent);
        notifyItemRemoved(i);
    }

    public void update(String state, int pos) {
        status = state;
        change = pos;
        notifyItemChanged(pos);
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
}
