package cn.lingyikz.soundbook.soundbook.utils;

import android.annotation.SuppressLint;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.Bundle;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import androidx.annotation.Nullable;
import cn.lingyikz.soundbook.soundbook.modle.v2.Album;
import cn.lingyikz.soundbook.soundbook.modle.AlbumDetail;
import cn.lingyikz.soundbook.soundbook.modle.v2.AlbumSound;

public class DataBaseHelper extends SQLiteOpenHelper {

    public final static int DB_VERSION = 1;
    public final static String DB_NAME = "audio";
    private volatile static DataBaseHelper dataBaseHelper ;
    private SQLiteDatabase db = null ;

    private DataBaseHelper(@Nullable Context context) {
        super(context, DB_NAME, null, DB_VERSION);
//        Log.i("TAG","DataBaseHelper");
    }
    public static DataBaseHelper getInstance(Context context) {
        if(dataBaseHelper == null){
            synchronized (DataBaseHelper.class){
                if(dataBaseHelper == null){
                    dataBaseHelper = new DataBaseHelper(context);
                }
            }
        }
        return dataBaseHelper;
    }
    @Override
    public void onCreate(SQLiteDatabase sqLiteDatabase) {
//        Log.i("TAG","SQLonCreate");
//        sqLiteDatabase.execSQL("CREATE TABLE collection(_id INTEGER PRIMARY KEY AUTOINCREMENT, albumId INTEGER, albumName TEXT,albumDes TEXT,albumThumb TEXT," +
//                "uuid TEXT);");
//        sqLiteDatabase.execSQL("CREATE TABLE playhistory(_id INTEGER PRIMARY KEY AUTOINCREMENT, albumId INTEGER, albumName TEXT,episodes INTEGER,audioTitle TEXT,audioDes TEXT," +
//                "audioCreated TEXT,audioDuration TEXT,audioSrc TEXT ,audioId INTEGER,uuid TEXT);");
        sqLiteDatabase.execSQL("CREATE TABLE downloadrecorde(_id INTEGER PRIMARY KEY AUTOINCREMENT,albumId LONG,soundId LONG,userId LONG,soundUrl TEXT,soundPath TEXT)");
    }

    @Override
    public void onUpgrade(SQLiteDatabase sqLiteDatabase, int i, int i1) {

//        Log.i("TAG","onUpgrade");
    }

    public SQLiteDatabase getReadLink(){
        if(db == null || !db.isOpen()){
            db = dataBaseHelper.getReadableDatabase();
        }
        return db ;
    }
    public SQLiteDatabase getWriteLink(){
        if(db == null || !db.isOpen()){
            db = dataBaseHelper.getWritableDatabase();
        }
        return db ;
    }
    public void close(){
        db.close();
    }

    /**
     * 添加下载记录
     * @param itemInfo
     * @param path
     */
    public void addDownLoadRecorde(AlbumSound.DataDTO.RowsDTO itemInfo,String path){
        db = getReadLink();
        Cursor cursor = db.rawQuery("select * from downloadrecorde where soundUrl = ? ",new String[]{itemInfo.getUrl()});
        if(cursor.getCount() == 0){
            //添加记录
            db = getWriteLink();
            ContentValues contentValues = new ContentValues();
            contentValues.put("albumId",itemInfo.getAlbumId());
            contentValues.put("soundId",itemInfo.getId());
            contentValues.put("userId",Constans.user.getId());
            contentValues.put("soundUrl",itemInfo.getUrl());
            contentValues.put("soundPath",path);
            db.insert("downloadrecorde",null,contentValues);
        }
        cursor.close();
        close();
    }

    @SuppressLint("Range")
    public String queryDownLoadRecorde(AlbumSound.DataDTO.RowsDTO itemInfo){
        String result = null ;
        db = getReadLink();
        Cursor cursor = db.rawQuery("select * from downloadrecorde where soundUrl = ? and albumId = ? and  soundId = ? and userId = ? "
                ,new String[]{itemInfo.getUrl(),String.valueOf(itemInfo.getAlbumId()),String.valueOf(itemInfo.getId()),String.valueOf(Constans.user.getId())});
        if(cursor.getCount() > 0){
            if(cursor.moveToFirst()){
                 result = cursor.getString(cursor.getColumnIndex("soundPath"));
            }
        }
//        Log.i("TAG","result:"+result);
        return result;
    }

