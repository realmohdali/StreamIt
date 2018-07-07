package it.stream.streamit;

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

class YearInArtistAdapter extends RecyclerView.Adapter<YearInArtistAdapter.ViewHolder> {
    private Context mContext;
    private List<YearInArtistList> mList;
    private String artist;

    public YearInArtistAdapter(Context mContext, List<YearInArtistList> mList, String artist) {
        this.mContext = mContext;
        this.mList = mList;
        this.artist = artist;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int i) {
        View v = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.list_item2, viewGroup, false);
        ViewHolder viewHolder = new ViewHolder(v);
        return viewHolder;
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder viewHolder, final int i) {
        viewHolder.t.setText(mList.get(i).getYear());
        Glide.with(mContext)
                .asBitmap()
                .load(mList.get(i).getImage())
                .into(viewHolder.iv);

        viewHolder.cv.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(view.getContext(), Album.class);
                intent.putExtra("year", mList.get(i).getYear());
                intent.putExtra("artist", artist);
                view.getContext().startActivity(intent);
            }
        });
    }

    @Override
    public int getItemCount() {
        return mList.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {

        TextView t;
        ImageView iv;
        CardView cv;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);

            t = itemView.findViewById(R.id.title);
            iv = itemView.findViewById(R.id.image);
            cv = itemView.findViewById(R.id.cardView);
        }
    }
}
