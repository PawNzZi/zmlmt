package cn.lingyikz.soundbook.soundbook.home.activity;

import android.annotation.SuppressLint;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.animation.LinearInterpolator;
import android.widget.SeekBar;
import com.kongzue.dialogx.dialogs.BottomMenu;
import com.kongzue.dialogx.dialogs.PopTip;
import com.kongzue.dialogx.interfaces.OnMenuItemClickListener;
import com.liys.onclickme.LOnClickMe;
import com.liys.onclickme_annotations.AClick;

import java.io.File;
import java.io.IOException;

import cn.hutool.core.util.ObjectUtil;
import cn.lingyikz.soundbook.soundbook.R;
import cn.lingyikz.soundbook.soundbook.api.RequestService;
import cn.lingyikz.soundbook.soundbook.base.BaseObsever;
import cn.lingyikz.soundbook.soundbook.databinding.ActivityPalyaduioBinding;
import cn.lingyikz.soundbook.soundbook.main.BaseActivity;

import cn.lingyikz.soundbook.soundbook.modle.v2.BaseModel;
import cn.lingyikz.soundbook.soundbook.modle.v2.PlayHistory;
import cn.lingyikz.soundbook.soundbook.modle.v2.Sound;
import cn.lingyikz.soundbook.soundbook.service.AudioService;
import cn.lingyikz.soundbook.soundbook.utils.Constans;
import cn.lingyikz.soundbook.soundbook.utils.DataBaseHelper;
import cn.lingyikz.soundbook.soundbook.utils.SharedPreferences;
import cn.lingyikz.soundbook.soundbook.utils.SuperMediaPlayer;
import rx.Observable;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;

public class PlayAudioActivity extends BaseActivity implements SeekBar.OnSeekBarChangeListener {

    private static ActivityPalyaduioBinding binding ;
    private static final int UPDATE_PROGRESS = 0;
    private static final int CHANGE_SECONDE = 15 ;
    private static final int UPDATE_CURRENT_POSITION = 1;

    private Bundle bundle ;
    private Animation animation ;
    private SuperMediaPlayer superMediaPlayer = SuperMediaPlayer.getInstance();
    private DataBaseHelper dataBaseHelper = null ;


    @Override
    protected void setData() {
        initData();

    }

    @Override
    protected void setView() {
        animation = AnimationUtils.loadAnimation(this,R.anim.cover_roate);
        animation.setRepeatMode(Animation.RESTART);
        animation.setInterpolator(new LinearInterpolator());
        animation.setRepeatCount(-1);

        binding.spinKit.setVisibility(View.VISIBLE);
//        WaitDialog.show("");
        binding.titleBar.goSearch.setVisibility(View.GONE);
        binding.titleBar.goPlay.setVisibility(View.GONE);
        binding.titleBar.titleSpinKit.setVisibility(View.GONE);
        binding.titleBar.goBacK.setVisibility(View.VISIBLE);
        binding.titleBar.setBlock.setVisibility(View.VISIBLE);
        bundle = getIntent().getExtras();
//        Log.i("TAG",bundle+"");
        binding.titleBar.title.setText(bundle.getString("title"));
        binding.seekbar.setOnSeekBarChangeListener(this);
    }

    @Override
    protected View setLayout() {
        binding = ActivityPalyaduioBinding.inflate(getLayoutInflater());
        LOnClickMe.init(this,binding.getRoot());
        return binding.getRoot();
    }