    @SuppressLint("Range")
    public String queryDownLoadRecorde(String url,Long albumId,Long soundId,Long userId){
        String result = null ;
        db = getReadLink();
        Cursor cursor = db.rawQuery("select * from downloadrecorde where soundUrl = ? and albumId = ? and  soundId = ? and userId = ? "
                ,new String[]{url,String.valueOf(albumId),String.valueOf(soundId),String.valueOf(userId)});
        if(cursor.getCount() > 0){
            if(cursor.moveToFirst()){
                result = cursor.getString(cursor.getColumnIndex("soundPath"));
            }
        }
//        Log.i("TAG","result:"+result);
        return result;
    }
    /**
     * 查询改专辑是否被收藏
     * @param albumId
     * @return
     */
    public int queryCollectionCount(Long albumId){
        db = getReadLink();
        Cursor cursor = db.rawQuery("select * from collection where albumId = ? ",new String[]{String.valueOf(albumId)});
        int count = cursor.getCount();
        cursor.close();
        return count;
    }

    /**
     * 删除收藏历史
     * @param albumId
     */
    public void cancleCollection(Long albumId){
        db = getWriteLink();
        String sql = "delete from collection where albumId = "+ albumId;
        db.execSQL(sql);

    }

    /**
     * 插入收藏历史
     * @param albumDetail
     */
    public void addCollection(Album.DataDTO.RowsDTO albumDetail){
        db = getWriteLink();
        ContentValues contentValues = new ContentValues();
        contentValues.put("albumId",albumDetail.getId());
        contentValues.put("albumName",albumDetail.getName());
        contentValues.put("albumDes",albumDetail.getDescription());
        contentValues.put("albumThumb",albumDetail.getThumb().toString());

        db.insert("collection",null,contentValues);

    }

    /**
     * 查询收藏历史列表
     * @return
     */
    @SuppressLint("Range")
    public List<Album.DataDTO.RowsDTO> queryCollectionAll(){
        db = getReadLink();
        List<Album.DataDTO.RowsDTO> mList = new ArrayList<>();
        Cursor cursor = db.rawQuery("select * from collection ",new String[]{});
//        Log.i("TAG","有:"+cursor.getCount());
        if(cursor.moveToFirst()){
            for (int i = 0; i < cursor.getCount(); i++) {
                Album.DataDTO.RowsDTO listDTO = new Album.DataDTO.RowsDTO();
                listDTO.setId(cursor.getLong(cursor.getColumnIndex("albumId")));
                listDTO.setName(cursor.getString(cursor.getColumnIndex("albumName")));
                listDTO.setDescription(cursor.getString(cursor.getColumnIndex("albumDes")));
                listDTO.setThumb(cursor.getString(cursor.getColumnIndex("albumThumb")));
                mList.add(listDTO);
                cursor.moveToNext();
            }
        }
        cursor.close();
        return mList ;
    }

    /**
     * 插入或修改播放历史
     * @param bundle
     */
    public void addPlayHistory(Bundle bundle){
//        Log.i("TAG","日期:"+map.get("audioCreated").toString());
        db = getWriteLink();
        String querySql = "select * from playhistory where albumId = ? ";
        Cursor cursor = db.rawQuery(querySql,new String[]{String.valueOf(bundle.getLong("albumId"))});
        int count = cursor.getCount();
//        Log.i("TAG","COUNT:"+cursor.getCount());
        if(count == 0){
            //
            ContentValues contentValues = new ContentValues();
            contentValues.put("albumId",bundle.getLong("albumId"));
            contentValues.put("episodes",bundle.getLong("episodes"));
            contentValues.put("audioTitle",bundle.getString("title"));
            contentValues.put("audioDes",bundle.getString("audioDes"));
            contentValues.put("audioDuration",bundle.getString("audioDuration"));
            contentValues.put("audioCreated",bundle.getString("audioCreated"));
            contentValues.put("audioSrc",bundle.getString("src"));
            contentValues.put("audioId",bundle.getInt("audioId"));

            db.insert("playhistory",null,contentValues);

        }
//        else if(count == 1){
//            //
//
//            if(cursor.moveToFirst()){
//                int _id = cursor.getInt(cursor.getColumnIndex("_id"));
//                String updateSql = "update playhistory set audioDuration = '"+ String.valueOf(map.get("audioDuration")) +"' where _id = "+_id;
//                db.execSQL(updateSql);
//            }
//        }
        else if(count > 0){
            String deleteSql = "delete from playhistory where albumId = "+ bundle.getLong("albumId");
            db.execSQL(deleteSql);
            ContentValues contentValues = new ContentValues();
            contentValues.put("albumId",bundle.getLong("albumId"));
            contentValues.put("episodes",bundle.getInt("episodes"));
            contentValues.put("audioTitle",bundle.getString("title"));
            contentValues.put("audioDes",bundle.getString("audioDes"));
            contentValues.put("audioDuration",bundle.getString("audioDuration"));
            contentValues.put("audioCreated",bundle.getString("audioCreated"));
            contentValues.put("audioSrc",bundle.getString("src"));
            contentValues.put("audioId",bundle.getLong("audioId"));

            db.insert("playhistory",null,contentValues);
        }
        cursor.close();

    }
    public void deletePlayHistory(Long albumId){
        db = getWriteLink();
        String deleteSql = "delete from playhistory where albumId = "+ albumId;
        db.execSQL(deleteSql);

//        return queryPlayHistoryAll();
    }
    /**
     * 查询播放历史列表
     * @return
     */
    @SuppressLint("Range")
    public List<AlbumSound.DataDTO.RowsDTO> queryPlayHistoryAll(){
        db = getReadLink();
        List<AlbumSound.DataDTO.RowsDTO> mList = new ArrayList<>();
        Cursor cursor = db.rawQuery("select * from playhistory order by audioCreated desc",new String[]{});
//        Log.i("TAG","有:"+cursor.getCount());
        if(cursor.moveToFirst()){
            for (int i = 0; i < cursor.getCount(); i++) {

//                Log.i("TAG",i+"");
                AlbumSound.DataDTO.RowsDTO listDTO = new AlbumSound.DataDTO.RowsDTO();
                listDTO.setId(cursor.getLong(cursor.getColumnIndex("audioId")));
                listDTO.setName(cursor.getString(cursor.getColumnIndex("audioTitle")));
                listDTO.setDescription(cursor.getString(cursor.getColumnIndex("audioDes")));
                listDTO.setAlbumId(cursor.getLong(cursor.getColumnIndex("albumId")));
                listDTO.setEpisodes(cursor.getInt(cursor.getColumnIndex("episodes")));
                listDTO.setCreateTime(cursor.getString(cursor.getColumnIndex("audioCreated")));
                listDTO.setUrl(cursor.getString(cursor.getColumnIndex("audioSrc")));
//                listDTO.setUrl(cursor.getString(cursor.getColumnIndex("audioDuration")));
                mList.add(listDTO);
                cursor.moveToNext();
            }
        }
        cursor.close();
        return mList ;
    }

