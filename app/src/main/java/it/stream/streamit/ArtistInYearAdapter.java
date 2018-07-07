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

class ArtistInYearAdapter extends RecyclerView.Adapter<ArtistInYearAdapter.ViewHolder> {

    private Context mContext;
    private List<ArtistInYearList> mList;
    private String year;

    public ArtistInYearAdapter(Context mContext, List<ArtistInYearList> mList, String year) {
        this.mContext = mContext;
        this.mList = mList;
        this.year = year;
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
        viewHolder.t.setText(mList.get(i).getArtist());

        Glide.with(mContext)
                .asBitmap()
                .load(mList.get(i).getImageUrl())
                .into(viewHolder.iv);

        viewHolder.cv.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(v.getContext(), Album.class);
                intent.putExtra("year", year);
                intent.putExtra("artist", mList.get(i).getArtist());
                v.getContext().startActivity(intent);
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
