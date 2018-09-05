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
import android.widget.Toast;

import com.bumptech.glide.Glide;

import java.util.List;

import it.stream.streamit.dataList.ArtistList;
import it.stream.streamit.database.ConnectionCheck;
import it.stream.streamit.R;
import it.stream.streamit.YearInArtist;

public class ArtistHomeAdapter extends RecyclerView.Adapter<ArtistHomeAdapter.ViewHolder> {

    private List<ArtistList> mArtistList;
    private Context mContext;
    private Activity mActivity;

    public ArtistHomeAdapter(List<ArtistList> mArtistList, Context mContext, Activity mActivity) {
        this.mArtistList = mArtistList;
        this.mContext = mContext;
        this.mActivity = mActivity;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int i) {
        View v = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.recent_item, viewGroup, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull final ViewHolder viewHolder, int i) {
        viewHolder.nationality.setVisibility(View.VISIBLE);
        viewHolder.year.setVisibility(View.VISIBLE);

        viewHolder.title.setText(mArtistList.get(i).getArtist());
        viewHolder.nationality.setText(mArtistList.get(i).getNationality());
        viewHolder.year.setText(mArtistList.get(i).getYears());
        Glide.with(mContext)
                .asBitmap()
                .load(mArtistList.get(i).getImageUrl())
                .into(viewHolder.iv);
        final String image = mArtistList.get(i).getImageUrl();
        viewHolder.item.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (ConnectionCheck.isConnected(mContext)) {
                    Intent mIntent = new Intent(view.getContext(), YearInArtist.class);
                    mIntent.putExtra("artist", mArtistList.get(viewHolder.getAdapterPosition()).getArtist());
                    mIntent.putExtra("image", image);
                    view.getContext().startActivity(mIntent);
                    mActivity.overridePendingTransition(0, 0);
                } else {
                    Toast.makeText(mContext, R.string.offline, Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    @Override
    public int getItemCount() {
        return mArtistList.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {

        CardView item;
        ImageView iv;
        TextView title, nationality, year;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            item = itemView.findViewById(R.id.item);
            iv = itemView.findViewById(R.id.image);
            title = itemView.findViewById(R.id.title);
            nationality = itemView.findViewById(R.id.nationality);
            year = itemView.findViewById(R.id.years);
        }
    }
}
