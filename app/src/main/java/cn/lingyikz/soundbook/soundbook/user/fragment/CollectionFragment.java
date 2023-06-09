package cn.lingyikz.soundbook.soundbook.user.fragment;



import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import java.util.ArrayList;
import java.util.List;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.kongzue.dialogx.dialogs.MessageDialog;
import com.kongzue.dialogx.dialogs.PopTip;
import com.kongzue.dialogx.interfaces.OnDialogButtonClickListener;
import cn.hutool.core.util.ObjectUtil;
import cn.lingyikz.soundbook.soundbook.R;
import cn.lingyikz.soundbook.soundbook.api.RequestService;
import cn.lingyikz.soundbook.soundbook.base.BaseObsever;
import cn.lingyikz.soundbook.soundbook.databinding.FragmentCollectionBinding;
import cn.lingyikz.soundbook.soundbook.main.BaseFragment;
import cn.lingyikz.soundbook.soundbook.modle.v2.BaseModel;
import cn.lingyikz.soundbook.soundbook.modle.v2.CollectionHistory;
import cn.lingyikz.soundbook.soundbook.user.adapter.CollectionAdapter;
import cn.lingyikz.soundbook.soundbook.utils.Constans;
import cn.lingyikz.soundbook.soundbook.utils.EndlessRecyclerOnScrollListener;
import rx.Observable;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;

public class CollectionFragment extends BaseFragment  implements CollectionAdapter.ItemOperaCallBack {

    private FragmentCollectionBinding binding ;
    private CollectionAdapter adapter = null;
    private List<CollectionHistory.DataDTO.RowsDTO> albumList = new ArrayList<>();
//    private DataBaseHelper dataBaseHelper ;

    private int nextPage = 1;
    private boolean hasNextPage = false;

    public static CollectionFragment newInstance() {
        return new CollectionFragment();
    }


    @Override
    protected View setLayout(LayoutInflater inflater, ViewGroup container) {
        binding = FragmentCollectionBinding.inflate(LayoutInflater.from(getContext()),container,false);
        return binding.getRoot();
    }


    @Override
    protected void setView() {
        binding.swipeRecyclerView.addOnScrollListener(onScrollListener);

    }

    @Override
    protected void setData() {

    }

    private void initData() {
//        Log.i("TAG","initData");
        albumList.clear();
        if(ObjectUtil.isNotNull(Constans.user)){
           this.collectionList();
        }else {
            if(ObjectUtil.isNotNull(adapter)){
                adapter.notifyDataSetChanged();
            }
        }
    }

    @Override
    public void onResume() {
        super.onResume();

//        Log.i("TAG","CollectionFragment:onResume");
    }

    @Override
    public void onStart() {
        super.onStart();
        initData();
//        Log.i("TAG","CollectionFragment:onStart");
    }



    @Override
    public void onStop() {
        super.onStop();
//        Log.i("TAG","CollectionFragment:onStop");
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        binding = null ;
    }

    private void changeCollection(Long albumId){
        Observable<BaseModel> observable  = RequestService.getInstance().getApi().changeCollection(Constans.user.getId(),albumId);
        observable.subscribeOn(Schedulers.io()) // 在子线程中进行Http访问
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new BaseObsever<BaseModel>() {
                    @Override
                    public void onNext(BaseModel baseModel) {
                        if(baseModel.getCode() == 200){
                          initData();
                        }else {
                            PopTip.show(R.mipmap.warn_tip,"修改失败："+baseModel.getMessage()).showShort().setAutoTintIconInLightOrDarkMode(false);
                        }

                    }
                });
        observable.unsubscribeOn(Schedulers.io());
    }
    private void collectionList(){
        Observable<CollectionHistory> observable  = RequestService.getInstance().getApi().collectionList(Constans.user.getId(),nextPage,Constans.PAGE_SIZE);
        observable.subscribeOn(Schedulers.io()) // 在子线程中进行Http访问
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new BaseObsever<CollectionHistory>() {
                    @Override
                    public void onNext(CollectionHistory baseModel) {
                        if(baseModel.getCode() == 200 && baseModel.getData().getRows().size() > 0){
                            Log.i("TAG",baseModel.toString());
                            if(ObjectUtil.isNull(albumList)){
                                albumList = new ArrayList<>();
                            }
                            List<CollectionHistory.DataDTO.RowsDTO> newList = baseModel.getData().getRows();
                            albumList.addAll(newList);
                            if(ObjectUtil.isNull(adapter)){
                                binding.swipeRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
                                adapter = new CollectionAdapter(albumList,getContext(),CollectionFragment.this);
                                binding.swipeRecyclerView.setAdapter(adapter);
                            }


                            nextPage = baseModel.getData().getNextPage();
                            hasNextPage = baseModel.getData().getHasNextPage();
                        }else {
                           // PopTip.show("修改失败："+baseModel.getMessage()).showLong();

                        }
                        adapter.notifyDataSetChanged();

                    }
                });
        observable.unsubscribeOn(Schedulers.io());
    }

    private  RecyclerView.OnScrollListener  onScrollListener = new EndlessRecyclerOnScrollListener() {
        @Override
        public void onLoadMore() {
            if(hasNextPage){
                adapter.setLoadState(adapter.LOADING);
                collectionList();
            }else{
                //不加载
                adapter.setLoadState(adapter.LOADING_END);
                //Toast.makeText(getContext(), Constans.NO_LOADING, Toast.LENGTH_SHORT).show();
            }
        }
    };

    @Override
    public void deleteItem(Long albumId) {
        MessageDialog.show(Constans.WARN_TIP, "您确定要取消对该专辑的收藏嘛？", Constans.DIALOG_SURE_BUTTON,Constans.DIALOG_CANCEL_BUTTON).setOkButton(new OnDialogButtonClickListener<MessageDialog>() {
            @Override
            public boolean onClick(MessageDialog baseDialog, View v) {
                changeCollection(albumId);

                return false;
            }
        });

    }
}
