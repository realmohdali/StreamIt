package it.stream.streamit.adapters;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.v7.widget.CardView;
import android.support.v7.widget.RecyclerView;
import android.text.method.ScrollingMovementMethod;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;

import java.util.List;

import it.stream.streamit.dataList.ArtistList;
import it.stream.streamit.database.ConnectionCheck;
import it.stream.streamit.R;
import it.stream.streamit.YearInArtist;
import jp.wasabeef.glide.transformations.RoundedCornersTransformation;

import static com.bumptech.glide.request.RequestOptions.bitmapTransform;

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
        View v = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.artist_list_item, viewGroup, false);
        ViewHolder mViewHolder = new ViewHolder(v);
        return mViewHolder;
    }

    @Override
    public void onBindViewHolder(@NonNull final ViewHolder viewHolder, final int i) {
        viewHolder.t.setText(mArtistList.get(i).getArtist());
        viewHolder.nat.setText(mArtistList.get(i).getNationality());
        viewHolder.year.setText(mArtistList.get(i).getYears());
        Glide.with(mContext)
                .asBitmap()
                .load(mArtistList.get(i).getImageUrl())
                .apply(bitmapTransform(new RoundedCornersTransformation(5, 0, RoundedCornersTransformation.CornerType.ALL)))
                .into(viewHolder.iv);
        final String image = mArtistList.get(i).getImageUrl();
        viewHolder.item.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (ConnectionCheck.isConnected(mContext)) {
                    Intent mIntent = new Intent(view.getContext(), YearInArtist.class);
                    mIntent.putExtra("artist", mArtistList.get(i).getArtist());
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
        TextView t, nat, year;
        ImageView iv;
        LinearLayout item;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            t = itemView.findViewById(R.id.title);
            iv = itemView.findViewById(R.id.image);
            item = itemView.findViewById(R.id.item);
            nat = itemView.findViewById(R.id.nationality);
            year = itemView.findViewById(R.id.years);
            t.setSelected(true);
        }
    }
}