    /**
     * 查询当前播放进度
     * @param albumId
     * @param audioId
     * @return
     */
    @SuppressLint("Range")
    public Bundle queryPlayHistory(Long albumId, Long audioId){
        db = getReadLink();
        Bundle bundle = new Bundle();
        String querySql = "select * from playhistory where albumId = ? and audioId = ?";
        Cursor cursor = db.rawQuery(querySql,new String[]{String.valueOf(albumId),String.valueOf(audioId)});
//        Log.i("TAG","bofanglishi:"+cursor.getCount());
        if(cursor.moveToFirst()){
            for (int i = 0; i < cursor.getCount(); i++) {
                bundle.putLong("albumId",cursor.getLong(cursor.getColumnIndex("albumId")));
                bundle.putLong("audioId",cursor.getLong(cursor.getColumnIndex("audioId")));
                bundle.putInt("episodes",cursor.getInt(cursor.getColumnIndex("episodes")));
                bundle.putString("title",cursor.getString(cursor.getColumnIndex("audioTitle")));
                bundle.putString("audioDes",cursor.getString(cursor.getColumnIndex("audioDes")));
                bundle.putString("src",cursor.getString(cursor.getColumnIndex("audioSrc")));
                bundle.putString("audioCreated",cursor.getString(cursor.getColumnIndex("audioCreated")));
                bundle.putString("audioDuration",cursor.getString(cursor.getColumnIndex("audioDuration")));
                cursor.moveToNext();
            }
        }
        cursor.close();
        return bundle;
    }
    @SuppressLint("Range")
    public Bundle queryPlayHistoryRecent(){
        Bundle bundle = new Bundle() ;
        db = getReadLink();
        String querySql = "select max(audioCreated),* from playhistory ";
        Cursor cursor = db.rawQuery(querySql,new String[]{});
        Log.i("TAG","COUNT:"+cursor.getCount());
        if(cursor.getCount() != 0){
            if(cursor.moveToFirst()){
                for (int i = 0; i < cursor.getCount(); i++) {
                    bundle.putInt("albumId",cursor.getInt(cursor.getColumnIndex("albumId")));
                    bundle.putInt("audioId",cursor.getInt(cursor.getColumnIndex("audioId")));
                    bundle.putInt("episodes",cursor.getInt(cursor.getColumnIndex("episodes")));
                    bundle.putString("title",cursor.getString(cursor.getColumnIndex("audioTitle")));
                    bundle.putString("audioDes",cursor.getString(cursor.getColumnIndex("audioDes")));
                    bundle.putString("src",cursor.getString(cursor.getColumnIndex("audioSrc")));
                    Log.i("TAG","COUNT:"+cursor.getString(cursor.getColumnIndex("audioSrc")));
                    bundle.putString("audioCreated",cursor.getString(cursor.getColumnIndex("audioCreated")));
                    bundle.putString("audioDuration",cursor.getString(cursor.getColumnIndex("audioDuration")));
                    cursor.moveToNext();
                }
            }
        }
         cursor.close();
         return bundle;
    }

}
