package com.libre;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.libre.ManageDevice.DataItem;
import com.libre.util.PicassoTrustCertificates;
import com.squareup.picasso.MemoryPolicy;
import com.squareup.picasso.NetworkPolicy;

import java.util.ArrayList;

/**
 * Created by khajan on 5/5/16.
 */
public class RecyclerViewAdapter extends RecyclerView.Adapter<RecyclerViewAdapter.ViewHolder> {
    RemoteItemClickListener itemClickListener;

    private ArrayList<DataItem> viewItemList;
    private Context mContext;
    public RecyclerViewAdapter(Context mContext ,ArrayList<DataItem> viewItemList,
                               RemoteItemClickListener itemClickListener) {
        this.itemClickListener = itemClickListener;
        this.viewItemList = viewItemList;
        this.mContext=mContext;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        View itemView = inflater.inflate(R.layout.ct_remotecommand_item, parent, false);
        return new ViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {

        setViews(position, holder);
        holder.setClickListener(new ItemClickListener() {
            @Override
            public void onClick(View view, int position) {

                switch (view.getId()) {
                    case R.id.row_layout:
                        itemClickListener.onRemoteItemClicked(position);
                        break;
                    case R.id.item_fav_button:
                        itemClickListener.onRemoteFavClicked(position);
                        break;
                }

            }
        });
    }

    private void setViews(int position, ViewHolder holder) {

        String name = viewItemList.get(position).getItemName();

        holder.mTextView.setText(name);
            holder.mTextView.setSelected(true);

        String folder = " Folder";
        String file = " File ";

        if (viewItemList.get(position).getItemType().equals(folder)) {
                holder.mImageview.setImageResource(R.drawable.album_borderless);
        } else {
            if(viewItemList.get(position).getItemAlbumURL()!=null && !viewItemList.get(position).getItemAlbumURL().trim().isEmpty()){
                // default image for tidal file is tidal logo, else load the image in the URL
                if (viewItemList.get(position).getItemType().equals(folder)) {
                    PicassoTrustCertificates.getInstance(mContext).
                            load(viewItemList.get(position).getItemAlbumURL())
                            .placeholder(R.drawable.album_borderless)
                            .error(R.drawable.album_borderless)
                            .into(holder.mImageview);
                } else {
                    PicassoTrustCertificates.getInstance(mContext).
                            load(viewItemList.get(position).getItemAlbumURL())
                            .placeholder(R.drawable.songs_borderless)
                            .error(R.drawable.songs_borderless)
                            .into(holder.mImageview);
                }
            }else {
                holder.mImageview.setImageResource(R.drawable.songs_borderless);
            }
        }

        holder.mFavImageview.setVisibility(View.VISIBLE);
        if (viewItemList.get(position) != null && viewItemList.get(position).getFavorite() == null)
            return;

        switch (viewItemList.get(position).getFavorite()) {
            case 0:
                holder.mFavImageview.setVisibility(View.GONE);
                break;
            case 1:
                holder.mFavImageview.setImageResource(R.mipmap.ic_remote_not_favorite);
                break;
            case 2:
                holder.mFavImageview.setImageResource(R.mipmap.ic_remote_favorite);
                break;
        }


    }

    @Override
    public int getItemCount() {
        return viewItemList.size();
    }

    public void replaceAdapterArray(ArrayList<DataItem> viewItemArray) {
        this.viewItemList = viewItemArray;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder
            implements View.OnClickListener {

        public TextView mTextView;
        public ImageView mImageview;
        public ImageView mFavImageview;
        public LinearLayout mRowLayout;
        private ItemClickListener clickListener;

        public ViewHolder(View itemView) {
            super(itemView);

            mImageview = (ImageView) itemView.findViewById(R.id.item_icon);
            mTextView = (TextView) itemView.findViewById(R.id.item_title);
            mFavImageview = (ImageView) itemView.findViewById(R.id.item_fav_button);
            mRowLayout = itemView.findViewById(R.id.row_layout);


            mFavImageview.setOnClickListener(this);
            mRowLayout.setOnClickListener(this);
        }

        public void setClickListener(ItemClickListener itemClickListener) {
            this.clickListener = itemClickListener;
        }

        @Override
        public void onClick(View view) {
            clickListener.onClick(view, getPosition());
        }


    }


    interface ItemClickListener {
        void onClick(View view, int position);
    }

    public interface RemoteItemClickListener {
        void onRemoteItemClicked(int position);

        void onRemoteFavClicked(int position);
    }

    public interface OnItemClickListener {
        void onItemClick(int position);
    }
}
