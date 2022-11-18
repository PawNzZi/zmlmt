package cn.lingyikz.soundbook.soundbook.user.adapter;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import java.util.List;

import cn.hutool.core.bean.BeanUtil;
import cn.lingyikz.soundbook.soundbook.category.adapter.BottomAdapter;
import cn.lingyikz.soundbook.soundbook.databinding.FootviewBinding;
import cn.lingyikz.soundbook.soundbook.databinding.ItemHomeBinding;
import cn.lingyikz.soundbook.soundbook.home.activity.AudioDetailActivity;
import cn.lingyikz.soundbook.soundbook.home.adapter.HomeAdapter;
import cn.lingyikz.soundbook.soundbook.modle.v2.Album;
import cn.lingyikz.soundbook.soundbook.modle.v2.CategoryAlbum;
import cn.lingyikz.soundbook.soundbook.modle.v2.CollectionHistory;
import cn.lingyikz.soundbook.soundbook.utils.IntentAction;

public class CollectionAdapter  extends RecyclerView.Adapter<RecyclerView.ViewHolder>{

    private List<CollectionHistory.DataDTO.RowsDTO> list ;
    private Context context ;

    private ItemOperaCallBack itemOperaCallBack;
    // 普通布局
    private final int TYPE_ITEM = 1;
    // 脚布局
    private final int TYPE_FOOTER = 2;
    // 当前加载状态，默认为加载完成
    private int loadState = 2;
    // 正在加载
    public final int LOADING = 1;
    // 加载完成
    public final int LOADING_COMPLETE = 2;
    // 加载到底
    public final int LOADING_END = 3;


    public CollectionAdapter(List<CollectionHistory.DataDTO.RowsDTO> list, Context context,ItemOperaCallBack itemOperaCallBack){
        this.list = list ;
        this.context = context ;
        this.itemOperaCallBack = itemOperaCallBack;
    }

    @Override
    public int getItemViewType(int position) {
        // 最后一个item设置为FooterView
        if (position + 1 == getItemCount()) {
            return TYPE_FOOTER;
        } else {
            return TYPE_ITEM;
        }

    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {

        if(viewType == TYPE_ITEM){
            ItemHomeBinding itemHomeBinding = ItemHomeBinding.inflate(LayoutInflater.from(parent.getContext()),parent,false);
            return new CollectionAdapter.ViewHolder(itemHomeBinding);
        }else if(viewType == TYPE_FOOTER){
            FootviewBinding footviewBinding = FootviewBinding.inflate(LayoutInflater.from(parent.getContext()),parent,false);
            return new CollectionAdapter.FootViewHolder(footviewBinding);
        }
        return null ;
    }

    @Override
    public void onBindViewHolder(@NonNull  RecyclerView.ViewHolder holder, int position) {
        if(holder instanceof CollectionAdapter.FootViewHolder){
            CollectionAdapter.FootViewHolder footViewHolder = (CollectionAdapter.FootViewHolder) holder;
            switch (loadState){
                case LOADING://正在加载
                    footViewHolder.footviewBinding.loadingLinearLayout.setVisibility(View.VISIBLE);
                    footViewHolder.footviewBinding.noDataLinearLayout.setVisibility(View.GONE);
                    break;
                case LOADING_COMPLETE://加载完成
                    footViewHolder.footviewBinding.loadingLinearLayout.setVisibility(View.GONE);
                    footViewHolder.footviewBinding.noDataLinearLayout.setVisibility(View.GONE);
                    break;

                case LOADING_END://加载到底
                    footViewHolder.footviewBinding.loadingLinearLayout.setVisibility(View.GONE);
                    footViewHolder.footviewBinding.noDataLinearLayout.setVisibility(View.VISIBLE);
                    break;

                default:
                    break;
            }
        }else if(holder instanceof CollectionAdapter.ViewHolder){
            CollectionAdapter.ViewHolder viewHolder = (CollectionAdapter.ViewHolder) holder;
            viewHolder.itemHomeBinding.itemBookName.setText(list.get(position).getZmlmAlbum().getName());
            viewHolder.itemHomeBinding.collectionIcon.setVisibility(View.VISIBLE);
//        Log.i("tag",list.get(position).getBookName());
            viewHolder.itemHomeBinding.itemBookDescription.setText(list.get(position).getZmlmAlbum().getDescription());
            Glide.with(viewHolder.itemHomeBinding.getRoot()).load(list.get(position).getZmlmAlbum().getThumb()).into(viewHolder.itemHomeBinding.itemThumb);
            viewHolder.itemHomeBinding.collectionIcon.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    itemOperaCallBack.deleteItem(list.get(position).getZmlmAlbum().getId());
                }
            });
            viewHolder.itemHomeBinding.contentLayout.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Bundle bundle = new Bundle();

                    Album.DataDTO.RowsDTO albumDetail = new Album.DataDTO.RowsDTO();
//                    BeanUtil.copyProperties(list.get(position), albumDetail);
                    albumDetail.setThumb(list.get(position).getZmlmAlbum().getThumb());
                    albumDetail.setDescription(list.get(position).getZmlmAlbum().getDescription());
                    albumDetail.setName(list.get(position).getZmlmAlbum().getName());
                    albumDetail.setId(list.get(position).getAlbumId());
//                    albumDetail.setCategories(list.get(position).getAlbum().getCategories());
                    albumDetail.setAuthor(list.get(position).getZmlmAlbum().getAuthor());

                    bundle.putSerializable("bookObject", albumDetail);
                    IntentAction.setValueContext(context, AudioDetailActivity.class,bundle);
                }
            });
        }


    }

    @Override
    public int getItemCount() {
        return list.size() + 1;
    }

    public class ViewHolder extends RecyclerView.ViewHolder {

        ItemHomeBinding itemHomeBinding ;
        public ViewHolder(ItemHomeBinding itemHomeBinding) {
            super(itemHomeBinding.getRoot());
            this.itemHomeBinding = itemHomeBinding ;
        }
    }

    public class FootViewHolder extends  RecyclerView.ViewHolder{

        FootviewBinding footviewBinding ;
        public FootViewHolder(FootviewBinding footviewBinding ) {
            super(footviewBinding.getRoot());
            this.footviewBinding = footviewBinding ;
        }
    }

    /**
     * 设置上拉加载状态
     *
     * @param loadState 0.正在加载 1.加载完成 2.加载到底
     */
    public void setLoadState(int loadState) {
        this.loadState = loadState;
        notifyDataSetChanged();
    }

    public interface ItemOperaCallBack{
        void deleteItem(Long albumId);
    }
}
