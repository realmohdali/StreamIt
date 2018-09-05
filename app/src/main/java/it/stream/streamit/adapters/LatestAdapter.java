package it.stream.streamit.adapters;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.v7.widget.CardView;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;

import java.util.List;

import it.stream.streamit.R;
import it.stream.streamit.YearInArtist;
import it.stream.streamit.dataList.RecentList;

public class LatestAdapter extends RecyclerView.Adapter<LatestAdapter.ViewHolder> {

    private Context context;
    private List<RecentList> list;
    private Activity activity;

    public LatestAdapter(Context context, List<RecentList> list, Activity activity) {
        this.context = context;
        this.list = list;
        this.activity = activity;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int i) {
        return new ViewHolder(LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.recent_item, viewGroup, false));
    }

    @Override
    public void onBindViewHolder(@NonNull final ViewHolder viewHolder, int i) {
        String ttl = list.get(i).getArtist() + " " + list.get(i).getYear();
        viewHolder.title.setText(ttl);
        Glide.with(context)
                .asBitmap()
                .load(list.get(i).getImg())
                .into(viewHolder.imageView);
        viewHolder.item.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String artist = list.get(viewHolder.getAdapterPosition()).getArtist();
                String image = list.get(viewHolder.getAdapterPosition()).getImg();

                Intent intent = new Intent(context, YearInArtist.class);
                intent.putExtra("artist", artist);
                intent.putExtra("image", image);
                view.getContext().startActivity(intent);
                activity.overridePendingTransition(0, 0);
            }
        });
    }

    @Override
    public int getItemCount() {
        return list.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        ImageView imageView;
        TextView title;
        CardView item;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            imageView = itemView.findViewById(R.id.image);
            title = itemView.findViewById(R.id.title);
            item = itemView.findViewById(R.id.item);
        }
    }
}
