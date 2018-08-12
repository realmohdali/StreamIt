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
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;

import java.util.List;

import it.stream.streamit.ArtistInYear;
import it.stream.streamit.database.ConnectionCheck;
import it.stream.streamit.R;
import it.stream.streamit.dataList.YearList;
import jp.wasabeef.glide.transformations.RoundedCornersTransformation;

import static com.bumptech.glide.request.RequestOptions.bitmapTransform;

public class YearHomeAdapter extends RecyclerView.Adapter<YearHomeAdapter.ViewHolder> {
    private List<YearList> mYearList;
    private Context mContext;
    private Activity mActivity;

    public YearHomeAdapter(Context mContext, List<YearList> mYearList, Activity mActivity) {
        this.mYearList = mYearList;
        this.mContext = mContext;
        this.mActivity = mActivity;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int i) {
        View v = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.list_item, viewGroup, false);
        ViewHolder viewHolder = new ViewHolder(v);
        return viewHolder;
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder viewHolder, final int i) {
        //viewHolder.t.setText(mYearList.get(i).getYear());
        Glide.with(mContext)
                .asBitmap()
                .load(mYearList.get(i).getImageUrl())
                .apply(bitmapTransform(new RoundedCornersTransformation(5, 0, RoundedCornersTransformation.CornerType.ALL)))
                .into(viewHolder.iv);
        viewHolder.item.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (ConnectionCheck.isConnected(mContext)) {
                    Intent intent = new Intent(view.getContext(), ArtistInYear.class);
                    intent.putExtra("year", mYearList.get(i).getYear());
                    view.getContext().startActivity(intent);
                    mActivity.overridePendingTransition(0, 0);
                } else {
                    Toast.makeText(mContext, R.string.offline, Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    @Override
    public int getItemCount() {
        return mYearList.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        //TextView t;
        ImageView iv;
        //CardView cv;
        RelativeLayout item;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            //t = itemView.findViewById(R.id.title);
            iv = itemView.findViewById(R.id.image);
            item = itemView.findViewById(R.id.item);
            //cv = itemView.findViewById(R.id.cardView);
        }
    }
}
