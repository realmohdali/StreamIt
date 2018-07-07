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

class ArtistAdapter extends RecyclerView.Adapter<ArtistAdapter.ViewHolder> {

    private Context mContext;
    private List<ArtistList> mList;

    public ArtistAdapter(Context mContext, List<ArtistList> mList) {
        this.mContext = mContext;
        this.mList = mList;
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
        final String image = mList.get(i).getImageUrl();
        viewHolder.cv.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent mIntent = new Intent(view.getContext(), YearInArtist.class);
                mIntent.putExtra("artist", mList.get(i).getArtist());
                mIntent.putExtra("image", image);
                view.getContext().startActivity(mIntent);
            }
        });
    }

    @Override
    public int getItemCount() {
        return mList.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {

        ImageView iv;
        TextView t;
        CardView cv;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);

            t = itemView.findViewById(R.id.title);
            iv = itemView.findViewById(R.id.image);
            cv = itemView.findViewById(R.id.cardView);
        }
    }
}
