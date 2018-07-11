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

import java.time.Year;
import java.util.List;

import jp.wasabeef.glide.transformations.RoundedCornersTransformation;

import static com.bumptech.glide.request.RequestOptions.bitmapTransform;

class YearAdapter extends RecyclerView.Adapter<YearAdapter.ViewHolder> {

    private Context mContext;
    private List<YearList> mYearList;

    public YearAdapter(Context mContext, List<YearList> mYearList) {
        this.mContext = mContext;
        this.mYearList = mYearList;
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
        viewHolder.t.setText(mYearList.get(i).getYear());
        Glide.with(mContext)
                .asBitmap()
                .load(mYearList.get(i).getImageUrl())
                .apply(bitmapTransform(new RoundedCornersTransformation(5,0, RoundedCornersTransformation.CornerType.ALL)))
                .into(viewHolder.iv);

        viewHolder.cv.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(view.getContext(), ArtistInYear.class);
                intent.putExtra("year", mYearList.get(i).getYear());
                view.getContext().startActivity(intent);
            }
        });
    }

    @Override
    public int getItemCount() {
        return mYearList.size();
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