    /**
     * 初始化数据
     */
    @SuppressLint("SetTextI18n")
    private void initData() {
        superMediaPlayer.setOnPreparedListener(onPreparedListener);
        superMediaPlayer.setOnSeekCompleteListener(onSeekCompleteListener);
        superMediaPlayer.setOnCompletionListener(onCompletionListener);
        superMediaPlayer.setOnErrorListener(onErrorListener);
        dataBaseHelper = DataBaseHelper.getInstance(this);

        //查询是否有定时关闭
        Bundle blockBundle = SharedPreferences.getBolckClose(this);
        if(blockBundle.getInt("index") == -1){
            binding.bolckIime.setVisibility(View.GONE);
        }else {
            binding.bolckIime.setVisibility(View.VISIBLE);
            binding.bolckIime.setText(blockBundle.getString("lable")+" 后关闭");
        }

        if(ObjectUtil.isNotNull(Constans.user)){
            getPlayHistory();
        }else {
            noLoginPlay();
        }

    }
    //查询当前音频是否有播放记录
    private void getPlayHistory(){
        Observable<PlayHistory> observable  = RequestService.getInstance().getApi()
                .getPlayHistory(Constans.user.getId(),bundle.getLong("albumId"),bundle.getLong("audioId"));
        observable.subscribeOn(Schedulers.io()) // 在子线程中进行Http访问
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new BaseObsever<PlayHistory>() {
                    @Override
                    public void onNext(PlayHistory baseModel) {
                        if(baseModel.getCode() == 200 && ObjectUtil.isNotNull(baseModel.getData())){
                            loginPlay(baseModel.getData());
                        }else {
                            noLoginPlay();
                        }

                    }
                });
        observable.unsubscribeOn(Schedulers.io());
    }
    //修改或创建播放记录
    private void changePlayHistory(Long albumId,Long soundId,Long playPiont){
        Observable<BaseModel> observable  = RequestService.getInstance().getApi()
                .changePlayHistory(Constans.user.getId(),albumId
                        ,soundId,playPiont);
        observable.subscribeOn(Schedulers.io()) // 在子线程中进行Http访问
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new BaseObsever<BaseModel>() {
                    @Override
                    public void onNext(BaseModel baseModel) {

                    }
                });
        observable.unsubscribeOn(Schedulers.io());
    }
    private void noLoginPlay(){
        SuperMediaPlayer.error = 0 ;
        Bundle currentPlayHistoryInfo = SharedPreferences.currentPlayHistoryInfo(this);
        if(superMediaPlayer.isPlaying()){
            if(currentPlayHistoryInfo.getString("src").equals(bundle.getString("src"))){
                binding.startPlay.setImageDrawable(getResources().getDrawable(R.mipmap.activity_start, null));
//                WaitDialog.dismiss();
                binding.spinKit.setVisibility(View.GONE);
                binding.bookThumb.startAnimation(animation);
            }else {
                superMediaPlayer.stop();
                if(ObjectUtil.isNotNull(Constans.user) && currentPlayHistoryInfo.getLong("audioId") != 0){
                    changePlayHistory(currentPlayHistoryInfo.getLong("albumId"),currentPlayHistoryInfo.getLong("audioId"),(long) superMediaPlayer.getCurrentPosition());
                }
                superMediaPlayer.reset();
                onRead(bundle.getString("src"));
            }
        }else{
            superMediaPlayer.stop();
            superMediaPlayer.reset();
            onRead(bundle.getString("src"));
        }
//        binding.spinKit.setVisibility(View.GONE);
        SharedPreferences.saveCurrentPlayHistoryInfo(this,bundle);
    }

    private void loginPlay(PlayHistory.DataDTO data){
        SuperMediaPlayer.error = 0 ;
        Bundle currentPlayHistoryInfo = SharedPreferences.currentPlayHistoryInfo(this);
        if(superMediaPlayer.isPlaying()){
            if(currentPlayHistoryInfo.getString("src").equals(bundle.getString("src"))){
                binding.startPlay.setImageDrawable(getResources().getDrawable(R.mipmap.activity_start, null));
                binding.spinKit.setVisibility(View.GONE);
                binding.bookThumb.startAnimation(animation);
//                WaitDialog.dismiss();
            }else{
                superMediaPlayer.stop();
                if(ObjectUtil.isNotNull(Constans.user) && currentPlayHistoryInfo.getLong("audioId") != 0){
                    changePlayHistory(currentPlayHistoryInfo.getLong("albumId"),currentPlayHistoryInfo.getLong("audioId"),(long) superMediaPlayer.getCurrentPosition());
                }
                superMediaPlayer.reset();
                String soundPath = dataBaseHelper.queryDownLoadRecorde(data.getZmlmSound().getUrl(),data.getAlbumId()
                ,data.getSoundId(),Constans.user.getId()) ;
                if(ObjectUtil.isNotNull(soundPath) && new File(soundPath).exists()){

                }else {
                    soundPath = data.getZmlmSound().getUrl();
                }
                if(data.getPlayPiont() > 0 ){
                    onSeekToRead(soundPath,data.getPlayPiont());
                }else{
                    onRead(soundPath);
                }
            }

        }else{
            superMediaPlayer.stop();
            superMediaPlayer.reset();
            String soundPath = dataBaseHelper.queryDownLoadRecorde(data.getZmlmSound().getUrl(),data.getAlbumId()
                    ,data.getSoundId(),Constans.user.getId()) ;
            if(ObjectUtil.isNotNull(soundPath) && new File(soundPath).exists()){

            }else {
                soundPath = data.getZmlmSound().getUrl();
            }
            if(data.getPlayPiont() > 0 ){
                onSeekToRead(soundPath,data.getPlayPiont());
            }else{
                onRead(soundPath);
            }

        }
//        binding.spinKit.setVisibility(View.GONE);
        SharedPreferences.saveCurrentPlayHistoryInfo(this,bundle);
    }

    private void onRead(String url){
        try {
            superMediaPlayer.setDataSource(url);
            superMediaPlayer.prepareAsync();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    private void onSeekToRead(String url,long duration){

        try {
            superMediaPlayer.setDataSource(url);
            superMediaPlayer.prepare();
            superMediaPlayer.seekTo((int) duration);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private final SuperMediaPlayer.OnPreparedListener onPreparedListener = new MediaPlayer.OnPreparedListener() {
        @Override
        public void onPrepared(MediaPlayer mediaPlayer) {
            if(binding != null){
                binding.startPlay.setImageDrawable(getResources().getDrawable(R.mipmap.activity_start, null));
                binding.spinKit.setVisibility(View.GONE);
//                WaitDialog.dismiss();
                binding.bookThumb.startAnimation(animation);
            }
            mediaPlayer.start();
        }
    };
    private final SuperMediaPlayer.OnSeekCompleteListener onSeekCompleteListener = new MediaPlayer.OnSeekCompleteListener() {
        @Override
        public void onSeekComplete(MediaPlayer mediaPlayer) {
            if(binding != null){
                binding.startPlay.setImageDrawable(getResources().getDrawable(R.mipmap.activity_start, null));
                binding.spinKit.setVisibility(View.GONE);
                binding.bookThumb.startAnimation(animation);
//                WaitDialog.dismiss();
            }

            mediaPlayer.start();
        }
    };
    private final SuperMediaPlayer.OnErrorListener onErrorListener = (mediaPlayer, i, i1) -> {
        mediaPlayer.stop();
        mediaPlayer.reset();
        SuperMediaPlayer.error = 1;
//        Toast.makeText(PlayAudioActivity.this, "加载失败,重新加载中", Toast.LENGTH_LONG).show();
        PopTip.show(R.mipmap.fail_tip,"加载失败,重新加载中").showShort().setAutoTintIconInLightOrDarkMode(false);
        return false;
    };
    private final SuperMediaPlayer.OnCompletionListener onCompletionListener = new MediaPlayer.OnCompletionListener() {
        @Override
        public void onCompletion(MediaPlayer mediaPlayer) {
            if(SuperMediaPlayer.error == 1){
                if(ObjectUtil.isNotNull(Constans.user)){
                    getPlayHistory();
                }else {
                    noLoginPlay();
                }
            }else {
                if(binding != null){
                    binding.startPlay.setImageDrawable(getResources().getDrawable(R.mipmap.activity_pause, null));
                    binding.bookThumb.clearAnimation();
                }

                Observable<Sound> observable  = RequestService.getInstance().getApi().getNextPlay(bundle.getLong("albumId"),bundle.getInt("episodes") + 1);
                observable.subscribeOn(Schedulers.io()) // 在子线程中进行Http访问
                        .observeOn(AndroidSchedulers.mainThread()) // UI线程处理返回接口
                        .subscribe(new BaseObsever<Sound>() { // 订阅
                            @Override
                            public void onNext(Sound sound) {
//                            Log.i("TAG", xmlyNextPaly.toString() + "");
                                if(sound.getCode() == 200 && sound.getData() != null && SuperMediaPlayer.error == 0) {
//                                Log.i("TAG", xmlyNextPaly.toString() + "");
                                    Sound.DataDTO dataDTO = sound.getData();
                                    if(binding != null){
                                        binding.titleBar.title.setText(dataDTO.getName());
                                    }
                                    Bundle reslutBundle = new Bundle();
                                    reslutBundle.putLong("albumId",dataDTO.getAlbumId());
                                    reslutBundle.putInt("episodes",dataDTO.getEpisodes());
                                    reslutBundle.putString("title",dataDTO.getName());
                                    reslutBundle.putString("audioDes","");
                                    reslutBundle.putString("audioCreated", dataDTO.getCreateTime());
                                    reslutBundle.putString("src",dataDTO.getUrl());
                                    reslutBundle.putLong("audioId",dataDTO.getId());
                                    superMediaPlayer.stop();
                                    if(ObjectUtil.isNotNull(Constans.user)){
                                        changePlayHistory(bundle.getLong("albumId"),bundle.getLong("audioId"),(long) superMediaPlayer.getCurrentPosition());
                                    }
                                    superMediaPlayer.reset();
                                    onRead(dataDTO.getUrl());
                                    bundle = reslutBundle;
                                    SharedPreferences.saveCurrentPlayHistoryInfo(PlayAudioActivity.this,reslutBundle);

                                }else if(sound.getCode() == 200 && sound == null){
                                    superMediaPlayer.stop();
                                    if(ObjectUtil.isNotNull(Constans.user)){
                                        changePlayHistory(bundle.getLong("albumId"),bundle.getLong("audioId"),(long) superMediaPlayer.getCurrentPosition());
                                    }
                                    superMediaPlayer.reset();

                                }
                            }
                        });
            }

        }
    };

    //使用handler定时更新进度条
    @SuppressLint("HandlerLeak")
    private Handler handler = new Handler(Looper.myLooper()) {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case UPDATE_PROGRESS:
                    updateProgress();
                    break;
            }
        }
    };
    //更新进度条
    private void updateProgress() {
        if(superMediaPlayer.isPlaying()){
            //设置进度条的进度
            binding.seekbar.setProgress((int) superMediaPlayer.getCurrentPosition());
            binding.seekbar.setMax(superMediaPlayer.getDuration());
            binding.totalTime.setText(get(superMediaPlayer.getDuration() / 1000));
        }
//        Log.i("TAG","currenPostion:"+superMediaPlayer.getCurrentPosition());
//        binding.seekbar.setProgress(superMediaPlayer.getCurrentPosition());
        //使用Handler每500毫秒更新一次进度条

        handler.sendEmptyMessageDelayed(UPDATE_PROGRESS, 500);
    }
    @SuppressLint("HandlerLeak")
    private Handler updateCurrentPositionHandler = new Handler(Looper.myLooper()) {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case UPDATE_CURRENT_POSITION:
                    updateCurrentPosition();
                    break;
            }
        }
    };
    private void updateCurrentPosition(){
        int bock = superMediaPlayer.getCurrentPosition();

        binding.currentTime.setText( get(bock / 1000));
        updateCurrentPositionHandler.sendEmptyMessageDelayed(UPDATE_CURRENT_POSITION,1000);

    }
    public String get(int bock){
        String mm ;
        String ss ;
        int s = bock % 60 ;
        int m = bock / 60 ;
        if(s < 10){
            ss = "0"+String.valueOf(s);
        }else{
            ss = String.valueOf(s);
        }
        if(m < 10){
            mm = "0"+String.valueOf(m);
        }else {
            mm = String.valueOf(m);
        }
        return mm+":"+ss;

    }
    @Override
    protected void onResume() {
        super.onResume();
        //进入到界面后开始更新进度条
//        Log.i("TAG","onResume");
        handler.sendEmptyMessage(UPDATE_PROGRESS);
        updateCurrentPositionHandler.sendEmptyMessage(UPDATE_CURRENT_POSITION);

    }
    @Override
    protected void onStop() {
        super.onStop();
        //停止更新进度条的进度
        handler.removeCallbacksAndMessages(null);
        updateCurrentPositionHandler.removeCallbacksAndMessages(null);
    }
    @Override
    protected void onDestroy() {
        super.onDestroy();
        binding = null ;
    }

    @Override
    public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
        if (b){
            superMediaPlayer.seekTo(i);
        }
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {

    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {

    }

    @AClick({R.id.go_bacK,  R.id.startPlay,R.id.kuaituiClick,R.id.kuaijinClick,R.id.set_block})
    public void click(View view) {
        switch (view.getId()) {
            case R.id.go_bacK:
                finish();
                overridePendingTransition(android.R.anim.slide_in_left, android.R.anim.slide_out_right);
                break;
            case R.id.startPlay:
                if(superMediaPlayer.isPlaying()){
                    superMediaPlayer.pause();
                    if(ObjectUtil.isNotNull(Constans.user)){
                        changePlayHistory(bundle.getLong("albumId"),bundle.getLong("audioId"),(long) superMediaPlayer.getCurrentPosition());
                    }
                    SharedPreferences.saveCurrentPlayHistoryInfo(this,bundle);
                    binding.startPlay.setImageDrawable(getResources().getDrawable(R.mipmap.activity_pause, null));
                    binding.bookThumb.clearAnimation();
                }else {
                    binding.startPlay.setImageDrawable(getResources().getDrawable(R.mipmap.activity_start, null));
                    superMediaPlayer.start();
                    binding.bookThumb.startAnimation(animation);
                }
                break;
            case R.id.kuaituiClick:
                Log.i("TAG",superMediaPlayer.getCurrentPosition()+"");
                superMediaPlayer.seekTo(superMediaPlayer.getCurrentPosition() - CHANGE_SECONDE * 1000);
                break;
            case R.id.kuaijinClick:
                superMediaPlayer.seekTo(superMediaPlayer.getCurrentPosition() + CHANGE_SECONDE * 1000);
                break;
            case R.id.set_block:
                BottomMenu.show(new String[]{"30分钟", "60分钟", "90分钟","120分钟","150分钟","取消定时"})
                        .setMessage("定时关闭")
                        .setOnMenuItemClickListener(new OnMenuItemClickListener<BottomMenu>() {
                            @SuppressLint("SetTextI18n")
                            @Override
                            public boolean onClick(BottomMenu dialog, CharSequence text, int index) {
//                                Toast.makeText(PlayAudioActivity.this, text.toString()+index, Toast.LENGTH_SHORT).show();
                                if(index == 5 ){
                                    Bundle spBundle = new Bundle();
                                    spBundle.putString("lable","");
                                    spBundle.putInt("index",-1);
                                    SharedPreferences.saveBolckClose(PlayAudioActivity.this,spBundle);
                                    binding.bolckIime.setVisibility(View.GONE);
                                    binding.bolckIime.setText("");
                                }else{
                                    binding.bolckIime.setVisibility(View.VISIBLE);
                                    binding.bolckIime.setText(text.toString()+" 后关闭");
                                    Bundle spbundle = new Bundle();
                                    spbundle.putString("lable",text.toString());
                                    spbundle.putInt("index",index);
                                    SharedPreferences.saveBolckClose(PlayAudioActivity.this,spbundle);
                                    Intent intent = new Intent(PlayAudioActivity.this, AudioService.class);
                                    intent.setAction(Constans.SET_BLOCK);
                                    intent.putExtra("index",index);
                                    intent.putExtras(bundle);
                                    PlayAudioActivity.this.startService(intent);
                                }


                                return false;
                            }
                        });
                break;
            default:
                break;
        }
    }

    public static class PlaystateReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            if(intent.getAction().equals(Constans.CHANGE_PLAY_IMG)){
//                Log.i("TAG","onReceive");
                if(binding != null){
                    binding.startPlay.setImageDrawable(context.getResources().getDrawable(R.mipmap.activity_pause, null));
                    binding.bookThumb.clearAnimation();
                    binding.bolckIime.setText("");
                    binding.bolckIime.setVisibility(View.GONE);
                }
            }
        }
    }

}
